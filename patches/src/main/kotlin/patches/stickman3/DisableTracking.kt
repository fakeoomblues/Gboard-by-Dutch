package patches.stickman3

import app.morphe.patcher.patch.resourcePatch
import patches.shared.disableComponents
import patches.shared.removePermissions
import patches.shared.setMetaData

private const val APPLOVIN_INIT = "com.applovin.sdk.AppLovinInitProvider"
private const val IRONSOURCE_LIFECYCLE = "com.ironsource.lifecycle.IronsourceLifecycleProvider"

/**
 * The game's tracking surface is entirely in the manifest: the Unity engine talks to its own cloud
 * services from IL2CPP, while the three ad SDKs that ship alongside Unity Ads are wired in here.
 */
@Suppress("unused")
val stickman3DisableTracking = resourcePatch(
    name = "Disable tracking",
    description = "Removes the advertising identifier and install-referrer permissions, stops " +
        "AppLovin and IronSource from initializing at launch, and turns off Facebook's advertiser " +
        "identifier and event upload. Play Billing and Facebook sign-in keep working.",
    default = true,
) {
    compatibleWith(stickman3)
    dependsOn(originalInput)

    execute {
        document("AndroidManifest.xml").use { manifest ->
            removePermissions(
                manifest,
                "com.google.android.gms.permission.AD_ID",
                "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            )
            // Both providers initialize their SDK before any game code runs. The game only reaches
            // those SDKs through Unity Ads mediation, which this bundle never starts, so stopping
            // the providers keeps AppLovin and IronSource from identifying the device at launch.
            disableComponents(manifest, APPLOVIN_INIT, IRONSOURCE_LIFECYCLE)
            // The Unity Facebook plugin stays for sign-in, but neither its event upload nor the
            // advertiser identifier may leave the device.
            setMetaData(manifest, "com.facebook.sdk.AutoLogAppEventsEnabled", "false")
            setMetaData(manifest, "com.facebook.sdk.AdvertiserIDCollectionEnabled", "false")
        }
    }
}
