# Verify Myspoti custom branding

Build the normal Dutch App Patches bundle with `./gradlew :patches:buildAndroid :patches:test`. Use the repository's normal authenticated build environment.
The resulting bundle contains both Myspoti patches; no separate pack is needed.

Run the APK regression matrix against an original publisher APK:

```sh
python3 scripts/myspoti/verify_branding.py --bundle /path/to/patches.mpp --apk /path/to/myspoti.apk --cli /path/to/morphe-desktop-all.jar --aapt2 /path/to/sdk/build-tools/36.1.0/aapt2 --output /path/to/verification
```

Requires Java, Python 3, APKtool and Android SDK aapt2. The matrix checks every
icon, original branding, independent name/icon selection, punctuation and
resource-like names, and branding combined with Gearslip personal library.
It rebuilds APKs, checks resolved labels, inspects manifest/resource references,
compares the bundled image bytes and proves branding-only outputs preserve DEX.
Outputs are unsigned APKs and a `verification.json` report. This does not install
an APK or verify a device launcher or signed-in account.

To use the bundle in Morphe, import the `.mpp`, select Myspoti and enable
**Custom branding**. Set **App name** and choose **App icon**. Keep **Gearslip
personal library** enabled if wanted. Original icon and an empty name preserve
those respective values. Repatch to change branding again.
