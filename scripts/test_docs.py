import unittest
from unittest.mock import patch
import docs

class DocumentationTests(unittest.TestCase):
    def test_fresh_spanish_preserves_locale_and_revision(self):
        source, fallback = docs.translated_source('es', 'api', docs.translations())
        self.assertEqual(source, docs.ROOT/'docs/es/api.md')
        self.assertFalse(fallback)

    def test_stale_spanish_uses_same_scope_english(self):
        source, fallback = docs.translated_source('es', 'api', {'api.md': 'stale'})
        self.assertEqual(source, docs.ROOT/'docs/agents/api.md')
        self.assertTrue(fallback)

    def test_reviewed_source_change_is_rejected_before_rendering(self):
        with patch.object(docs, 'sources', return_value={}), patch.object(docs, 'build') as build:
            with self.assertRaisesRegex(ValueError, 'Source contract changed'):
                docs.verify()
            build.assert_not_called()

    def test_stale_review_manifest_is_rejected_before_rendering(self):
        with patch.object(docs, 'translations', return_value={}), patch.object(docs, 'build') as build:
            with self.assertRaisesRegex(ValueError, 'Spanish translations stale'):
                docs.verify()
            build.assert_not_called()

    def test_translated_code_change_is_rejected_before_rendering(self):
        original = docs.Path.read_text
        def changed(path, *args, **kwargs):
            result = original(path, *args, **kwargs)
            return result + '\n```kotlin\nwrong()\n```\n' if path == docs.ROOT/'docs/es/api.md' else result
        with patch.object(docs.Path, 'read_text', changed), patch.object(docs, 'build') as build:
            with self.assertRaisesRegex(ValueError, 'Code fences differ: api'):
                docs.verify()
            build.assert_not_called()

if __name__ == '__main__':
    unittest.main()
