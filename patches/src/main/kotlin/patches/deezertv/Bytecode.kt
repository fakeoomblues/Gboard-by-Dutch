package patches.deezertv

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

/** Index of the activity's super.onCreate(Bundle) call. */
context(_: BytecodePatchContext)
internal fun superOnCreateIndex(owner: String): Int {
    val method = exact(owner, "onCreate", "V", "Landroid/os/Bundle;").method
    val index = method.instructions.indexOfFirst { instruction ->
        if (instruction.opcode != Opcode.INVOKE_SUPER && instruction.opcode != Opcode.INVOKE_SUPER_RANGE) {
            return@indexOfFirst false
        }
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        reference?.name == "onCreate" &&
            reference.returnType == "V" &&
            reference.parameterTypes.map(CharSequence::toString) == listOf("Landroid/os/Bundle;")
    }
    check(index >= 0) { "$owner has no super.onCreate call" }
    return index
}

/**
 * Let an activity's normal super.onCreate run, then close it before it can render a
 * subscription screen. Keeping the super call preserves the activity lifecycle
 * Android already started.
 */
context(_: BytecodePatchContext)
internal fun closeAfterSuperOnCreate(owner: String) {
    val method = exact(owner, "onCreate", "V", "Landroid/os/Bundle;").method
    method.addInstructions(
        superOnCreateIndex(owner) + 1,
        """
            invoke-virtual/range {p0 .. p0}, $owner->finish()V
            return-void
        """,
    )
}

/**
 * Run the activity's normal super.onCreate, then start the leanback home surface and
 * finish the subscription activity. Used where the subscription activity can be the
 * task root (for example at startup), where a plain finish would drop to the launcher.
 */
context(_: BytecodePatchContext)
internal fun redirectAfterSuperOnCreate(owner: String, target: String) {
    val method = exact(owner, "onCreate", "V", "Landroid/os/Bundle;").method
    method.addInstructions(
        superOnCreateIndex(owner) + 1,
        """
            new-instance p1, Landroid/content/Intent;
            const-class p2, $target
            invoke-direct {p1, p0, p2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
            const/high16 p2, 0x14000000
            invoke-virtual {p1, p2}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
            invoke-virtual {p0, p1}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
            invoke-virtual {p0}, $owner->finish()V
            return-void
        """,
    )
}
