package patches.nexuslauncher

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val PREFS_BRIDGE = "Lapp/morphe/extension/nexuslauncher/PrefsBridge;"

/** Resource id of `dimen/config_wallpaperMaxScale`, an alias into the framework table. */
private const val WALLPAPER_MAX_SCALE_ID = 0x7f070131

private val getSharedPrefs = Fingerprint(
    definingClass = "Lcom/android/launcher3/LauncherPrefs;",
    name = "getSharedPrefs",
    returnType = "Landroid/content/SharedPreferences;",
    parameters = listOf("Lcom/android/launcher3/Item;"),
)

private val wallpaperScaleRead = Fingerprint(
    definingClass = "Lcom/android/launcher3/deviceprofile/DeviceProfileBuilder;",
    custom = { method, _ ->
        method.instructions.any { instruction ->
            instruction.opcode == Opcode.CONST &&
                (instruction as? NarrowLiteralInstruction)?.narrowLiteral == WALLPAPER_MAX_SCALE_ID
        }
    },
)

private val perDisplayGet = Fingerprint(
    definingClass = "Lcom/android/app/displaylib/PerDisplayInstanceRepositoryImpl;",
    name = "get",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("I"),
)

@Suppress("unused")
val runtimeCompatibility = bytecodePatch(
    name = "Experimental: runtime compatibility for a sideloaded launcher",
    description = "Three defects that only appear when the launcher runs as an ordinary app " +
        "instead of a privileged system app: a cold boot reads credential-encrypted " +
        "preferences before user 0 is unlocked (the process is killed), the wallpaper scale " +
        "is read through a framework resource id that only exists on Pixel, and the " +
        "per-display component gate rejects every display because the taskbar-owned display " +
        "surface never reports one." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    dependsOn(originalInput)
    extendWith("extensions/nexuslauncher.mpe")

    execute {
        // 1. Cold boot: swap the credential context for a device-protected one while locked.
        val prefs = getSharedPrefs.method
        val contextRead = prefs.instructions.withIndex().first { (_, instruction) ->
            val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            field?.name == "encryptedContext"
        }
        prefs.replaceInstruction(
            contextRead.index,
            "invoke-static {p0}, $PREFS_BRIDGE->context(Ljava/lang/Object;)Landroid/content/Context;",
        )
        prefs.addInstructions(contextRead.index + 1, "move-result-object p0")

        // 2. The wallpaper scale is an alias into the framework resource table, whose ids
        //    differ outside Pixel; use the documented default instead of reading it.
        val scaleMethod = wallpaperScaleRead.method
        val scaleIndex = scaleMethod.instructions.withIndex().single { (_, instruction) ->
            instruction.opcode == Opcode.CONST &&
                (instruction as? NarrowLiteralInstruction)?.narrowLiteral == WALLPAPER_MAX_SCALE_ID
        }.index
        // const vId, <dimen>; invoke-virtual {resources, vId}, getFloat(I)F; move-result vOut
        val scaleCall = scaleMethod.getInstruction(scaleIndex + 1) as? ReferenceInstruction
        check((scaleCall?.reference as? MethodReference)?.name == "getFloat") {
            "Wallpaper scale read changed shape"
        }
        check(scaleMethod.getInstruction(scaleIndex + 2).opcode == Opcode.MOVE_RESULT) {
            "Wallpaper scale result instruction changed shape"
        }
        val scaleOut = (scaleMethod.getInstruction(scaleIndex + 2) as OneRegisterInstruction).registerA
        scaleMethod.replaceInstruction(scaleIndex, "nop")
        scaleMethod.replaceInstruction(scaleIndex + 1, "nop")
        scaleMethod.replaceInstruction(scaleIndex + 2, "const/high16 v$scaleOut, 0x3f800000")
        val patchedScale = true
        check(patchedScale) { "Could not find the config_wallpaperMaxScale read" }

        // 3. Per-display components: the allowedDisplays set is fed by the window-manager
        //    display-decoration listener, which cannot register here, so every display is
        //    rejected and Dagger's provider throws "Required value was null". The display's
        //    existence is already proven above this check, which is what matters.
        val perDisplay = perDisplayGet.method
        val body = perDisplay.implementation!!
        // get() tests two sets: displayIds first, then allowedDisplays. Only the second one
        // rejects the display ("not allowed by lifecycle manager"), so pick the contains()
        // call whose failure path logs that message.
        val containsIndices = body.instructions.withIndex().filter { (_, instruction) ->
            val call = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            call?.name == "contains" && call.definingClass == "Ljava/util/Set;"
        }.map { it.index }
        check(containsIndices.isNotEmpty()) { "PerDisplayInstanceRepositoryImpl.get has no set test" }
        val allowedIndex = containsIndices.firstOrNull { start ->
            body.instructions.drop(start).take(24).any { instruction ->
                val text = instruction as? com.android.tools.smali.dexlib2.iface.reference.StringReference
                (text?.string ?: (instruction as? ReferenceInstruction)?.reference
                    ?.let { (it as? com.android.tools.smali.dexlib2.iface.reference.StringReference)?.string })
                    ?.contains("not allowed by lifecycle") == true
            }
        } ?: containsIndices.last()
        val resultIndex = allowedIndex + 1
        check(perDisplay.getInstruction(resultIndex).opcode == Opcode.MOVE_RESULT) {
            "PerDisplayInstanceRepositoryImpl.get no longer tests the allowed display set directly"
        }
        val resultRegister = (perDisplay.getInstruction(resultIndex) as OneRegisterInstruction).registerA
        perDisplay.addInstructions(resultIndex + 1, "const/4 v$resultRegister, 0x1")
    }
}
