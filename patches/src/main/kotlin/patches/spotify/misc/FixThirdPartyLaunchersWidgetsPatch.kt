package patches.spotify.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import patches.spotify.COMPATIBILITY_SPOTIFY
import patches.spotify.CanBindAppWidgetPermissionFingerprint
import patches.spotify.CanBindAppWidgetPermissionNoFlagsFingerprint
import java.util.logging.Logger

@Suppress("unused")
val fixThirdPartyLaunchersWidgetsPatch = bytecodePatch(
    name = "Fix third party launchers widgets",
    description = "Allows the Spotify widgets to be added to third party launchers.",
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        // Only the stock launcher has the BIND_APPWIDGET permission Spotify checks for.
        val match = CanBindAppWidgetPermissionFingerprint.matchOrNull()
            ?: CanBindAppWidgetPermissionNoFlagsFingerprint.matchOrNull()

        if (match == null) {
            // Do not fail the whole patching for this optional fix.
            Logger.getLogger(this::class.java.name).warning(
                "The BIND_APPWIDGET permission check was not found, skipping the widget fix"
            )
            return@execute
        }

        match.method.returnEarly(true)
    }
}
