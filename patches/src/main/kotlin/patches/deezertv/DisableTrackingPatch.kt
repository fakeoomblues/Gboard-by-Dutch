package patches.deezertv

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val ANDROID = "http://schemas.android.com/apk/res/android"

private val trackingResources = resourcePatch {
    dependsOn(originalInput)
    execute {
        document("AndroidManifest.xml").use { doc ->
            val applications = doc.getElementsByTagName("application")
            check(applications.length == 1) { "Expected one Deezer TV application" }
            val application = applications.item(0) as Element

            fun meta(name: String, value: String) {
                val nodes = application.getElementsByTagName("meta-data")
                val matches = (0 until nodes.length).map { nodes.item(it) as Element }
                    .filter { it.getAttributeNS(ANDROID, "name") == name }
                check(matches.size <= 1) { "Duplicate Deezer TV manifest flag: $name" }
                val element = matches.singleOrNull() ?: doc.createElement("meta-data").apply {
                    setAttributeNS(ANDROID, "android:name", name)
                }
                if (matches.isEmpty()) application.appendChild(element)
                element.setAttributeNS(ANDROID, "android:value", value)
            }

            // Firebase Analytics, Crashlytics, Performance and Sessions collection.
            meta("firebase_analytics_collection_enabled", "false")
            meta("firebase_analytics_collection_deactivated", "true")
            meta("firebase_sessions_enabled", "false")
            meta("firebase_crashlytics_collection_enabled", "false")
            meta("firebase_performance_collection_enabled", "false")
            meta("firebase_performance_collection_deactivated", "true")
            meta("google_analytics_automatic_screen_reporting_enabled", "false")
            meta("google_analytics_default_allow_ad_personalization_signals", "false")
            meta("google_analytics_default_allow_ad_user_data", "false")
            meta("google_analytics_default_allow_ad_storage", "false")
            meta("google_analytics_default_allow_analytics_storage", "false")

            // Facebook is used for login on the phone build; keep the provider but stop
            // automatic event and advertising-ID collection.
            meta("com.facebook.sdk.AutoInitEnabled", "false")
            meta("com.facebook.sdk.AutoLogAppEventsEnabled", "false")
            meta("com.facebook.sdk.AdvertiserIDCollectionEnabled", "false")

            val advertisingPermissions = setOf(
                "com.google.android.gms.permission.AD_ID",
                "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
                "android.permission.ACCESS_ADSERVICES_AD_ID",
                "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
                "com.adjust.preinstall.READ_PERMISSION",
            )
            val permissions = doc.getElementsByTagName("uses-permission")
            val removed = (0 until permissions.length).map { permissions.item(it) as Element }
                .filter { it.getAttributeNS(ANDROID, "name") in advertisingPermissions }
            check(removed.size == 6) {
                "Deezer TV advertising/attribution permission inventory changed: found ${removed.size}"
            }
            removed.forEach { it.parentNode.removeChild(it) }

            // The Adjust and Facebook manifest providers initialize their SDKs before
            // Application.onCreate; disable them so the patched initializers are final.
            listOf(
                "com.adjust.sdk.SystemLifecycleContentProvider",
                "com.facebook.internal.FacebookInitProvider",
            ).forEach { name ->
                val providers = doc.getElementsByTagName("provider")
                val matches = (0 until providers.length).map { providers.item(it) as Element }
                    .filter { it.getAttributeNS(ANDROID, "name") == name }
                check(matches.size == 1) { "Expected one Deezer TV manifest provider $name, found ${matches.size}" }
                matches.single().setAttributeNS(ANDROID, "android:enabled", "false")
            }
        }
    }
}

/**
 * The TV build starts Adjust, Segment (with its Braze destination) and Firebase from
 * DZMidlet.onCreate. Closing the initializer entry points keeps sign-in, catalogue and
 * playback intact while no analytics or attribution payload leaves the device.
 */
@Suppress("unused")
val deezerTvDisableTracking = bytecodePatch(
    name = "Disable tracking (TV)",
    description = "Disables the inventoried Deezer TV analytics and attribution collectors: Adjust " +
        "initialization and events, Segment event/identify delivery and its Braze destination, the " +
        "Firebase Analytics/Crashlytics/Performance/Sessions collectors and Facebook automatic app " +
        "events and advertising IDs. Sign-in, catalogue, playback and functional notifications are " +
        "retained.",
    default = true,
) {
    compatibleWith(deezerTv)
    dependsOn(originalInput, trackingResources)

    execute {
        // Adjust is never configured, so its lifecycle provider and event calls are inert.
        exact("LIllllllIIIlIIIl;", "IIIllllIIlllll", "V", "Landroid/content/Context;")
            .method.addInstructions(0, "return-void")

        // Segment's setup has to run: later callers read the initialized Analytics
        // instance and crash if the lateinit field is empty. Keep the pipeline and
        // disable its inputs plus the Braze SDK boot instead.
        val segmentSetup = exact("LIlllIIIlIllIIIII;", "IIIllllIIlllll", "V").method
        val brazeInit = segmentSetup.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let { reference ->
                    reference.definingClass == "Lcom/braze/Braze\$Companion;" && reference.name == "getInstance"
                } == true
        }
        check(brazeInit >= 0) { "Deezer TV Braze initialization changed" }
        segmentSetup.replaceInstruction(brazeInit, "nop")

        // The Braze destination registers its own in-app-message lifecycle callbacks,
        // which initialize the Braze SDK on the first activity. Leave the plugin
        // constructed but do not add it to the Segment timeline.
        val brazeAdd = (1 until segmentSetup.instructions.size).firstOrNull { index ->
            val instruction = segmentSetup.instructions[index]
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            val previous = segmentSetup.instructions[index - 1]
            instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.definingClass == "Lcom/segment/analytics/kotlin/core/Analytics;" &&
                reference.name == "add" &&
                previous.opcode == Opcode.SPUT_OBJECT &&
                (previous as? ReferenceInstruction)?.reference?.toString()?.contains("IlIlllIIIIIlIIll") == true
        } ?: error("Deezer TV Braze destination registration changed")
        segmentSetup.replaceInstruction(brazeAdd, "nop")

        // Every Segment screen/event and identify call goes through these two methods.
        exact("LIlllIIIlIllIIIII;", "IlIllllIlIIlllll", "V", "LIlIlIlIlIllIIIIl;")
            .method.addInstructions(0, "return-void")
        exact("LIlllIIIlIllIIIII;", "IIlIIlIIIIIlII", "V", "Ljava/lang/String;", "Ljava/lang/String;", "Z", "Z")
            .method.addInstructions(0, "return-void")

        // The separate Braze identity tracker changes the Braze user on login.
        exact("LIllIIIlIIllIIlI;", "IIlIIlIIIIIlII", "V", "Ljava/lang/String;", "Ljava/lang/String;", "Z", "Z")
            .method.addInstructions(0, "return-void")

        // Deezer's engage/content-card setup calls Braze.enableSdk from
        // Application.onCreate. Leave the SDK object initialization intact (callers
        // depend on it) but stop it from enabling network collection.
        exact("Lcom/braze/Braze\$Companion;", "enableSdk", "V", "Landroid/content/Context;")
            .method.addInstructions(0, "return-void")

        // Segment fetches remote settings from api.segment.io on first start. Keep the
        // pipeline on the stored/default settings and skip the request entirely.
        exact(
            "Lcom/segment/analytics/kotlin/core/SettingsKt;",
            "fetchSettings",
            "Lcom/segment/analytics/kotlin/core/Settings;",
            "Lcom/segment/analytics/kotlin/core/Analytics;",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
        ).method.addInstructions(0, "const/4 p0, 0x0\nreturn-object p0")

        // Braze is configured from the Segment destination. Keep the plugin registered
        // (Segment expects it) but stop it from configuring the SDK or forwarding events.
        val brazeDestination = "Lcom/segment/analytics/kotlin/destinations/braze/BrazeDestination;"
        exact(
            brazeDestination,
            "update",
            "V",
            "Lcom/segment/analytics/kotlin/core/Settings;",
            "Lcom/segment/analytics/kotlin/core/platform/Plugin\$UpdateType;",
        ).method.addInstructions(0, "return-void")
        exact(
            brazeDestination,
            "track",
            "Lcom/segment/analytics/kotlin/core/BaseEvent;",
            "Lcom/segment/analytics/kotlin/core/TrackEvent;",
        ).method.addInstructions(0, "return-object p1")
        exact(
            brazeDestination,
            "identify",
            "Lcom/segment/analytics/kotlin/core/BaseEvent;",
            "Lcom/segment/analytics/kotlin/core/IdentifyEvent;",
        ).method.addInstructions(0, "return-object p1")
        exact(brazeDestination, "flush", "V").method.addInstructions(0, "return-void")
        listOf("onActivityStarted", "onActivityStopped", "onActivityResumed", "onActivityPaused")
            .forEach { name ->
                exact(brazeDestination, name, "V", "Landroid/app/Activity;")
                    .method.addInstructions(0, "return-void")
            }
    }
}
