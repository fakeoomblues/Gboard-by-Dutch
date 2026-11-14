package patches.messages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import org.w3c.dom.Element

internal const val CAR_SERVICE_NAME = "$MESSAGES_PACKAGE.auto.MessagingTemplateScreenServiceModule"
internal const val CAR_SERVICE = "Lcom/google/android/apps/messaging/auto/MessagingTemplateScreenServiceModule;"
private const val CAR_APP_SERVICE_ACTION = "androidx.car.app.CarAppService"
private const val MESSAGING_CATEGORY = "androidx.car.app.category.MESSAGING"

/** Logged only by the Car App Library's HostValidator when it was built to allow every host. */
internal const val ALLOW_ALL_LOG = "Accepted - Validator disabled, all hosts allowed"

private fun ClassDef.hasString(value: String) = methods.any { method ->
    method.implementation?.instructions?.any {
        ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == value
    } == true
}

/**
 * Messages ships its car screen service disabled and turns it on only after a Google settings sync,
 * when it is the default SMS app. A patched copy may never receive that sync, so the manifest
 * enables the service from the start. Only the enabled flag changes.
 */
private val enableCarServicePatch = resourcePatch {
    dependsOn(originalInput)
    execute {
        document("AndroidManifest.xml").use { manifest ->
            val services = manifest.getElementsByTagName("service")
            val service = (0 until services.length).map { services.item(it) as Element }
                .singleOrNull { it.getAttribute("android:name") == CAR_SERVICE_NAME }
                ?: throw PatchException("Messages car screen service is missing")
            val actions = service.getElementsByTagName("action")
            val categories = service.getElementsByTagName("category")
            if ((0 until actions.length).none { (actions.item(it) as Element).getAttribute("android:name") == CAR_APP_SERVICE_ACTION } ||
                (0 until categories.length).none { (categories.item(it) as Element).getAttribute("android:name") == MESSAGING_CATEGORY }
            ) throw PatchException("Messages car screen service is no longer a messaging car app")
            if (service.getAttribute("android:exported") != "true") throw PatchException("Messages car screen service is not exported")
            if (service.getAttribute("android:enabled") != "false") throw PatchException("Messages car screen service state changed or patch already applied")
            service.setAttribute("android:enabled", "true")
        }
    }
}

/**
 * The car screen service builds its host validator from two resource lists that name only Google's
 * own Android Auto and car templates host. Its validator method now returns the library's allow-all
 * validator instead, which Messages already carries. The Car App Library's handshake still checks
 * that the caller is the package it claims to be and that its car API level is supported.
 */
@Suppress("unused")
val carHostPatch = bytecodePatch(
    name = "Allow any Android Auto host",
    description = "Experimental. Lets any Android Auto compatible host open Messages' car screen, " +
        "including DriveDeck, Gearslip, AARE and Flywheel, whatever its package name or signing key. " +
        "Google's Android Auto keeps working.",
    default = false,
) {
    compatibleWith(messages)
    dependsOn(originalInput, playServicesRefusalPatch, enableCarServicePatch)

    execute {
        val owner = mutableClassDefBy(CAR_SERVICE)
        val candidates = owner.methods.filter { method ->
            method.parameterTypes.isEmpty() && !AccessFlags.STATIC.isSet(method.accessFlags) &&
                method.returnType.startsWith("L") &&
                classDefByOrNull(method.returnType)?.hasString(ALLOW_ALL_LOG) == true
        }
        val original = candidates.singleOrNull()
            ?: throw PatchException("Expected one Messages host validator method, found ${candidates.size}")
        val validator = original.returnType
        val allowAll = classDefBy(validator).fields.filter {
            it.type == validator && AccessFlags.STATIC.isSet(it.accessFlags) && AccessFlags.FINAL.isSet(it.accessFlags)
        }.singleOrNull() ?: throw PatchException("Messages allow-all host validator is missing")
        val references = original.implementation?.instructions?.mapNotNull {
            (it as? ReferenceInstruction)?.reference?.toString()
        }.orEmpty()
        if (references.none { it == "$validator->${allowAll.name}:$validator" }) {
            throw PatchException("Messages host validator no longer selects between allow-all and allow-lists, or patch already applied")
        }

        val replacement = ImmutableMethod(original.definingClass, original.name, original.parameters,
            original.returnType, original.accessFlags, original.annotations, original.hiddenApiRestrictions,
            MutableMethodImplementation(2)).toMutable().apply {
            addInstructions(0, """
                sget-object v0, $validator->${allowAll.name}:$validator
                return-object v0
            """)
        }
        check(owner.methods.remove(original)) { "Messages host validator method disappeared" }
        owner.methods.add(replacement)
    }
}
