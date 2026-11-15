package patches.nexuslauncher

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

/**
 * Pixel Launcher 17 (versionCode 907). The launcher is shipped as a privileged system app,
 * so a sideloaded copy hits hidden-API link denial and a set of privileged-only services.
 * These patches make one specific build run as an ordinary app on a non-Pixel device.
 */
internal val pixelLauncher = Compatibility(
    name = "Pixel Launcher",
    packageName = "com.google.android.apps.nexuslauncher",
    apkFileType = ApkFileType.APK,
    signatures = setOf("a86bdb059f28f265162d64ce6c8d9772901d227e741581d1f04a5ed132a574d0"),
    targets = listOf(
        AppTarget(version = "17", versionCode = 907, minSdk = 37),
    ),
)

internal val originalInput = patches.shared.originalInput(pixelLauncher)
