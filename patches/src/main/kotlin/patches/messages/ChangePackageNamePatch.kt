package patches.messages

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableStringEncodedValue
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue
import org.w3c.dom.Document
import org.w3c.dom.Element

internal const val MESSAGES_MORPHE_PACKAGE = "app.morphe.android.apps.messaging"

/** Google's namespace for the identities Messages declares outside its own package. */
private const val GOOGLE_NAMESPACE = "com.google.android."
private const val SHARING_AUTHORITY = "forwarding_and_sharing_file_provider_authority"

private fun Document.elements(tag: String): List<Element> =
    getElementsByTagName(tag).let { list -> (0 until list.length).map { list.item(it) as Element } }

/**
 * Where an identity of Messages moves. Values under the stock package move under the new one;
 * the RCS, IMS and Compose identities Messages declares in Google's wider namespace move under the
 * new package with that namespace dropped. Two installed apps can never share an authority or a
 * custom permission, so all of them move.
 */
internal fun movedIdentity(value: String, old: String, new: String): String = when {
    value == old -> new
    value.startsWith("$old.") || value.startsWith("$old:") -> new + value.substring(old.length)
    value.startsWith(GOOGLE_NAMESPACE) -> "$new." + value.removePrefix(GOOGLE_NAMESPACE)
    else -> throw PatchException("Unexpected Messages identity $value")
}

/**
 * One run's renaming: old to new for the package and every provider authority, collected from
 * the original manifest. Set when Change package name executes and read by the code half's
 * finalize, which runs after every execute.
 */
private class Renaming(val oldPackage: String, val newPackage: String, val authorities: Map<String, String>) {
    /** The replacement for one code string, or null when it names nothing that moves. */
    fun code(value: String): String? {
        if (value == oldPackage) return newPackage
        authorities[value]?.let { return it }
        if (!value.startsWith("content://")) return null
        val rest = value.removePrefix("content://")
        val authority = rest.substringBefore('/')
        val path = rest.substring(authority.length)
        val moved = if (authority == oldPackage) newPackage else authorities[authority] ?: return null
        return "content://$moved$path"
    }
}

private var renaming: Renaming? = null

/**
 * Code strings that name an identity which moves, by original value, as counted in this Messages
 * build. Everything else Messages derives from getPackageName() at run time. Logger names that
 * happen to equal an authority are counted too; renaming them only changes a log tag.
 */
private val expectedCodeStrings = mapOf(
    MESSAGES_PACKAGE to 60,
    "content://$MESSAGES_PACKAGE" to 3,
    "content://$MESSAGES_PACKAGE.shared.datamodel.BugleContentProvider" to 1,
    "content://$MESSAGES_PACKAGE.shared.datamodel.provider.sharedstorage.SharedStorageProvider" to 1,
    "$MESSAGES_PACKAGE.shared.datamodel.MediaScratchFileProvider" to 2,
    "$MESSAGES_PACKAGE.shared.datamodel.provider.RbmBusinessInfoFileProvider" to 1,
    "$MESSAGES_PACKAGE.shared.datamodel.richcard.RichCardMediaFileProvider" to 1,
    "com.google.android.libraries.compose.core.data.content" to 8,
    "com.google.android.rcs.service.fileprovider" to 1,
    "content://com.google.android.rcs.service.preferences" to 1,
    "com.google.android.rcs.client.businessinfo" to 1,
    "content://com.google.android.rcs.client.businessinfo/businessinfo" to 1,
    "content://com.google.android.rcs.client.businessinfo/businessinfoproperties" to 1,
)
private val expectedFieldValues = mapOf(
    "com.google.android.rcs.client.businessinfo" to 1,
    "content://com.google.android.rcs.client.businessinfo/" to 1,
)

private fun constString(instruction: Instruction): String? =
    if (instruction.opcode == Opcode.CONST_STRING || instruction.opcode == Opcode.CONST_STRING_JUMBO) {
        ((instruction as ReferenceInstruction).reference as? StringReference)?.string
    } else null

/** Points the strings in Messages' code at the moved authorities and package. */
private val codeIdentitiesPatch = bytecodePatch {
    dependsOn(originalInput)

    finalize {
        val run = renaming ?: return@finalize
        val owners = mutableListOf<ClassDef>()
        classDefForEach { cls ->
            val code = cls.methods.any { method ->
                method.implementation?.instructions?.any { ins -> constString(ins)?.let(run::code) != null } == true
            }
            val fields = cls.fields.any { field -> (field.initialValue as? StringEncodedValue)?.value?.let(run::code) != null }
            if (code || fields) owners += cls
        }
        val codeCounts = linkedMapOf<String, Int>()
        val fieldCounts = linkedMapOf<String, Int>()
        for (owner in owners) {
            val cls = mutableClassDefBy(owner)
            for (method in cls.methods) {
                val instructions = method.implementation?.instructions?.toList() ?: continue
                instructions.forEachIndexed { index, instruction ->
                    val old = constString(instruction) ?: return@forEachIndexed
                    val new = run.code(old) ?: return@forEachIndexed
                    val register = (instruction as OneRegisterInstruction).registerA
                    val opcode = if (instruction.opcode == Opcode.CONST_STRING_JUMBO) "const-string/jumbo" else "const-string"
                    method.replaceInstruction(index, "$opcode v$register, \"$new\"")
                    codeCounts[old] = (codeCounts[old] ?: 0) + 1
                }
            }
            for (field in cls.fields) {
                val old = (field.initialValue as? StringEncodedValue)?.value ?: continue
                val new = run.code(old) ?: continue
                field.initialValue = MutableStringEncodedValue(ImmutableStringEncodedValue(new))
                fieldCounts[old] = (fieldCounts[old] ?: 0) + 1
            }
        }
        if (codeCounts != expectedCodeStrings || fieldCounts != expectedFieldValues) {
            throw PatchException(
                "Messages code names its own identity differently than expected: strings $codeCounts, fields $fieldCounts",
            )
        }
    }
}

/**
 * Installs Messages under its own package name, beside the Google Messages that most phones have
 * built in and signed by Google (a patched copy under the stock name cannot install there).
 *
 * Moves everything two installed apps may not share: the package, all provider authorities
 * (including the RCS, IMS and Compose ones in Google's wider namespace), all custom permissions and
 * the references to them, the share-sheet authority resource, static shortcuts, and the code strings
 * that name those identities. Runs last, so values other patches add are moved too.
 *
 * Deliberately left alone: class and activity-alias names (real class names), intent actions,
 * the queries naming stock Messages, the Google settings (Phenotype) registrations and the
 * deep-link host.
 */
@Suppress("unused")
val changePackageNamePatch = resourcePatch(
    name = "Change package name",
    description = "Experimental. Installs Messages under its own package name, alongside the stock Google Messages. " +
        "On by default, because a patched copy cannot replace the Google Messages that comes with the phone.",
    default = true,
) {
    compatibleWith(messages)
    dependsOn(originalInput, codeIdentitiesPatch)

    val packageName by stringOption(
        key = "packageName",
        default = MESSAGES_MORPHE_PACKAGE,
        title = "Package name",
        description = "The package name to install under. Must be a valid Android package name that differs from $MESSAGES_PACKAGE.",
        required = true,
    ) {
        it != null && it != MESSAGES_PACKAGE && !it.startsWith("$MESSAGES_PACKAGE.") && !it.startsWith(GOOGLE_NAMESPACE) &&
            it.length <= 100 && it.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$"))
    }

    execute {
        val old = MESSAGES_PACKAGE
        val new = packageName!!
        val authorities = linkedMapOf<String, String>()
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.elements("application").single()
            for (provider in manifest.elements("provider").filter { it.parentNode == application }) {
                for (authority in provider.getAttribute("android:authorities").split(';')) {
                    if (authority.startsWith("@")) continue
                    authorities[authority] = movedIdentity(authority, old, new)
                }
            }
        }
        // The share sheet's FileProvider takes its authority from a string resource.
        document("res/values/strings.xml").use { strings ->
            val entry = strings.elements("string").singleOrNull { it.getAttribute("name") == SHARING_AUTHORITY }
                ?: throw PatchException("Messages sharing authority resource is missing")
            authorities[entry.textContent] = movedIdentity(entry.textContent, old, new)
        }
        renaming = Renaming(old, new, authorities)
    }

    finalize {
        val old = MESSAGES_PACKAGE
        val new = packageName!!
        val counts = linkedMapOf<String, Int>()
        fun count(what: String) { counts[what] = (counts[what] ?: 0) + 1 }

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement
            if (root.getAttribute("package") != old) throw PatchException("Messages package was already changed")
            root.setAttribute("package", new); count("package")

            val ownPermissions = manifest.elements("permission").associate {
                val name = it.getAttribute("android:name")
                name to movedIdentity(name, old, new)
            }
            for (permission in manifest.elements("permission")) {
                permission.setAttribute("android:name", ownPermissions.getValue(permission.getAttribute("android:name"))); count("permission")
            }
            for (element in manifest.elements("*")) {
                if (element.tagName == "permission") continue
                for (attribute in listOf("android:name", "android:permission", "android:readPermission", "android:writePermission")) {
                    if (attribute == "android:name" && element.tagName != "uses-permission") continue
                    val moved = ownPermissions[element.getAttribute(attribute)] ?: continue
                    element.setAttribute(attribute, moved); count("permission reference")
                }
            }

            val application = manifest.elements("application").single()
            for (provider in manifest.elements("provider").filter { it.parentNode == application }) {
                val value = provider.getAttribute("android:authorities")
                if (value.startsWith("@")) continue
                provider.setAttribute("android:authorities", value.split(';').joinToString(";") { movedIdentity(it, old, new) })
                count("authorities")
            }

            // The satellite data property names this app itself.
            for (meta in manifest.elements("meta-data").filter { it.parentNode == application }) {
                if (meta.getAttribute("android:value") == old) { meta.setAttribute("android:value", new); count("self meta-data") }
            }

            if (manifest.elements("*").any { element -> element.getAttribute("android:process").contains(old) } ||
                root.hasAttribute("android:sharedUserId")
            ) throw PatchException("Messages now names its package in a process or shared user")
        }

        document("res/values/strings.xml").use { strings ->
            val entry = strings.elements("string").single { it.getAttribute("name") == SHARING_AUTHORITY }
            entry.textContent = movedIdentity(entry.textContent, old, new); count("sharing authority")
        }

        document("res/xml/shortcuts.xml").use { shortcuts ->
            for (element in shortcuts.elements("*")) {
                if (element.getAttribute("android:targetPackage") == old) {
                    element.setAttribute("android:targetPackage", new); count("shortcut package")
                }
            }
        }

        // Measured against this Messages build; anything else means the manifest changed shape.
        val expected = mapOf(
            "package" to 1, "permission" to 6, "permission reference" to 6, "authorities" to 24,
            "self meta-data" to 1, "sharing authority" to 1, "shortcut package" to 1,
        )
        if (counts != expected) throw PatchException("Messages package rename found $counts, expected $expected")
    }
}
