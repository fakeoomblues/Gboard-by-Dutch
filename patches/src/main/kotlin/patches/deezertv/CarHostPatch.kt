package patches.deezertv

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import patches.shared.androidAutoHostVisibility

private const val MEDIA_BROWSER = "LIllllllIIlIlIll;"
private const val ADMISSION = "IlIllllIlIIlllll"
private const val ROOT = "LllIIIIll\$IIIllllIIlllll;"

/**
 * The TV media-browser service validates the caller against the package/signature
 * allow-list and then nulls the browser root when the caller is not trusted. Unlike
 * the phone build the root itself is host-agnostic (it comes from the media session
 * and carries the caller package as extras), so skipping the trust verdict is enough
 * to serve any Android Auto host.
 */
@Suppress("unused")
val deezerTvAllowAnyCarHost = bytecodePatch(
    name = "Allow any Android Auto host (TV)",
    description = "Lets the TV media browser accept any Android Auto host instead of only the " +
        "packages in Deezer's signature allow-list, while keeping the native media session and " +
        "browse tree. Adds package visibility for Android 11 and newer. Playback and account " +
        "services are unchanged.",
    default = true,
) {
    compatibleWith(deezerTv)
    dependsOn(originalInput, androidAutoHostVisibility)

    execute {
        val method = exact(
            MEDIA_BROWSER,
            ADMISSION,
            ROOT,
            "I",
            "Landroid/os/Bundle;",
            "Ljava/lang/String;",
        ).method
        val implementation = method.implementation
            ?: error("Deezer TV media-browser admission has no code")

        // The trust verdict is a boolean local. When it is false the compiler emits:
        //   if-eqz trust, :null
        //   goto :build
        //   :null const/4 root, 0
        //   :build if-eqz root, :no_root
        // Skipping the first branch keeps the tree returned by the media session and
        // builds the root for any caller.
        val instructions = implementation.instructions
        val verdict = (0 until instructions.size - 3).singleOrNull { index ->
            val branch = method.getInstruction(index) as? OneRegisterInstruction ?: return@singleOrNull false
            val clear = method.getInstruction(index + 2) as? OneRegisterInstruction ?: return@singleOrNull false
            val rootCheck = method.getInstruction(index + 3) as? OneRegisterInstruction ?: return@singleOrNull false
            branch.opcode == Opcode.IF_EQZ &&
                method.getInstruction(index + 1).opcode == Opcode.GOTO &&
                clear.opcode == Opcode.CONST_4 &&
                (method.getInstruction(index + 2) as NarrowLiteralInstruction).narrowLiteral == 0 &&
                rootCheck.opcode == Opcode.IF_EQZ &&
                clear.registerA == rootCheck.registerA
        } ?: error("Deezer TV media-browser trust verdict changed")

        method.replaceInstruction(verdict, "nop")
    }
}
