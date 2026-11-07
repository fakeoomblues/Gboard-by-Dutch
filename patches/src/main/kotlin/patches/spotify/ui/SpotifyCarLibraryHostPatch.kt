package patches.spotify.ui

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val ANDROID_AUTO_ROOT = "Lp/n64;"

/**
 * `n64.b(package)` decides whether the Android Auto media-browser root is offered. It matches
 * only `com.google.android.projection.gearhead` (and Spotify's own mediatest app), so any other
 * car host falls through to a null root even when the trust check accepts it. Accept every caller
 * so the Android Auto root provider is selected, like the Zoom host-admission patch.
 */
@Suppress("unused")
val spotifyCarLibraryHostPatch = bytecodePatch(
    name = "Offer the car library to any host",
    description = "Selects Spotify's Android Auto media-browser root for any car host instead " +
        "of only Google's Gearhead package, so custom hosts can browse Home and Library.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val owner = mutableClassDefBy(ANDROID_AUTO_ROOT)
        val method = owner.methods.singleOrNull { candidate ->
            candidate.name == "b" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        } ?: throw IllegalStateException("Spotify Android Auto root selector changed")
        method.returnEarly(true)
    }
}
