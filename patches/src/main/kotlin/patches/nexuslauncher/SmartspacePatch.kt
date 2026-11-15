package patches.nexuslauncher

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

/**
 * The at-a-glance row asks the smartspace service for updates and is refused with
 * `Permission Denial: Cannot call requestSmartspaceUpdate`. `ACCESS_SMARTSPACE` is declared
 * `signature|privileged|development`, so declaring it is enough for the shell to grant it:
 * the development flag makes it grantable outside /system/priv-app.
 */
@Suppress("unused")
internal val smartspacePermission = resourcePatch(
    name = "Experimental: allow at-a-glance smartspace updates",
    description = "Declares android.permission.ACCESS_SMARTSPACE so the smartspace service " +
        "accepts updates from a sideloaded launcher. The permission is signature|privileged " +
        "with the development flag, so it can be granted to an ordinary app with " +
        "`pm grant`; without the declaration the request is always refused and the " +
        "at-a-glance row stays empty even when Pixel Weather is installed." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    dependsOn(originalInput)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val all = doc.getElementsByTagName("*")
            val nodes = (0 until all.length).map { all.item(it) as Element }
            val manifest = nodes.single { it.tagName == "manifest" }

            val existing = nodes.filter {
                it.tagName == "uses-permission" &&
                    it.getAttributeNS(ANDROID_NS, "name") == "android.permission.ACCESS_SMARTSPACE"
            }
            check(existing.size <= 1) { "Duplicate ACCESS_SMARTSPACE declaration" }
            if (existing.isEmpty()) {
                val node = doc.createElement("uses-permission")
                node.setAttributeNS(ANDROID_NS, "android:name", "android.permission.ACCESS_SMARTSPACE")
                manifest.appendChild(node)
            }
        }
    }
}
