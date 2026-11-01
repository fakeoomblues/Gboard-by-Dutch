package patches.gearslip

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val TEMPLATE = "Lapp/seb3thehacker/gearslip/host/TemplateApp;"
private const val MEDIA = "Lapp/seb3thehacker/gearslip/media/MediaApp;"
private const val SCREEN = "Lapp/seb3thehacker/gearslip/car/AppTile\$Screen;"
private const val PLAYER = "Lapp/seb3thehacker/gearslip/car/AppTile\$Player;"
private const val OWNER = "Lapp/seb3thehacker/gearslip/car/CallOverlayKt;"
private const val KNOWN = "Lapp/seb3thehacker/gearslip/host/KnownApps;"
private const val LIST = "Ljava/util/List;"

private val gearslip = Compatibility(
    name = "Gearslip", packageName = "app.seb3thehacker.gearslip", apkFileType = ApkFileType.APK,
    appIconColor = 0x7C95D6,
    targets = listOf(AppTarget(version = "0.3.12-alpha", versionCode = 12), AppTarget(version = "0.3.31-alpha", versionCode = 13)),
)

@Suppress("unused")
val gearslipMusicPlayers = bytecodePatch(
    name = "Keep music player entries",
    description = "Keeps a music app's player available when it also provides a car Settings screen, including YT Music Morphe. Uses Gearslip's existing docked player.",
) {
    compatibleWith(gearslip)
    execute {
        val owner = mutableClassDefBy(OWNER)
        val tiles = owner.methods.singleOrNull {
            it.name == "access\$tilesFor" && it.returnType == LIST &&
                it.parameterTypes.map(CharSequence::toString) in listOf(
                    listOf(TEMPLATE, MEDIA, "Lapp/seb3thehacker/gearslip/notify/MessagingApp;"),
                    listOf(TEMPLATE, MEDIA, "Lapp/seb3thehacker/gearslip/notify/MessagingApp;", "Ljava/lang/String;"),
                )
        } ?: throw PatchException("Gearslip tile selection contract changed")
        if (tiles.implementation!!.registerCount <= tiles.parameterTypes.size ||
            tiles.accessFlags and AccessFlags.STATIC.value == 0) throw PatchException("Gearslip tile registers changed")
        for ((type, parameters) in listOf(PLAYER to listOf(MEDIA), SCREEN to listOf(TEMPLATE, "Ljava/lang/String;"))) {
            if (classDefBy(type).methods.none { it.name == "<init>" && it.parameterTypes.map(CharSequence::toString) == parameters }) {
                throw PatchException("Gearslip tile constructor changed: $type")
            }
        }
        val helper = "morpheSettingsPlayer"
        if (owner.methods.any { it.name == helper }) throw PatchException("Gearslip player patch already applied")
        owner.methods.add(ImmutableMethod(
            OWNER, helper,
            listOf(TEMPLATE, MEDIA).map { ImmutableMethodParameter(it, null, null) }, LIST,
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value, null, null, MutableMethodImplementation(7),
        ).toMutable().apply {
            addInstructions(0, """
                if-eqz p0, :original
                if-eqz p1, :original
                invoke-virtual { p0 }, $TEMPLATE->getKind()Ljava/lang/String;
                move-result-object v0
                const-string v1, "settings"
                invoke-virtual { v1, v0 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :original
                new-instance v0, Ljava/util/ArrayList;
                invoke-direct { v0 }, Ljava/util/ArrayList;-><init>()V
                new-instance v1, $PLAYER
                invoke-direct { v1, p1 }, $PLAYER-><init>($MEDIA)V
                invoke-interface { v0, v1 }, Ljava/util/List;->add(Ljava/lang/Object;)Z
                iget-object v2, p0, $TEMPLATE->label:Ljava/lang/String;
                const-string v3, " · Settings"
                invoke-virtual { v2, v3 }, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v2
                new-instance v1, $SCREEN
                invoke-direct { v1, p0, v2 }, $SCREEN-><init>(${TEMPLATE}Ljava/lang/String;)V
                invoke-interface { v0, v1 }, Ljava/util/List;->add(Ljava/lang/Object;)Z
                return-object v0
                :original
                const/4 v0, 0x0
                return-object v0
            """)
        })
        tiles.addInstructionsWithLabels(0, """
            invoke-static/range { p0 .. p1 }, $OWNER->$helper($TEMPLATE$MEDIA)$LIST
            move-result-object v0
            if-eqz v0, :original_tiles
            return-object v0
        """, ExternalLabel("original_tiles", tiles.implementation!!.instructions.first()))
    }
}

@Suppress("unused")
val gearslipHideApps = bytecodePatch(
    name = "Hide apps from drawer",
    description = "Hides selected packages from Gearslip's discovered app tiles. Does not disable or uninstall apps; Gearslip's built-in Settings remain available.",
    default = false,
) {
    compatibleWith(gearslip)
    val hiddenPackages by stringOption(
        key = "hiddenPackages", default = "", title = "Packages to hide",
        description = "Comma-separated Android package names. Empty keeps Gearslip's own exclusions only. All discovered tiles from each listed package are hidden.",
        required = false,
    ) { value -> value != null && (value.isBlank() || value.split(',').all {
        it.trim().matches(Regex("[A-Za-z][\\w]*(\\.[A-Za-z][\\w]*)+"))
    }) }
    execute {
        val packages = hiddenPackages.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty).distinct()
        if (packages.isNotEmpty()) {
            val owner = mutableClassDefBy(KNOWN)
            val init = owner.methods.single { it.name == "<clinit>" }
            val writes = init.implementation!!.instructions.withIndex().filter {
                it.value.opcode == Opcode.SPUT_OBJECT &&
                    ((it.value as? ReferenceInstruction)?.reference?.toString() == "$KNOWN->hidden:Ljava/util/Set;")
            }
            if (writes.size != 1) throw PatchException("Gearslip exclusion set initializer changed")
            val register = (writes.single().value as OneRegisterInstruction).registerA
            val helper = "morpheHiddenApps"
            if (owner.methods.any { it.name == helper }) throw PatchException("Gearslip drawer filter already applied")
            owner.methods.add(ImmutableMethod(
                KNOWN, helper, listOf(ImmutableMethodParameter("Ljava/util/Set;", null, null)), "Ljava/util/Set;",
                AccessFlags.PRIVATE.value or AccessFlags.STATIC.value, null, null, MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(0, buildString {
                    append("new-instance v0, Ljava/util/HashSet;\n")
                    append("invoke-direct { v0, p0 }, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V\n")
                    packages.forEach {
                        append("const-string v1, \"$it\"\n")
                        append("invoke-virtual { v0, v1 }, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z\n")
                    }
                    append("return-object v0")
                })
            })
            init.addInstructions(writes.single().index, """
                invoke-static/range { v$register .. v$register }, $KNOWN->$helper(Ljava/util/Set;)Ljava/util/Set;
                move-result-object v$register
            """)
        }
    }
}

