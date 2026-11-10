/*
 * Copyright (C) 2026 Morphe
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * GmsCore name routing ported from MorpheApp/morphe-patches:
 * https://github.com/MorpheApp/morphe-patches/commit/31a66d95932f6ccf4e39a5804c750af72a606d15
 * Commit 31a66d95932f6ccf4e39a5804c750af72a606d15 (2026-09-11),
 * patches/src/main/kotlin/app/morphe/patches/shared/misc/gms/GmsCoreSupportPatch.kt
 */
package patches.soundcloud

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.findMutableMethodOf
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import java.security.MessageDigest
import org.w3c.dom.Element
import patches.shared.uniqueMatch

private const val ANDROID = "http://schemas.android.com/apk/res/android"
private const val GOOGLE_VENDOR_GROUP_ID = "com.google"
private const val GMS_CORE_VENDOR_GROUP_ID = "app.revanced"
private const val MICROG = "app.revanced.android.gms"

/** The Play services permissions SoundCloud's manifest holds; microG declares these under its own names. */
private val MANIFEST_PERMISSIONS = mapOf(
    "com.google.android.c2dm.permission.RECEIVE" to "$GMS_CORE_VENDOR_GROUP_ID.android.c2dm.permission.RECEIVE",
    "com.google.android.gms.permission.AD_ID" to "$MICROG.permission.AD_ID",
)

private val ACTIONS = setOf(
    "com.google.android.c2dm.intent.RECEIVE",
    "com.google.android.c2dm.intent.REGISTER",
    "com.google.android.c2dm.intent.REGISTRATION",
    "com.google.android.c2dm.intent.UNREGISTER",
    "com.google.android.gcm.intent.SEND",
    "com.google.android.gms.accounts.ACCOUNT_SERVICE",
    "com.google.android.gms.ads.gservice.START",
    "com.google.android.gms.ads.identifier.service.EVENT_ATTESTATION",
    "com.google.android.gms.ads.service.CACHE",
    "com.google.android.gms.ads.service.CONSENT_LOOKUP",
    "com.google.android.gms.ads.service.HTTP",
    "com.google.android.gms.auth.account.authapi.START",
    "com.google.android.gms.auth.account.authenticator.auto.service.START",
    "com.google.android.gms.auth.account.authenticator.chromeos.START",
    "com.google.android.gms.auth.account.authenticator.tv.service.START",
    "com.google.android.gms.auth.account.data.service.START",
    "com.google.android.gms.auth.api.credentials.PICKER",
    "com.google.android.gms.auth.api.credentials.service.START",
    "com.google.android.gms.auth.api.identity.service.authorization.START",
    "com.google.android.gms.auth.api.identity.service.credentialsaving.START",
    "com.google.android.gms.auth.api.identity.service.signin.START",
    "com.google.android.gms.auth.api.phone.service.InternalService.START",
    "com.google.android.gms.auth.api.signin.service.START",
    "com.google.android.gms.auth.GOOGLE_SIGN_IN",
    "com.google.android.gms.auth.login.LOGIN",
    "com.google.android.gms.auth.service.START",
    "com.google.android.gms.car.service.START",
    "com.google.android.gms.cast.firstparty.START",
    "com.google.android.gms.cast.remote_display.service.START",
    "com.google.android.gms.cast.service.BIND_CAST_DEVICE_CONTROLLER_SERVICE",
    "com.google.android.gms.checkin.BIND_TO_SERVICE",
    "com.google.android.gms.chromesync.service.START",
    "com.google.android.gms.clearcut.service.START",
    "com.google.android.gms.common.account.CHOOSE_ACCOUNT",
    "com.google.android.gms.common.download.START",
    "com.google.android.gms.common.service.START",
    "com.google.android.gms.common.telemetry.service.START",
    "com.google.android.gms.config.START",
    "com.google.android.gms.deviceconnection.service.START",
    "com.google.android.gms.droidguard.service.INIT",
    "com.google.android.gms.droidguard.service.PING",
    "com.google.android.gms.droidguard.service.START",
    "com.google.android.gms.gass.START",
    "com.google.android.gms.googlehelp.service.GoogleHelpService.START",
    "com.google.android.gms.identity.service.BIND",
    "com.google.android.gms.measurement.START",
    "com.google.android.gms.nearby.bootstrap.service.NearbyBootstrapService.START",
    "com.google.android.gms.nearby.connection.service.START",
    "com.google.android.gms.nearby.fastpair.START",
    "com.google.android.gms.phenotype.service.START",
    "com.google.android.gms.playlog.service.START",
    "com.google.android.gms.plus.service.default.INTENT",
    "com.google.android.gms.plus.service.image.INTENT",
    "com.google.android.gms.plus.service.internal.START",
    "com.google.android.gms.plus.service.START",
    "com.google.android.gms.potokens.service.START",
    "com.google.android.gms.safetynet.service.START",
    "com.google.android.gms.signin.service.START",
    "com.google.android.gms.stats.ACTION_UPLOAD_DROPBOX_ENTRIES",
    "com.google.android.gms.udc.service.START",
    "com.google.android.gms.update.START_API_SERVICE",
    "com.google.android.gms.update.START_SERVICE",
    "com.google.android.gms.update.START_SINGLE_USER_API_SERVICE",
    "com.google.android.gms.userlocation.service.START",
    "com.google.android.gms.wearable.BIND",
    "com.google.android.gms.wearable.BIND_LISTENER",
    "com.google.android.gsf.action.GET_GLS",
    "com.google.android.location.internal.GoogleLocationManagerService.START",
    "com.google.android.gms.locationsharingreporter.service.START",
    "com.google.android.gms.location.reporting.service.START",
    "com.google.iid.TOKEN_REQUEST",
)

private val AUTHORITIES = setOf(
    "com.google.android.gms.auth.accounts",
    "com.google.android.gms.chimera",
    "com.google.android.gms.fonts",
    "com.google.android.gms.phenotype",
    "com.google.android.gsf.gservices",
    "com.google.android.gsf.subscribedfeeds",
    "com.google.settings",
)

private val GMS_CORE_NAMES: Map<String, String> =
    (setOf(GOOGLE_VENDOR_GROUP_ID, "$GOOGLE_VENDOR_GROUP_ID.android.gms") + MANIFEST_PERMISSIONS.keys + ACTIONS + AUTHORITIES)
        .associateWith { it.replaceFirst(GOOGLE_VENDOR_GROUP_ID, GMS_CORE_VENDOR_GROUP_ID) }

private fun gmsCoreName(value: String): String? {
    GMS_CORE_NAMES[value]?.let { return it }
    if (!value.startsWith("content://")) return null
    return AUTHORITIES.firstOrNull { value.startsWith("content://$it") }
        ?.let { value.replaceFirst(GOOGLE_VENDOR_GROUP_ID, GMS_CORE_VENDOR_GROUP_ID) }
}

private fun Instruction.stringConstant(): String? {
    if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) return null
    return ((this as? ReferenceInstruction)?.reference as? StringReference)?.string
}

private val microgManifestPatch = resourcePatch {
    dependsOn(originalInput)
    execute {
        val certificate = packageMetadata.signingCertificates.values.flatten()
            .distinctBy { it.encoded.joinToString("") { byte -> "%02x".format(byte.toInt()) } }
            .singleOrNull() ?: throw PatchException("SoundCloud input has no unique signing certificate")
        val certificateSha1 = MessageDigest.getInstance("SHA-1").digest(certificate.encoded)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

        document("AndroidManifest.xml").use { doc ->
            // 1. The Play services permissions SoundCloud holds, under microG's names.
            var routed = 0
            val permissions = doc.getElementsByTagName("uses-permission")
            for (index in 0 until permissions.length) {
                val element = permissions.item(index) as Element
                val to = MANIFEST_PERMISSIONS[element.getAttributeNS(ANDROID, "name")] ?: continue
                element.setAttributeNS(ANDROID, "android:name", to)
                routed++
            }
            if (routed != MANIFEST_PERMISSIONS.size)
                throw PatchException("expected ${MANIFEST_PERMISSIONS.size} Play services permissions, routed $routed")

            // 2. Package visibility: SoundCloud must be allowed to see microG.
            val queries = doc.getElementsByTagName("queries").item(0) as? Element
                ?: throw PatchException("SoundCloud manifest has no queries block")
            val packages = queries.getElementsByTagName("package")
            if ((0 until packages.length).none {
                (packages.item(it) as Element).getAttributeNS(ANDROID, "name") == MICROG
            }) queries.appendChild(doc.createElement("package").apply {
                setAttributeNS(ANDROID, "android:name", MICROG)
            })

            // 3. Whom microG vouches for: stock SoundCloud's package and publisher certificate.
            val application = doc.getElementsByTagName("application").item(0) as Element
            val metadata = application.getElementsByTagName("meta-data")
            val values = mapOf(
                "$MICROG.SPOOFED_PACKAGE_NAME" to soundcloud.packageName,
                "$MICROG.SPOOFED_PACKAGE_SIGNATURE" to certificateSha1,
                "$GMS_CORE_VENDOR_GROUP_ID.MICROG_PACKAGE_NAME" to MICROG,
                "$MICROG.MICROG_PACKAGE_NAME" to MICROG,
            )
            check((0 until metadata.length).none {
                (metadata.item(it) as Element).getAttributeNS(ANDROID, "name") in values
            }) { "SoundCloud already declares microG identity metadata" }
            values.forEach { (name, value) ->
                application.appendChild(doc.createElement("meta-data").apply {
                    setAttributeNS(ANDROID, "android:name", name)
                    setAttributeNS(ANDROID, "android:value", value)
                })
            }
        }
    }
}

/**
 * Routes SoundCloud's Google services through an installed MicroG-RE (MicroG-RE or
 * ReVanced GmsCore, both installed as [MICROG]).
 *
 * SoundCloud signs in through its own web flow (secure.soundcloud.com in a browser
 * tab, redirecting back over the sc:// deep link), which needs no Play services and
 * keeps working unchanged. The Google plumbing around the account does need them:
 * sign-in and listening notifications arrive over Firebase Cloud Messaging, which
 * registers through Play services' c2dm, and the app gates that registration -- and
 * Cast, ads identifiers and in-app review -- on its embedded Play services
 * availability check. On a phone without Google Play services none of that runs.
 * This patch points every Play services name in the app and its manifest at
 * microG's, answers the availability check itself, and tells microG to vouch for
 * stock SoundCloud's package and publisher certificate, so notifications and the
 * other services work through microG instead.
 */
@Suppress("unused")
val microgSupportPatch = bytecodePatch(
    name = "Add microG-RE support",
    description = "Routes SoundCloud's Google services -- sign-in and listening notifications over " +
        "Firebase Cloud Messaging, Cast, the ads identifier and in-app review -- through an installed " +
        "MicroG-RE (MicroG-RE or ReVanced GmsCore) on phones without Google Play services. The sign-in " +
        "itself is SoundCloud's own web login and needs neither. Off on phones with real Play services.",
) {
    compatibleWith(soundcloud)
    dependsOn(originalInput, microgManifestPatch)

    execute {
        // The availability gate: the embedded client library would look for Play services
        // under microG's name and reject its signature. Report success on every overload,
        // as the shared GmsCore support patch does; unavailable microG services fail per
        // connection.
        for ((owner, parameters) in listOf(
            "Lcom/google/android/gms/common/GoogleApiAvailability;" to listOf("Landroid/content/Context;"),
            "Lcom/google/android/gms/common/GoogleApiAvailability;" to listOf("Landroid/content/Context;", "I"),
            "Lcom/google/android/gms/common/GoogleApiAvailabilityLight;" to listOf("Landroid/content/Context;"),
            "Lcom/google/android/gms/common/GoogleApiAvailabilityLight;" to listOf("Landroid/content/Context;", "I"),
        )) Fingerprint(
            definingClass = owner,
            name = "isGooglePlayServicesAvailable",
            returnType = "I",
            parameters = parameters,
        ).uniqueMatch().method.returnEarly(0)

        // Every Play services name in SoundCloud's code: the package, the account type,
        // its services, permissions and content URIs. The bundled extension carries none
        // of these names, so it keeps asking for microG by its real name where it must.
        var rewritten = 0
        classDefForEach { classDef ->
            if (classDef.type.startsWith("Lnl/johninnl/")) return@classDefForEach
            for (method in classDef.methods) {
                val instructions = method.implementation?.instructions ?: continue
                if (instructions.none { it.stringConstant()?.let(::gmsCoreName) != null }) continue
                val mutableMethod = mutableClassDefBy(classDef).findMutableMethodOf(method)
                mutableMethod.implementation!!.instructions.indices.reversed().forEach { index ->
                    val to = mutableMethod.getInstruction(index).stringConstant()?.let(::gmsCoreName) ?: return@forEach
                    val register = (mutableMethod.getInstruction(index) as OneRegisterInstruction).registerA
                    mutableMethod.replaceInstruction(index, "const-string v$register, \"$to\"")
                    rewritten++
                }
            }
        }
        // Measured on this SoundCloud build: 57 package, 18 account type and the rest.
        if (rewritten < 80) throw PatchException("rewrote only $rewritten Play services names; SoundCloud changed shape")
    }
}
