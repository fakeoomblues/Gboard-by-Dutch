package patches.gboard

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import dev.jason.gboardpatches.patches.gboard.shared.GboardFlagFamilyFeature
import dev.jason.gboardpatches.patches.gboard.shared.gboardFlagFamilyFeaturePatch
import org.w3c.dom.Document
import org.w3c.dom.Element

// Rambler features Gboard 18.6.3 ships switched off behind server flags. Each one is its own
// experimental patch: nothing here has been confirmed on a phone yet.

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
private const val RAMBLER_SETTINGS = "res/xml/setting_rambler_voice_typing.xml"

/** The preference key Gboard reads (R.string pref_key_rambler_custom_instruction). */
internal const val CUSTOM_INSTRUCTION_KEY = "pref_key_rambler_custom_instruction"

private val customInstructionTexts = mapOf(
    "values" to listOf(
        "Custom instructions",
        "Style rules Rambler applies to everything you dictate, such as spelling, punctuation or tone",
        "For example: Write Dutch with Dutch quotation marks. Use je, not u.",
    ),
    "values-nl" to listOf(
        "Eigen instructies",
        "Stijlregels die Rambler toepast op alles wat je dicteert, zoals spelling, interpunctie of toon",
        "Bijvoorbeeld: Schrijf Nederlands met Nederlandse aanhalingstekens. Gebruik je, niet u.",
    ),
)
private val customInstructionStringNames = listOf(
    "morphe_rambler_custom_instruction_title",
    "morphe_rambler_custom_instruction_summary",
    "morphe_rambler_custom_instruction_hint",
)

/**
 * Adds a text field for Gboard's own custom-instruction preference as the first entry of the
 * Rambler voice typing screen. Gboard stores and reads the value itself; the field only writes it.
 */
internal fun addCustomInstructionPreference(screen: Document) {
    val root = screen.documentElement
    val nodes = screen.getElementsByTagName("*")
    val elements = (0 until nodes.length).map { nodes.item(it) as Element }
    if (elements.any { it.getAttributeNS(ANDROID_NS, "key") == CUSTOM_INSTRUCTION_KEY }) {
        throw PatchException("Rambler custom instructions are already in the settings screen")
    }
    val category = (0 until root.childNodes.length).map(root.childNodes::item).filterIsInstance<Element>()
        .firstOrNull { it.tagName.endsWith("PreferenceCategory") }
        ?: throw PatchException("Rambler voice typing settings have no preference category")
    val field = screen.createElement("EditTextPreference").apply {
        setAttributeNS(ANDROID_NS, "android:key", CUSTOM_INSTRUCTION_KEY)
        setAttributeNS(ANDROID_NS, "android:persistent", "true")
        setAttributeNS(ANDROID_NS, "android:defaultValue", "")
        setAttributeNS(ANDROID_NS, "android:title", "@string/${customInstructionStringNames[0]}")
        setAttributeNS(ANDROID_NS, "android:summary", "@string/${customInstructionStringNames[1]}")
        setAttributeNS(ANDROID_NS, "android:dialogTitle", "@string/${customInstructionStringNames[0]}")
        setAttributeNS(ANDROID_NS, "android:dialogMessage", "@string/${customInstructionStringNames[2]}")
    }
    category.insertBefore(field, category.firstChild)
}

private val customInstructionSettings = resourcePatch(
    description = "Add a custom instructions field to Gboard settings → Voice typing → Rambler.",
) {
    compatibleWith(gboard)

    execute {
        for ((folder, texts) in customInstructionTexts) {
            val path = "res/$folder/strings.xml"
            if (folder != "values" && !get(path).isFile) continue
            document(path).use { strings ->
                for ((name, value) in customInstructionStringNames.zip(texts)) {
                    strings.documentElement.appendChild(strings.createElement("string").apply {
                        setAttribute("name", name)
                        textContent = value
                    })
                }
            }
        }
        if (!get(RAMBLER_SETTINGS).isFile) throw PatchException("$RAMBLER_SETTINGS not found")
        document(RAMBLER_SETTINGS).use(::addCustomInstructionPreference)
    }
}

@Suppress("unused")
val ramblerCustomInstructions = bytecodePatch(
    name = "Rambler custom instructions (experimental)",
    description = "Adds Custom instructions to Gboard settings → Voice typing → Rambler. Rambler receives that text " +
        "with every dictation and follows it for style only: spelling, punctuation, language conventions, " +
        "vocabulary and tone. Requires Rambler voice typing.",
    default = false,
) {
    compatibleWith(gboard)
    dependsOn(
        originalInput, ramblerVoiceTyping, customInstructionSettings,
        gboardFlagFamilyFeaturePatch(
            description = "Send Rambler's custom instructions with each dictation.",
            feature = GboardFlagFamilyFeature.RAMBLER_CUSTOM_INSTRUCTIONS,
        ),
    )
}

@Suppress("unused")
val ramblerCursorEditing = bytecodePatch(
    name = "Rambler cursor-aware editing (experimental)",
    description = "Rambler also sees the text after the cursor, so dictating in the middle of a text fits in. " +
        "With text selected, only the selection is sent, so a spoken edit such as \"make this more formal\" " +
        "applies to it, and the result is selected again for the next edit. Requires Rambler voice typing.",
    default = false,
) {
    compatibleWith(gboard)
    dependsOn(
        originalInput, ramblerVoiceTyping,
        gboardFlagFamilyFeaturePatch(
            description = "Send the text around the cursor and the selection to Rambler.",
            feature = GboardFlagFamilyFeature.RAMBLER_CURSOR_EDITING,
        ),
    )
}

@Suppress("unused")
val ramblerAppContext = bytecodePatch(
    name = "Rambler app context (experimental)",
    description = "Rambler also receives the name of the app you dictate in and the text field's hint, so it can " +
        "match the tone of a chat, an e-mail or a search. This tells Google which app you are typing in. " +
        "Requires Rambler voice typing.",
    default = false,
) {
    compatibleWith(gboard)
    dependsOn(
        originalInput, ramblerVoiceTyping,
        gboardFlagFamilyFeaturePatch(
            description = "Send the app's name and field hint to Rambler.",
            feature = GboardFlagFamilyFeature.RAMBLER_APP_CONTEXT,
        ),
    )
}

@Suppress("unused")
val ramblerDutchSendCommand = bytecodePatch(
    name = "Rambler Dutch send command (experimental)",
    description = "Saying only \"verstuur\", \"versturen\", \"verzend\" or \"verzenden\" with Rambler presses the " +
        "app's Send button, as \"send\" already does in English. It only works in fields with a Send action. " +
        "Requires Rambler voice typing.",
    default = false,
) {
    compatibleWith(gboard)
    dependsOn(
        originalInput, ramblerVoiceTyping,
        gboardFlagFamilyFeaturePatch(
            description = "Accept Dutch words for Rambler's send command.",
            feature = GboardFlagFamilyFeature.RAMBLER_SEND_COMMAND,
        ),
    )
}

@Suppress("unused")
val ramblerAutomaticRetry = bytecodePatch(
    name = "Rambler automatic retry (experimental)",
    description = "When a Rambler request fails, Gboard retries the recording on its own, together with Google's " +
        "on-device recognizer, instead of waiting for you to tap Retry. Requires Rambler voice typing.",
    default = false,
) {
    compatibleWith(gboard)
    dependsOn(
        originalInput, ramblerVoiceTyping,
        gboardFlagFamilyFeaturePatch(
            description = "Retry failed Rambler requests automatically.",
            feature = GboardFlagFamilyFeature.RAMBLER_AUTO_RETRY,
        ),
    )
}
