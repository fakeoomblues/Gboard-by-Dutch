package patches.nexuslauncher

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Features that a privileged system install gets for free and a sideloaded copy cannot have.
 * Each one throws a SecurityException on a worker thread, and Android kills the whole process
 * when any thread dies uncaught, so a single refused service takes the launcher down. The
 * entries here disable only the privileged feature and leave everything else intact.
 */
private val initOverlaySurface = Fingerprint(
    definingClass = "Lcom/android/launcher3/overlay/InputMonitorManager;",
    name = "initOverlaySurface",
    returnType = "V",
    parameters = emptyList(),
)

private val overlayMonitorRunnable = Fingerprint(
    definingClass = "Lcom/android/launcher3/overlay/InputMonitorManager\$1;",
    name = "run",
    returnType = "V",
    parameters = emptyList(),
)

private val registerPredictor = Fingerprint(
    definingClass = "Lcom/android/launcher3/model/PredictorState;",
    name = "registerPredictor",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Landroid/app/prediction/AppPredictionContext;",
        "Lcom/android/launcher3/LauncherModel;",
        "Lfd/e;",
    ),
)

@Suppress("unused")
val privilegedFeatureFallbacks = bytecodePatch(
    name = "Experimental: disable privileged-only overlay and prediction features",
    description = "The input-monitor overlay needs MONITOR_INPUT and the app-prediction " +
        "session needs PACKAGE_USAGE_STATS, both of which only a privileged install can " +
        "hold. Each call throws SecurityException from a worker thread, which terminates the " +
        "launcher process. Skipping them keeps the home screen, app list and launching " +
        "working; only the task overlay and the predicted-apps row are unavailable." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    dependsOn(originalInput)

    execute {
        // The overlay monitor would be created twice: from initOverlaySurface() and from this
        // runnable. Disable both entry points, because a stand-in InputMonitor hands back a
        // released SurfaceControl and reparent() then kills the process.
        initOverlaySurface.method.addInstructions(0, "return-void")
        overlayMonitorRunnable.method.addInstructions(0, "return-void")

        // Prediction is NOT disabled here: predictionToleratesDenial wraps the registration in
        // catch(SecurityException) so the loader survives while prediction still registers
        // wherever PACKAGE_USAGE_STATS is held.
    }
}
