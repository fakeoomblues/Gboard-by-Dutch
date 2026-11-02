# Myspoti stable promotion

On 2026-11-02 the user reported “myspoti works perfect promote to main” after testing development release 1.27.0-dev.2 on Myspoti 2.2.0. Device acceptance applies to 2.2.0; 2.3.0 has automated validation only. This explicitly accepts Myspoti and authorizes its stable promotion. It does not approve the separate YouTube Music personal-library patch.

The promoted Myspoti patch, extension and discovery tests are unchanged from accepted development commit `5b333f79e55763aac4ad2d3c9d4feae68550ef70`. It supports publisher inputs 2.2.0 / 1116 and 2.3.0 / 1121. The shared catalogue, navigation and downloads edits add only Myspoti. Existing stable YouTube Music host support remains unchanged.

Prior development validation passed 27 patch tests, 415 Python tests with 45 skips, strict patching and rebuilds of both Myspoti inputs, and six accountless API 36 checks per input against actual emitted DEX. The device acceptance is the user's reported result; automated checks alone do not establish account contents or playback. No account-bearing phone installation is changed by this promotion.

Fresh stable-candidate validation passed the complete release Gradle preflight, including 29 patch tests; 414 Python regressions with 45 skips; 15 Medium reader regressions; the Medium bundle/private ABI checks; and both app-page and Markdown-link checks. The five Myspoti class/extension entries built for stable match the accepted published development bundle byte-for-byte.

## Myspoti 2.3.0 Manager archive report

The user separately reported that Manager 2.3.0 / Patcher 2.1.0 fails before patch-session construction while reading the local-file header for `lib/arm64-v8a/libandroidx.graphics.path.so` at offset 14,057,336. The two supplied local filenames, including the refreshed `(1)` file, are byte-identical: 18,499,227 bytes, SHA-256 `2633fbc80e79c2b6ccadb7cda34f381fc5537770d7c2d77b49abb37dd4ef1e5f`. Both pass CRC verification for all 1,054 ZIP entries and APK v2 signature verification with publisher certificate SHA-256 `2b8f54daf60ce32cee1ab9206553c7e5741e96f2f32efdf2545758bf210a2c41`. The reported offset contains a valid local-file header.

Fresh strict patching and rebuild of the refreshed file passes with Morphe desktop 1.19.1 through its Arsclib resource coder and this stable candidate. The bounded workaround is desktop patching of this exact input; device acceptance remains on Myspoti 2.2.0. The selected phone file's bytes and Manager's archive-reader path are not established by the local result. This report does not show the patch callbacks failing.
