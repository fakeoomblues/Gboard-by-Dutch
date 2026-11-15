package patches.nexuslauncher

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FRAMEWORK_SHIM = "Lapp/morphe/extension/nexuslauncher/FrameworkShim;"

private val FLAG_CLASSES = setOf(
    "Landroid/window/DesktopExperienceFlags;",
    "Landroid/window/DesktopModeFlags;",
)

/**
 * The desktop-mode feature flags are Pixel framework additions: the launcher reads a static
 * flag object and calls isTrue() on it, which fails with NoSuchFieldError on images that lack
 * them (ENABLE_DESKTOP_FIRST_BASED_DEFAULT_TO_DESKTOP_BUGFIX in build 17). The flag object is
 * replaced by its name and resolved through FrameworkShim, which returns the real value when
 * the framework has the flag and false otherwise.
 */
@Suppress("unused")
val desktopModeFlags = bytecodePatch(
    name = "Experimental: resolve Pixel desktop-mode flags through the extension",
    description = "Rewrites every read of a DesktopExperienceFlags/DesktopModeFlags flag into a " +
        "call on FrameworkShim.desktopFlag(name), which resolves the flag reflectively and " +
        "returns false when the image does not define it. Without this the launcher dies with " +
        "NoSuchFieldError on AOSP-based images." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    dependsOn(originalInput)
    extendWith("extensions/nexuslauncher.mpe")

    execute {
        var rewritten = 0
        classDefForEach { classDef ->
            val mutable = mutableClassDefBy(classDef)
            for (method in mutable.methods) {
                val body = method.implementation ?: continue
                var index = 0
                while (index < body.instructions.size) {
                    val instruction = method.getInstruction(index)
                    if (instruction.opcode != Opcode.SGET_OBJECT) {
                        index++
                        continue
                    }
                    val field = (instruction as ReferenceInstruction).reference as? FieldReference
                    if (field == null || field.definingClass !in FLAG_CLASSES) {
                        index++
                        continue
                    }
                    // sget-object vF, L…Flags;->NAME:L…Flags;
                    // invoke-virtual {vF}, L…Flags;->isTrue()Z
                    // move-result vD
                    val next = method.getInstruction(index + 1)
                    val call = (next as? ReferenceInstruction)?.reference as? MethodReference
                    if (call == null || call.returnType != "Z" || call.parameterTypes.isNotEmpty()) {
                        index++
                        continue
                    }
                    val result = (method.getInstruction(index + 2) as OneRegisterInstruction).registerA
                    method.replaceInstruction(index, "const-string v$result, \"${field.name}\"")
                    method.replaceInstruction(
                        index + 1,
                        "invoke-static {v$result}, $FRAMEWORK_SHIM->desktopFlag(Ljava/lang/String;)Z",
                    )
                    method.replaceInstruction(index + 2, "move-result v$result")
                    rewritten++
                    index += 3
                }
            }
        }
        check(rewritten > 10) { "Expected the desktop-mode flag reads, rewrote $rewritten" }
        System.err.println("nexuslauncher: rewrote $rewritten desktop-mode flag reads")
    }
}
