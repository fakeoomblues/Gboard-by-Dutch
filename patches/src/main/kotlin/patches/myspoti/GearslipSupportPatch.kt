package patches.myspoti

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import org.w3c.dom.Element

private const val SERVICE = "Lapp/myspoti/host/android/PlaybackService;"
private const val SUPPORT = "Lpatches/myspoti/GearslipSupport;"
private const val ANDROID = "http://schemas.android.com/apk/res/android"

@Suppress("unused")
val gearslipMyspotiSupport = bytecodePatch(
    name = "Gearslip personal library",
    description = "Allows the configured Gearslip host to use Myspoti's signed-in library, search and playback while retaining Android caller verification.",
) {
    compatibleWith(Compatibility(name = "Myspoti", packageName = "app.myspoti.stable", apkFileType = ApkFileType.APK,
        targets = listOf(AppTarget(version = null))))
    extendWith("extensions/gearslip-myspoti.mpe")
    val packageNames by stringOption(
        key = "packageNames", default = "app.seb3thehacker.gearslip,app.seb3thehacker.gearslip.dev,app.johninnl.drivedeck,app.johninnl.drivedeck.dev", title = "Trusted Gearslip and DriveDeck packages",
        description = "Comma-separated Gearslip or DriveDeck host packages. Myspoti retains its UID ownership checks for browsing and search.",
        required = true,
    ) { value -> value != null && value.length <= 2000 && value.split(',').all {
        Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$").matches(it.trim())
    } }
    val visibility = resourcePatch {
        execute {
            document("AndroidManifest.xml").use { manifest ->
                val queries = (manifest.getElementsByTagName("queries").item(0) as? Element)
                    ?: manifest.createElement("queries").also(manifest.documentElement::appendChild)
                val existing = queries.getElementsByTagName("package")
                packageNames!!.split(',').map(String::trim).distinct().forEach { host ->
                    if ((0 until existing.length).none {
                        (existing.item(it) as Element).getAttributeNS(ANDROID, "name") == host
                    }) queries.appendChild(manifest.createElement("package").apply {
                        setAttributeNS(ANDROID, "android:name", host)
                    })
                }
            }
        }
    }
    dependsOn(visibility)
    execute {
        val owner = mutableClassDefBy(SERVICE)
        val dispatchMethods = mutableListOf<com.android.tools.smali.dexlib2.iface.Method>()
        classDefForEach { cls -> if (cls.type != SERVICE) dispatchMethods.addAll(cls.methods) }
        val discovered = BrowserDiscovery.discover(owner, dispatchMethods)
        val predicate = owner.methods.single { it.toString() == discovered.toString() }
        val originalName = "morpheOriginalBrowsePermission"
        check(owner.methods.none { it.name == originalName }) { "Myspoti Gearslip browser support is already applied" }
        val configured = mutableClassDefBy(SUPPORT).methods.single { it.name == "getConfiguredPackageNames" }
        configured.addInstructions(0, "const-string v0, \"${packageNames!!.split(',').joinToString(",") { it.trim() }}\"\nreturn-object v0")
        val original = ImmutableMethod(predicate.definingClass, originalName, predicate.parameters,
            predicate.returnType, predicate.accessFlags, predicate.annotations, predicate.hiddenApiRestrictions,
            predicate.implementation).toMutable()
        val wrapper = ImmutableMethod(predicate.definingClass, predicate.name, predicate.parameters,
            predicate.returnType, predicate.accessFlags, predicate.annotations, predicate.hiddenApiRestrictions,
            MutableMethodImplementation(3)).toMutable()
        val invoke = if (AccessFlags.PRIVATE.isSet(predicate.accessFlags)) "invoke-direct" else "invoke-virtual"
        wrapper.addInstructions(0, """
            invoke-static { p1 }, $SUPPORT->isConfiguredPackage(Ljava/lang/String;)Z
            move-result v0
            if-eqz v0, :original_permission
            return v0
            :original_permission
            $invoke { p0, p1 }, $SERVICE->$originalName(Ljava/lang/String;)Z
            move-result v0
            return v0
        """)
        check(owner.methods.remove(predicate))
        owner.methods.add(original)
        owner.methods.add(wrapper)
    }
}
