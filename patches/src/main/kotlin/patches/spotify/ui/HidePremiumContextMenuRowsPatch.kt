package patches.spotify.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.spotify.COMPATIBILITY_SPOTIFY
import patches.spotify.shared.EXTENSION
import patches.spotify.shared.EXTENSION_PACKAGE

private const val CONTEXT_MENU_VIEW_MODEL = "Lp/kjm;"
private const val HEADER = "Lp/igm;"
private const val ITEMS = "Ljava/util/List;"
private const val EXTENSION_CLASS = "$EXTENSION_PACKAGE/spotify/ContextMenuFilter;"

/**
 * Removes the Download and Jam rows from context menus. These are the rows Spotify decorates with
 * its Premium badge; they cannot do anything for a Free account, so leaving them out is purely a
 * UI change. Playlist, album and track actions remain.
 */
@Suppress("unused")
val hidePremiumContextMenuRowsPatch = bytecodePatch(
    name = "Hide premium context-menu rows",
    description = "Removes Download and Start a Jam (and the related premium-only Jam rows) from " +
        "song, album and playlist context menus. Playback, library and sharing actions stay.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)
    extendWith(EXTENSION)

    execute {
        val owner = mutableClassDefBy(CONTEXT_MENU_VIEW_MODEL)
        val constructor = owner.methods.singleOrNull { method ->
            method.name == "<init>" &&
                method.returnType == "V" &&
                method.parameterTypes.map(CharSequence::toString) == listOf(HEADER, ITEMS, "Z")
        } ?: throw IllegalStateException("Spotify context-menu view model changed")

        constructor.addInstructions(
            0,
            """
                invoke-static {p2}, $EXTENSION_CLASS->filter(Ljava/util/List;)Ljava/util/List;
                move-result-object p2
            """,
        )
    }
}
