package patches.spotify.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val CAR_ROOT_WRAPPER = "Lp/w6o0;"
private const val LIBRARY_PROVIDER = "Lp/v1v0;"

/**
 * Two Free/custom-host car providers return empty when the host is not Google's:
 *
 * - `w6o0` chooses between the real car Home loader and an empty fallback based on `r9c.g`.
 * - `v1v0` delegates the Library tab to an empty model.
 *
 * Use the primary Home loader and route Library to Spotify's regular car feed loader, with an
 * empty model only if that loader is absent.
 */
@Suppress("unused")
val carFeedProvidersPatch = bytecodePatch(
    name = "Use the car Home and Library feeds",
    description = "Uses Spotify's real car Home loader and car feed provider for Free/custom " +
        "hosts, so the Home and Library tabs show playable items instead of empty fallbacks.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val root = mutableClassDefBy(CAR_ROOT_WRAPPER)
        var patched = 0
        root.methods.filter { it.implementation != null }.forEach { method ->
            val instructions = method.implementation!!.instructions.toList()
            instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.IGET_BOOLEAN) return@forEachIndexed
                val field = instruction.getReference<FieldReference>() ?: return@forEachIndexed
                if (field.definingClass != "Lp/r9c;" || field.name != "g" || field.type != "Z") return@forEachIndexed
                if (index + 1 >= instructions.size) return@forEachIndexed
                if (instructions[index + 1].opcode != Opcode.IF_EQZ) return@forEachIndexed
                method.replaceInstruction(index + 1, "nop")
                patched++
            }
        }
        check(patched >= 2) { "Spotify car root fallback changed" }

        val library = mutableClassDefBy(LIBRARY_PROVIDER)
        val feed = library.methods.singleOrNull { candidate ->
            candidate.name == "b" && candidate.implementation != null &&
                candidate.returnType == "Lio/reactivex/rxjava3/core/Single;"
        } ?: throw IllegalStateException("Spotify car library provider changed")

        feed.addInstructions(
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
