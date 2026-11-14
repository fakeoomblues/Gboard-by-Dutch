package patches.messages

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import org.w3c.dom.Element

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
private const val LAUNCHER = "android.intent.category.LAUNCHER"
private const val BRANDING_NAME = "morphe_messages_app_name"
private const val BRANDING_ICON = "morphe_messages_launcher"
internal const val MESSAGES_DEFAULT_NAME = "Messages AA"
private const val DEFAULT_ICON = "aa"
private const val ORIGINAL_ICON = "original"

/** Bundled launcher icons under resources/messages/branding/<id>/. */
internal val messagesIconStyles = linkedMapOf(
    "Messages AA (default)" to DEFAULT_ICON,
    "Original Messages" to ORIGINAL_ICON,
)
internal val messagesBrandingResources = listOf(
    "mipmap-xxxhdpi/$BRANDING_ICON.png",
    "mipmap-anydpi-v26/$BRANDING_ICON.xml",
    "drawable-nodpi/morphe_messages_artwork.png",
    "drawable/morphe_messages_background.xml",
    "drawable/morphe_messages_foreground.xml",
)

private fun Element.isLauncher(): Boolean {
    if (tagName != "activity" && tagName != "activity-alias") return false
    val categories = getElementsByTagName("category")
    return (0 until categories.length).any { (categories.item(it) as Element).getAttributeNS(ANDROID_NS, "name") == LAUNCHER }
}

/**
 * Names the car copy of Messages and gives it its own launcher icon, so it can be told apart from
 * the stock Google Messages it installs beside. Only the application and its launcher entry change;
 * Messages' own screens, including its car screen title, keep calling themselves Messages. The icon
 * keeps Messages' own monochrome layer for themed icons.
 */
@Suppress("unused")
val customBrandingPatch = resourcePatch(
    name = "Custom branding",
    description = "Experimental. Changes Messages' app name and launcher icon. Defaults to \"$MESSAGES_DEFAULT_NAME\" with " +
        "the Messages AA icon; Messages' original icon is also available.",
    default = false,
) {
    compatibleWith(messages)
    dependsOn(originalInput)

    val customName by stringOption(
        key = "customName", title = "App name", default = MESSAGES_DEFAULT_NAME,
        description = "Name shown in the launcher and in app information. Leave empty to keep Messages.",
    ) { value -> value == null || (value.trim().length <= 40 && value.trim().none { it.code < 32 }) }
    val appIcon by stringOption(
        key = "appIcon", title = "App icon", default = DEFAULT_ICON,
        description = "Launcher icon applied when patching.",
        values = messagesIconStyles, required = true,
    ) { it in messagesIconStyles.values }

    execute {
        val icon = appIcon!!
        val name = customName?.trim()?.takeUnless(String::isBlank)

        if (icon != ORIGINAL_ICON) {
            for (resource in messagesBrandingResources) {
                val source = object {}.javaClass.getResourceAsStream("/messages/branding/$icon/$resource")
                    ?: throw PatchException("Missing bundled Messages icon: $icon/$resource")
                val destination = get("res/$resource")
                destination.parentFile.mkdirs()
                source.use { input -> destination.outputStream().use(input::copyTo) }
            }
        }

        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("Messages application element is missing")
            val all = application.getElementsByTagName("*")
            val launchers = (0 until all.length).map { all.item(it) as Element }.filter { it.isLauncher() }
            if (launchers.isEmpty()) throw PatchException("Messages launcher entry is missing")
            if (icon != ORIGINAL_ICON) {
                for (element in listOf(application) + launchers) {
                    for (attribute in listOf("icon", "roundIcon")) {
                        if (element === application || element.hasAttributeNS(ANDROID_NS, attribute)) {
                            element.setAttributeNS(ANDROID_NS, "android:$attribute", "@mipmap/$BRANDING_ICON")
                        }
                    }
                }
            }
            if (name != null) {
                val originalLabel = application.getAttributeNS(ANDROID_NS, "label")
                application.setAttributeNS(ANDROID_NS, "android:label", "@string/$BRANDING_NAME")
                // The launcher entry repeats the application label, which would override it.
                for (launcher in launchers) {
                    if (launcher.getAttributeNS(ANDROID_NS, "label") == originalLabel) {
                        launcher.setAttributeNS(ANDROID_NS, "android:label", "@string/$BRANDING_NAME")
                    }
                }
            }
        }

        if (name != null) {
            document("res/values/strings.xml").use { strings ->
                val resources = strings.documentElement
                val entries = resources.getElementsByTagName("string")
                val entry = (0 until entries.length).map { entries.item(it) as Element }
                    .singleOrNull { it.getAttribute("name") == BRANDING_NAME }
                    ?: strings.createElement("string").also(resources::appendChild)
                entry.setAttribute("name", BRANDING_NAME)
                entry.setAttribute("translatable", "false")
                entry.setAttribute("formatted", "false")
                val escaped = name.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
                entry.textContent = "\"${if (name.first() in "@?#") "\\" else ""}$escaped\""
            }
        }
    }
}
