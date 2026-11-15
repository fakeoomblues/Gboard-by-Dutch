package patches.nexuslauncher

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * Build 17 reports only element 0 (the hotseat) for the All Apps state, because it draws all
 * apps in a separate "all apps surface" window owned by the taskbar process. A sideloaded
 * launcher cannot own that window, so the in-window panel stays GONE and the drawer never
 * opens. The first 17-907 release reported 131 (hotseat | apps view | indicator); restoring
 * that value makes the launcher's own state machinery animate the in-window drawer.
 */
private val allAppsVisibleElements = Fingerprint(
    definingClass = "Lcom/android/launcher3/uioverrides/states/AllAppsState;",
    name = "getVisibleElements",
    returnType = "I",
    parameters = listOf("Lcom/android/launcher3/LauncherUiState;"),
)

@Suppress("unused")
val inWindowAppDrawer = bytecodePatch(
    name = "Experimental: draw the app drawer in the launcher window",
    description = "Restores the element mask used by the first 17-907 release so the all-apps " +
        "panel is shown inside the launcher activity instead of the taskbar-owned all-apps " +
        "surface, which an sideloaded launcher cannot obtain. Without it a swipe reaches the " +
        "All Apps state but nothing is drawn." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    dependsOn(originalInput)

    execute {
        val method = allAppsVisibleElements.method
        val first = method.getInstruction(0)
        check(first is NarrowLiteralInstruction && first.narrowLiteral == 1) {
            "AllAppsState.getVisibleElements no longer reports hotseat-only (element 1)"
        }
        check((first as OneRegisterInstruction).registerA == 0) { "Unexpected result register" }

        method.replaceInstruction(0, "const/16 v0, 0x83")
    }
}
