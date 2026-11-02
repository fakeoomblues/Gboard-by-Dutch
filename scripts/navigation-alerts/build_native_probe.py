#!/usr/bin/env python3
"""Build same-test-signer native lifecycle probes; no device interaction."""
import argparse
from pathlib import Path
import subprocess
import zipfile

TARGETS = {'vela': 'app.vela', 'maps': 'com.google.android.apps.maps'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--target', choices=TARGETS, required=True)
    parser.add_argument('--key', type=Path, required=True)
    parser.add_argument('--alias', default='native-warning-test')
    args = parser.parse_args()
    source = Path(__file__).with_name('probe')
    tools = args.sdk / 'build-tools/36.0.0'
    android = args.sdk / 'platforms/android-36/android.jar'
    output = args.output
    classes, dex = output / 'classes', output / 'dex'
    classes.mkdir(parents=True, exist_ok=True)
    dex.mkdir(parents=True, exist_ok=True)
    target_source = source / ('VelaVoiceProbe.java' if args.target == 'vela' else 'MapsVoiceProbe.java')

    def run(*command):
        subprocess.run([str(item) for item in command], check=True)

    run('javac', '--release', '8', '-cp', android, '-d', classes,
        source / 'NativeWarningProbe.java', target_source)
    run(tools / 'd8', '--min-api', '29', '--lib', android, '--output', dex,
        *sorted(classes.rglob('*.class')))
    package = 'nl.johninnl.warningprobe.' + args.target
    manifest = output / 'AndroidManifest.xml'
    manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="' + package + '">'
                        '<uses-sdk android:minSdkVersion="29" android:targetSdkVersion="34"/>'
                        '<application android:label="Native warning lifecycle probe"/>'
                        '<instrumentation android:name="nl.johninnl.warningprobe.NativeWarningProbe" android:targetPackage="'
                        + TARGETS[args.target] + '"/></manifest>')
    raw, apk = output / 'unaligned.apk', output / 'probe.apk'
    run(tools / 'aapt2', 'link', '-I', android, '--manifest', manifest, '-o', raw)
    with zipfile.ZipFile(raw, 'a', zipfile.ZIP_DEFLATED) as archive:
        for item in sorted(dex.glob('classes*.dex')):
            archive.write(item, item.name)
    run(tools / 'zipalign', '-f', '4', raw, apk)
    run(tools / 'apksigner', 'sign', '--ks', args.key, '--ks-key-alias', args.alias,
        '--ks-pass', 'pass:android', '--key-pass', 'pass:android', apk)
    run(tools / 'apksigner', 'verify', apk)
    print(apk)


if __name__ == '__main__':
    main()
