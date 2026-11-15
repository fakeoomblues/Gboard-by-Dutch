package patches.nexuslauncher

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

/**
 * Shizuku/Shevery hands its binder to a client app through a provider the client declares
 * with the authority "<applicationId>.shizuku". Without that entry the client classes are
 * present but the bridge can never receive the server binder, so search sessions cannot be
 * created as shell.
 */
@Suppress("unused")
internal val searchUiProvider = resourcePatch(
    name = "Experimental: allow search sessions through the shell bridge",
    description = "Declares the Shizuku/Shevery client provider (rikka.shizuku.ShizukuProvider " +
        "with authority <applicationId>.shizuku) so the launcher can receive the shell binder " +
        "and create search sections with the MANAGE_SEARCH_UI permission it cannot hold itself." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    dependsOn(originalInput)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val all = doc.getElementsByTagName("*")
            val nodes = (0 until all.length).map { all.item(it) as Element }
            val application = nodes.single { it.tagName == "application" }

            val existing = nodes.filter {
                it.tagName == "provider" &&
                    it.getAttributeNS(ANDROID_NS, "authorities").endsWith(".shizuku")
            }
            check(existing.size <= 1) { "Duplicate Shizuku provider declaration" }
            if (existing.isEmpty()) {
                val node = doc.createElement("provider")
                node.setAttributeNS(ANDROID_NS, "android:name", "rikka.shizuku.ShizukuProvider")
                node.setAttributeNS(
                    ANDROID_NS,
                    "android:authorities",
                    "\${applicationId}.shizuku",
                )
                node.setAttributeNS(ANDROID_NS, "android:exported", "true")
                node.setAttributeNS(ANDROID_NS, "android:enabled", "true")
                node.setAttributeNS(ANDROID_NS, "android:multiprocess", "false")
                application.appendChild(node)
            }
        }
    }
}
