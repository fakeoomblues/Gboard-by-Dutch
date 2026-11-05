package patches.myspoti

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.shared.uniqueMatch

private const val SERVICE = "Lapp/myspoti/host/android/PlaybackService;"
private const val EXPORT = "Lnl/johninnl/myspoti/queue/QueueExport;"
private const val SONG = "Lcom/music/spotui/data/entity/SongsModel;"
private val queueTarget = Compatibility(
    name = "Myspoti", packageName = "app.myspoti.stable", apkFileType = ApkFileType.APK,
    signatures = setOf("2b8f54daf60ce32cee1ab9206553c7e5741e96f2f32efdf2545758bf210a2c41"),
    targets = listOf(AppTarget(version = "2.3.0", versionCode = 1121, minSdk = 29)),
)

@Suppress("unused")
val upNext = bytecodePatch(
    name = "Android Auto Up next",
    description = "Publishes Myspoti's actual playback queue with titles, artists, artwork and the current occurrence. Android Auto and DriveDeck can display and select upcoming playlist and radio tracks through the existing player.",
    default = false,
) {
    compatibleWith(queueTarget)
    dependsOn(patches.shared.originalInput(queueTarget))
    extendWith("extensions/myspoti-queue.mpe")

    execute {
        fun exact(owner: String, name: String, result: String = "V", vararg args: String) =
            Fingerprint(definingClass = owner, name = name, returnType = result, parameters = args.toList())
                .uniqueMatch().method

        // Fail closed if any reflected member or callback no longer has the audited shape.
        val fields = mapOf(
            SERVICE to mapOf("t" to "Ltv4;", "J" to "Lag5;"),
            "Ltv4;" to mapOf("l" to "Ljava/lang/Object;"),
            "Lsv4;" to mapOf("a" to "Landroid/media/session/MediaSession;"),
            "Lke7;" to mapOf("a" to "Lke7;", "t" to "Ler5;", "u" to "Lvb6;"),
            "Ldg4;" to mapOf("a" to "Ler5;", "b" to "Ljava/util/ArrayList;"),
            "Lra7;" to mapOf("a" to SONG, "b" to "Ljava/util/List;", "c" to "Ljava/util/List;"),
            "Lva7;" to mapOf("a" to SONG, "b" to "Ljava/lang/String;", "c" to "Ljava/lang/String;", "d" to "Ljava/lang/String;"),
            "Lqv4;" to mapOf("a" to "Lat4;"), "Lzv5;" to mapOf("e" to SERVICE),
            "Lag5;" to mapOf("a" to "Ljava/lang/String;"),
            "Lng4;" to mapOf("a" to "J", "b" to "Landroid/net/Uri;"),
        )
        fields.forEach { (owner, expected) ->
            val cls = mutableClassDefBy(owner)
            expected.forEach { (name, type) ->
                check(cls.fields.count { it.name == name && it.type == type } == 1) {
                    "Unsupported Myspoti queue field $owner->$name:$type"
                }
            }
        }
        exact("Lke7;", "M0", "V", "Lva7;")
        exact("Lvb6;", "b", "Lzf5;")
        exact("Lzf5;", "k", "Ljava/lang/String;")
        exact("Lzf5;", "j", "Ljava/lang/String;")
        exact("Ldg4;", "i", "Lng4;")
        exact("Ler5;", "getValue", "Ljava/lang/Object;")
        exact("Lqv4;", "a", "Lsv4;")
        listOf("getTitle", "getSinger", "getAlbum", "getCoverUri", "getUrl", "getSpotifyTrackId").forEach {
            exact(SONG, it, "Ljava/lang/String;")
        }
        exact(SONG, "getDurationMs", "I")

        val create = exact(SERVICE, "onCreate")
        check(create.instructions.none {
            ((it as? ReferenceInstruction)?.reference as? MethodReference)?.definingClass == EXPORT
        }) { "Myspoti Up next is already applied" }
        create.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }
            .map { it.index }.reversed().forEach {
                create.addInstruction(it, "invoke-static/range {p0 .. p0}, $EXPORT->bind(Ljava/lang/Object;)V")
            }
        exact(SERVICE, "onDestroy").addInstruction(0,
            "invoke-static/range {p0 .. p0}, $EXPORT->unbind(Ljava/lang/Object;)V")

        // The app's compat adapter has one framework state sink. Preserve the original
        // object and attach the matching public queue ID without rebuilding app policy.
        val state = exact("Ltv4;", "Y", "V", "Landroid/support/v4/media/session/PlaybackStateCompat;")
        val sink = state.instructions.withIndex().single { (_, instruction) ->
            ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.toString() ==
                "Landroid/media/session/MediaSession;->setPlaybackState(Landroid/media/session/PlaybackState;)V"
        }
        val registers = sink.value as Instruction35c
        state.replaceInstruction(sink.index,
            "invoke-static {v${registers.registerC}, v${registers.registerD}}, $EXPORT->publishState(Landroid/media/session/MediaSession;Landroid/media/session/PlaybackState;)V")

        val setter = exact("Ler5;", "setValue", "V", "Ljava/lang/Object;")
        setter.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }
            .map { it.index }.reversed().forEach {
                setter.addInstruction(it, "invoke-static/range {p1 .. p1}, $EXPORT->changed(Ljava/lang/Object;)V")
            }
        val remote = exact("Lke7;", "d", "V", "Lvb6;")
        remote.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }
            .map { it.index }.reversed().forEach {
                remote.addInstruction(it, "invoke-static {}, $EXPORT->refreshSoon()V")
            }
        val select = exact("Lqv4;", "onSkipToQueueItem", "V", "J")
        check(select.instructions.count { it.opcode == Opcode.RETURN_VOID } == 1)
        select.addInstruction(0, "invoke-static/range {p0 .. p2}, $EXPORT->select(Ljava/lang/Object;J)V")
    }
}
