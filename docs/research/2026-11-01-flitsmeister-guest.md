# Flitsmeister guest mode: implementation evidence

The user chose guest mode after reproducing failed Google login on Flitsmeister 14.17/1287. The phone's Google service reported `UNREGISTERED_ON_API_CONSOLE`, followed by `GetCredentialCancellationException: [16] Account reauth failed.` No Google credential reached the existing backend login path.

## Native guest path

The onboarding landing content `Lu8d;->invoke` retains a guest callback in its `d:Lz39;` field, but the Compose group where a third button belonged is empty. The patch inserts a native design-library button into that group, with an English/Dutch label. It invokes the existing callback rather than creating an account or token.

The callback is `Lnad;->invoke`. It checks fine-location permission: when granted it invokes native onboarding completion; otherwise it opens the existing permission screen. Completion calls `OnboardingActivity.u`, which saves onboarding completion and opens the main activity. Explicit login/register screens remain available.

The startup decision `Lcei;->a` separately redirects users who have previously logged in but currently have no account. The patch bypasses only that historical-login branch. First-run onboarding, coroutine suspension/resumption and Dash setup remain intact. It does not change global account providers or Android Auto's separate account gate.

## Verified scope

- The supplied APKMirror APKM's base and arm64/English/xxxhdpi split payloads exactly match the repository's inspected APKS profile. They were extracted without modifying or re-signing the publisher payloads.
- The original guest verifier passes in disabled mode and fails in enabled mode for both the missing button and the previous-login redirect before production edits.
- The emitted guest-only and all-seven-control APKs pass patch reports, signing verification and the complete Flitsmeister decoded verifier, including the existing startup, car callback, privacy and native-library contracts.
- The new verifier executes the entire startup decision across first-run/completed, previous-login, genuine-account and Dash combinations, including resumed coroutine states. It executes the inserted button block to check callback/composer/size identity and resource selection. These are explicit framework boundaries, not proof of rendered Compose behavior.
- Whole-method hashes retain the native guest permission callback, completion persistence, navigation and Google credential login.
- The Gradle Android bundle build passes. The Python suite passes: 393 tests, 39 skipped. All 84 bilingual app pages pass against generated review metadata; release-owned metadata is left for semantic-release.

The four-architecture APKM also contains native libraries outside the repository's pinned arm64 profile. Its first full verification correctly rejected those extra libraries; the guard was not weakened. Final artifact validation uses the exact supported arm64 profile extracted from the user's archive.

## Pending device verification

The new button has not been rendered or tapped in a running patched app. Accountless map loading, live traffic, destination search, route calculation, navigation and cold relaunch remain unverified on-device. Do not infer these from a successful build or instruction execution.

The attached phone's installed signing key differs from the Mac's test signing key. The user declined reinstalling/clearing Flitsmeister and requested publishing the patch on `main` so it can be patched and tested through Morphe using the phone's existing key. No phone app data was cleared and no replacement APK was installed.

Local build and artifact reports are in the ignored `build/research/flitsmeister-google-login/` directory of the primary checkout. Raw log captures and APKs stay outside Git. No live Google credentials were used in the protocol probe or copied into repository files.
