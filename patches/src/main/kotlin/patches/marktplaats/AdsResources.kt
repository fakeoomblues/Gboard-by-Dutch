package patches.marktplaats

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

internal val adsResources = resourcePatch {
    dependsOn(originalInput)
    execute {
        document("AndroidManifest.xml").use { doc ->
            val providers = doc.getElementsByTagName("provider")
            val matches = (0 until providers.length).map { providers.item(it) as Element }
                .filter { it.getAttribute("android:name") == "com.google.android.gms.ads.MobileAdsInitProvider" }
            if (packageMetadata.versionName == "16.19.0") {
                // The NextGen SDK has no legacy manifest initializer. Its
                // explicit initializer is stopped by the bytecode control.
                check(matches.isEmpty()) { "Marktplaats 16.19.0 legacy ad provider unexpectedly present" }
                return@use
            }
            check(matches.size == 1 && !matches.single().hasAttribute("android:enabled")) {
                "Marktplaats original MobileAds initializer changed"
            }
            matches.single().setAttributeNS("http://schemas.android.com/apk/res/android", "android:enabled", "false")
        }
    }
}
