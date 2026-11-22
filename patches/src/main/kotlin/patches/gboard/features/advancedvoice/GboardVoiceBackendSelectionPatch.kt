package patches.gboard.features.advancedvoice

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import patches.gboard.Constants.COMPATIBILITY_GBOARD
import patches.gboard.features.rambler.rambler1853WrapperBody
import patches.gboard.shared.GboardMethodTarget
import patches.gboard.shared.addHelperMethodIfMissing
import patches.gboard.shared.findMutableMethodOrThrow
import patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import patches.gboard.shared.mutableClass

private const val VOICE = "Lcom/google/android/apps/inputmethod/latin/preference/VoiceSettingsFragment;"
private const val SELECTOR =
    "Lcom/google/android/libraries/inputmethod/preferencewidgets/CustomSelectorWithWidgetPreference;"

/**
 * Makes Gboard's Voice typing screen treat the agentic backend as official.
 *
 * The 18.6.3 selector patch wrapped four VoiceSettingsFragment methods and a capability check so
 * Rambler appeared next to Standard. Three of those members survive in 18.8 under new names; the
 * capability class does not, so only the four wrappers are installed:
 *
 *  - `aE()I`, `ab()V` and `e(Bundle)V` run inside the official-selection scope,
 *  - `aG(Z, ahws, selector, selector)V` additionally reports the built selector list, which is what
 *    puts the extra backend entry on screen.
 *
 * Without this, forcing the NGA flags leaves the Voice typing list unchanged.
 */
internal val gboardVoiceBackendSelectionPatch = bytecodePatch(
    description = "Treat the agentic voice backend as official in Gboard's Voice typing screen.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val targets = listOf(
            GboardMethodTarget(VOICE, "aE", emptyList(), "I"),
            GboardMethodTarget(VOICE, "ab", emptyList(), "V"),
            GboardMethodTarget(VOICE, "e", listOf("Landroid/os/Bundle;"), "V"),
            GboardMethodTarget(VOICE, "aG", listOf("Z", "Lahws;", SELECTOR, SELECTOR), "V"),
        )
        val methods = targets.map { it.resolve(this) }
        methods.forEachIndexed { index, method ->
            val owner = mutableClass(method.definingClass)
            val helper = "morpheVoiceBackendStock${method.name}"
            check(owner.methods.none { it.name == helper }) { "Duplicate voice-backend scope wrapper" }
            val static = method.accessFlags and AccessFlags.STATIC.value != 0
            check(static == (index == 3)) { "Unexpected static shape for ${method.name}" }
            val renamed = ImmutableMethod(
                method.definingClass,
                helper,
                method.parameters,
                method.returnType,
                AccessFlags.PRIVATE.value or (if (static) AccessFlags.STATIC.value else 0),
                method.annotations,
                method.hiddenApiRestrictions,
                method.implementation,
            ).toMutable()
            check(owner.methods.remove(method) && owner.methods.add(renamed))
            val words = method.parameterTypes.size + if (static) 0 else 1
            owner.addHelperMethodIfMissing(
                method.name,
                method.parameterTypes.map { it.toString() },
                method.returnType,
                method.accessFlags,
                words + 2,
                rambler1853WrapperBody(
                    "${method.definingClass}->$helper${targets[index].descriptor}",
                    static,
                    words,
                    method.returnType,
                    index,
                ),
            )
        }
    }
}
