package org.ungoogled.patches.maps.ui.car

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.ungoogled.patches.shared.Constants.COMPATIBILITY_MAPS

/**
 * Car-side features that Google ships switched off and does not expose anywhere in the UI.
 *
 * Every one of them is a boolean setting read as `settings.V(<flag>, false)`, so the build
 * compiles to `sget-object <flag>` / `const/4 0` / `invoke-interface V(flag, Z)Z`. Turning the
 * option on replaces that literal zero with one at each read site; nothing else in the method
 * changes, and a user who has already chosen a value keeps it (the literal is only the fallback).
 *
 * These are convenience switches, not new capabilities: satellite imagery still needs the
 * server tiles the app already requests, and none of this touches Maps' entitlement checks.
 */
@Suppress("unused")
val androidAutoOptionsPatch = bytecodePatch(
    name = "Android Auto options",
    description = "Exposes car-side settings Google ships switched off: satellite view in the car " +
        "and restoring satellite view on startup. Both are ordinary settings, so a value you set in " +
        "Maps yourself still wins.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_MAPS)

    val satelliteInCar = booleanOption(
        key = "carSatellite",
        default = false,
        title = "Satellite view in the car",
        description = "Show satellite imagery on Maps' car map (Android Auto and other hosts), instead of " +
            "leaving it off until the car screen offers its own layer control.",
    )
    val satelliteOnStartup = booleanOption(
        key = "satelliteOnStartup",
        default = false,
        title = "Restore satellite view on startup",
        description = "Start with satellite imagery when the last session used it, instead of starting on the " +
            "plain map every time.",
    )

    execute {
        /**
         * Flip the literal default of one `bace` boolean at every `V(flag, false)` read site.
         * Returns how many sites changed.
         */
        fun enableDefault(field: String): Int {
            var changed = 0
            classDefForEach { classDef ->
                if (classDef.type.startsWith("Lorg/ungoogled/")) return@classDefForEach
                for (method in classDef.methods) {
                    val instructions = method.implementation?.instructions?.toList() ?: continue
                    val sites = mutableListOf<Int>()
                    for (i in 1 until instructions.size - 1) {
                        val read = instructions[i - 1] as? ReferenceInstruction ?: continue
                        if (read.opcode != Opcode.SGET_OBJECT) continue
                        val flag = read.reference as? FieldReference ?: continue
                        if (flag.definingClass != "Lbace;" || flag.name != field) continue
                        val zero = instructions[i]
                        if (zero.opcode != Opcode.CONST_4) continue
                        if ((zero as NarrowLiteralInstruction).narrowLiteral != 0) continue
                        val call = instructions[i + 1] as? ReferenceInstruction ?: continue
                        if (call.opcode != Opcode.INVOKE_INTERFACE) continue
                        val getter = call.reference as? MethodReference ?: continue
                        if (getter.definingClass != "Lbabo;" || getter.name != "V") continue
                        if (getter.parameterTypes.map(CharSequence::toString) != listOf("Lbabv;", "Z")) continue
                        sites += i
                    }
                    if (sites.isEmpty()) continue
                    val mutable = mutableClassDefBy(classDef.type).methods.single {
                        it.name == method.name && it.returnType == method.returnType &&
                            it.parameterTypes.map(CharSequence::toString) ==
                            method.parameterTypes.map(CharSequence::toString)
                    }
                    for (index in sites.asReversed()) {
                        val register = (instructions[index] as OneRegisterInstruction).registerA
                        mutable.replaceInstruction(index, "const/4 v$register, 0x1")
                        changed++
                    }
                }
            }
            return changed
        }

        if (satelliteInCar.value == true) {
            val sites = enableDefault("fW") // car_satellite
            if (sites == 0) throw PatchException("car satellite setting has no read site any more")
            println("Android Auto options: car_satellite default flipped at $sites read sites")
        }
        if (satelliteOnStartup.value == true) {
            val sites = enableDefault("N") // satellite_on_at_startup
            if (sites == 0) throw PatchException("satellite-on-startup setting has no read site any more")
            println("Android Auto options: satellite_on_at_startup default flipped at $sites read sites")
        }
    }
}
