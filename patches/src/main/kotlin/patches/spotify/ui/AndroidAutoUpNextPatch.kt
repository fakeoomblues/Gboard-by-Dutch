package patches.spotify.ui

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val NOW_PLAYING_MODEL = "Lp/d520;"

/**
 * Spotify builds the media-session queue from `PlayerQueue.nextTracks()` only when
 * `PlayerState.restrictions().disallowPeekingNextReasons()` is empty. A Free account sets that
 * restriction, so the queue is replaced with an empty one even though the player has upcoming
 * tracks. Skip both gate branches around the queue builder so the car's Up next receives the
 * player's real list. This is opt-in because it also enters the queue builder when Spotify has
 * no queue yet during startup.
 */
@Suppress("unused")
val spotifyAndroidAutoUpNextPatch = bytecodePatch(
    name = "Android Auto Up next",
    description = "Publishes the player's real upcoming tracks to the media session on accounts " +
        "whose peek-next restriction would otherwise hide the car's Up next list.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val method = mutableClassDefBy(NOW_PLAYING_MODEL).methods.single { candidate ->
            candidate.name == "p" &&
                candidate.implementation != null &&
                candidate.parameterTypes.map(CharSequence::toString).take(2) ==
                listOf(
                    "Lcom/spotify/player/model/PlayerState;",
                    "Lcom/spotify/player/model/PlayerQueue;",
                )
        }
        val instructions = method.implementation!!.instructions.toList()
        val peekGate = instructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.INVOKE_STATIC) return@indexOfFirst false
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                ?: return@indexOfFirst false
            reference.definingClass == "Lp/o4p1;" && reference.name == "t" &&
                reference.returnType == "Z" &&
                reference.parameterTypes.map(CharSequence::toString) ==
                listOf("Lcom/spotify/player/model/PlayerState;")
        }
        check(peekGate >= 0) { "Spotify peek-next gate changed" }
        val emptyGate = (peekGate - 1 downTo maxOf(0, peekGate - 5))
            .firstOrNull { instructions[it].opcode == Opcode.IF_EQZ }
            ?: throw IllegalStateException("Spotify queue empty gate changed")
        val peekBranch = (peekGate + 1 until minOf(instructions.size, peekGate + 5))
            .firstOrNull { instructions[it].opcode == Opcode.IF_NEZ }
            ?: throw IllegalStateException("Spotify queue peek gate changed")
        method.replaceInstruction(emptyGate, "nop")
        method.replaceInstruction(peekBranch, "nop")
    }
}
