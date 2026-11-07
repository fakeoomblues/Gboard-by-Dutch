package patches.deezer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.shared.uniqueMatch

/** Resolve exactly one method with the given owner, name, return type and parameters. */
context(_: BytecodePatchContext)
internal fun exact(owner: String, name: String, returnType: String, vararg parameters: String) =
    Fingerprint(
        definingClass = owner,
        name = name,
        returnType = returnType,
        parameters = parameters.toList(),
    ).uniqueMatch()

/**
 * Let an activity's normal super.onCreate run, then close it before it can render a
 * subscription screen. Keeping the super call preserves the activity lifecycle
 * Android already started.
 */
context(_: BytecodePatchContext)
internal fun closeAfterSuperOnCreate(owner: String) {
    val method = exact(owner, "onCreate", "V", "Landroid/os/Bundle;").method
    val superCall = method.instructions.indexOfFirst { instruction ->
        if (instruction.opcode != Opcode.INVOKE_SUPER && instruction.opcode != Opcode.INVOKE_SUPER_RANGE) {
            return@indexOfFirst false
        }
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        reference?.name == "onCreate" &&
            reference.returnType == "V" &&
            reference.parameterTypes.map(CharSequence::toString) == listOf("Landroid/os/Bundle;")
    }
    check(superCall >= 0) { "$owner has no super.onCreate call" }
    method.addInstructions(
        superCall + 1,
        """
            invoke-virtual/range {p0 .. p0}, $owner->finish()V
            return-void
        """,
    )
}
