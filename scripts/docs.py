#!/usr/bin/env python3
"""Generate Development docs. Source manifests require intentional review/acceptance."""
import hashlib, html, json, re, shutil, sys
from pathlib import Path
from urllib.parse import urlsplit, unquote
import markdown

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'sites/dist'
GUIDES = ['index','installation','quickstart','concepts','api','recipes','troubleshooting','migration','limitations','testing']
LABELS = {
'en':['Overview','Installation','Quickstart','Concepts','API','Recipes','Troubleshooting','Migration','Limitations','Testing'],
'es':['Resumen','Instalación','Inicio rápido','Conceptos','API','Recetas','Problemas','Migración','Limitaciones','Pruebas']}

def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def sources():
    paths = sorted(p for root in ['protocol/src/main','signature/src/main','examples/integration/src/main'] for p in (ROOT/root).rglob('*') if p.is_file())
    return {str(p.relative_to(ROOT)):digest(p) for p in paths}
def translations(): return {name+'.md':digest(ROOT/'docs/agents'/f'{name}.md') for name in GUIDES}
def write_json(path, data): path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
def translated_source(locale, name, manifest):
    canonical = ROOT/'docs/agents'/f'{name}.md'
    translated = ROOT/'docs/es'/f'{name}.md'
    fresh = translated.is_file() and manifest.get(f'{name}.md') == digest(canonical)
    return (translated, False) if locale == 'es' and fresh else (canonical, locale == 'es')

def build():
    if OUT.exists(): shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    catalog = json.loads((ROOT/'docs/releases.json').read_text())
    if catalog['confirmedReleases']: raise ValueError('Historical rendering must be implemented before adding confirmed releases')
    manifest = json.loads((ROOT/'docs/translations.json').read_text())
    style = '''body{margin:0;background:#f5f5f0;color:#182c31;font:17px/1.65 system-ui,sans-serif}header{background:#123e45;color:white;padding:1.3rem max(1rem,5vw)}header a{color:#b8eddf}header p{margin:.3rem 0}.layout{display:grid;grid-template-columns:230px minmax(0,850px);gap:3rem;max-width:1250px;margin:2rem auto;padding:0 1.5rem}nav a{display:block;padding:.25rem .5rem;color:#164f59}nav a[aria-current]{font-weight:700;background:#dbebe5;border-radius:5px}main{min-width:0}a{color:#086877;text-underline-offset:3px}h1,h2,h3{line-height:1.2}h1{font-size:2.5rem}pre{overflow:auto;padding:1rem;background:#e7ece8;border-radius:8px;font-size:14px}code{overflow-wrap:anywhere}table{border-collapse:collapse;width:100%;display:block;overflow-x:auto;font-size:15px}td,th{padding:.6rem;border-bottom:1px solid #b8c8c3;text-align:left}th{background:#e1ebe6}.notice{padding:1rem;border-left:4px solid #bd772b;background:#fff0d4}footer{border-top:1px solid #bdccc7;margin-top:2rem;padding:1rem 0}a:focus-visible{outline:3px solid #a45f00;outline-offset:3px}@media(max-width:760px){.layout{display:block;margin:1rem auto}nav{display:flex;flex-wrap:wrap;margin-bottom:2rem}nav a{padding:.4rem}h1{font-size:2rem}}'''
    (OUT/'style.css').write_text(style)
    for locale in LABELS:
        folder = OUT/locale/'signature/development'; folder.mkdir(parents=True)
        raw = OUT/'raw'/locale/'signature/development'; raw.mkdir(parents=True)
        for name in GUIDES:
            source, fallback = translated_source(locale,name,manifest)
            text = source.read_text()
            notice = ('Traducción ausente o desactualizada: se muestra el inglés de la misma revisión de Desarrollo.' if fallback else '')
            raw_text = (f'> {notice}\n\n' if fallback else '') + text
            (raw/f'{name}.md').write_text(raw_text)
            rendered = markdown.markdown(text,extensions=['extra','toc','codehilite'])
            rendered = re.sub(r'href="([^"#:]*)\.md(#[^"]*)?"',lambda m:f'href="{m[1]}.html{m[2] or ""}"',rendered)
            menu = ''.join(f'<a href="{n}.html"'+(' aria-current="page"' if n==name else '')+f'>{label}</a>' for n,label in zip(GUIDES,LABELS[locale]))
            other = 'es' if locale == 'en' else 'en'
            scope = 'Development · No confirmed package release' if locale == 'en' else 'Desarrollo · Sin publicación confirmada'
            title = html.escape(text.splitlines()[0].lstrip('# '))
            warning = f'<p class="notice">{notice}</p>' if notice else ''
            doc = f'''<!doctype html><html lang="{ 'en' if fallback else locale}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>{title} | Sworn Ink</title><link rel="stylesheet" href="../../../style.css"><link rel="alternate" hreflang="{other}" href="../../../{other}/signature/development/{name}.html"></head><body><header><a href="index.html">Sworn Ink / Android signatures</a><p>{scope}</p><a href="../../../{other}/signature/development/{name}.html" lang="{other}">{'Español' if other=='es' else 'English'}</a> · <a href="../../../raw/{locale}/signature/development/{name}.md">Markdown</a></header><div class="layout"><nav aria-label="{'Guides' if locale=='en' else 'Guías'}">{menu}</nav><main>{warning}{rendered}<footer>{scope} · <a href="https://github.com/lambdawalker/android.apexfission.signature/tree/feat/sworn-ink-signatures">{'Source and example' if locale=='en' else 'Código y ejemplo'}</a></footer></main></div></body></html>'''
            (folder/f'{name}.html').write_text(doc)
    (OUT/'index.html').write_text('<!doctype html><html lang="en"><head><meta charset="utf-8"><meta http-equiv="refresh" content="0;url=en/signature/development/index.html"><title>Sworn Ink — Development</title></head><body><a href="en/signature/development/index.html">Development documentation — no confirmed stable release</a></body></html>')
    (OUT/'llms.txt').write_text('# Sworn Ink Android — Development\nNo confirmed package release.\n\n- English: raw/en/signature/development/index.md\n- Español: raw/es/signature/development/index.md\n- Catalog: catalog.json\n')
    write_json(OUT/'catalog.json',catalog)
    validate_links()

def validate_links():
    for path in OUT.rglob('*'):
        if path.suffix not in ['.html','.md']: continue
        content=path.read_text()
        links=re.findall(r'(?:href|src)="([^"]+)"',content) if path.suffix=='.html' else re.findall(r'\]\(([^)]+)\)',content)
        for link in links:
            parsed=urlsplit(html.unescape(link))
            if parsed.scheme or parsed.netloc: continue
            target=(path.parent/unquote(parsed.path)).resolve() if parsed.path else path
            if not target.is_relative_to(OUT.resolve()) or not target.is_file(): raise ValueError(f'Broken link: {path}: {link}')
            if parsed.fragment and target.suffix=='.html' and f'id="{parsed.fragment}"' not in target.read_text(): raise ValueError(f'Broken anchor: {path}: {link}')

def verify():
    expected=json.loads((ROOT/'docs/source-manifest.json').read_text())
    if expected != sources(): raise ValueError('Source contract changed. Review guides, then accept-source deliberately.')
    if json.loads((ROOT/'docs/translations.json').read_text()) != translations(): raise ValueError('Spanish translations stale. Review translations before accept-translations; build uses explicit English fallback meanwhile.')
    for name in GUIDES:
        en=(ROOT/'docs/agents'/f'{name}.md').read_text();es=(ROOT/'docs/es'/f'{name}.md').read_text()
        if re.findall(r'```.*?```',en,re.S) != re.findall(r'```.*?```',es,re.S): raise ValueError(f'Code fences differ: {name}')
    build()
    print('Docs verified: 20 HTML guides, 20 raw guides, links, source hashes, translation/code freshness; Development only.')

if __name__=='__main__':
    command=sys.argv[1] if len(sys.argv)>1 else 'verify'
    if command=='accept-translations': write_json(ROOT/'docs/translations.json',translations())
    elif command=='accept-source': write_json(ROOT/'docs/source-manifest.json',sources())
    elif command=='build': build()
    elif command=='verify': verify()
    else: raise SystemExit('Usage: docs.py build|verify|accept-translations|accept-source')
