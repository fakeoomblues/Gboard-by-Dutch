"""Inspect emitted APK DEX hooks. This is structural evidence, not Android execution."""
import argparse
from pathlib import Path
import shutil
import subprocess

HELPER = 'Lpatches/ytmusic/GearslipSupport;'
LAYOUTS = {
    '9.30.53': ('kyh', 'awld', 'awjm', 'cbi'),
    '9.44.52': ('hdv', 'zph', 'znr', 'bnc'),
}


def code(cli, apk, cls, method):
    result = subprocess.run([cli, 'dex', 'code', '--class', cls, '--method', method, str(apk)],
                            capture_output=True, text=True, check=True)
    return result.stdout


def verify(cli, apk, version):
    owner, package, client, root = LAYOUTS[version]
    trust = code(cli, apk, owner, f'g(L{package};L{client};I)Z')
    decision = f'{HELPER}->decision(Landroid/content/Context;Ljava/lang/String;I)I'
    assert decision in trust, 'Missing actual-callback-UID trust hook'
    prefix, original = trust.split(decision, 1)
    assert f'L{package};->b:Ljava/lang/String;' in prefix, 'Caller package field changed'
    assert f'L{owner};->q:Landroid/content/Context;' in prefix, 'PackageManager context changed'
    call = prefix.splitlines()[-1]
    assert 'p3' in call, 'Trust hook is not consuming callback UID parameter p3'
    assert 'if-ltz' in original and 'return' in original, 'Configured rejection is not terminal'
    assert '0x2bb084d' in original, 'Trust hook must precede server-flag 45811789'
    assert 'getPackagesForUid(I)' in original, 'Original caller UID validation was lost'
    eligible = code(cli, apk, owner, f'i(L{client};)Z')
    assert f'{HELPER}->isConfiguredPackage(Ljava/lang/String;)Z' in eligible, 'Missing browser eligibility hook'
    assert f'L{client};->b:Ljava/lang/String;' in eligible, 'Client package field changed'
    assert '.registers 3' in eligible, 'Eligibility wrapper must have its own local register'
    assert f'L{owner};->morpheOriginalBrowserEligibility(L{client};)Z' in eligible, 'Original eligibility fallback was lost'
    preserved = code(cli, apk, owner, f'morpheOriginalBrowserEligibility(L{client};)Z')
    assert '.registers 2' in preserved, 'Original eligibility parameter registers changed'
    assert 'Ljava/util/Set;->contains' in preserved, 'Original eligibility policy was lost'
    assert 'monitor-enter p0' in preserved and 'monitor-exit p0' in preserved, 'Original eligibility synchronization changed'
    root_code = code(cli, apk, 'com.google.android.apps.youtube.music.mediabrowser.MusicBrowserService',
                     f'a(Ljava/lang/String;ILandroid/os/Bundle;)L{root};')
    assert f'L{owner};->g(L{package};L{client};I)Z' in root_code, 'Browser root trust call changed'
    assert 'getCallingUid' not in trust, 'Handler callback must use its captured UID, not current Binder identity'
    print(f'PASS {version}: actual emitted DEX retains caller ownership and original fallback')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--version', choices=LAYOUTS, required=True)
    parser.add_argument('--apkanalyzer', default=shutil.which('apkanalyzer'))
    args = parser.parse_args()
    if not args.apkanalyzer:
        parser.error('Pass --apkanalyzer pointing to the installed Android SDK tool')
    try:
        verify(args.apkanalyzer, args.apk, args.version)
    except AssertionError as failure:
        raise SystemExit(f'FAIL {args.version}: {failure}') from failure


if __name__ == '__main__':
    main()
