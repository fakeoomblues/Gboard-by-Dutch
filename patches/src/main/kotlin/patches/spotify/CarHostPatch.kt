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
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import patches.shared.androidAutoHostVisibility

private const val CAR_APP_VALIDATOR = "Lp/ux60;"
private const val AUTO_MEDIA_CLIENT = "Lp/n64;"
private const val CAR_CLIENT_HELPER = "Lp/jo31;"
private const val CAR_ROOT_FILTER = "Lp/z54;"
private const val GEARHEAD = "com.google.android.projection.gearhead"
private const val STRING_EQUALS = "Lp/u890;->j(Ljava/lang/Object;Ljava/lang/Object;)Z"

/**
 * Spotify recognizes only Google's projection host and its own media-test app as car clients.
 * Three places implement that policy:
 *
 * - `n64.b(package)` advertises the Android Auto browser root only to those packages,
 * - `jo31.o(package)` is Spotify's shared "is a car client" predicate,
 * - `z54.a(root)` filters the Browse, Recently played and Your Library roots by that package,
 * - `ux60.b(host)` is the Car App Library host validator used by its bundled car service.
 *
 * Accept every caller in these checks; the host is still verified by its Binder UID where the
 * framework provides one.
 */
@Suppress("unused")
val spotifyCustomAndroidAutoHosts = bytecodePatch(
    name = "Allow custom Android Auto hosts",
    description = "Lets Spotify's car media browser and bundled car service accept a third-party " +
        "Android Auto host such as DriveDeck or Gearslip, including its Browse, Recently played " +
        "and Your Library roots. Spotify's account, catalogue and playback services are unchanged.",
    default = true,
) {
    compatibleWith(spotify)
    dependsOn(originalInput, androidAutoHostVisibility)

    execute {
        // Car App Library host validator: accepts a package/signature pair.
        mutableClassDefBy(CAR_APP_VALIDATOR).methods.singleOrNull { candidate ->
            candidate.name == "b" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Lp/tw60;")
        } ?: throw IllegalStateException("Spotify car host validator changed")
        mutableClassDefBy(CAR_APP_VALIDATOR).methods.single {
            it.name == "b" && it.returnType == "Z" &&
                it.parameterTypes.map(CharSequence::toString) == listOf("Lp/tw60;")
        }.returnEarly(true)

        // The Android Auto media root is offered to a caller for which this returns true.
        mutableClassDefBy(AUTO_MEDIA_CLIENT).methods.singleOrNull { candidate ->
            candidate.name == "b" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        } ?: throw IllegalStateException("Spotify Android Auto root filter changed")
        mutableClassDefBy(AUTO_MEDIA_CLIENT).methods.single {
            it.name == "b" && it.returnType == "Z" &&
                it.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        }.returnEarly(true)

        // Shared "is a car client" helper used by playback origin and root selection.
        mutableClassDefBy(CAR_CLIENT_HELPER).methods.singleOrNull { candidate ->
            candidate.name == "o" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        } ?: throw IllegalStateException("Spotify car client helper changed")
        mutableClassDefBy(CAR_CLIENT_HELPER).methods.single {
            it.name == "o" && it.returnType == "Z" &&
                it.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;")
        }.returnEarly(true)

        // Root filters compare the caller package with gearhead/mediatest before allowing the root.
        val rootFilter = mutableClassDefBy(CAR_ROOT_FILTER).methods.singleOrNull { candidate ->
            candidate.name == "a" && candidate.returnType == "Z" &&
                candidate.parameterTypes.map(CharSequence::toString) == listOf("Lp/r9c;")
        } ?: throw IllegalStateException("Spotify car root filter changed")
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

        // Every root filter already checks the root id first; only the caller test is relaxed.
        verdicts.forEach { index ->
            val register = (rootFilter.getInstruction(index) as OneRegisterInstruction).registerA
            rootFilter.replaceInstruction(index, "const/4 v$register, 0x1")
        }
    }
}
