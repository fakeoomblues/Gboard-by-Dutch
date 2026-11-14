package patches.messages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/** The trace name of the one method that inspects the phone's displays. */
private const val DISPLAY_CHECK = "ScreenDetectionChecker#isScreenCaptureProbablyActiveWithoutLogging"

private fun Method.hasString(value: String) = implementation?.instructions?.any {
    ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == value
} == true

/**
 * Messages treats every extra display it cannot vouch for as screen sharing, unless Google's own
 * Android Auto or TalkBack owns it. A car host such as DriveDeck projects onto exactly such a
 * display, so Messages hides message text in its notifications (Android 14 and older) and can warn
 * that "someone has access to your screen".
 *
 * Every caller of the detector goes through its one display check, which now always reports that
 * no screen is being shared. The detector's flag, logging and dialogs are otherwise unchanged.
 */
@Suppress("unused")
val screenShareWarningPatch = bytecodePatch(
    name = "Disable screen-share warning",
    description = "Experimental. Stops Messages treating a car host's screen as screen sharing, so it does not hide message " +
        "text in notifications or warn that someone has access to your screen while a host such as DriveDeck is connected. " +
        "This also turns the warning off for real screen sharing in this copy of Messages.",
    default = true,
) {
    compatibleWith(messages)
    dependsOn(originalInput)

    execute {
        val owners = mutableListOf<ClassDef>()
        classDefForEach { cls -> if (cls.methods.any { it.hasString(DISPLAY_CHECK) }) owners += cls }
        val owner = mutableClassDefBy(owners.singleOrNull()
            ?: throw PatchException("Expected one Messages screen-share detector, found ${owners.size}"))
        val original = owner.methods.filter { it.hasString(DISPLAY_CHECK) }.singleOrNull()
            ?: throw PatchException("Messages screen-share display check is missing")
        val result = original.returnType
        if (original.parameterTypes.isNotEmpty() || AccessFlags.STATIC.isSet(original.accessFlags) || !result.startsWith("L")) {
            throw PatchException("Messages screen-share display check changed shape")
        }
        val constructor = "$result-><init>(ZLjava/lang/String;)V"
        if (original.implementation!!.instructions.none { (it as? ReferenceInstruction)?.reference?.toString() == constructor }) {
            throw PatchException("Messages screen-share result changed shape")
        }

        val replacement = ImmutableMethod(original.definingClass, original.name, original.parameters,
            original.returnType, original.accessFlags, original.annotations, original.hiddenApiRestrictions,
            MutableMethodImplementation(3)).toMutable().apply {
            addInstructions(0, """
                new-instance v0, $result
                const/4 v1, 0x0
                const/4 v2, 0x0
                invoke-direct {v0, v1, v2}, $constructor
                return-object v0
            """)
        }
        check(owner.methods.remove(original)) { "Messages screen-share display check disappeared" }
        owner.methods.add(replacement)
    }
}
