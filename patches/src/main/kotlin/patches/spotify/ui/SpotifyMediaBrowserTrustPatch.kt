package patches.spotify.ui

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val MEDIA_BROWSER_SERVICE_HANDLER = "Lp/n5b1;"

/**
 * `n5b1.d(uid, package)` is the media-browser root admission check. It accepts only
 * "android.media.session.MediaController" or a package registered with Spotify's host validator;
 * DriveDeck is neither, so `onGetRoot` returned null with "IGNORING request from untrusted package"
 * and the car fell back to the playback session. Accept every caller here, like the Zoom host
 * validator change. The root-specific filters are handled by the existing car patch.
 */
@Suppress("unused")
val spotifyMediaBrowserTrustPatch = bytecodePatch(
    name = "Accept custom Android Auto media browsers",
    description = "Allows Spotify's media-browser service to answer any Android Auto host, so a " +
        "custom host can browse its library instead of falling back to session-only playback.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val owner = mutableClassDefBy(MEDIA_BROWSER_SERVICE_HANDLER)
        val method = owner.methods.singleOrNull { candidate ->
            candidate.name == "d" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) ==
                listOf("I", "Ljava/lang/String;")
        } ?: throw IllegalStateException("Spotify media-browser trust check changed")
        method.returnEarly(true)
    }
}
