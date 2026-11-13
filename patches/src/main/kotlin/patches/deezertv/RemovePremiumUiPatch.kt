package patches.deezertv

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val ANDROID = "http://schemas.android.com/apk/res/android"

/**
 * TV has no Premium tab: the tab bar is a fixed list of Home, My Music, Explore, Search,
 * Now playing and the profile. The subscription pressure instead lives in three
 * places: the "Subscribe" button on the profile tab, the dynamic-page subscription
 * brick and the conversion banner. Remove those surfaces and close the informational
 * subscribe page so no route can reopen it.
 */
private val premiumResources = resourcePatch {
    dependsOn(originalInput)
    execute {
        fun Document.singleElement(attribute: String, value: String): Element {
            val elements = getElementsByTagName("*")
            val matches = (0 until elements.length).map { elements.item(it) as Element }
                .filter { it.getAttribute(attribute) == value }
            check(matches.size == 1) { "Expected one $attribute=$value, found ${matches.size}" }
            return matches.single()
        }

        document("res/layout/tv_fragment_user_settings.xml").use { doc ->
            doc.singleElement("android:id", "@id/action_subscribe").apply {
                setAttributeNS(ANDROID, "android:visibility", "gone")
                setAttributeNS(ANDROID, "android:layout_height", "0dp")
            }
        }

        for (layout in listOf("brick__subscription", "brick__conversion_banner")) {
            document("res/layout/$layout.xml").use { doc ->
                doc.documentElement.apply {
                    setAttributeNS(ANDROID, "android:visibility", "gone")
                    setAttributeNS(ANDROID, "android:layout_height", "0dp")
                }
            }
        }
    }
}

@Suppress("unused")
val deezerTvRemovePremiumUi = bytecodePatch(
    name = "Remove premium upsell UI (TV)",
    description = "Removes the Subscribe button from the TV profile tab, the subscription and " +
        "conversion bricks from dynamic pages, and closes the informational subscribe screen. " +
        "Catalogue, playback, account and the remaining settings entries are unchanged.",
    default = true,
) {
    compatibleWith(deezerTv)
    dependsOn(originalInput, premiumResources)

    execute {
        val info = exact(
            "Lcom/deezer/android/tv/ui/feature/info/TvInfoMessageActivity;",
            "onCreate",
            "V",
            "Landroid/os/Bundle;",
        ).method
        info.addInstructions(
            superOnCreateIndex("Lcom/deezer/android/tv/ui/feature/info/TvInfoMessageActivity;") + 1,
            """
                invoke-virtual {p0}, Lcom/deezer/android/tv/ui/feature/info/TvInfoMessageActivity;->getIntent()Landroid/content/Intent;
                move-result-object p1
                const-string v0, "contentId"
                invoke-virtual {p1, v0}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
                if-eqz p1, :deezer_tv_info_continue
                const-string v0, "info_subscribe"
                invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :deezer_tv_info_continue
                invoke-virtual {p0}, Lcom/deezer/android/tv/ui/feature/info/TvInfoMessageActivity;->finish()V
                return-void
                :deezer_tv_info_continue
            """,
        )
    }
}
