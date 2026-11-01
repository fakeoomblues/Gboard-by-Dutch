package patches.waze

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import patches.waze.CatalogCompatibility.COMPATIBILITY_WAZE

@Suppress("unused")
val wazeGearslipPatch = resourcePatch(
    name = "Support Gearslip",
    description = "Allows the official Gearslip release host while retaining Android Auto host validation.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_WAZE)
    execute {
        val trusted = "4cc56ebdd13a54d260eca9b7e2b2ae6b30e2f225bfda8f508f0709edf084948b,app.seb3thehacker.gearslip"
        document("AndroidManifest.xml").use { doc ->
            val queries = doc.getElementsByTagName("queries").item(0) as? Element
                ?: doc.createElement("queries").also { doc.documentElement.appendChild(it) }
            val packages = queries.getElementsByTagName("package")
            if ((0 until packages.length).none {
                (packages.item(it) as Element).getAttribute("android:name") == "app.seb3thehacker.gearslip"
            }) queries.appendChild(doc.createElement("package").apply {
                setAttribute("android:name", "app.seb3thehacker.gearslip")
            })
        }
        document("res/values/arrays.xml").use { doc ->
            val all = doc.getElementsByTagName("*")
            val arrays = (0 until all.length).mapNotNull { all.item(it) as? Element }.filter {
                it.tagName in setOf("array", "string-array") && it.getAttribute("name") == "hosts_allowlist"
            }
            val array = arrays.singleOrNull() ?: throw PatchException("Waze host allowlist changed")
            val items = array.getElementsByTagName("item")
            if ((0 until items.length).none { items.item(it).textContent.replace(" ", "").trim() == trusted }) {
                array.appendChild(doc.createElement("item").apply { textContent = trusted })
            }
        }
    }
}
