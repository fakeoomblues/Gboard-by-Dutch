package patches.deezer

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import patches.shared.androidAutoHostVisibility

private const val CAR_SERVICE = "LIIlIIIIlllIlIlI;"
private const val ROOT = "LlIlIlllllIlllIl;"
private const val ROOT_METHOD = "lllIlIIIlllIIII"
private val GOTO = setOf(Opcode.GOTO, Opcode.GOTO_16, Opcode.GOTO_32)

/**
 * Deezer's media-browser service verifies the caller with the same package/signature allow-list
 * pattern as TIDAL: `zBooleanValue` is false for a host it does not know, and the browser root is
 * then replaced by null. The root lookup below that verdict is host-agnostic, so only the verdict
 * branch needs to be skipped.
 */
@Suppress("unused")
val deezerCustomAndroidAutoHosts = bytecodePatch(
    name = "Allow custom Android Auto hosts",
    description = "Lets Deezer's car media browser accept a third-party Android Auto host such as " +
        "DriveDeck or Gearslip. Deezer's account, catalogue and playback services are unchanged.",
    default = true,
) {
    compatibleWith(deezer)
    dependsOn(originalInput, androidAutoHostVisibility)

    execute {
        val method = mutableClassDefBy(CAR_SERVICE).methods.singleOrNull { candidate ->
            candidate.name == ROOT_METHOD && candidate.returnType == ROOT &&
                candidate.parameterTypes.map(CharSequence::toString) ==
                listOf("I", "Landroid/os/Bundle;", "Ljava/lang/String;")
        } ?: throw IllegalStateException("Deezer car browser admission method changed")

        val implementation = method.implementation
            ?: throw IllegalStateException("Deezer car browser admission has no code")
        val instructions = implementation.instructions
        val thisRegister = implementation.registerCount - 4
        val last = instructions.size - 3

        // `if (!accepted) root = null` is compiled as a conditional branch over a GOTO into a
        // move-object of p0, which the compiler already overwrote with zero at that point.
        val verdicts = (0..last).filter { index ->
            val branch = method.getInstruction(index)
            if (branch.opcode != Opcode.IF_EQZ) return@filter false
            val target = (branch as? BuilderOffsetInstruction)?.target?.location?.index
                ?: return@filter false
            if (target != index + 2) return@filter false
            if (method.getInstruction(index + 1).opcode !in GOTO) return@filter false
            val nullStore = method.getInstruction(index + 2)
            nullStore.opcode in setOf(Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_16, Opcode.MOVE_OBJECT_FROM16) &&
                (nullStore as TwoRegisterInstruction).registerB == thisRegister
        }
        check(verdicts.size == 1) { "Deezer car host verdict changed" }

        // Fall through to the existing GOTO that keeps the browser root.
        method.replaceInstruction(verdicts.single(), "nop")
    }
}
