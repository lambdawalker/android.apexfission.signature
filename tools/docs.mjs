#!/usr/bin/env node
// Project acceptance contracts only; the shared CLI owns every rendered page.
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, readdirSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { execFileSync } from 'node:child_process';

export const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const GUIDES = ['index', 'installation', 'quickstart', 'concepts', 'api', 'recipes', 'troubleshooting', 'migration', 'limitations', 'testing'];
const read = path => readFileSync(path, 'utf8');
const json = path => JSON.parse(read(path));
const hash = path => createHash('sha256').update(readFileSync(path)).digest('hex');
const canonical = (root, name) => join(root, name === 'installation' ? 'IMPORT.md' : `docs/agents/${name}.md`);
const spanish = (root, name) => join(root, name === 'installation' ? 'docs/es/IMPORT.md' : `docs/es/${name}.md`);
const writeJson = (path, value) => writeFileSync(path, JSON.stringify(value, null, 2) + '\n');

export function sourceHashes(root = ROOT) {
  const result = {};
  function walk(path) {
    for (const entry of readdirSync(join(root, path), { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
      const relative = `${path}/${entry.name}`;
      if (entry.isDirectory()) walk(relative);
      else if (entry.isFile()) result[relative] = hash(join(root, relative));
    }
  }
  for (const path of ['protocol/src/main', 'signature/src/main', 'examples/integration/src/main']) walk(path);
  return Object.fromEntries(Object.entries(result).sort(([a], [b]) => a.localeCompare(b)));
}
export function translationHashes(root = ROOT) {
  return Object.fromEntries([
    ...GUIDES.filter(name => name !== 'installation').map(name => [`${name}.md`, hash(canonical(root, name))]),
    ['IMPORT.md', hash(join(root, 'IMPORT.md'))], ['releases.md', hash(join(root, 'docs/releases.md'))],
  ]);
}
export function translatedSource(root, locale, name, manifest) {
  const original = canonical(root, name), translated = spanish(root, name);
  const key = name === 'installation' ? 'IMPORT.md' : `${name}.md`;
  const fresh = existsSync(translated) && manifest[key] === hash(original);
  return locale === 'es' && fresh ? { path: translated, fallback: false } : { path: original, fallback: locale === 'es' };
}
export function validateReviewed(root = ROOT) {
  assert.equal(read(join(root, 'sites/astro/public/catalog.json')), read(join(root, 'docs/releases.json')), 'Static catalog must match the reviewed no-release boundary');
  assert.deepEqual(json(join(root, 'docs/releases.json')).confirmedReleases, [], 'No confirmed publications exist');
  assert.deepEqual(sourceHashes(root), json(join(root, 'docs/source-manifest.json')), 'Source contract changed. Review guides before accept-source.');
  assert.deepEqual(translationHashes(root), json(join(root, 'docs/es/translations.json')), 'Spanish translations stale. Review meaning before accept-translations.');
  for (const name of GUIDES) {
    assert.deepEqual(read(canonical(root, name)).match(/```[\s\S]*?```/g) ?? [], read(spanish(root, name)).match(/```[\s\S]*?```/g) ?? [], `Code fences differ: ${name}`);
  }
}
function cli(...args) {
  const windows = process.platform === 'win32';
  execFileSync(windows ? 'cmd.exe' : 'sh', [...(windows ? ['/d', '/c', join(ROOT, 'docs.bat')] : [join(ROOT, 'docs.sh')]), ...args], { cwd: ROOT, stdio: 'inherit' });
}
export function validateOutput() {
  const out = join(ROOT, 'sites/dist'), scopes = json(join(out, 'versions.json'));
  const head = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: ROOT, encoding: 'utf8' }).trim();
  assert.equal(scopes.length, 2, 'Only two bilingual Development scopes may exist');
  assert.deepEqual(scopes.map(s => [s.language, s.module, s.version]).sort(), [['en', 'signature', 'development'], ['es', 'signature', 'development']]);
  for (const scope of scopes) {
    assert.equal(scope.source, head); assert.equal(scope.documentation_ref, head); assert.equal(scope.development, true);
    const directory = join(out, scope.language, 'signature/development');
    for (const name of GUIDES) {
      assert.ok(existsSync(join(directory, name === 'index' ? 'index.html' : `${name}/index.html`)), `Missing ${scope.language} ${name}`);
      if (name !== 'index') assert.ok(read(join(directory, `${name}.html`)).includes(`href="${scope.root}${name}/"`), 'Legacy human route drift');
      const raw = read(join(directory, 'raw', `${name}.md`));
      assert.equal(read(join(out, 'raw', scope.language, 'signature/development', `${name}.md`)), raw, 'Legacy raw route drift');
      assert.ok(raw.includes(`Source: \`${head}\``), 'Missing raw commit provenance');
      if (scope.language === 'es') assert.ok(!raw.includes('La traducción está ausente o desactualizada'), 'Reviewed Spanish unexpectedly fell back');
    }
  }
  assert.ok(read(join(out, 'index.html')).toLowerCase().includes('development'));
  assert.equal(read(join(out, 'catalog.json')), read(join(ROOT, 'docs/releases.json')));
}
function main() {
  const [command = 'verify', ...args] = process.argv.slice(2);
  if (command === 'accept-source') return writeJson(join(ROOT, 'docs/source-manifest.json'), sourceHashes());
  if (command === 'accept-translations') return writeJson(join(ROOT, 'docs/es/translations.json'), translationHashes());
  if (command === 'check') return validateReviewed();
  if (!['build', 'verify'].includes(command)) throw new Error('Usage: node tools/docs.mjs check|build|verify|accept-source|accept-translations [--base /prefix/]');
  if (command === 'verify') validateReviewed();
  const base = args[0] === '--base' && args.length === 2 ? args[1] : process.env.ANDROID_REPO_DOCS_BASE || '/';
  assert.ok(args.length === 0 || args[0] === '--base' && args.length === 2, 'Expected --base /prefix/');
  cli('build', '--base', base, '--check');
  validateOutput();
  console.log('Verified shared Astro output, legacy routes, Development provenance and reviewed documentation.');
}
if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) main();
