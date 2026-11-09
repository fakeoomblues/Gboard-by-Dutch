package patches.angrybirds2

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import com.android.tools.smali.dexlib2.Opcode
import patches.shared.disableMeasurementAndAttribution
import patches.shared.removeMetaData

internal const val DEVICE_INFO = "Lcom/rovio/beacon/DeviceInfo;"
internal const val ATTRIBUTION = "Lcom/rovio/beacon/Attribution;"

private val trackingResources = resourcePatch {
    dependsOn(originalInput)
    execute {
        document("AndroidManifest.xml").use { manifest ->
            disableMeasurementAndAttribution(manifest)
            // No DSN means the Sentry Android auto-init stays disabled.
            removeMetaData(manifest, "io.sentry.dsn")
        }
    }
}

internal val angryBirdsTrackingGate = bytecodePatch {
    dependsOn(originalInput, trackingResources)

    execute {
        // Install referrer (Play, Facebook, Instagram providers) feeds the attribution table.
        val referrer = mutableClassDefBy(ATTRIBUTION).methods.singleOrNull {
            it.name == "getInstallReferrer" && it.returnType == "V" && it.parameterTypes.size == 1
        } ?: error("Angry Birds 2 install referrer entry changed")
        check(referrer.implementation?.instructions?.firstOrNull()?.opcode != Opcode.RETURN_VOID) {
            "Angry Birds 2 install referrer already patched"
        }
        referrer.addInstructions(0, "return-void")

        // Advertising ID, app set ID and user agent handed to the C# analytics layer: complete
        // immediately with empty values, matching the "unavailable" shape the game already handles.
        val deviceInfo = mutableClassDefBy(DEVICE_INFO)
        val listener = "Lcom/rovio/beacon/DeviceInfo${'$'}AsyncIdsListener;"
        // getAsyncIdentifiers(Z, listener) keeps the listener in p2; getAdvertisingId(listener) in p1.
        for ((name, parameters, listenerRegister) in listOf(
            Triple("getAsyncIdentifiers", 2, "p2"),
            Triple("getAdvertisingId", 1, "p1"),
        )) {
            val method = deviceInfo.methods.singleOrNull {
                it.name == name && it.returnType == "V" && it.parameterTypes.size == parameters
            } ?: error("Angry Birds 2 device identifiers entry changed: $name")
            check(method.implementation?.instructions?.firstOrNull()?.opcode != Opcode.RETURN_VOID) {
                "Angry Birds 2 device identifiers already patched: $name"
            }
            method.addInstructions(
                0,
                """
                const-string v0, ""
                const/4 v1, 0x0
                invoke-interface { $listenerRegister, v0, v1, v0, v0 }, $listener->onComplete(Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)V
                return-void
                """,
            )
        }
    }
}

@Suppress("unused")
val angryBirds2DisableTracking = bytecodePatch(
    name = "Disable tracking",
    description = "Stops install-referrer attribution, advertising identifier collection, Google " +
        "measurement, Firebase Analytics, Crashlytics and Sessions upload, and removes the Sentry " +
        "crash-report endpoint. The game's own cloud services keep working; its in-game telemetry " +
        "that runs inside the Unity engine cannot be reached from this layer.",
    default = true,
) {
    compatibleWith(angryBirds2)
    dependsOn(angryBirdsTrackingGate)
}
