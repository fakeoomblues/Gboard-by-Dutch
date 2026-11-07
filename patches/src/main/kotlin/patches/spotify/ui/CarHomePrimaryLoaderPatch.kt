package patches.spotify.ui

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val CAR_ROOT_WRAPPER = "Lp/w6o0;"

/**
 * `w6o0` chooses between the real car Home loader and an empty fallback based on `r9c.g`.
 * Free/custom-host requests set that flag false and get the empty fallback, which is why Home
 * appears empty. Skip that branch so both Single and Observable loading use the primary loader.
 */
@Suppress("unused")
val carHomePrimaryLoaderPatch = bytecodePatch(
    name = "Use the car Home loader",
    description = "Stops the car root wrapper from falling back to the empty provider on Free or " +
        "custom hosts, so the Home/related feed uses its primary loader.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val owner = mutableClassDefBy(CAR_ROOT_WRAPPER)
        var patched = 0
        owner.methods.filter { it.implementation != null }.forEach { method ->
            val instructions = method.implementation!!.instructions.toList()
            instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.IGET_BOOLEAN) return@forEachIndexed
                val field = instruction.getReference<FieldReference>() ?: return@forEachIndexed
                if (field.definingClass != "Lp/r9c;" || field.name != "g" || field.type != "Z") return@forEachIndexed
                if (index + 1 >= instructions.size) return@forEachIndexed
                if (instructions[index + 1].opcode != Opcode.IF_EQZ) return@forEachIndexed
                method.replaceInstruction(index + 1, "nop")
                patched++
            }
        }
        check(patched >= 2) { "Spotify car root fallback changed" }
    }
}
