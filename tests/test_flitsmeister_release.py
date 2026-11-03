"""Negative controls for exact current-main additions on both Flitsmeister versions."""
import copy
import unittest
import xml.etree.ElementTree as ET

import verify_flitsmeister_release as release
from verify_flitsmeister import NAMES


class ManifestTests(unittest.TestCase):
    def manifest(self, nav=False, hosts=False):
        root = ET.Element('manifest', {'package': 'nl.flitsmeister'})
        queries = ET.SubElement(root, 'queries')
        ET.SubElement(queries, 'package', {release.A + 'name': 'original.native.package'})
        second = ET.SubElement(root, 'queries')
        ET.SubElement(second, 'intent', {'original': 'retained'})
        if hosts:
            for package in release.HOST_PACKAGES:
                ET.SubElement(queries, 'package', {release.A + 'name': package})
        app = ET.SubElement(root, 'application')
        ET.SubElement(app, 'service', {release.A + 'name': 'nl.flitsmeister.services.base.FlitsmeisterBaseService',
                                       release.A + 'exported': 'false', release.A + 'permission': 'original.permission'})
        if nav:
            ET.SubElement(app, 'service', {release.A + 'name': 'nl.johninnl.flitsmeister.NavigationControlService', release.A + 'exported': 'true'})
            ET.SubElement(app, 'service', {release.A + 'name': 'nl.johninnl.flitsmeister.NavigationEngineService', release.A + 'exported': 'false', release.A + 'foregroundServiceType': 'location'})
        return root

    def test_only_explicit_services_and_queries_are_normalized(self):
        for version in (14, 15):
            for nav in (False, True):
                for hosts in (False, True):
                    selected = ({release.BROADCAST} if nav else set()) | ({release.SUPPORT} if hosts else set())
                    original = self.manifest()
                    clean = release.normalize_manifest(self.manifest(nav, hosts), selected, version)
                    self.assertEqual(ET.tostring(clean), ET.tostring(original))
                    self.assertEqual(len(clean.findall('queries')), 2)
                    native = clean.find('application/service')
                    self.assertEqual(native.get(release.A + 'exported'), 'false')
                    self.assertEqual(native.get(release.A + 'permission'), 'original.permission')

    def test_wrong_export_or_component_permission_cannot_be_hidden(self):
        for attribute, value in ((release.A + 'exported', 'true'), (release.A + 'permission', 'new.permission')):
            doc = self.manifest(True)
            doc.find('application').findall('service')[-1].set(attribute, value)
            with self.assertRaisesRegex(AssertionError, 'exact export/foreground/permission'):
                release.normalize_manifest(doc, {release.BROADCAST}, 15)

    def test_extra_duplicate_or_unselected_query_is_rejected(self):
        doc = self.manifest(hosts=True)
        with self.assertRaisesRegex(AssertionError, 'selected-only package visibility'):
            release.normalize_manifest(doc, set(), 14)
        ET.SubElement(doc.find('queries'), 'package', {release.A + 'name': release.HOST_PACKAGES[0]})
        with self.assertRaisesRegex(AssertionError, 'selected-only package visibility'):
            release.normalize_manifest(doc, {release.SUPPORT}, 15)


class AudioTests(unittest.TestCase):
    NATIVE = '''.method public final onDestroy()V
.locals 2
invoke-virtual {v0, v1}, Landroid/speech/tts/TextToSpeech;->setOnUtteranceProgressListener(Landroid/speech/tts/UtteranceProgressListener;)I
move-result v0
return-void
.end method'''

    def test_exact_teardown_replacement_preserves_registers_and_result(self):
        managed = self.NATIVE.replace('invoke-virtual', 'invoke-static').replace(release.NATIVE_LISTENER, release.MANAGED_LISTENER)
        normalized = release.normalize_destroy(managed, True)
        self.assertEqual(release.canonical(normalized), release.canonical(self.NATIVE))
        self.assertEqual(release.shape(normalized), release.shape(self.NATIVE))
        self.assertEqual(release.normalize_destroy(self.NATIVE, False), self.NATIVE)
        with self.assertRaisesRegex(AssertionError, 'iff navigation selected'):
            release.normalize_destroy(managed, False)

    def test_no_broad_provider_call_or_duplicate_normalization(self):
        managed = self.NATIVE.replace('invoke-virtual', 'invoke-static').replace(release.NATIVE_LISTENER, release.MANAGED_LISTENER)
        duplicate = managed.replace('move-result v0', 'invoke-static {v0, v1}, ' + release.MANAGED_LISTENER + '\nmove-result v0')
        with self.assertRaisesRegex(AssertionError, 'Exactly one'):
            release.normalize_destroy(duplicate, True)
        extra = managed.replace('return-void', 'invoke-static {}, ' + release.AUDIO_OWNER + '->muted()Z\nreturn-void')
        with self.assertRaisesRegex(AssertionError, 'No other teardown'):
            release.normalize_destroy(extra, True)

    def test_focus_gate_self_loop_is_rejected_even_with_identical_shape(self):
        descriptor = 'Lorg/maplibre/navigation/android/navigation/ui/v5/voice/Api26AudioFocusDelegate;->requestFocus()V'
        body = """.method public requestFocus()V
.locals 1
invoke-static/range {p0 .. p0}, Lnl/johninnl/navigation/startup/ProviderAudio;->trackMapFocus(Ljava/lang/Object;)V
invoke-static {}, Lnl/johninnl/navigation/startup/ProviderAudio;->muted()Z
move-result v0
:bad_loop
if-eqz v0, :native_audio
return-void
:native_audio
nop
iget-object v0, p0, Lorg/maplibre/navigation/android/navigation/ui/v5/voice/Api26AudioFocusDelegate;->audioManager:Landroid/media/AudioManager;
iget-object p0, p0, Lorg/maplibre/navigation/android/navigation/ui/v5/voice/Api26AudioFocusDelegate;->audioFocusRequest:Landroid/media/AudioFocusRequest;
invoke-virtual {v0, p0}, Landroid/media/AudioManager;->requestAudioFocus(Landroid/media/AudioFocusRequest;)I
return-void
.end method"""
        broken = body.replace('if-eqz v0, :native_audio', 'if-eqz v0, :bad_loop')
        self.assertEqual(release.shape(body), release.shape(broken))
        release.verify_focus_behavior(body)
        with self.assertRaisesRegex(AssertionError, 'Fragment did not complete'):
            release.verify_focus_behavior(broken)
        for version in (14, 15):
            release.assert_profile(body, release.AUDIO[version][descriptor]['managed'], descriptor)
            with self.assertRaisesRegex(AssertionError, 'full publisher flow'):
                release.assert_profile(broken, release.AUDIO[version][descriptor]['managed'], descriptor)

    def test_both_versions_keep_complete_native_cohort_and_uid_methods(self):
        self.assertEqual(len(NAMES), 9)
        self.assertIn(release.SUPPORT, NAMES)
        for version in (14, 15):
            self.assertEqual(len(release.AUDIO[version]), 18)
            self.assertEqual(sum(row['role'] == 'retained' for row in release.AUDIO[version].values()), 2)
            for signature, row in release.HOST_VALIDATOR[version].items():
                if signature.startswith('isAllowListed('):
                    self.assertNotEqual(row['original'][0], row['selected'][0])
                    self.assertEqual(row['selected'][1][1], row['original'][1][1] + 19)
                else:
                    self.assertEqual(row['original'], row['selected'])
            self.assertIn('validateHost(Landroidx/car/app/HostInfo;)Z', release.HOST_VALIDATOR[version])
            batch = next(row for row in release.AUDIO[version].values() if row['role'] == 'batch')
            self.assertEqual(batch['managed'][1][1], batch['original'][1][1] + 5)


if __name__ == '__main__':
    unittest.main()
