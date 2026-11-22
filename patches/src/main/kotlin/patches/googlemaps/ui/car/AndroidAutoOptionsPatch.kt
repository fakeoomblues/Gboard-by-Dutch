package org.ungoogled.patches.maps.ui.car

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.ungoogled.patches.shared.Constants.COMPATIBILITY_MAPS

/** The navatar models Maps itself knows (`vps.p` in the 26.47 APK); any other name maps to "unknown". */
private val NAVATAR_MODELS = linkedMapOf(
    "Sedan" to "Navatar_Sedan",
    "SUV" to "Navatar_SUV",
    "City" to "Navatar_City",
    "Sportscar" to "Navatar_Sportscar",
    "Truck" to "Navatar_Truck",
    "Moped" to "Navatar_Moped",
    "Scooter" to "Navatar_Scooter",
    "Sportbike" to "Navatar_Sportbike",
    "Classic bike" to "Navatar_ClassicBike",
    "Light bike" to "Navatar_LightBike",
)

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
    val navatarKeepState = booleanOption(
        key = "navatarKeepState",
        default = false,
        title = "Keep your navatar in the car",
        description = "Maps only sends your navigation avatar to a car when it believes you chose it " +
            "yourself; a navatar that was enabled automatically is treated as not wanted, so the plain arrow " +
            "shows instead. Answer that check as \"chosen by me\" so the car keeps the avatar and style you " +
            "already have. Nothing is written to your Maps settings.",
    )

    val navatarModel = stringOption(
        key = "navatarModel",
        default = "keep",
        values = linkedMapOf("Maps decides" to "keep") + NAVATAR_MODELS,
        title = "Preselect a navatar",
        description = "Start with one of the built-in navigation avatars already chosen, instead of the " +
            "plain arrow the app uses until you pick one. The model still has to exist on your account, so " +
            "a model Maps has never offered simply has nothing to draw.",
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
        /**
         * Replace the result of one `bace` getter at every call site: insert [code] on the `move-result`
         * register and leave the call itself in place. Unlike [enableDefault] this works whether or not
         * the fallback literal sits next to the getter or in a hoisted, shared register.
         */
        fun overrideResult(field: String, getter: String, params: List<String>, code: (Int) -> String): Int {
            var changed = 0
            classDefForEach { classDef ->
                if (classDef.type.startsWith("Lorg/ungoogled/")) return@classDefForEach
                for (method in classDef.methods) {
                    val instructions = method.implementation?.instructions?.toList() ?: continue
                    val inserts = mutableListOf<Pair<Int, Int>>()
                    for (index in instructions.indices) {
                        val call = instructions[index] as? ReferenceInstruction ?: continue
                        if (call.opcode != Opcode.INVOKE_INTERFACE) continue
                        val reference = call.reference as? MethodReference ?: continue
                        if (reference.definingClass != "Lbabo;" || reference.name != getter) continue
                        if (reference.parameterTypes.map(CharSequence::toString) != params) continue
                        var owner: FieldReference? = null
                        for (back in 1..3) {
                            val candidate = instructions.getOrNull(index - back) ?: break
                            val read = candidate as? ReferenceInstruction ?: continue
                            if (read.opcode != Opcode.SGET_OBJECT) continue
                            owner = read.reference as? FieldReference
                            break
                        }
                        if (owner == null || owner.definingClass != "Lbace;" || owner.name != field) continue
                        val move = instructions.getOrNull(index + 1) ?: continue
                        val register = when (move.opcode) {
                            Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT -> (move as OneRegisterInstruction).registerA
                            else -> continue
                        }
                        inserts += (index + 1) to register
                    }
                    if (inserts.isEmpty()) continue
                    val mutable = mutableClassDefBy(classDef.type).methods.single {
                        it.name == method.name && it.returnType == method.returnType &&
                            it.parameterTypes.map(CharSequence::toString) ==
                            method.parameterTypes.map(CharSequence::toString)
                    }
                    for ((index, register) in inserts.asReversed()) {
                        mutable.addInstruction(index + 1, code(register))
                        changed++
                    }
                }
            }
            return changed
        }

        val preset = navatarModel.value
        if (preset != null && preset != "keep") {
            if (preset !in NAVATAR_MODELS.values) throw PatchException("Unknown navatar model: $preset")
            val names = overrideResult("it", "u", listOf("Lbaca;", "Ljava/lang/String;")) { register ->
                "const-string v$register, \"$preset\""
            }
            if (names == 0) throw PatchException("navatar model has no read site any more")
            val optIn = overrideResult("is", "c", listOf("Lbabx;", "I")) { register ->
                "const/4 v$register, 0x1"
            }
            if (optIn == 0) throw PatchException("navatar opt-in has no read site any more")
            println("Android Auto options: navatar preset $preset at $names read sites (custom chevron opted in at $optIn)")
        }

        if (navatarKeepState.value == true) {
            // `vpg.e`/`vok.e` feeds the car an empty navatar model when `u(mode)` says the navatar was
            // auto-enabled rather than chosen. Making that guard answer false returns the saved model
            // name and colour instead; the stored navatar_enabled_state is left untouched.
            val guard = listOf("Lvot;" to "Lckmg;", "Lvok;" to "Lckbl;").firstNotNullOfOrNull { (owner, mode) ->
                runCatching {
                    mutableClassDefBy(owner).methods.single {
                        it.name == "u" && it.parameterTypes.map(CharSequence::toString) == listOf(mode) &&
                            it.returnType == "Z"
                    }
                }.getOrNull()?.let { owner to it }
            } ?: throw PatchException("the Android Auto navatar guard has no known read site")
            guard.second.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
            println("Android Auto options: navatar guard answers no at ${guard.first} (car keeps its avatar)")
        }
    }
}
