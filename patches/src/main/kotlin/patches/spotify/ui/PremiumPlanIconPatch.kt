package patches.spotify.ui

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val PLAN_LOGO = "Lp/cxm;"
private const val LIGHT_SPOTIFY_LOGO = 0x7f080a73
private const val PREMIUM_LOGO = 0x7f080bee

/**
 * Uses Spotify's bundled premium logo for the account/plan row instead of the white Spotify glyph.
 * Only the drawable id in the plan-logo renderer changes; no account or playback state is read.
 */
@Suppress("unused")
val premiumPlanIconPatch = bytecodePatch(
    name = "Use the Premium plan icon",
    description = "Shows the bundled premium logo on the account/plan row instead of the white " +
        "Spotify glyph. Label-only change; the account tier and entitlements are untouched.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        var replacements = 0
        val owner = mutableClassDefBy(PLAN_LOGO)
        owner.methods.filter { it.implementation != null }.forEach { method ->
            method.implementation!!.instructions.toList().forEachIndexed { index, instruction ->
                if (instruction !is NarrowLiteralInstruction) return@forEachIndexed
                if (instruction.narrowLiteral.toInt() != LIGHT_SPOTIFY_LOGO) return@forEachIndexed
                val register = (instruction as OneRegisterInstruction).registerA
                method.replaceInstruction(index, "const v$register, $PREMIUM_LOGO")
                replacements++
            }
        }
        check(replacements > 0) { "Spotify plan logo resource changed" }
    }
}
