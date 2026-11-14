package patches.messages

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

internal const val MESSAGES_PACKAGE = "com.google.android.apps.messaging"

/**
 * The Samsung phone build of Google Messages, as one complete APKS archive (base, arm64 and
 * display/language splits). Google signs Messages with a rotated key: the current signer
 * (Android 13+) and the original one in its lineage are both accepted.
 *
 * Experimental: these patches have not yet been confirmed on a phone.
 */
internal val messages = Compatibility(
    name = "Google Messages",
    packageName = MESSAGES_PACKAGE,
    apkFileType = ApkFileType.APKS,
    appIconColor = 0x3186FF,
    signatures = setOf(
        "3284b53cbc699d6adf924945cc5a26dc5a0066b1d6767ffcd7bca44d0ea9fab8",
        "cc75526d6c0f80ac3b3a84eb44838440dbbe6b124443a7c7d7bd19182ac022c9",
    ),
    targets = listOf(
        AppTarget(
            version = "messages.android_20261030_00_RC00.phone_samsung_dynamic",
            versionCodes = mapOf(SupportedAbi.ARM64_V8A to 327677900),
            isExperimental = true,
            minSdk = 32,
        ),
    ),
)

internal val originalInput = patches.shared.originalInput(messages)
