package patches.gboard

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

internal const val GBOARD_PACKAGE = "com.google.android.inputmethod.latin"

// Google ships 18.8.0 as separate armeabi-v7a (176124765) and arm64-v8a
// (176124766) beta builds whose versionName embeds the ABI, and publishes each
// build in minAPI26 and minAPI32 variants. The lowest supported variant
// sets the catalog minSdk; the extension targets the same Android 8.0+.
private fun gboardTargets() = listOf(
    AppTarget(
        version = "18.8.0.1022711984-beta-armeabi-v7a", minSdk = 26,
        versionCodes = mapOf(SupportedAbi.ARMEABI_V7A to 176124765),
    ),
    AppTarget(
        version = "18.8.0.1022711984-beta-arm64-v8a", minSdk = 26,
        versionCodes = mapOf(SupportedAbi.ARM64_V8A to 176124766),
    ),
)

private val gboardSignatures = setOf(
    // Google's rotated Gboard signer (Android 13+) and its original signer (Android 7-12).
    "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053",
    "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83",
)

internal val gboard = Compatibility(
    name = "Gboard", packageName = GBOARD_PACKAGE, apkFileType = ApkFileType.APK,
    appIconColor = 0x4285F4,
    signatures = gboardSignatures,
    targets = gboardTargets(),
)

internal val originalInput = patches.shared.originalInput(gboard)
