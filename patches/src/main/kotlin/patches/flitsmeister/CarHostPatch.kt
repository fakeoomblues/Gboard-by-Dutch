package patches.flitsmeister

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.smali.ExternalLabel
import org.w3c.dom.Element

private const val ANDROID = "http://schemas.android.com/apk/res/android"

/** Gearslip and its DriveDeck rebrand, release and development builds. */
private val CAR_HOST_PACKAGES = listOf(
    "app.seb3thehacker.gearslip", "app.seb3thehacker.gearslip.dev",
    "app.johninnl.drivedeck", "app.johninnl.drivedeck.dev",
)

/** HostValidator reads the host's PackageInfo before it reaches the allowlist. */
private val carHostVisibility = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { doc ->
            val queries = doc.getElementsByTagName("queries").item(0) as? Element
                ?: doc.createElement("queries").also { doc.documentElement.appendChild(it) }
            val packages = queries.getElementsByTagName("package")
            for (host in CAR_HOST_PACKAGES) if ((0 until packages.length).none {
                (packages.item(it) as Element).getAttributeNS(ANDROID, "name") == host
            }) queries.appendChild(doc.createElement("package").apply {
                setAttributeNS(ANDROID, "android:name", host)
            })
        }
    }
}

@Suppress("unused")
val carHostPatch = bytecodePatch(
    name = "Support Gearslip and DriveDeck",
    description = "Accepts Gearslip and DriveDeck as car hosts by package name, whichever key signed them, " +
        "so DriveDeck works without the original Gearslip installed. Other hosts keep Android Auto host validation.",
    default = true,
) {
    compatibleWith(flitsmeister)
    dependsOn(originalInput, startupCompatibility, carHostVisibility)
    execute {
        // HostValidator.validateHost resolves the package, ties its UID to the Binder caller and
        // only then asks isAllowListed(package, signatures). Accepting the package here keeps
        // forged package claims rejected.
        val method = exact("Landroidx/car/app/validation/HostValidator;", "isAllowListed", "Z",
            "Ljava/lang/String;", "[Landroid/content/pm/Signature;").method
        val implementation = method.implementation ?: throw PatchException("Flitsmeister host allowlist has no code")
        if (implementation.registerCount <= method.parameterTypes.size + 1)
            throw PatchException("Flitsmeister host allowlist check has no free register")
        method.addInstructionsWithLabels(0, CAR_HOST_PACKAGES.joinToString("\n") { host -> """
            const-string v0, "$host"
            invoke-virtual { v0, p1 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v0
            if-nez v0, :car_host
        """ } + """
            goto :original_allowlist
            :car_host
            const/4 v0, 0x1
            return v0
        """, ExternalLabel("original_allowlist", method.getInstruction(0)))
    }
}
