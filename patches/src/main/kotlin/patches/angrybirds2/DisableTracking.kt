package patches.angrybirds2

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import com.android.tools.smali.dexlib2.Opcode
import patches.shared.disableMeasurementAndAttribution
import patches.shared.setMetaData

internal const val DEVICE_INFO = "Lcom/rovio/beacon/DeviceInfo;"
internal const val ATTRIBUTION = "Lcom/rovio/beacon/Attribution;"

private val trackingResources = resourcePatch {
    dependsOn(originalInput)
    execute {
        document("AndroidManifest.xml").use { manifest ->
            disableMeasurementAndAttribution(manifest)
            // An empty DSN disables the Sentry SDK cleanly; removing the meta-data makes the
            // auto-init provider crash with "DSN is required".
            setMetaData(manifest, "io.sentry.dsn", "")
        }
    }
}

internal val angryBirdsTrackingGate = bytecodePatch {
    dependsOn(originalInput, trackingResources)

    execute {
        // Neutralize tracking at the data level with the original threading intact: the
        // originals always complete their listeners on their own threads, and a silent skip or
        // a synchronous callback would hang the C# bootstrap (observed on device).
        fun forceReturn(owner: String, name: String, kind: String, value: String, marker: Opcode) {
            val method = mutableClassDefBy(owner).methods.singleOrNull {
                it.name == name && it.parameterTypes.isEmpty() && it.returnType == kind
            } ?: error("Angry Birds 2 tracking entry changed: $name")
            val body = method.implementation?.instructions?.toList().orEmpty()
            check(body.isNotEmpty() && body.first().opcode != marker) {
                "Angry Birds 2 tracking already patched: $name"
            }
            method.addInstructions(0, "$value\n${if (kind == "V") "return-void" else if (kind == "Z") "return v0" else "return-object v0"}")
        }

        // Advertising identifiers handed to the C# analytics layer.
        forceReturn(DEVICE_INFO, "getAdvertisingIdentifier", "Ljava/lang/String;",
            "const-string v0, \"\"", Opcode.RETURN_OBJECT)
        forceReturn(DEVICE_INFO, "getAdvertisingTrackingEnabled", "Z",
            "const/4 v0, 0x0", Opcode.RETURN)
        forceReturn(DEVICE_INFO, "getAppSetId", "Ljava/lang/String;",
            "const-string v0, \"\"", Opcode.RETURN_OBJECT)

        // Install-referrer attribution: report it as already queried so the C# bootstrap skips
        // the Play/Facebook/Instagram referrer queries entirely; no listener is ever involved.
        forceReturn(ATTRIBUTION, "hasQueriedReferrer", "Z",
            "const/4 v0, 0x1", Opcode.RETURN)
        // Meta (Facebook/Instagram) referrer JSON: the DeviceInfo wrapper null-checks this.
        forceReturn(ATTRIBUTION, "getMetaInstallReferrer", "Lorg/json/JSONObject;",
            "const/4 v0, 0x0", Opcode.RETURN_OBJECT)
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
