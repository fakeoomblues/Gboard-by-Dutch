package patches.spotify.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import patches.spotify.COMPATIBILITY_SPOTIFY

private const val HOME_SECTION_RENDERER = "Lp/uqs;"

/**
 * Spotify's resolved Home feed renders one model per section type. The video section payloads
 * (`WATCH_FEED`, `VIDEO_PROMO`, `VIDEO_BRAND_AD`, `VIDEO_CAROUSEL`) share this renderer, so the
 * "Video Podcasts"/video rows appear wherever Home is shown. Route those section types to the
 * same empty switch case Spotify already uses for unsupported sections; audio sections keep the
 * normal model mapping.
 */
@Suppress("unused")
val hideVideoHomeSectionsPatch = bytecodePatch(
    name = "Hide video shelves",
    description = "Drops video/watch sections from Spotify's resolved Home feed, so video " +
        "podcast and music-video shelves are not shown.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val owner = mutableClassDefBy(HOME_SECTION_RENDERER)
        val method = owner.methods.singleOrNull { candidate ->
            candidate.name == "j" &&
                candidate.returnType == "Lp/jn60;" &&
                candidate.parameterTypes.map(CharSequence::toString) ==
                listOf("Lcom/spotify/casita/v1/resolved/Section;")
        } ?: throw IllegalStateException("Spotify Home section renderer changed")

        val instructions = method.implementation!!.instructions.toList()
        val caseCall = instructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL &&
                instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE
            ) return@indexOfFirst false
            val reference = (instruction as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference as? MethodReference ?: return@indexOfFirst false
            reference.definingClass == "Lcom/spotify/casita/v1/resolved/Section;" &&
                reference.name == "z" && reference.returnType == "I"
        }
        check(caseCall >= 0) { "Spotify Home section case call changed" }

        val moveResult = caseCall + 1
        check(instructions[moveResult].opcode == Opcode.MOVE_RESULT) {
            "Spotify Home section case result changed"
        }
        val caseRegister = (instructions[moveResult] as OneRegisterInstruction).registerA
        val afterCase = moveResult + 1
        val sectionSwitch = (afterCase until instructions.size).firstOrNull {
            instructions[it].opcode == Opcode.PACKED_SWITCH
        } ?: throw IllegalStateException("Spotify Home section switch changed")

        method.addInstructionsWithLabels(
            afterCase,
            """
                const/16 v0, 0xb
                if-eq v$caseRegister, v0, :video_section
                const/16 v0, 0x12
                if-eq v$caseRegister, v0, :video_section
                const/16 v0, 0x14
                if-eq v$caseRegister, v0, :video_section
                const/16 v0, 0x19
                if-eq v$caseRegister, v0, :video_section
                goto :non_video
                :video_section
                const/4 v$caseRegister, 0x5
                goto :video_switch
            """,
            ExternalLabel("non_video", method.getInstruction(afterCase)),
            ExternalLabel("video_switch", method.getInstruction(sectionSwitch)),
        )
    }
}
