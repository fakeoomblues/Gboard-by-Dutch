package patches.wuplaymobile

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.*
import org.w3c.dom.Element
import patches.shared.originalInput
import patches.shared.uniqueMatch

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
private val wuplayMobile = Compatibility(
    name = "WuPlay Mobile", packageName = "app.wuplay.android", apkFileType = ApkFileType.APK,
    formFactor = FormFactor.MOBILE,
    signatures = setOf("ae406f7d398ae1ae418238f83d07554e6aa6cf5855b66edb9c4c85995d435e7f"),
    targets = listOf(AppTarget(version = "0.1.9-alpha", versionCode = 9, minSdk = 24)),
)
private val mobileInput = originalInput(wuplayMobile)
private val carInterfaceResources = resourcePatch {
    dependsOn(mobileInput)
    execute {
        document("AndroidManifest.xml").use { xml ->
            val root = xml.documentElement
            val application = xml.getElementsByTagName("application").item(0) as Element
            val component = "nl.johninnl.wuplaycar.WuplayCarService"
            check((0 until xml.getElementsByTagName("service").length).none {
                (xml.getElementsByTagName("service").item(it) as Element).getAttributeNS(ANDROID_NS,"name") == component
            }) { "WuPlay car interface already installed" }
            for (permission in listOf("FOREGROUND_SERVICE", "FOREGROUND_SERVICE_MEDIA_PLAYBACK", "POST_NOTIFICATIONS")) {
                val node = xml.createElement("uses-permission")
                node.setAttributeNS(ANDROID_NS,"android:name","android.permission.$permission")
                root.appendChild(node)
            }
            for (permission in listOf("ACCESS_SURFACE", "NAVIGATION_TEMPLATES")) {
                val node = xml.createElement("uses-permission")
                node.setAttributeNS(ANDROID_NS,"android:name","androidx.car.app.$permission")
                root.appendChild(node)
            }
            val queries = (xml.getElementsByTagName("queries").item(0) as? Element)
                ?: xml.createElement("queries").also { root.appendChild(it) }
            for (host in listOf("app.seb3thehacker.gearslip", "app.seb3thehacker.gearslip.dev")) {
                val node = xml.createElement("package")
                node.setAttributeNS(ANDROID_NS,"android:name",host); queries.appendChild(node)
            }
            val service = xml.createElement("service")
            service.setAttributeNS(ANDROID_NS,"android:name",component)
            service.setAttributeNS(ANDROID_NS,"android:exported","true")
            service.setAttributeNS(ANDROID_NS,"android:label","WuPlay Car")
            service.setAttributeNS(ANDROID_NS,"android:foregroundServiceType","mediaPlayback")
            val filter = xml.createElement("intent-filter")
            val action = xml.createElement("action"); action.setAttributeNS(ANDROID_NS,"android:name","androidx.car.app.CarAppService")
            val category = xml.createElement("category"); category.setAttributeNS(ANDROID_NS,"android:name","androidx.car.app.category.NAVIGATION")
            filter.appendChild(action); filter.appendChild(category); service.appendChild(filter); application.appendChild(service)
            val api = xml.createElement("meta-data")
            api.setAttributeNS(ANDROID_NS,"android:name","androidx.car.app.minCarApiLevel"); api.setAttributeNS(ANDROID_NS,"android:value","5")
            application.appendChild(api)
        }
        for (name in listOf("NOTICE.txt", "Apache-2.0.txt")) {
            val destination = get("assets/morphe-wuplay-car/$name")
            destination.parentFile.mkdirs()
            MobileIdentity::class.java.getResourceAsStream("/licenses/wuplay-car/$name").use { source ->
                checkNotNull(source) { "Car interface license resource missing" }
                destination.outputStream().use { output -> source.copyTo(output) }
            }
        }
    }
}

@Suppress("unused")
val wuplayGearslip = bytecodePatch(
    name = "Add Gearslip car interface",
    description = "Browse the selected WuPlay profile and play videos with a separate Gearslip car interface. Uses native WuPlay add-ons and playback; no root or phone-screen mirroring.",
    default = false,
) {
    compatibleWith(wuplayMobile)
    dependsOn(mobileInput, carInterfaceResources)
    extendWith("extensions/wuplay-gearslip.mpe")
    execute {
        val container = Fingerprint(definingClass = "Lapp/wuplay/androidtv/AppContainer;", name = "<init>",
            parameters = listOf("Lapp/wuplay/android/MobileApplication;", "Ljava/lang/Integer;"), returnType = "V").uniqueMatch().method
        MobileIdentity.retain(container)
        val continuation = "Lkotlin/coroutines/jvm/internal/ContinuationImpl;"
        val classes = mapOf(
            "Lapp/wuplay/android/MobileApplication;" to mapOf("container" to "Lapp/wuplay/androidtv/AppContainer;"),
            "Lapp/wuplay/androidtv/AppContainer;" to mapOf("catalogRepository" to "Lapp/wuplay/androidtv/data/CatalogRepositoryImpl;", "addonRepository" to "Lapp/wuplay/androidtv/data/AddonRepository;"),
            "Lapp/wuplay/androidtv/domain/stremio/MetaResponse;" to mapOf("meta" to "Lapp/wuplay/androidtv/domain/stremio/Meta;"),
            "Lapp/wuplay/androidtv/domain/stremio/Meta;" to mapOf("videos" to "Ljava/util/List;", "description" to "Ljava/lang/String;"),
            "Lapp/wuplay/androidtv/domain/model/Profile;" to mapOf("profileKey" to "Ljava/lang/String;", "hasPinSet" to "Z"),
            "Lapp/wuplay/androidtv/data/CatalogRepositoryImpl;" to mapOf("definitions" to "Lkotlinx/coroutines/flow/Flow;"),
            "Lapp/wuplay/androidtv/data/UserSettingsRepositoryImpl;" to mapOf("activeProfileId" to "Lkotlinx/coroutines/flow/Flow;", "activeProfile" to "Lkotlinx/coroutines/flow/Flow;"),
            "Lapp/wuplay/androidtv/data/AddonRepository;" to mapOf("enabledAddons" to "Lkotlinx/coroutines/flow/Flow;"),
        )
        for ((owner, fields) in classes) for ((name,type) in fields) {
            check(mutableClassDefBy(owner).fields.count { it.name == name && it.type == type } == 1) { "WuPlay car ABI changed: $owner->$name" }
        }
        Fingerprint(definingClass = "Lapp/wuplay/androidtv/data/CatalogRepositoryImpl;", name = "fetchContent-BWLJW6A",
            parameters = listOf("Lapp/wuplay/androidtv/domain/model/CatalogDefinition;", "Ljava/util/Map;", "Ljava/lang/String;", continuation), returnType = "Ljava/lang/Object;").uniqueMatch()
        Fingerprint(definingClass = "Lapp/wuplay/androidtv/data/AddonRepository;", name = "fetchStreams-hUnOzRk",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/util/Map;", "Ljava/lang/Integer;", continuation), returnType = "Ljava/lang/Object;").uniqueMatch()
        Fingerprint(definingClass = "Lapp/wuplay/androidtv/playback/PlaybackFacade;", name = "buildPlayer\$default",
            parameters = listOf("Lapp/wuplay/androidtv/playback/PlaybackFacade;", "Lapp/wuplay/androidtv/playback/EngineConfig;"), returnType = "Landroidx/media3/exoplayer/ExoPlayerImpl;").uniqueMatch()
        Fingerprint(definingClass = "Landroidx/compose/runtime/snapshots/SnapshotKt;", name = "access\$advanceGlobalSnapshot",
            parameters = emptyList(), returnType = "V").uniqueMatch()
        Fingerprint(definingClass = "Landroidx/media3/exoplayer/ExoPlayerImpl;", name = "setVideoSurfaceHolder",
            parameters = listOf("Landroid/view/SurfaceHolder;"), returnType = "V").uniqueMatch()
        Fingerprint(definingClass = "Landroidx/compose/runtime/Recomposer\$\$ExternalSyntheticLambda4;", name = "<init>",
            parameters = listOf("Ljava/lang/Object;", "I", "Ljava/lang/Object;"), returnType = "V").uniqueMatch()
        Fingerprint(definingClass = "Landroidx/navigation/compose/NavHostKt\$NavHost\$16\$1\$invoke\$\$inlined\$onDispose\$1;", name = "dispose",
            parameters = emptyList(), returnType = "V").uniqueMatch()
    }
}
