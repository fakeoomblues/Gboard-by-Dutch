package patches.gboard

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import dev.jason.gboardpatches.patches.gboard.shared.GboardFlagFamilyFeature
import dev.jason.gboardpatches.patches.gboard.shared.gboardFlagFamilyFeaturePatch

// Gboard logs its original class and method names; these anchor the obfuscated extension.
private const val AGENTIC_EXTENSION_CLASS =
    "com/google/android/apps/inputmethod/libs/agenticdictation/extension/AgenticDictationExtension"
private const val ON_CREATE_EXTENSION = "onCreateExtension"

private val ramblerFlags = gboardFlagFamilyFeaturePatch(
    description = "Supply the agentic-dictation flags a re-signed Gboard never receives.",
    feature = GboardFlagFamilyFeature.RAMBLER_CLOUD,
)

/**
 * The first time Gboard's Rambler extension starts with agentic dictation enabled, it stores
 * "Rambler selected" unless a choice is already saved. Store "Standard" instead, so Parakeet (or
 * Google's standard recognizer) stays the default and Rambler is only used once the user picks it.
 */
private val ramblerStandardDefault = bytecodePatch(
    description = "Keep Standard voice typing as the default until the user picks Rambler.",
) {
    compatibleWith(gboard)

    execute {
        val method = onCreateExtension()
        val index = firstRunRamblerDefaultIndex(method.implementation?.instructions?.toList().orEmpty())
        val register = (method.implementation!!.instructions.elementAt(index) as OneRegisterInstruction).registerA
        val mutable = mutableClassDefBy(method.definingClass).methods.single {
            it.name == method.name && it.parameterTypes == method.parameterTypes && it.returnType == method.returnType
        }
        mutable.replaceInstruction(index, "const/4 v$register, 0x0")
    }
}

context(context: BytecodePatchContext)
private fun onCreateExtension(): Method {
    val matches = context.classDefByStrings(AGENTIC_EXTENSION_CLASS, StringComparisonType.EQUALS).flatMap { owner ->
        owner.methods.filter { method ->
            method.parameterTypes.isEmpty() && method.returnType == "V" &&
                method.implementation?.instructions?.any { instruction ->
                    ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string == ON_CREATE_EXTENSION
                } == true
        }
    }
    return matches.singleOrNull()
        ?: throw PatchException("Expected one Rambler onCreateExtension method, found ${matches.size}")
}

/**
 * Finds the `const/4 vX, 1` that feeds `prefs.put(key, vX)` right after `prefs.contains(key)` on
 * the same preferences object and key register. That contains-then-put pair on one key appears
 * exactly once in onCreateExtension: it is the first-run Rambler default.
 */
internal fun firstRunRamblerDefaultIndex(instructions: List<Instruction>): Int {
    val candidates = instructions.indices.filter { putIndex ->
        val put = instructions[putIndex]
        val putRef = (put as? ReferenceInstruction)?.reference as? MethodReference ?: return@filter false
        if (put.opcode != Opcode.INVOKE_VIRTUAL || putRef.returnType != "V" ||
            putRef.parameterTypes.map(CharSequence::toString) != listOf("I", "Z")
        ) return@filter false
        put as FiveRegisterInstruction
        if (put.registerCount != 3) return@filter false
        val literal = instructions.getOrNull(putIndex - 1)
        if (literal?.opcode != Opcode.CONST_4 || (literal as OneRegisterInstruction).registerA != put.registerE ||
            (literal as NarrowLiteralInstruction).narrowLiteral != 1
        ) return@filter false
        instructions.subList(0, putIndex).any { check ->
            val checkRef = (check as? ReferenceInstruction)?.reference as? MethodReference
            check.opcode == Opcode.INVOKE_VIRTUAL && checkRef != null && checkRef.returnType == "Z" &&
                checkRef.parameterTypes.map(CharSequence::toString) == listOf("I") &&
                (check as FiveRegisterInstruction).registerCount == 2 &&
                check.registerC == put.registerC && check.registerD == put.registerD
        }
    }
    return candidates.singleOrNull()?.minus(1)
        ?: throw PatchException("Expected one first-run Rambler default in onCreateExtension, found ${candidates.size}")
}

@Suppress("unused")
val ramblerVoiceTyping = bytecodePatch(
    name = "Rambler voice typing",
    description = "Adds Google's Rambler to Gboard settings → Voice typing, as a choice next to Standard. Rambler sends " +
        "your speech to Google's servers, which turn it into finished text and carry out spoken edits. Standard stays " +
        "the default and keeps using Parakeet voice typing when that patch is applied.",
    default = false,
) {
    compatibleWith(gboard)
    dependsOn(originalInput, ramblerFlags, ramblerStandardDefault)
}
