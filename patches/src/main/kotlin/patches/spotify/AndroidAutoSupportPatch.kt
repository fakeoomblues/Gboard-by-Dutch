package patches.spotify

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import patches.shared.androidAutoHostVisibility

private const val MEDIA_BROWSER_SERVICE_HANDLER = "Lp/n5b1;"
private const val CAR_APP_VALIDATOR = "Lp/ux60;"
private const val AUTO_MEDIA_CLIENT = "Lp/n64;"
private const val CAR_CLIENT_HELPER = "Lp/jo31;"
private const val CAR_ROOT_FILTER = "Lp/z54;"
private const val CAR_ROOT_WRAPPER = "Lp/w6o0;"
private const val LIBRARY_PROVIDER = "Lp/v1v0;"
private const val GEARHEAD = "com.google.android.projection.gearhead"
private const val STRING_EQUALS = "Lp/u890;->j(Ljava/lang/Object;Ljava/lang/Object;)Z"

/**
 * One patch for Spotify's Android Auto support:
 *
 * - the media-browser admission check (`n5b1.d`) accepts any host;
 * - the bundled Car App Library validator (`ux60.b`), Android Auto root selector (`n64.b`),
 *   shared car-client predicate (`jo31.o`) and Browse/Recently played/Library root filters
 *   (`z54.a`) accept a custom host;
 * - the Free/custom-host car providers (`w6o0`, `v1v0`) use the real Home and Library feeds;
 *
 * Spotify's account, catalogue and playback services are unchanged.
 */
@Suppress("unused")
val spotifyAndroidAutoSupport = bytecodePatch(
    name = "Android Auto support",
    description = "Makes Spotify's car media browser, Home and Library work with " +
        "DriveDeck, Gearslip or another compatible Android Auto host, including the bundled " +
        "car service. Spotify's account, catalogue and playback services are unchanged.",
    default = true,
) {
    compatibleWith(spotify)
    dependsOn(originalInput, androidAutoHostVisibility)

    execute {
        // Media-browser admission: accepts any host package/UID.
        mutableClassDefBy(MEDIA_BROWSER_SERVICE_HANDLER).methods.single { candidate ->
            candidate.name == "d" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) ==
                listOf("I", "Ljava/lang/String;")
        }.returnEarly(true)

        // Car App Library host validator.
        mutableClassDefBy(CAR_APP_VALIDATOR).methods.single { candidate ->
            candidate.name == "b" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Lp/tw60;")
        }.returnEarly(true)

        // The Android Auto media root is offered to a caller for which this returns true.
        mutableClassDefBy(AUTO_MEDIA_CLIENT).methods.single { candidate ->
            candidate.name == "b" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        }.returnEarly(true)

        // Shared "is a car client" helper used by playback origin and root selection.
        mutableClassDefBy(CAR_CLIENT_HELPER).methods.single { candidate ->
            candidate.name == "o" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        }.returnEarly(true)

        // Root filters compare the caller package with gearhead/mediatest before allowing the root.
        val rootFilter = mutableClassDefBy(CAR_ROOT_FILTER).methods.single { candidate ->
            candidate.name == "a" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Lp/r9c;")
        }
        val size = rootFilter.implementation!!.instructions.size
        val gearhead = (0 until size).firstOrNull { index ->
            ((rootFilter.getInstruction(index) as? ReferenceInstruction)?.reference as? StringReference)
                ?.string == GEARHEAD
        } ?: throw IllegalStateException("Spotify car root gearhead constant changed")
        val gearheadRegister = (rootFilter.getInstruction(gearhead) as OneRegisterInstruction).registerA

        val verdicts = (gearhead + 1 until size - 1).filter { index ->
            val instruction = rootFilter.getInstruction(index)
            instruction.opcode == Opcode.INVOKE_STATIC &&
                instruction.getReference<MethodReference>()?.toString() == STRING_EQUALS &&
                (instruction as FiveRegisterInstruction).registerD == gearheadRegister &&
                rootFilter.getInstruction(index + 1).opcode == Opcode.MOVE_RESULT
        }.map { it + 1 }
        check(verdicts.size == 3) { "Spotify car root filters changed" }

        verdicts.forEach { index ->
            val register = (rootFilter.getInstruction(index) as OneRegisterInstruction).registerA
            rootFilter.replaceInstruction(index, "const/4 v$register, 0x1")
        }

        // Free/custom-host Home wrapper: use the real Home loader on both Single and Observable paths.
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

        // Library provider: use the regular car feed loader, with an empty model only if absent.
        val library = mutableClassDefBy(LIBRARY_PROVIDER).methods.single { candidate ->
            candidate.name == "b" && candidate.implementation != null &&
                candidate.returnType == "Lio/reactivex/rxjava3/core/Single;"
        }
        library.addInstructions(
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
