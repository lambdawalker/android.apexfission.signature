#!/usr/bin/env python3
"""Generate Development docs. Source manifests require intentional review/acceptance."""
import hashlib, json, os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'sites/dist'
GUIDES = ['index','installation','quickstart','concepts','api','recipes','troubleshooting','migration','limitations','testing']
def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def sources():
    paths = sorted(p for root in ['protocol/src/main','signature/src/main','examples/integration/src/main'] for p in (ROOT/root).rglob('*') if p.is_file())
    return {str(p.relative_to(ROOT)):digest(p) for p in paths}
def translations(): return {**{name+'.md':digest(ROOT/'docs/agents'/f'{name}.md') for name in GUIDES if name != 'installation'}, 'IMPORT.md':digest(ROOT/'IMPORT.md'), 'releases.md':digest(ROOT/'docs/releases.md')}
def write_json(path, data): path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
def translated_source(locale, name, manifest):
    canonical = ROOT/'IMPORT.md' if name == 'installation' else ROOT/'docs/agents'/f'{name}.md'
    translated = ROOT/'docs/es/IMPORT.md' if name == 'installation' else ROOT/'docs/es'/f'{name}.md'
    fresh = translated.is_file() and manifest.get('IMPORT.md' if name == 'installation' else f'{name}.md') == digest(canonical)
    return (translated, False) if locale == 'es' and fresh else (canonical, locale == 'es')

def cli(*args):
    executable = os.environ.get('ANDROID_REPO_DOCS_BIN')
    command = [executable] if executable else (['cmd', '/c', str(ROOT/'docs.bat')] if os.name == 'nt' else [str(ROOT/'docs.sh')])
    subprocess.run(command + list(args), cwd=ROOT, check=True)

def build():
    # Rendering, Markdown rewriting, localization and immutable provenance belong to the shared CLI.
    cli('build', '--base', os.environ.get('ANDROID_REPO_DOCS_BASE', '/'), '--check')
    validate_output()

def validate_output():
    scopes = json.loads((OUT/'versions.json').read_text())
    head = subprocess.check_output(['git','rev-parse','HEAD'], cwd=ROOT, text=True).strip()
    if len(scopes) != 2 or {(s['language'],s['module'],s['version']) for s in scopes} != {('en','signature','development'),('es','signature','development')}:
        raise ValueError('Expected only bilingual Signature Development scopes')
    for scope in scopes:
        if scope['source'] != head or scope['documentation_ref'] != head or not scope['development']:
            raise ValueError('Development provenance must identify the actual build commit')
    for locale in ['en','es']:
        scope=OUT/locale/'signature/development'
        for name in GUIDES:
            page=scope/('index.html' if name=='index' else name+'/index.html')
            # index.html is the canonical overview; its legacy URL is already canonical.
            if not page.is_file(): raise ValueError(f'Missing page {page}')
            href = next(s['root'] for s in scopes if s['language'] == locale) + name + '/'
            if name != 'index' and f'href="{href}"' not in (scope/f'{name}.html').read_text():
                raise ValueError('Human compatibility redirect drift')
            raw=scope/'raw'/f'{name}.md'
            if (OUT/'raw'/locale/'signature/development'/f'{name}.md').read_bytes()!=raw.read_bytes():
                raise ValueError('Raw compatibility alias drift')
            if f'Source: `{head}`' not in raw.read_text(): raise ValueError('Missing raw provenance')
            if locale == 'es' and 'La traducción está ausente o desactualizada' in raw.read_text():
                raise ValueError('Reviewed Spanish unexpectedly fell back to English')
    if 'development' not in (OUT/'index.html').read_text().lower(): raise ValueError('Root must open Development')
    if (OUT/'catalog.json').read_bytes() != (ROOT/'docs/releases.json').read_bytes(): raise ValueError('Catalog compatibility endpoint drift')
    if json.loads((OUT/'catalog.json').read_text())['confirmedReleases']:
        raise ValueError('No confirmed releases exist')

def verify():
    if (ROOT/'sites/astro/public/catalog.json').read_bytes() != (ROOT/'docs/releases.json').read_bytes(): raise ValueError('Static catalog must match the reviewed no-release boundary')
    if json.loads((ROOT/'docs/releases.json').read_text())['confirmedReleases']: raise ValueError('No confirmed releases exist')
    expected=json.loads((ROOT/'docs/source-manifest.json').read_text())
    if expected != sources(): raise ValueError('Source contract changed. Review guides, then accept-source deliberately.')
    if json.loads((ROOT/'docs/es/translations.json').read_text()) != translations(): raise ValueError('Spanish translations stale. Review translations before accept-translations; build uses explicit English fallback meanwhile.')
    for name in GUIDES:
        en=(ROOT/'IMPORT.md' if name=='installation' else ROOT/'docs/agents'/f'{name}.md').read_text();es=(ROOT/'docs/es/IMPORT.md' if name=='installation' else ROOT/'docs/es'/f'{name}.md').read_text()
        if re.findall(r'```.*?```',en,re.S) != re.findall(r'```.*?```',es,re.S): raise ValueError(f'Code fences differ: {name}')
    build()
    print('Docs verified: shared Astro output, compatibility aliases, immutable provenance, source hashes, translation/code freshness; Development only.')

if __name__=='__main__':
    command=sys.argv[1] if len(sys.argv)>1 else 'verify'
    if command=='accept-translations': write_json(ROOT/'docs/es/translations.json',translations())
    elif command=='accept-source': write_json(ROOT/'docs/source-manifest.json',sources())
    elif command=='build': build()
    elif command=='verify': verify()
    else: raise SystemExit('Usage: docs.py build|verify|accept-translations|accept-source')
