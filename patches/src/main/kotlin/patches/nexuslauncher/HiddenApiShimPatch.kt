package patches.nexuslauncher

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.security.MessageDigest

private const val SHIM = "Lapp/morphe/extension/nexuslauncher/HiddenShim;"

/** Diagnostics switch: `-DnexRewriteFields=false` keeps field access untouched. */
private val GET_FIELDS = setOf(
    Opcode.IGET, Opcode.IGET_OBJECT, Opcode.IGET_BOOLEAN,
    Opcode.IGET_BYTE, Opcode.IGET_CHAR, Opcode.IGET_SHORT, Opcode.IGET_WIDE,
)
private val SET_FIELDS = setOf(
    Opcode.IPUT, Opcode.IPUT_OBJECT, Opcode.IPUT_BOOLEAN,
    Opcode.IPUT_BYTE, Opcode.IPUT_CHAR, Opcode.IPUT_SHORT, Opcode.IPUT_WIDE,
)
private val GET_STATIC_FIELDS = setOf(
    Opcode.SGET, Opcode.SGET_OBJECT, Opcode.SGET_BOOLEAN,
    Opcode.SGET_BYTE, Opcode.SGET_CHAR, Opcode.SGET_SHORT, Opcode.SGET_WIDE,
)
private val SET_STATIC_FIELDS = setOf(
    Opcode.SPUT, Opcode.SPUT_OBJECT, Opcode.SPUT_BOOLEAN,
    Opcode.SPUT_BYTE, Opcode.SPUT_CHAR, Opcode.SPUT_SHORT, Opcode.SPUT_WIDE,
)

private val REWRITE_FIELDS = System.getProperty("nexRewriteFields", "true").toBoolean()

private class Member(val name: String, val tag: String) {
    val instanceMethod = "$name\$$tag"
    val staticMethod = "$name\$$tag\$s"
    val instanceGetter = "get_$name\$$tag\$i"
    val staticGetter = "get_$name\$$tag"
    val instanceSetter = "set_$name\$$tag\$i"
    val staticSetter = "set_$name\$$tag"
}

private fun tagOf(classDescriptor: String): String {
    val normalised = classDescriptor.removePrefix("L").removeSuffix(";").replace('/', '.').replace('$', '.')
    val digest = MessageDigest.getInstance("MD5").digest(normalised.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }.take(8)
}

/** Reference element types widen to Object; primitive arrays keep their exact descriptor. */
private fun mapType(type: String): String =
    if (type.trimStart('[').startsWith("L")) "Ljava/lang/Object;" else type

private fun mapDescriptor(descriptor: String): String {
    val close = descriptor.indexOf(')')
    if (!descriptor.startsWith("(") || close < 0) return mapType(descriptor)
    val out = StringBuilder("(")
    var i = 1
    while (i < close) {
        val start = i
        while (descriptor[i] == '[') i++
        i = if (descriptor[i] == 'L') descriptor.indexOf(';', i) + 1 else i + 1
        out.append(mapType(descriptor.substring(start, i)))
    }
    return out.append(')').append(mapType(descriptor.substring(close + 1))).toString()
}

/** Instance shims take the receiver as their first parameter; static shims do not. */
private fun shimDescriptor(descriptor: String, static: Boolean): String {
    val mapped = mapDescriptor(descriptor)
    if (static) return mapped
    val close = mapped.indexOf(')')
    return "(" + "Ljava/lang/Object;" + mapped.substring(1, close) + ")" + mapped.substring(close + 1)
}

private fun methodDescriptor(reference: MethodReference): String =
    buildString {
        append('(')
        reference.parameterTypes.forEach { append(it) }
        append(')').append(reference.returnType)
    }

private val methodIndex = HashMap<String, Member>()
private val fieldIndex = HashMap<String, Member>()

private fun methodKey(cls: String, name: String, descriptor: String) = "$cls|$name|$descriptor"
private fun fieldKey(cls: String, name: String, type: String) = "$cls|$name|$type"

private fun loadTable() {
    if (methodIndex.isNotEmpty() || fieldIndex.isNotEmpty()) return
    for (entry in nexLauncherHiddenMembers) {
        val arrow = entry.indexOf(";->")
        val classDescriptor = entry.substring(0, arrow + 1)
        val rest = entry.substring(arrow + 3)
        val tag = tagOf(classDescriptor)
        if (rest.contains('(')) {
            val paren = rest.indexOf('(')
            val name = rest.substring(0, paren)
            if (name == "<init>") continue
            methodIndex[methodKey(classDescriptor, name, rest.substring(paren))] = Member(name, tag)
        } else {
            val colon = rest.indexOf(':')
            fieldIndex[fieldKey(classDescriptor, rest.substring(0, colon), rest.substring(colon + 1))] =
                Member(rest.substring(0, colon), tag)
        }
    }
}

private fun registers(instruction: Instruction): String = when (instruction) {
    is RegisterRangeInstruction ->
        "{v${instruction.startRegister} .. v${instruction.startRegister + instruction.registerCount - 1}}"
    is FiveRegisterInstruction -> {
        val used = listOf(
            instruction.registerC, instruction.registerD, instruction.registerE,
            instruction.registerF, instruction.registerG,
        ).take(instruction.registerCount)
        "{" + used.joinToString(", ") { "v$it" } + "}"
    }
    else -> throw IllegalStateException("Unexpected invoke shape ${instruction.opcode}")
}

private fun moveResultFor(type: String): String = when {
    type == "J" || type == "D" -> "move-result-wide"
    mapType(type) == "Ljava/lang/Object;" -> "move-result-object"
    else -> "move-result"
}

/**
 * Rewrites every reference to an audited hidden member into a call on HiddenShim, which
 * resolves the member reflectively with a hidden-API exemption and returns a type-appropriate
 * default when the member is absent. Registers are reused unchanged for invokes because DEX
 * already passes the receiver first.
 */
@Suppress("unused")
val hiddenApiShim = bytecodePatch(
    name = "Experimental: resolve hidden framework members through a reflective shim",
    description = "The launcher is a privileged system app on Pixel and links against 680 " +
        "framework members that a sideloaded copy is denied or that do not exist off Pixel. " +
        "Every reference is rewritten to HiddenShim in the nexuslauncher extension. Without " +
        "it the process dies on the first denied link, before any UI is created." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    // Flag reads are handled first so their Pixel-only objects never reach the table pass.
    dependsOn(originalInput, desktopModeFlags)
    extendWith("extensions/nexuslauncher.mpe")

    execute {
        loadTable()
        check(methodIndex.size + fieldIndex.size > 600) {
            "Hidden member table did not parse (${methodIndex.size} methods, ${fieldIndex.size} fields)"
        }

        var rewritten = 0
        var seenInvokes = 0
        var skippedHighRegister = 0
        val samples = ArrayList<String>()
        classDefForEach { classDef ->
            val mutable = mutableClassDefBy(classDef)
            for (method in mutable.methods) {
                val body = method.implementation ?: continue
                var index = 0
                while (index < body.instructions.size) {
                    val instruction = method.getInstruction(index)
                    var advance = 1

                    if (instruction.opcode == Opcode.INVOKE_VIRTUAL ||
                        instruction.opcode == Opcode.INVOKE_INTERFACE ||
                        instruction.opcode == Opcode.INVOKE_STATIC
                    ) {
                        val reference = (instruction as ReferenceInstruction).reference as MethodReference
                        val static = instruction.opcode == Opcode.INVOKE_STATIC
                        seenInvokes++
                        if (samples.size < 3) {
                            samples.add(methodKey(reference.definingClass, reference.name, methodDescriptor(reference)))
                        }
                        val member = methodIndex[methodKey(
                            reference.definingClass, reference.name, methodDescriptor(reference),
                        )]
                        if (member != null) {
                            val name = if (static) member.staticMethod else member.instanceMethod
                            val descriptor = shimDescriptor(methodDescriptor(reference), static)
                            val opcode = if (instruction is RegisterRangeInstruction) {
                                "invoke-static/range"
                            } else {
                                "invoke-static"
                            }
                            method.replaceInstruction(
                                index,
                                "$opcode ${registers(instruction)}, $SHIM->$name$descriptor",
                            )
                            // A reference return comes back as Object; cast it back so the
                            // verifier still sees the original type at the use sites.
                            val returnType = reference.returnType
                            if (mapType(returnType) == "Ljava/lang/Object;" &&
                                index + 1 < body.instructions.size &&
                                method.getInstruction(index + 1).opcode == Opcode.MOVE_RESULT_OBJECT
                            ) {
                                val result = (method.getInstruction(index + 1) as OneRegisterInstruction).registerA
                                method.addInstructions(index + 2, "check-cast v$result, $returnType")
                                advance = 3
                            }
                            rewritten++
                        }
                    } else if (REWRITE_FIELDS && instruction.opcode in GET_FIELDS) {
                        val reference = (instruction as ReferenceInstruction).reference as FieldReference
                        val member = fieldIndex[fieldKey(
                            reference.definingClass, reference.name, reference.type,
                        )]
                        if (member != null) {
                            val target = instruction as TwoRegisterInstruction
                            val value = target.registerA
                            val objectRegister = target.registerB
                            if (maxOf(value, objectRegister) > 15) {
                                skippedHighRegister++
                                index += advance
                                continue
                            }
                            try {
                                method.replaceInstruction(
                                    index,
                                    "invoke-static {v$objectRegister}, $SHIM->${member.instanceGetter}" +
                                        "(Ljava/lang/Object;)${mapType(reference.type)}",
                                )
                            } catch (t: Throwable) {
                                throw IllegalStateException("iget ${reference.definingClass}->${reference.name}: ${reference.type} in ${method.definingClass}->${method.name}", t)
                            }
                            method.addInstructions(index + 1, "${moveResultFor(reference.type)} v$value")
                            advance = 2
                            if (mapType(reference.type) == "Ljava/lang/Object;") {
                                method.addInstructions(index + 2, "check-cast v$value, ${reference.type}")
                                advance = 3
                            }
                            rewritten++
                        }
                    } else if (REWRITE_FIELDS && instruction.opcode in SET_FIELDS) {
                        val reference = (instruction as ReferenceInstruction).reference as FieldReference
                        val member = fieldIndex[fieldKey(
                            reference.definingClass, reference.name, reference.type,
                        )]
                        if (member != null) {
                            val target = instruction as TwoRegisterInstruction
                            if (maxOf(target.registerA, target.registerB) > 15) {
                                skippedHighRegister++
                                index += advance
                                continue
                            }
                            val valueRegs = if (reference.type == "J" || reference.type == "D") {
                                "v${target.registerA}, v${target.registerA + 1}"
                            } else {
                                "v${target.registerA}"
                            }
                            val text = "invoke-static {v${target.registerB}, $valueRegs}, $SHIM->" +
                                "${member.instanceSetter}(Ljava/lang/Object;${mapType(reference.type)})V"
                            try {
                                method.replaceInstruction(index, text)
                            } catch (t: Throwable) {
                                throw IllegalStateException(
                                    "iput ${reference.definingClass}->${reference.name}: ${reference.type} " +
                                        "in ${method.definingClass}->${method.name} smali=[$text]",
                                    t,
                                )
                            }
                            rewritten++
                        }
                    } else if (REWRITE_FIELDS && instruction.opcode in GET_STATIC_FIELDS) {
                        val reference = (instruction as ReferenceInstruction).reference as FieldReference
                        val member = fieldIndex[fieldKey(
                            reference.definingClass, reference.name, reference.type,
                        )]
                        if (member != null) {
                            val value = (instruction as OneRegisterInstruction).registerA
                            if (value > 15) {
                                skippedHighRegister++
                                index += advance
                                continue
                            }
                            method.replaceInstruction(
                                index,
                                "invoke-static {}, $SHIM->${member.staticGetter}" +
                                    "()${mapType(reference.type)}",
                            )
                            method.addInstructions(index + 1, "${moveResultFor(reference.type)} v$value")
                            advance = 2
                            if (mapType(reference.type) == "Ljava/lang/Object;") {
                                method.addInstructions(index + 2, "check-cast v$value, ${reference.type}")
                                advance = 3
                            }
                            rewritten++
                        }
                    } else if (REWRITE_FIELDS && instruction.opcode in SET_STATIC_FIELDS) {
                        val reference = (instruction as ReferenceInstruction).reference as FieldReference
                        val member = fieldIndex[fieldKey(
                            reference.definingClass, reference.name, reference.type,
                        )]
                        if (member != null) {
                            val value = (instruction as OneRegisterInstruction).registerA
                            if (value > 15) {
                                skippedHighRegister++
                                index += advance
                                continue
                            }
                            val valueRegs = if (reference.type == "J" || reference.type == "D") {
                                "v$value, v${value + 1}"
                            } else {
                                "v$value"
                            }
                            method.replaceInstruction(
                                index,
                                "invoke-static {$valueRegs}, $SHIM->${member.staticSetter}" +
                                    "(${mapType(reference.type)})V",
                            )
                            rewritten++
                        }
                    }
                    index += advance
                }
            }
        }
        System.err.println("seen invokes: $seenInvokes, skipped high-register field sites: $skippedHighRegister")
        samples.forEach { System.err.println("  sample: $it") }
        System.err.println("table: ${methodIndex.size} methods, ${fieldIndex.size} fields")
        methodIndex.keys.take(2).forEach { System.err.println("  key: $it") }
        check(rewritten > 600) { "Only $rewritten hidden member references were rewritten" }
        System.err.println("nexuslauncher: rewrote $rewritten hidden references")
    }
}

