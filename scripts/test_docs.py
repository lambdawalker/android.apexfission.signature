import unittest
import tempfile
from pathlib import Path
import docs

class DocumentationTests(unittest.TestCase):
    def setUp(self):
        self.output = tempfile.TemporaryDirectory()
        self.previous_output = docs.OUT
        docs.OUT = Path(self.output.name)/'dist'

    def tearDown(self):
        docs.OUT = self.previous_output
        self.output.cleanup()

    def test_fresh_spanish_preserves_locale_and_revision(self):
        source, fallback = docs.translated_source('es','api',docs.translations())
        self.assertEqual(source, docs.ROOT/'docs/es/api.md')
        self.assertFalse(fallback)

    def test_stale_spanish_uses_same_scope_english(self):
        source, fallback = docs.translated_source('es','api',{'api.md':'stale'})
        self.assertEqual(source,docs.ROOT/'docs/agents/api.md')
        self.assertTrue(fallback)

    def test_generated_routes_raw_links_and_locales(self):
        docs.build()
        for locale in ['en','es']:
            for name in docs.GUIDES:
                page=docs.OUT/locale/'signature/development'/f'{name}.html'
                raw=docs.OUT/'raw'/locale/'signature/development'/f'{name}.md'
                self.assertTrue(page.is_file())
                self.assertTrue(raw.is_file())
                other='es' if locale=='en' else 'en'
                self.assertIn(f'../../../{other}/signature/development/{name}.html',page.read_text())
                self.assertIn(f'../../../raw/{locale}/signature/development/{name}.md',page.read_text())
        docs.validate_links()
        self.assertIn('No confirmed', (docs.OUT/'index.html').read_text().replace('no confirmed','No confirmed'))

if __name__=='__main__': unittest.main()
