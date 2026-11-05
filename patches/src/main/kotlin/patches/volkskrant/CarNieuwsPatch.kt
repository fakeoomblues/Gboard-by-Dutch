package patches.volkskrant

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val NIEUWS = "Lnl/johninnl/volkskrant/CarNieuws;"
private const val SERVICE = "Lnl/dpgmedia/mcdpg/amalia/player/audio/player/audio/service/AmaliaAudioPlayerService;"
private const val START_LAMBDA = "Lnl/dpgmedia/mcdpg/amalia/car/data/network/CarApi\$getTabStart\$\$inlined\$get\$default\$1;"
private const val ROOT = "Lnl/dpgmedia/mcdpg/amalia/car/ui/controller/RootController;"
private const val SUCCESS_INIT = "Lnl/dpgmedia/mcdpg/amalia/util/kotlin/result/AmaliaResult\$Success;-><init>(Ljava/lang/Object;)V"
private const val START_TAB_KEY = "Lnl/dpgmedia/mcdpg/amalia/assets/strings/StringKey\$Car\$Tab\$Start;->INSTANCE:Lnl/dpgmedia/mcdpg/amalia/assets/strings/StringKey\$Car\$Tab\$Start;"
private const val TAB_MAPPER_CREATE_TAB = "Lnl/dpgmedia/mcdpg/amalia/car/ui/mediaitem/CarMediaItemTabMapper;->createTab(ILjava/lang/String;Ljava/lang/String;)Landroidx/media3/common/MediaItem;"
private const val MAPPER = "Lnl/dpgmedia/mcdpg/amalia/car/ui/mediaitem/CarMediaItemMapper;"
private const val TO_LANE_ITEM_QUEUES = "Lnl/dpgmedia/mcdpg/amalia/car/ui/mediaitem/CarMediaItemQueueMapper;->toLaneItemQueues(Ljava/util/List;)Ljava/util/List;"
private const val QUEUE = "Lnl/dpgmedia/mcdpg/amalia/car/data/model/CarAudioQueue;"
private const val QUEUE_DECODER = "Lnl/dpgmedia/mcdpg/amalia/car/data/model/CarAudioQueue\$Companion;->fromJsonString(Ljava/lang/String;)Lnl/dpgmedia/mcdpg/amalia/car/data/model/CarAudioQueue;"
private const val CALLBACK = "Lnl/dpgmedia/mcdpg/amalia/car/ui/browser/CarMediaSessionCallbackDelegate;"
private const val TAB_TITLE = "Nieuws"

/**
 * Rewrites the Android Auto start-tab pipeline instead of adding a parallel
 * one. Every synthesized row is a spoken_article of the app's own car SDK, so
 * browsing, queue construction, history, tracking, premium handling and
 * playback resolution all run through unmodified Amalia code paths.
 */
@Suppress("unused")
val carNieuws = bytecodePatch(
    name = "Android Auto nieuws",
    description = "Makes Nieuws the primary Android Auto tab and lists the latest headlines from the app's home feed with their publisher-supplied spoken audio. Rows render and play through the app's existing car player; premium audio keeps its entitlement.",
) {
    compatibleWith(volkskrant)
    dependsOn(originalInput)
    extendWith("extensions/volkskrant.mpe")

    execute {
        // 1. Capture the application context from the media browser service the
        //    car connects to, before any browse request can arrive.
        val onCreate = exact(SERVICE, "onCreate").method
        check(onCreate.instructions.none { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference
            reference is MethodReference && reference.definingClass == NIEUWS
        }) { "Volkskrant car service init hook already present" }
        onCreate.addInstruction(0, "invoke-static/range {p0 .. p0}, $NIEUWS->init(Landroid/content/Context;)V")

        // 2. Replace the fetched start-tab response with the Nieuws component.
        //    The start-tab coroutine builds exactly one AmaliaResult.Success
        //    from the deserialized CarTabSdUiResponse; rewrite that value.
        val startTab = exact(START_LAMBDA, "invokeSuspend", "Ljava/lang/Object;", "Ljava/lang/Object;").method
        check(startTab.instructions.none { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference
            reference is MethodReference && reference.definingClass == NIEUWS
        }) { "Volkskrant start-tab hook already present" }
        val wrapped = startTab.instructions.withIndex().single { (_, instruction) ->
            (instruction as? ReferenceInstruction)?.reference?.toString() == SUCCESS_INIT
        }
        val resultRegister = (wrapped.value as Instruction35c).registerC
        startTab.addInstructions(wrapped.index + 1, """
            invoke-static/range {v$resultRegister .. v$resultRegister}, $NIEUWS->withNieuws(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object v$resultRegister
        """.trimIndent())

        // 3. Advertise the tab as Nieuws: replace the localized Start title
        //    right after it is resolved, keeping route and icon untouched.
        val root = exact(ROOT, "get", "Ljava/util/List;").method
        val startKey = root.instructions.withIndex().single { (_, instruction) ->
            (instruction as? ReferenceInstruction)?.reference?.toString() == START_TAB_KEY
        }
        val title = root.instructions.withIndex().drop(startKey.index).first { (_, instruction) ->
            instruction.opcode == Opcode.MOVE_RESULT_OBJECT
        }
        val titleRegister = (title.value as OneRegisterInstruction).registerA
        root.addInstruction(title.index + 1, "const-string v$titleRegister, \"$TAB_TITLE\"")

        // 4. Render the Nieuws tab's children as list rows: big title, small
        //    leading artwork, duration and date, exactly like the app's own
        //    episode rows in list mode.
        val startTabItem = root.instructions.withIndex().drop(title.index).first { (_, instruction) ->
            (instruction as? ReferenceInstruction)?.reference?.toString() == TAB_MAPPER_CREATE_TAB
        }
        val tabRegister = (root.instructions.get(startTabItem.index + 1) as OneRegisterInstruction).registerA
        root.addInstruction(startTabItem.index + 2,
            "invoke-static/range {v$tabRegister .. v$tabRegister}, $NIEUWS->styleTab(Ljava/lang/Object;)V")

        // 5. Keep the full feed deliverable: the mapper attaches the whole lane
        //    queue to every row, so a large spoken-news lane overflows the
        //    browse transaction. Article browse rows carry just their own item.
        val fromUiDto = exact(MAPPER, "fromUiDto", "Ljava/lang/Object;",
            "Lnl/dpgmedia/mcdpg/amalia/car/data/model/CarTabUi;",
            "Lnl/dpgmedia/mcdpg/amalia/car/data/model/CarItemTrackingArgs\$ScreenName;",
            "Lkotlin/coroutines/Continuation;").method
        val queueCall = fromUiDto.instructions.withIndex().single { (_, instruction) ->
            (instruction as? ReferenceInstruction)?.reference?.toString() == TO_LANE_ITEM_QUEUES
        }
        val queueRegister = (fromUiDto.instructions.get(queueCall.index + 1) as OneRegisterInstruction).registerA
        fromUiDto.addInstructions(queueCall.index + 2, """
            invoke-static/range {v$queueRegister .. v$queueRegister}, $NIEUWS->slimQueues(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object v$queueRegister
        """.trimIndent())

        // 6. Restore the complete queue inside the app when a row is selected.
        //    Browse delivery is linear in feed size; native next/previous and
        //    entitlement handling still receive the original full queue.
        val readQueue = exact(CALLBACK, "readLaneQueue", QUEUE, "Landroid/os/Bundle;").method
        val decoder = readQueue.instructions.withIndex().single { (_, instruction) ->
            (instruction as? ReferenceInstruction)?.reference?.toString() == QUEUE_DECODER
        }
        val decoded = readQueue.instructions.get(decoder.index + 1)
        check(decoded.opcode == Opcode.MOVE_RESULT_OBJECT) { "Volkskrant queue decoder contract changed" }
        val decodedRegister = (decoded as OneRegisterInstruction).registerA
        readQueue.addInstructions(decoder.index + 2, """
            invoke-static/range {v$decodedRegister .. v$decodedRegister}, $NIEUWS->restoreQueue(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object v$decodedRegister
            check-cast v$decodedRegister, $QUEUE
        """.trimIndent())
    }
}
