package patches.waze

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.smali.ExternalLabel
import org.w3c.dom.Element
import patches.waze.CatalogCompatibility.COMPATIBILITY_WAZE

/** Gearslip and its DriveDeck rebrand, release and development builds. */
private val CAR_HOST_PACKAGES = listOf(
    "app.seb3thehacker.gearslip", "app.seb3thehacker.gearslip.dev",
    "app.johninnl.drivedeck", "app.johninnl.drivedeck.dev",
)

/** HostValidator cannot look up a package Waze is not allowed to see. */
private val gearslipVisibility = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { doc ->
            val queries = doc.getElementsByTagName("queries").item(0) as? Element
                ?: doc.createElement("queries").also { doc.documentElement.appendChild(it) }
            val packages = queries.getElementsByTagName("package")
            for (host in CAR_HOST_PACKAGES) if ((0 until packages.length).none {
                (packages.item(it) as Element).getAttribute("android:name") == host
            }) queries.appendChild(doc.createElement("package").apply {
                setAttribute("android:name", host)
            })
        }
    }
}

@Suppress("unused")
val wazeGearslipPatch = bytecodePatch(
    name = "Support Gearslip",
    description = "Accepts Gearslip and DriveDeck as car hosts by package name, whichever key signed them, so " +
        "a patched Gearslip keeps working. Other hosts keep Android Auto host validation.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_WAZE)
    dependsOn(gearslipVisibility)

    execute {
        // Runs after validateHost has tied the package name to the caller's UID.
        HostValidatorAllowListFingerprint.method.apply {
            if (implementation!!.registerCount <= parameterTypes.size + 1)
                throw PatchException("Waze host allowlist check has no free register")
            addInstructionsWithLabels(0, CAR_HOST_PACKAGES.joinToString("\n") { host -> """
                const-string v0, "$host"
                invoke-virtual { v0, p1 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-nez v0, :car_host
            """ } + """
                goto :original_allowlist
                :car_host
                const/4 v0, 0x1
                return v0
            """, ExternalLabel("original_allowlist", getInstruction(0)))
        }
    }
}
