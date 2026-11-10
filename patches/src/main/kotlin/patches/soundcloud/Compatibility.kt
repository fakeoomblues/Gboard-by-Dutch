package patches.soundcloud

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal val soundcloud = Compatibility(
    name = "SoundCloud", packageName = "com.soundcloud.android", apkFileType = ApkFileType.APKM,
    signatures = setOf("aff930d671fa0a57b8c41d1678cc2a8ea717075a74e346942a140afa441339d2"),
    targets = listOf(AppTarget(version = "2026.11.02-release", versionCode = 379060, minSdk = 28)),
)

internal val originalInput = patches.shared.originalInput(soundcloud)
