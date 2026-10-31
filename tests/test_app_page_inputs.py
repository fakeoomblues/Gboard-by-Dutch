"""Keep simplified app pages accurate without restoring technical evidence prose."""
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'scripts'))
spec = importlib.util.spec_from_file_location('app_pages', ROOT / 'scripts/check_docs_app_pages.py')
pages = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pages)


def fixture(language='en', version='1.0', build='100', input_type='APKS', state=None):
    en = language == 'en'
    state = state or ('On' if en else 'Aan')
    return f'''# NPO Start

**{'App version' if en else 'Appversie'}:** {version} · **Build:** {build} · **{'Original file' if en else 'Origineel bestand'}:** {input_type}

[Use original](../guides/downloads.md#local-original-inputs)

## Patches

{pages.LANG[language]['table']}
| --- | --- | --- |
| **Hide ads** | {state} | Removes video advertising. |
'''


def metadata(path):
    def package(version, code, factor):
        return {'packageName': 'nl.uitzendinggemist', 'apkFileType': 'APKS', 'formFactor': factor,
                'targets': [{'version': version, 'versionCodes': {'ALL': code}}]}
    path.write_text(json.dumps({'patches': [
        {'name': 'Hide ads', 'default': True, 'compatiblePackages': [package('1.0', 100, 'MOBILE'), package('2.0', 200, 'TV')]},
        {'name': 'Hide unavailable content', 'default': False, 'compatiblePackages': [package('2.0', 200, 'TV')]},
    ]}))
    return pages.load_apps([path])


class AppPageTests(unittest.TestCase):
    def check(self, language='en', text=None, rows=None):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            apps = metadata(directory / 'metadata.json')
            target = directory / 'npo-start.md'
            target.write_text(text or fixture(language))
            rows = {'npo-start': {'url': '../guides/downloads.md#local-original-inputs', 'version': '1.0', 'build': '100'}} if rows is None else rows
            return pages.check_page(target, language, apps, rows)

    def test_local_original_inputs_work_in_both_languages_without_invented_download(self):
        for language in ('en', 'nl'):
            self.assertEqual(self.check(language), [])

    def test_phone_page_does_not_inherit_tv_controls(self):
        self.assertEqual(self.check(), [])
        bad = fixture().replace('| **Hide ads**', '| **Hide unavailable content**')
        self.assertTrue(any('patch names/defaults' in error for error in self.check(text=bad)))

    def test_defaults_and_targets_are_checked_independently(self):
        self.assertTrue(any('patch names/defaults' in error for error in self.check(text=fixture(state='Off'))))
        self.assertTrue(any('not a metadata target' in error for error in self.check(text=fixture(build='999'))))
        self.assertIn('original file type differs from metadata', self.check(text=fixture(input_type='APK')))

    def test_download_button_and_target_must_match_guide(self):
        self.assertTrue(any('downloads guide row' in error for error in self.check(rows={})))
        rows = {'npo-start': {'url': '../guides/downloads.md#local-original-inputs', 'version': '9.0', 'build': '900'}}
        self.assertIn('target differs from downloads guide row', self.check(rows=rows))

    def test_internal_implementation_detail_is_rejected(self):
        errors = self.check(text=fixture() + '\nThe smali payload dump uses a callback hook.\n')
        self.assertTrue(any('implementation/measurement detail' in error for error in errors))

    def test_downloads_guide_local_original_row_is_resolved(self):
        with tempfile.TemporaryDirectory() as temporary:
            site = Path(temporary)
            guide = site / 'en/guides/downloads.md'
            guide.parent.mkdir(parents=True)
            guide.write_text('| [NPO Start](../apps/npo-start.md) | 1.0 / 100 | Use your own file |\n')
            with patch.object(pages, 'SITE', site):
                self.assertEqual(pages.downloads('en'), {'npo-start': {
                    'url': '../guides/downloads.md#local-original-inputs', 'version': '1.0', 'build': '100'}})


if __name__ == '__main__':
    unittest.main()
