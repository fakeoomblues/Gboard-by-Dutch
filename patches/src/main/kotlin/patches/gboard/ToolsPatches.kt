package patches.gboard

import app.morphe.patcher.patch.resourcePatch
import patches.gboard.features.ocr.gboardOcrFeatureMarkerPatch
import patches.gboard.features.ocr.gboardOcrFlagValuePatch
import patches.gboard.features.ocr.gboardOcrModelAssetsPatch
import patches.gboard.features.ocr.gboardOcrBundledRecognizerPatch
import patches.gboard.features.ocr.gboardOcrRuntimePatch
import patches.gboard.features.writingtools.gboardAiWritingTools1863AutoFixAcceptancePatch
import patches.gboard.features.writingtools.gboardAiWritingToolsFeatureMarkerPatch
import patches.gboard.features.writingtools.gboardAiWritingToolsFlagValuePatch
import patches.gboard.features.betafeatures.gboardBetaFlagFeatureMarkerPatch
import patches.gboard.features.betafeatures.gboardBetaFlagValuePatch
import patches.gboard.features.hiddenflags.gboardHiddenFlagsBootstrapPatch
import patches.gboard.features.hiddenflags.gboardHiddenFlagsFeatureMarkerPatch
import patches.gboard.shared.gboardPatchesSettingsPatch

/**
 * Gboard's Scan Text (OCR).
 *
 * Gboard can recognise text from the camera, but a re-signed build is refused the
 * Play Services recognition modules. This forces the stock entry point and the
 * language gates open, then repairs the module request and the non-Latin thin
 * recogniser init so the modules can come down normally.
 */
@Suppress("unused")
val gboardOcrScanTextPatch = resourcePatch(
    name = "Enable OCR / Scan Text",
    description = "Enables Gboard's Scan Text camera entry point and runs it with an OCR " +
        "engine bundled in the patch, so it works without Play Services. Latin text only.",
    default = true,
) {
    compatibleWith(gboard)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardOcrFeatureMarkerPatch,
        gboardOcrFlagValuePatch,
        gboardOcrRuntimePatch,
        gboardOcrBundledRecognizerPatch,
        gboardOcrModelAssetsPatch,
    )
}

/**
 * Gboard's AI Writing Tools.
 *
 * The client ships inside Gboard but is gated behind signature-scoped flags plus a
 * v2 intent routing table a re-signed build never receives. This forces the flag
 * tuple, adds INTENT_AUTO_FIX to the routing set, and repairs the three init points
 * that otherwise throw on a client type the assistant never learns.
 */
@Suppress("unused")
val gboardAiWritingToolsPatch = resourcePatch(
    name = "AI Writing Tools",
    description = "Enables Gboard's Writing tools: proofread, rewrite and tone changes on " +
        "selected text, in every language and keyboard.",
    default = true,
) {
    compatibleWith(gboard)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardAiWritingToolsFeatureMarkerPatch,
        gboardAiWritingToolsFlagValuePatch,
        gboardAiWritingTools1863AutoFixAcceptancePatch,
    )
}

/**
 * New 18.8 beta gates.
 *
 * Five switches that exist in the beta but are off for a re-signed build, each with a live
 * consumer and no model or server dependency: the plain-text quick-paste chip, the floating
 * clipboard chip, invisible floating-keyboard resizing, the Emoji2 provider and the new
 * swipe-to-remove confirmation. The toggle lives under More settings; turning it off restores
 * Gboard's own values.
 */
@Suppress("unused")
val gboardBetaFeaturesPatch = resourcePatch(
    name = "Beta features (18.8)",
    description = "Unlocks five new Gboard 18.8 features that are off by default: plain-text " +
        "quick-paste chip, floating clipboard chip, invisible floating-keyboard resizing, the " +
        "Emoji2 provider and the swipe-to-remove confirmation.",
    default = false,
) {
    compatibleWith(gboard)
    dependsOn(
        gboardPatchesSettingsPatch,
        gboardBetaFlagFeatureMarkerPatch,
        gboardBetaFlagValuePatch,
    )
}

/**
 * Hidden flags.
 *
 * A screen with one switch per curated 18.8 flag. Each switch writes Gboard's own override entry
 * (`adfk.o(name)` for the flag, `adfk.h(adiu.OVERRIDE)` for the store), so the value is applied to
 * the running keyboard directly instead of going through a flag-getter hook. Adding a new boolean
 * gate only needs a catalog entry.
 */
@Suppress("unused")
val gboardHiddenFlagsPatch = resourcePatch(
    name = "Hidden flags (18.8)",
    description = "Adds a Hidden flags screen that forces individual Gboard 18.8 flags through " +
        "Gboard's own override store: app-specific word biasing, the app-specific suggestion bar " +
        "and the square search radius.",
    default = false,
) {
    compatibleWith(gboard)
    dependsOn(
        gboardPatchesSettingsPatch,
        gboardHiddenFlagsFeatureMarkerPatch,
        gboardHiddenFlagsBootstrapPatch,
    )
}
