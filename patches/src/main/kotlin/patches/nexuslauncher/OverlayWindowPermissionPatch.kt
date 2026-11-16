package patches.nexuslauncher

import app.morphe.patcher.patch.resourcePatch

import org.w3c.dom.Element

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
private const val OVERLAY = "android.permission.SYSTEM_ALERT_WINDOW"

/**
 * The launcher's own long-press popup (New folder / Browse widgets / Wallpaper & style /
 * Home settings) is added as window type 2038, `TYPE_APPLICATION_OVERLAY`. A system install
 * is allowed to do that through `SYSTEM_APPLICATION_OVERLAY`; a sideloaded copy is not, and the
 * popup dies with
 *
 *   BadTokenException: Unable to add window -- permission denied for window type 2038
 *
 * which leaves the home screen with no way to add widgets or open its settings at all.
 * Declaring `SYSTEM_ALERT_WINDOW` makes the overlay appop applicable, so the addon can grant it
 * (`appops set <package> SYSTEM_ALERT_WINDOW allow`) and the popup works.
 */
@Suppress("unused")
val overlayWindowPermission = resourcePatch(
    name = "Experimental: declare the overlay permission for the launcher's popups",
    description = "Adds android.permission.SYSTEM_ALERT_WINDOW to the launcher manifest. Without " +
        "it the long-press popup cannot be added (BadTokenException, window type 2038) and the " +
        "home screen has no widgets, no wallpaper entry and no home settings. The addon grants " +
        "the matching appop in its Grant permissions step." +
        " Requires the pixel-launcher-dhizuku addon (gitlab.com/johninnld/pixel-launcher-dhizuku) to be installed.",
    default = true,
) {
    compatibleWith(pixelLauncher)
    dependsOn(originalInput)

    execute {
        document("AndroidManifest.xml").use { dom ->
            val manifest = dom.documentElement as Element
            val existing = manifest.getElementsByTagName("uses-permission")
            for (i in 0 until existing.length) {
                val node = existing.item(i) as Element
                if (node.getAttributeNS(ANDROID_NS, "name") == OVERLAY) {
                    System.err.println("nexuslauncher: $OVERLAY already declared")
                    return@use
                }
            }
            val permission = dom.createElement("uses-permission")
            permission.setAttributeNS(ANDROID_NS, "android:name", OVERLAY)
            manifest.insertBefore(permission, manifest.firstChild)
            System.err.println("nexuslauncher: declared $OVERLAY")
        }
    }
}
