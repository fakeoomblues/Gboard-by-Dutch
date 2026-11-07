package patches.spotify.ui

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val ACCOUNT_ROW = "Lp/soi;"
private const val PLAN_OVERVIEW = "Lp/t1t0;"
private const val PREMIUM_LABEL = 0x7f132207
private val FREE_PLAN_LABELS = setOf(0x7f132800, 0x7f132802, 0x7f132852)

/**
 * Changes only the label the account/plan rows show for a Free account. The account product,
 * entitlements and server calls are untouched; the row now reads "Premium" instead of
 * "Free account"/"Free plan".
 */
@Suppress("unused")
val accountShowsPremiumPatch = bytecodePatch(
    name = "Show Premium in the account row",
    description = "Displays Premium instead of Free account/Free plan on the account and plan rows. " +
        "This is a label change only; the account tier and every entitlement stay unchanged.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        var replacements = 0
        listOf(ACCOUNT_ROW, PLAN_OVERVIEW).forEach { type ->
            val owner = mutableClassDefByOrNull(type) ?: return@forEach
            owner.methods.filter { it.implementation != null }.forEach { method ->
                val instructions = method.implementation!!.instructions.toList()
                instructions.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.CONST && instruction.opcode != Opcode.CONST_4 &&
                        instruction.opcode != Opcode.CONST_16 && instruction.opcode != Opcode.CONST_HIGH16
                    ) return@forEachIndexed
                    if (instruction !is NarrowLiteralInstruction) return@forEachIndexed
                    if (instruction.narrowLiteral.toInt() !in FREE_PLAN_LABELS) return@forEachIndexed
                    val register = (instruction as OneRegisterInstruction).registerA
                    method.replaceInstruction(index, "const v$register, $PREMIUM_LABEL")
                    replacements++
                }
            }
        }
        check(replacements >= 2) { "Spotify free plan labels changed" }
    }
}
