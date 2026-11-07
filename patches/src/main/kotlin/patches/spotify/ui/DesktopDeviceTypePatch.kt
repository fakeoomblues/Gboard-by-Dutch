package patches.spotify.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val DEVICE_INFO_BUILDER = "Lp/j1l;"
private const val CORE_SCOPE_BUILDER = "Lp/cpm;"
private const val SET_DEVICE_TYPE = "Lcom/spotify/connectivity/MobileDeviceInfo;->setDeviceType(I)V"
private const val SET_CORE_DEVICE_TYPE = "Lcom/spotify/core/corefullimpl/FullAuthenticatedScopeConfiguration;->setDeviceType(I)V"

/**
 * Reports the phone as Spotify's COMPUTER device type (enum ordinal 1) instead of SMARTPHONE (3).
 * Spotify's mobile client is what applies the Free shuffle/on-demand restriction; the desktop/web
 * client type is the one that allows normal play order. This changes only the advertised device
 * type, not the account product.
 */
@Suppress("unused")
val desktopDeviceTypePatch = bytecodePatch(
    name = "Report a desktop device",
    description = "Reports Spotify Connect as a COMPUTER device instead of SMARTPHONE. This is the " +
        "device type the web/desktop client uses to avoid the Free mobile shuffle restriction. " +
        "Playback, ads and account entitlement are otherwise unchanged.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        var patched = 0
        for (type in listOf(DEVICE_INFO_BUILDER, CORE_SCOPE_BUILDER)) {
            val owner = mutableClassDefByOrNull(type) ?: continue
            owner.methods.filter { it.implementation != null }.forEach { method ->
                val index = method.implementation!!.instructions.withIndex().firstOrNull { (_, instruction) ->
                    instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                        instruction.getReference<MethodReference>()?.toString() in setOf(SET_DEVICE_TYPE, SET_CORE_DEVICE_TYPE)
                }?.index ?: return@forEach
                val call = method.getInstruction(index) as FiveRegisterInstruction
                method.addInstructions(index, "const/4 v${call.registerD}, 0x1")
                patched++
            }
        }
        check(patched > 0) { "Spotify device type setter changed" }
    }
}
