package patches.ytmusic

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import org.w3c.dom.Element

private const val SUPPORT = "Lpatches/ytmusic/GearslipSupport;"
private const val BROWSER = "Lcom/google/android/apps/youtube/music/mediabrowser/MusicBrowserService;"
private const val HOST = "app.seb3thehacker.gearslip"
private const val ANDROID = "http://schemas.android.com/apk/res/android"
private const val ORIGINAL_ELIGIBILITY = "morpheOriginalBrowserEligibility"
private val packageNamePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

private data class Layout(val versionCode: String, val trust: String, val caller: String, val client: String, val root: String)
private val layouts = mapOf(
    "9.30.53" to Layout("93053240", "Lkyh;", "Lawld;", "Lawjm;", "Lcbi;"),
    "9.44.52" to Layout("94452240", "Lhdv;", "Lzph;", "Lznr;", "Lbnc;"),
)
private val packages = setOf("com.google.android.apps.youtube.music", "app.morphe.android.apps.youtube.music")
private val compatibility = packages.map { name ->
    Compatibility(
        name = "YouTube Music", packageName = name,
        apkFileType = if (name == "com.google.android.apps.youtube.music") ApkFileType.APKM else ApkFileType.APK,
        targets = layouts.map { (version, layout) -> AppTarget(version = version, versionCode = layout.versionCode.toInt()) },
    )
}

@Suppress("unused")
val gearslipMusicSupport = bytecodePatch(
    name = "Gearslip music browser support",
    description = "Allows installed Gearslip hosts to browse YouTube Music using their actual Android caller UID, without Google Android Auto.",
) {
    compatibleWith(*compatibility.toTypedArray())
    extendWith("extensions/gearslip-ytm.mpe")

    val packageNames by stringOption(
        key = "packageNames", default = HOST, title = "Trusted Gearslip packages",
        description = "Comma-separated installed Gearslip host package names. Each caller must own its package under its actual callback UID.",
        required = true,
    ) { value ->
        value != null && value.length <= 2000 && value.split(',').all { packageNamePattern.matches(it.trim()) }
    }

    val visibility = resourcePatch {
        execute {
            val layout = layouts[packageMetadata.versionName] ?: error("Unsupported YouTube Music version")
            check(packageMetadata.packageName in packages && packageMetadata.versionCode == layout.versionCode) {
                "Unsupported YouTube Music package or build"
            }
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
        val layout = layouts[packageMetadata.versionName] ?: error("Unsupported YouTube Music version")
        check(packageMetadata.packageName in packages && packageMetadata.versionCode == layout.versionCode) {
            "Unsupported YouTube Music package or build"
        }
        val trust = Fingerprint(definingClass = layout.trust, name = "g", returnType = "Z",
            parameters = listOf(layout.caller, layout.client, "I")).matchAll(1..1).single().method
        val eligible = Fingerprint(definingClass = layout.trust, name = "i", returnType = "Z",
            parameters = listOf(layout.client)).matchAll(1..1).single().method
        val browser = Fingerprint(definingClass = BROWSER, name = "a", returnType = layout.root,
            parameters = listOf("Ljava/lang/String;", "I", "Landroid/os/Bundle;")).matchAll(1..1).single().method
        fun calls(method: com.android.tools.smali.dexlib2.iface.Method, descriptor: String) =
            method.implementation!!.instructions.count {
                ((it as? ReferenceInstruction)?.reference as? MethodReference)?.toString() == descriptor
            }
        check(calls(trust, "Landroid/content/pm/PackageManager;->getPackagesForUid(I)[Ljava/lang/String;") == 1) {
            "${layout.trust}.g: expected exactly one original UID ownership lookup"
        }
        check(trust.implementation!!.instructions.any { (it as? WideLiteralInstruction)?.wideLiteral == 45811789L }) {
            "${layout.trust}.g: expected server experiment 45811789"
        }
        check(calls(browser, "${layout.trust}->g(${layout.caller}${layout.client}I)Z") == 2) {
            "$BROWSER.a: expected exactly two caller trust checks"
        }
        check(calls(eligible, "Ljava/util/Set;->contains(Ljava/lang/Object;)Z") == 1) {
            "${layout.trust}.i: expected exactly one original client-list membership check"
        }
        val trustRegisters = trust.implementation!!.registerCount
        check(trustRegisters - 4 >= 2 && trustRegisters <= 16) {
            "${layout.trust}.g: compact invoke requires two locals and at most 16 registers; found $trustRegisters"
        }
        check(eligible.implementation!!.registerCount == 2) {
            "${layout.trust}.i: expected verified two-register original body; found ${eligible.implementation!!.registerCount}"
        }
        check(classDefBy(layout.trust).fields.count { it.name == "q" && it.type == "Landroid/content/Context;" } == 1) {
            "${layout.trust}: expected unique PackageManager Context field q"
        }
        for (type in listOf(layout.caller, layout.client)) {
            check(classDefBy(type).fields.count { it.name == "b" && it.type == "Ljava/lang/String;" } == 1) {
                "$type: expected unique caller/client package String field b"
            }
        }
        val configured = mutableClassDefBy(SUPPORT).methods.single {
            it.name == "getConfiguredPackageNames" && it.parameterTypes.isEmpty() && it.returnType == "Ljava/lang/String;"
        }
        configured.addInstructions(0, "const-string v0, \"${packageNames!!.split(',').joinToString(",") { it.trim() }}\"\nreturn-object v0")

        // p3 is onGetRoot's captured calling UID. Calling Binder.getCallingUid() here would
        // observe the handler thread's identity instead of the original browser caller.
        trust.addInstructionsWithLabels(0, """
            iget-object v0, p0, ${layout.trust}->q:Landroid/content/Context;
            iget-object v1, p1, ${layout.caller}->b:Ljava/lang/String;
            invoke-static { v0, v1, p3 }, $SUPPORT->decision(Landroid/content/Context;Ljava/lang/String;I)I
            move-result v0
            if-ltz v0, :original_caller_policy
            return v0
        """, ExternalLabel("original_caller_policy", trust.implementation!!.instructions.first()))

        // Both verified i() bodies have no local registers: v0/v1 are p0/p1. Preserve the
        // entire original method, including monitor/catch instructions, under a unique name
        // and give the wrapper its own local. Increasing the original register count would
        // silently shift its parameter registers and corrupt the fallback.
        val owner = mutableClassDefBy(layout.trust)
        check(owner.methods.none { it.name == ORIGINAL_ELIGIBILITY }) {
            "${layout.trust}: original browser eligibility helper already exists"
        }
        val original = ImmutableMethod(layout.trust, ORIGINAL_ELIGIBILITY, eligible.parameters,
            eligible.returnType, eligible.accessFlags, eligible.annotations, eligible.hiddenApiRestrictions,
            eligible.implementation).toMutable()
        val wrapper = ImmutableMethod(layout.trust, eligible.name, eligible.parameters,
            eligible.returnType, eligible.accessFlags, eligible.annotations, eligible.hiddenApiRestrictions,
            MutableMethodImplementation(3)).toMutable()
        // Eligibility is not authentication: g() above still checks every configured caller's
        // UID. Keep the real client package for subsequent browser callbacks and root IDs.
        wrapper.addInstructions(0, """
            iget-object v0, p1, ${layout.client}->b:Ljava/lang/String;
            invoke-static { v0 }, $SUPPORT->isConfiguredPackage(Ljava/lang/String;)Z
            move-result v0
            if-eqz v0, :original_browser_eligibility
            return v0
            :original_browser_eligibility
            invoke-virtual { p0, p1 }, ${layout.trust}->$ORIGINAL_ELIGIBILITY(${layout.client})Z
            move-result v0
            return v0
        """)
        check(owner.methods.remove(eligible)) { "${layout.trust}: original eligibility method disappeared before wrapping" }
        owner.methods.add(original)
        owner.methods.add(wrapper)
    }
}
