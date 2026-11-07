package patches.spotify.ui

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val MEDIA_BROWSER_SERVICE_HANDLER = "Lp/n5b1;"
private const val ANDROID_AUTO_ROOT = "Lp/n64;"

/**
 * Two gates decide whether a custom host can browse Spotify's car library:
 *
 * - `n5b1.d(uid, package)` is the media-browser admission check. It accepts only
 *   "android.media.session.MediaController" or a package registered with Spotify's host validator.
 * - `n64.b(package)` decides whether the Android Auto media-browser root is offered. It matches
 *   only Google's Gearhead package and Spotify's own media test app.
 *
 * Accept both so the host reaches the existing Home/Library providers instead of the
 * session-only fallback.
 */
@Suppress("unused")
val spotifyCarHostAccessPatch = bytecodePatch(
    name = "Share the car media library with custom hosts",
    description = "Accepts any Android Auto host at Spotify's media-browser admission check and " +
        "offers it the Android Auto root, so the host can browse Home and Library.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val service = mutableClassDefBy(MEDIA_BROWSER_SERVICE_HANDLER)
        val trust = service.methods.singleOrNull { candidate ->
            candidate.name == "d" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) ==
                listOf("I", "Ljava/lang/String;")
        } ?: throw IllegalStateException("Spotify media-browser trust check changed")
        trust.returnEarly(true)

        val root = mutableClassDefBy(ANDROID_AUTO_ROOT)
        val selector = root.methods.singleOrNull { candidate ->
            candidate.name == "b" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        } ?: throw IllegalStateException("Spotify Android Auto root selector changed")
        selector.returnEarly(true)
    }
}
