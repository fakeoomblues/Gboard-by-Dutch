package patches.gboard

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal const val GBOARD_PACKAGE = "com.google.android.inputmethod.latin"

internal val gboard = Compatibility(
    name = "Gboard", packageName = GBOARD_PACKAGE, apkFileType = ApkFileType.APK,
    appIconColor = 0x4285F4,
    // Google's rotated Gboard signer (Android 13+) and its original signer (Android 7-12).
    signatures = setOf(
        "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053",
        "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83",
    ),
    targets = listOf(AppTarget(version = "18.6.3.1002251336-release-arm64-v8a", versionCode = 176071766, minSdk = 32)),
)

internal val originalInput = patches.shared.originalInput(gboard)
