package patches.nexuslauncher

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val SHIM = "Lapp/morphe/extension/nexuslauncher/HiddenShim;"
private const val INITIALIZER = "Lcom/android/quickstep/QuickstepProcessInitializer;"

/**
 * QuickstepProcessInitializer.init runs before any shimmed call site and reads
 * ThreadedRenderer.EGL_CONTEXT_PRIORITY_HIGH_IMG, a Pixel/OEM framework addition that AOSP
 * images do not have, so the launcher dies with NoSuchFieldError before creating any UI. The
 * rest of the method is performance and debug plumbing (setContextPriority, ProtoLog.init,
 * SurfaceControl.setDebugUsageAfterRelease, ViewRootImpl.setDebugWrongThreadInit), so the
 * initialiser is skipped and the hidden-API exemption is installed in its place.
 */
@Suppress("unused")
val earlyHiddenApiExemption = bytecodePatch(
    name = "Experimental: skip the Pixel-only process initialiser",
    description = "QuickstepProcessInitializer.init reads ThreadedRenderer." +
        "EGL_CONTEXT_PRIORITY_HIGH_IMG and calls setContextPriority, ProtoLog.init, " +
        "SurfaceControl.setDebugUsageAfterRelease and ViewRootImpl.setDebugWrongThreadInit. " +
        "Those are Pixel/OEM framework additions absent from AOSP images and affect only " +
        "performance and debug output, so the initialiser is skipped (the hidden-API " +
        "exemption is installed instead). Without this the launcher dies with " +
        "NoSuchFieldError before any UI exists." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    dependsOn(originalInput, hiddenApiShim)
    extendWith("extensions/nexuslauncher.mpe")

    execute {
        var patched = 0
        classDefForEach { classDef ->
            if (classDef.type != INITIALIZER) return@classDefForEach
            val mutable = mutableClassDefBy(classDef)
            for (method in mutable.methods) {
                if (method.name != "init") continue
                if (method.parameterTypes.joinToString("") != "Landroid/content/Context;") continue
                method.addInstructions(
                    0,
                    "invoke-static {}, $SHIM->ensureInitialized()V\nreturn-void",
                )
                patched++
            }
        }
        check(patched == 1) { "Expected one QuickstepProcessInitializer.init, patched $patched" }
    }
}
