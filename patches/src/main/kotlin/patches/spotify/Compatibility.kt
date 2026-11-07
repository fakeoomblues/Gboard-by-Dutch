package patches.spotify

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

internal val spotify = Compatibility(
    name = "Spotify", packageName = "com.spotify.music", apkFileType = ApkFileType.APKM,
    appIconColor = 0x1ed760,
    signatures = setOf("6505b181933344f93893d586e399b94616183f04349cb572a9e81a3335e28ffd"),
    targets = listOf(
        AppTarget(
            version = "9.2.0.26", minSdk = 24,
            versionCodes = mapOf(SupportedAbi.ARM64_V8A to 148373686),
        ),
    ),
)

internal val originalInput = patches.shared.originalInput(spotify)

internal val COMPATIBILITY_SPOTIFY get() = spotify
