package patches.nexuslauncher

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/**
 * Features that a privileged system install gets for free and a sideloaded copy cannot have.
 * Each one throws a SecurityException on a worker thread, and Android kills the whole process
 * when any thread dies uncaught, so a single refused service takes the launcher down. The
 * entries here disable only the privileged feature and leave everything else intact.
 *
 * The targets are located by scanning class definitions rather than through `Fingerprint`:
 * the overlay manager is a fixed, unobfuscated class in the release build, while fingerprint
 * matching has changed semantics between patcher versions (Morphe Manager's patcher 2.4.0
 * failed to match a method that is present in the APK).
 */
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
        // The overlay monitor would be created twice: from initOverlaySurface() and from the
        // runnable passed to the UI-helper executor. Disable both entry points, because a
        // stand-in InputMonitor hands back a released SurfaceControl and reparent() then kills
        // the process.
        val targets = listOf(
            "Lcom/android/launcher3/overlay/InputMonitorManager;" to "initOverlaySurface",
            "Lcom/android/launcher3/overlay/InputMonitorManager\$1;" to "run",
        )
        var patched = 0
        classDefForEach { classDef ->
            val wanted = targets.filter { it.first == classDef.type }.map { it.second }
            if (wanted.isEmpty()) return@classDefForEach
            val mutable = mutableClassDefBy(classDef)
            for (method in mutable.methods) {
                if (method.name in wanted && method.parameterTypes.isEmpty()) {
                    method.addInstructions(0, "return-void")
                    patched++
                }
            }
        }
        // Missing entry points only mean the overlay guard is not installed; never abort the
        // whole patch run for a launcher build that renames or drops them.
        if (patched < targets.size) {
            System.err.println(
                "nexuslauncher: overlay monitor entry points patched $patched of ${targets.size}",
            )
        }

        // Prediction is NOT disabled here: predictionToleratesDenial wraps the registration in
        // catch(SecurityException) so the loader survives while prediction still registers
        // wherever PACKAGE_USAGE_STATS is held.
    }
}
