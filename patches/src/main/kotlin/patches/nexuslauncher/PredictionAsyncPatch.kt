package patches.nexuslauncher

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val BRIDGE = "Lapp/morphe/extension/nexuslauncher/PredictorBridge;"

/**
 * QuickstepModelDelegate.registerHotseatPredictor calls PredictorState.registerPredictor on the
 * model-loader thread. That call creates a prediction session, which requires
 * PACKAGE_USAGE_STATS: on a sideloaded install the service throws SecurityException, the
 * launcher-loader thread dies, and the All Apps list renders empty (2 labels instead of 26).
 *
 * The call site is redirected to PredictorBridge, which runs the same registration on a
 * background thread and swallows the failure, so the loader finishes and the drawer stays
 * populated. Where the permission is available the session is still created.
 */
@Suppress("unused")
val predictionToleratesDenial = bytecodePatch(
    name = "Experimental: keep the app list when prediction is denied",
    description = "Moves the hotseat prediction registration off the model-loader thread and " +
        "makes a denied prediction session harmless. The session needs PACKAGE_USAGE_STATS, " +
        "which a sideloaded app cannot hold; left inline it kills launcher-loader and the All " +
        "Apps list stays empty. Prediction still registers wherever the permission is held." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    // On by default: without it a sideloaded launcher loses launcher-loader and the All Apps
    // list stays empty (measured: 2 labels instead of 26). It is harmless where the session
    // is permitted; it only moves registration off the loader thread and swallows denial.
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
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL &&
                        instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE
                    ) {
                        index++
                        continue
                    }
                    val reference = (instruction as ReferenceInstruction).reference as? MethodReference
                    if (reference?.name != "registerPredictor" ||
                        reference.definingClass != "Lcom/android/launcher3/model/PredictorState;"
                    ) {
                        index++
                        continue
                    }
                    val used = listOf(
                        (instruction as FiveRegisterInstruction).registerC,
                        instruction.registerD, instruction.registerE, instruction.registerF,
                        instruction.registerG,
                    ).take(instruction.registerCount)
                    check(used.size == 5) { "Unexpected register count ${used.size} at $index" }
                    method.replaceInstruction(
                        index,
                        "invoke-static {" + used.joinToString(", ") { "v$it" } + "}, $BRIDGE->" +
                            "registerAsync(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;" +
                            "Ljava/lang/Object;Ljava/lang/Object;)V",
                    )
                    rewritten++
                    index++
                }
            }
        }
        check(rewritten >= 1) { "No registerPredictor call sites were rewritten" }
        System.err.println("nexuslauncher: $rewritten prediction registrations moved off the loader thread")
    }
}
