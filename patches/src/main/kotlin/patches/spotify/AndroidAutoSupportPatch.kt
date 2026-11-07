package patches.spotify

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import patches.shared.androidAutoHostVisibility

private const val MEDIA_BROWSER_SERVICE_HANDLER = "Lp/j4a1;"
private const val CAR_APP_VALIDATOR = "Lp/di60;"
private const val AUTO_MEDIA_ROOT_PROVIDER = "Lp/y34;"
private const val CAR_CLIENT_HELPER = "Lp/r8p0;"
private const val CAR_ROOT_FILTER = "Lp/k34;"
private const val CAR_ROOT_WRAPPER = "Lp/bjn0;"
private const val CAR_REQUEST = "Lp/z4c;"
private const val GEARHEAD = "com.google.android.projection.gearhead"
private const val STRING_EQUALS = "Lp/xr80;->j(Ljava/lang/Object;Ljava/lang/Object;)Z"

/**
 * One patch for Spotify's Android Auto support:
 *
 * - the media-browser admission check (`j4a1.d`) accepts any host;
 * - the bundled Car App Library host validator (`di60.b`), Android Auto root provider (`y34.b`),
 *   shared car-client predicate (`r8p0.n`) and Browse/Recently played/Library root filters
 *   (`k34.a`) accept a custom host;
 * - the car Home wrapper (`bjn0`) uses the real Home feed for a custom host;
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
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Lp/bh60;")
        }.returnEarly(true)

        // The Android Auto media root is offered to a caller for which this returns true.
        mutableClassDefBy(AUTO_MEDIA_ROOT_PROVIDER).methods.single { candidate ->
            candidate.name == "b" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        }.returnEarly(true)

        // Shared "is a car client" helper used by playback origin and root selection.
        mutableClassDefBy(CAR_CLIENT_HELPER).methods.single { candidate ->
            candidate.name == "n" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        }.returnEarly(true)

        // Root filters compare the caller package with gearhead/mediatest before allowing the root.
        // The gearhead constant lives in a register and each root compares against it; force those
        // three verdicts to true and keep the Samsung-only branch untouched.
        val rootFilter = mutableClassDefBy(CAR_ROOT_FILTER).methods.single { candidate ->
            candidate.name == "a" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf(CAR_REQUEST)
        }
        val instructions = rootFilter.implementation!!.instructions.toList()
        val gearheadRegister = instructions.firstOrNull { instruction ->
            instruction.opcode == Opcode.CONST_STRING &&
                ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string == GEARHEAD
        }?.let { (it as OneRegisterInstruction).registerA }
            ?: throw IllegalStateException("Spotify car root gearhead constant changed")

        val verdicts = instructions.indices.filter { index ->
            val instruction = instructions[index]
            if (instruction.opcode != Opcode.INVOKE_STATIC &&
                instruction.opcode != Opcode.INVOKE_STATIC_RANGE
            ) return@filter false
            val reference = instruction.getReference<MethodReference>() ?: return@filter false
            val secondArgument = when (instruction) {
                is FiveRegisterInstruction -> instruction.registerD
                is RegisterRangeInstruction -> instruction.startRegister + 1
                else -> return@filter false
            }
            reference.toString() == STRING_EQUALS && secondArgument == gearheadRegister &&
                index + 1 < instructions.size &&
                instructions[index + 1].opcode == Opcode.MOVE_RESULT
        }
        check(verdicts.size == 3) { "Spotify car root filters changed" }

        verdicts.forEach { index ->
            val register = (instructions[index + 1] as OneRegisterInstruction).registerA
            rootFilter.replaceInstruction(index + 1, "const/4 v$register, 0x1")
        }

        // Car Home wrapper: use the real Home loader instead of the fallback for both the Single
        // and Observable paths.
        val root = mutableClassDefBy(CAR_ROOT_WRAPPER)
        var patched = 0
        root.methods.filter { it.implementation != null }.forEach { method ->
            val body = method.implementation!!.instructions.toList()
            body.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.IGET_BOOLEAN) return@forEachIndexed
                val field = instruction.getReference<FieldReference>() ?: return@forEachIndexed
                if (field.definingClass != CAR_REQUEST || field.name != "g" || field.type != "Z") {
                    return@forEachIndexed
                }
                if (index + 1 >= body.size || body[index + 1].opcode != Opcode.IF_EQZ) {
                    return@forEachIndexed
                }
                method.replaceInstruction(index + 1, "nop")
                patched++
            }
        }
        check(patched >= 2) { "Spotify car root fallback changed" }
    }
}
