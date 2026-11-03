package org.ungoogled.patches.maps.car

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.smali.ExternalLabel
import org.ungoogled.patches.shared.Constants.COMPATIBILITY_MAPS
import org.w3c.dom.Element

private const val ORIGINAL = "com.google.android.apps.gmm.car.GmmCarProjectionService"
private const val LEGACY_PROVIDER = "com.google.android.apps.gmm.car.projected.service.CarNavigationProviderService"
private const val ADAPTER = "patches.maps.gearslip.NativeCarService"
private const val ANDROID = "http://schemas.android.com/apk/res/android"

private val nativeNavigationResources = resourcePatch {
    execute {
        var descriptor: String? = null
        document("AndroidManifest.xml").use { doc ->
            val root = doc.documentElement
            val application = doc.getElementsByTagName("application").item(0) as Element
            fun elements(tag: String): List<Element> = doc.getElementsByTagName(tag).let { nodes ->
                (0 until nodes.length).map { nodes.item(it) as Element }
            }
            val original = elements("service").singleOrNull { it.getAttributeNS(ANDROID, "name") == ORIGINAL }
                ?: throw PatchException("Maps' original Android Auto service changed")
            val provider = elements("service").singleOrNull { it.getAttributeNS(ANDROID, "name") == LEGACY_PROVIDER }
                ?: throw PatchException("Maps' internal navigation provider changed")
            // The CAL adapter is the external navigation provider. The old Google-private
            // provider must remain available to Maps' own renderer, but external discovery
            // triggers a CAR_SPEED permission preflight that a re-signed client cannot satisfy.
            provider.setAttributeNS(ANDROID, "android:exported", "false")
            val providerFilters = provider.getElementsByTagName("intent-filter").let { nodes ->
                (0 until nodes.length).map { nodes.item(it) as Element }
            }
            for (filter in providerFilters) {
                val categories = filter.getElementsByTagName("category")
                if ((0 until categories.length).any {
                    (categories.item(it) as Element).getAttributeNS(ANDROID, "name") == "com.google.android.car.category.NAVIGATION_PROVIDER"
                }) provider.removeChild(filter)
            }
            if (elements("service").any { it.getAttributeNS(ANDROID, "name") == ADAPTER })
                throw PatchException("Native Maps navigation is already applied")
            val carDescriptor = elements("meta-data").singleOrNull {
                it.parentNode == application && it.getAttributeNS(ANDROID, "name") == "com.google.android.gms.car.application"
            } ?: throw PatchException("Maps' automotive descriptor changed")
            descriptor = carDescriptor.getAttributeNS(ANDROID, "resource").takeIf { it.startsWith("@xml/") }
                ?.removePrefix("@xml/") ?: throw PatchException("Maps' automotive descriptor is not an XML resource")

            // Same process as the original projection controller: the private native connection
            // stays inside the patched Maps UID and never binds stock Maps as a fallback.
            application.appendChild(doc.createElement("service").apply {
                setAttributeNS(ANDROID, "android:name", ADAPTER)
                setAttributeNS(ANDROID, "android:exported", "true")
                setAttributeNS(ANDROID, "android:foregroundServiceType", "location")
                original.getAttributeNS(ANDROID, "process").takeIf(String::isNotEmpty)?.let {
                    setAttributeNS(ANDROID, "android:process", it)
                }
                appendChild(doc.createElement("intent-filter").apply {
                    appendChild(doc.createElement("action").apply { setAttributeNS(ANDROID, "android:name", "androidx.car.app.CarAppService") })
                    appendChild(doc.createElement("category").apply { setAttributeNS(ANDROID, "android:name", "androidx.car.app.category.NAVIGATION") })
                })
                appendChild(doc.createElement("intent-filter").apply {
                    appendChild(doc.createElement("action").apply { setAttributeNS(ANDROID, "android:name", "androidx.car.app.action.NAVIGATE") })
                    appendChild(doc.createElement("category").apply { setAttributeNS(ANDROID, "android:name", "android.intent.category.DEFAULT") })
                    appendChild(doc.createElement("data").apply { setAttributeNS(ANDROID, "android:scheme", "geo") })
                })
            })
            for (permission in listOf("androidx.car.app.NAVIGATION_TEMPLATES", "androidx.car.app.ACCESS_SURFACE")) {
                if (elements("uses-permission").none { it.getAttributeNS(ANDROID, "name") == permission })
                    root.appendChild(doc.createElement("uses-permission").apply { setAttributeNS(ANDROID, "android:name", permission) })
            }
            if (elements("meta-data").none { it.parentNode == application && it.getAttributeNS(ANDROID, "name") == "androidx.car.app.minCarApiLevel" })
                application.appendChild(doc.createElement("meta-data").apply {
                    setAttributeNS(ANDROID, "android:name", "androidx.car.app.minCarApiLevel")
                    setAttributeNS(ANDROID, "android:value", "5")
                })
            val queries = elements("queries").firstOrNull() ?: doc.createElement("queries").also(root::appendChild)
            for (host in listOf("app.seb3thehacker.gearslip", "app.seb3thehacker.gearslip.dev", "app.johninnl.drivedeck", "app.johninnl.drivedeck.dev", "com.google.android.projection.gearhead")) {
                if (elements("package").none { it.parentNode == queries && it.getAttributeNS(ANDROID, "name") == host })
                    queries.appendChild(doc.createElement("package").apply { setAttributeNS(ANDROID, "android:name", host) })
            }
        }
        // Preserve the original descriptor's capabilities and add its one real navigation
        // adapter. Gearslip's navigation precedence suppresses incidental messages/media tiles.
        document("res/xml/${descriptor!!}.xml").use { doc ->
            val uses = doc.getElementsByTagName("uses")
            if ((0 until uses.length).none { (uses.item(it) as Element).getAttribute("name") == "template" })
                doc.documentElement.appendChild(doc.createElement("uses").apply { setAttribute("name", "template") })
        }
        val controls = mapOf(
            "morphe_maps_more_controls" to ("More controls" to "Meer bediening"),
            "morphe_maps_control_settings" to ("Settings" to "Instellingen"),
            "morphe_maps_control_sound" to ("Sound" to "Geluid"),
            "morphe_maps_control_compass" to ("Compass" to "Kompas"),
            "morphe_maps_control_zoom_in" to ("Zoom in" to "Inzoomen"),
            "morphe_maps_control_zoom_out" to ("Zoom out" to "Uitzoomen"),
            "morphe_maps_control_recenter" to ("Recenter" to "Opnieuw centreren"),
            "morphe_maps_control_pan" to ("Move map" to "Kaart verplaatsen"),
            "morphe_maps_control_report" to ("Report incident" to "Incident melden"),
            "morphe_maps_control_charging" to ("Charging filters" to "Laadfilters"),
        )
        document("res/values/public.xml").use { doc ->
            val nodes = doc.getElementsByTagName("public")
            val entries = (0 until nodes.length).map { nodes.item(it) as Element }
            if (entries.any { it.getAttribute("name") in controls })
                throw PatchException("Maps native control labels already exist")
            val largest = entries.filter { it.getAttribute("type") == "string" }
                .maxOf { it.getAttribute("id").removePrefix("0x").toLong(16) }
            if (largest ushr 24 != 0x7fL || (largest and 0xffff) + controls.size > 0xffff)
                throw PatchException("Maps string resource table has no room for native control labels")
            controls.keys.forEachIndexed { index, name ->
                doc.documentElement.appendChild(doc.createElement("public").apply {
                    setAttribute("type", "string")
                    setAttribute("name", name)
                    setAttribute("id", "0x${(largest + index + 1).toString(16)}")
                })
            }
        }
        for ((qualifier, dutch) in listOf("values" to false, "values-nl" to true)) {
            // Morphe's incremental encoder assigns a values file to its named type.
            // Keep these entries in strings.xml rather than a custom values filename.
            val path = "res/$qualifier/strings.xml"
            get(path).apply { if (!exists()) { parentFile.mkdirs(); writeText("<resources/>") } }
            document(path).use { doc ->
                for ((name, labels) in controls) doc.documentElement.appendChild(doc.createElement("string").apply {
                    setAttribute("name", name)
                    textContent = if (dutch) labels.second else labels.first
                })
            }
        }
    }
}

@Suppress("unused")
val nativeCarNavigationPatch = bytecodePatch(
    name = "Native navigation for Gearslip and Android Auto",
    description = "Shows this patched Maps app's original Android Auto map and navigation UI through a native navigation service. Requires the full Android Auto app for its car UI resources.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_MAPS)
    dependsOn(nativeNavigationResources)
    extendWith("extensions/maps-gearslip.mpe")
    execute {
        fun requireMethod(type: String, name: String, parameters: List<String>, returns: String) {
            if (classDefBy(type).methods.none {
                it.name == name && it.returnType == returns && it.parameterTypes.map(CharSequence::toString) == parameters
            }) throw PatchException("Maps' native car ABI changed: $type->$name")
        }
        fun requireField(type: String, name: String, fieldType: String) {
            if (classDefBy(type).fields.none { it.name == name && it.type == fieldType })
                throw PatchException("Maps' native car field changed: $type->$name")
        }
        requireMethod("L$ORIGINAL;".replace('.', '/'), "b", emptyList(), "Ljava/lang/Class;")
        requireField("Lbeiy;", "a", "Ljava/lang/ref/WeakReference;")
        requireField("Lbemm;", "H", "I")
        requireField("Lbemm;", "J", "Lbehk;")
        requireField("Lbemm;", "k", "Lbenb;")
        requireField("Lbemm;", "C", "Lcom/google/android/gms/car/DrawingSpec;")
        requireField("Lbemm;", "o", "Landroid/content/res/Configuration;")
        requireField("Lbemm;", "v", "Landroid/graphics/Rect;")
        requireMethod("Lbemz;", "A", listOf("Landroid/graphics/Rect;"), "V")
        requireField("Lbemz;", "h", "Lbemu;")
        requireMethod("Lbemu;", "f", listOf("Landroid/view/Surface;"), "V")
        requireMethod("Lbemm;", "v", emptyList(), "V")
        requireMethod("Lbemm;", "t", listOf("Landroid/content/res/Resources;"), "V")
        requireMethod("Lltu;", "b", listOf("Landroid/content/res/Configuration;"), "V")
        requireMethod("Lbemm;", "o", listOf("Lcom/google/android/gms/car/display/CarRegionId;", "Lcom/google/android/gms/car/DrawingSpec;", "Landroid/content/Intent;", "Landroid/content/res/Configuration;", "Landroid/os/Bundle;"), "V")
        requireMethod("Lbemm;", "m", listOf("I", "Z"), "V")
        requireMethod("Lbemm;", "k", emptyList(), "V")
        requireMethod("Lcom/google/android/gms/car/DrawingSpec;", "<init>", listOf("I", "I", "I", "Landroid/view/Surface;", "Landroid/graphics/Rect;", "I", "I", "Landroid/os/Bundle;"), "V")

        // A re-signed Maps cannot use Google's private vehicle/sensor/message channel.
        // Its global CarConnectionHandler otherwise throws an uncaught SecurityException
        // while genuine AA is connected. Keep only that optional token disconnected;
        // the adapter's own native presentation uses a separate beld connection.
        requireField("Lbeil;", "d", "Lbejy;")
        requireField("Lpww;", "a", "Lpwx;")
        requireMethod("Lbeil;", "e", emptyList(), "V")
        val connect = mutableClassDefBy("Lbeil;").methods.single {
            it.name == "e" && it.parameterTypes.isEmpty() && it.returnType == "V"
        }
        if (connect.implementation!!.registerCount <= 1) throw PatchException("Maps' optional car token has no scratch register")
        connect.addInstructionsWithLabels(0, """
            iget-object v0, p0, Lbeil;->d:Lbejy;
            instance-of v0, v0, Lpww;
            if-eqz v0, :original_car_token
            return-void
        """, ExternalLabel("original_car_token", connect.implementation!!.instructions.first()))

        // Native CAL navigation uses the phone's location. Vehicle speed is optional and
        // remains truthfully unavailable when not granted; it must not block GPS routing
        // or send a re-signed client into an impossible CAR_SPEED permission request.
        requireField("Ltpr;", "a", "Z")
        requireField("Ltpr;", "b", "Z")
        requireMethod("Ltpr;", "a", emptyList(), "Z")
        val permissions = mutableClassDefBy("Ltpr;").methods.single {
            it.name == "a" && it.parameterTypes.isEmpty() && it.returnType == "Z"
        }
        if (permissions.implementation!!.registerCount <= 1) throw PatchException("Maps' car permission preflight has no scratch register")
        permissions.addInstructions(0, """
            iget-boolean v0, p0, Ltpr;->a:Z
            return v0
        """)
    }
}
