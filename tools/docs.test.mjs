import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, readFileSync, writeFileSync, rmSync, readdirSync, copyFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { ROOT, validateReviewed, translatedSource, translationHashes } from './docs.mjs';

function fixture(t) {
  const root = mkdtempSync(join(tmpdir(), 'signature-docs-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  for (const directory of ['docs', 'sites/astro/public', 'protocol/src/main', 'signature/src/main', 'examples/integration/src/main']) {
    function copy(source, target) {
      mkdirSync(target, { recursive: true });
      for (const entry of readdirSync(source, { withFileTypes: true })) {
        if (entry.isDirectory()) copy(join(source, entry.name), join(target, entry.name));
        else copyFileSync(join(source, entry.name), join(target, entry.name));
      }
    }
    copy(join(ROOT, directory), join(root, directory));
  }
  copyFileSync(join(ROOT, 'IMPORT.md'), join(root, 'IMPORT.md'));
  return root;
}

test('reviewed repository source and translations satisfy acceptance contracts', () => validateReviewed());
test('fresh Spanish preserves the reviewed page', () => {
  assert.equal(translatedSource(ROOT, 'es', 'api', translationHashes()).fallback, false);
});
test('stale Spanish uses English from the same Development source', () => {
  const selected = translatedSource(ROOT, 'es', 'api', { 'api.md': 'stale' });
  assert.equal(selected.path, join(ROOT, 'docs/agents/api.md'));
  assert.equal(selected.fallback, true);
});
test('changed runtime source is rejected before any renderer runs', t => {
  const root = fixture(t), path = join(root, 'protocol/src/main/kotlin/io/apexfission/signature/protocol/Statement.kt');
  writeFileSync(path, readFileSync(path, 'utf8') + '\n// changed source\n');
  assert.throws(() => validateReviewed(root), /Source contract changed/);
});
test('stale translation review is rejected before rendering', t => {
  const root = fixture(t), path = join(root, 'docs/es/translations.json');
  writeFileSync(path, '{}\n');
  assert.throws(() => validateReviewed(root), /Spanish translations stale/);
});
test('translated code changes are rejected before rendering', t => {
  const root = fixture(t), path = join(root, 'docs/es/api.md');
  writeFileSync(path, readFileSync(path, 'utf8') + '\n```kotlin\nwrong()\n```\n');
  assert.throws(() => validateReviewed(root), /Code fences differ: api/);
});
