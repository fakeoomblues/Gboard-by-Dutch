package patches.spotify.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val LIBRARY_PROVIDER = "Lp/v1v0;"

/**
 * The Free/custom-host library provider delegates to an empty model. Route it to Spotify's
 * regular car feed loader (the `uw0.n(4)` provider) and fall back to an empty model if that
 * loader is unavailable, so the Library tab can list playable items instead of nothing.
 */
@Suppress("unused")
val carLibraryFeedPatch = bytecodePatch(
    name = "Use the car library feed",
    description = "Routes the car Library provider to Spotify's regular car feed loader with an " +
        "empty fallback, so the Library tab can show playable items on Free/custom hosts.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val owner = mutableClassDefBy(LIBRARY_PROVIDER)
        val method = owner.methods.singleOrNull { candidate ->
            candidate.name == "b" && candidate.implementation != null &&
                candidate.returnType == "Lio/reactivex/rxjava3/core/Single;"
        } ?: throw IllegalStateException("Spotify car library provider changed")

        method.addInstructions(
            0,
            """
                iget-object v0, p0, Lp/v1v0;->b:Lp/uw0;
                const/4 v1, 0x4
                invoke-virtual {v0, v1}, Lp/uw0;->n(I)Lp/rhh0;
                move-result-object v0
                if-nez v0, :empty_library
                invoke-interface {v0, p1}, Lp/rhh0;->b(Lp/r9c;)Lio/reactivex/rxjava3/core/Single;
                move-result-object v0
                return-object v0
                :empty_library
                new-instance v0, Lp/z310;
                sget-object v1, Lp/z6x;->a:Lp/z6x;
                invoke-direct {v0, v1}, Lp/z310;-><init>(Ljava/util/List;)V
                invoke-static {v0}, Lio/reactivex/rxjava3/core/Single;->just(Ljava/lang/Object;)Lio/reactivex/rxjava3/core/Single;
                move-result-object v0
                return-object v0
            """,
        )
    }
}
