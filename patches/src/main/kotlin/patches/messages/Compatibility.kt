package patches.messages

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

internal const val MESSAGES_PACKAGE = "com.google.android.apps.messaging"
private const val MESSAGES_BUILD = 328483063

/**
 * The regular phone build of Google Messages as APKMirror's universal APKM archive (base, four
 * architecture, display and language splits, one version code). Google signs Messages with a
 * rotated key: the current signer (Android 13+) and the original one in its lineage are both
 * accepted.
 *
 * Experimental: these patches have not yet been confirmed on a phone.
 */
internal val messages = Compatibility(
    name = "Google Messages",
    packageName = MESSAGES_PACKAGE,
    apkFileType = ApkFileType.APKM,
    appIconColor = 0x3186FF,
    signatures = setOf(
        "3284b53cbc699d6adf924945cc5a26dc5a0066b1d6767ffcd7bca44d0ea9fab8",
        "cc75526d6c0f80ac3b3a84eb44838440dbbe6b124443a7c7d7bd19182ac022c9",
    ),
    targets = listOf(
        AppTarget(
            version = "messages.android_20261106_00_RC01.phone_dynamic",
            versionCodes = mapOf(
                SupportedAbi.ARM64_V8A to MESSAGES_BUILD,
                SupportedAbi.ARMEABI_V7A to MESSAGES_BUILD,
                SupportedAbi.X86_64 to MESSAGES_BUILD,
                SupportedAbi.X86 to MESSAGES_BUILD,
            ),
            isExperimental = true,
            minSdk = 32,
        ),
    ),
)

internal val originalInput = patches.shared.originalInput(messages)
