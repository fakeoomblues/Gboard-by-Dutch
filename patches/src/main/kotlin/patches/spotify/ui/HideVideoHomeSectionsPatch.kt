package patches.spotify.ui

import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.builder.instruction.BuilderPackedSwitchPayload
import com.android.tools.smali.dexlib2.builder.instruction.BuilderSwitchElement
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val HOME_SECTION_RENDERER = "Lp/uqs;"

/** Switch keys for WATCH_FEED, VIDEO_PROMO, VIDEO_BRAND_AD and VIDEO_CAROUSEL. */
private val VIDEO_SWITCH_KEYS = setOf(15, 20, 23, 31)

/** The packed-switch key Spotify already uses for an unsupported section. */
private const val SKIP_SWITCH_KEY = 5

/**
 * Spotify's resolved Home feed switches on the section type. The video section payloads
 * (`WATCH_FEED`, `VIDEO_PROMO`, `VIDEO_BRAND_AD`, `VIDEO_CAROUSEL`) share the same packed switch
 * as every audio section.
 *
 * Point those switch entries at the existing skip target. This changes only switch targets, not
 * the method's code, which keeps the class verifier-safe (injecting new branches into this method
 * was rejected by the ART verifier).
 */
@Suppress("unused")
val hideVideoHomeSectionsPatch = bytecodePatch(
    name = "Hide video shelves (safe)",
    description = "Drops video/watch sections from Spotify's resolved Home feed, so video " +
        "podcast and music-video shelves are not shown.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val owner = mutableClassDefBy(HOME_SECTION_RENDERER)
        val method = owner.methods.singleOrNull { candidate ->
            candidate.name == "j" &&
                candidate.returnType == "Lp/jn60;" &&
                candidate.parameterTypes.map(CharSequence::toString) ==
                listOf("Lcom/spotify/casita/v1/resolved/Section;")
        } ?: throw IllegalStateException("Spotify Home section renderer changed")

        val payload = method.implementation!!.instructions
            .filterIsInstance<BuilderPackedSwitchPayload>()
            .singleOrNull()
            ?: throw IllegalStateException("Spotify Home section switch payload changed")

        val elements = payload.switchElements
        val keys = elements.map { it.key }.toSet()
        check(keys.containsAll(VIDEO_SWITCH_KEYS + SKIP_SWITCH_KEY)) {
            "Spotify Home section switch keys changed: $keys"
        }
        val skip = elements.single { it.key == SKIP_SWITCH_KEY }.target
        VIDEO_SWITCH_KEYS.forEach { key ->
            val index = elements.indexOfFirst { it.key == key }
            elements[index] = BuilderSwitchElement(payload, key, skip)
        }
    }
}
