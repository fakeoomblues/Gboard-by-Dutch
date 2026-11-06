package dev.jason.gboardpatches.patches.gboard.registry

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.ResourcePatchBuilder
import app.morphe.patcher.patch.resourcePatch
import dev.jason.gboardpatches.patches.gboard.features.about.gboardAboutPageResourcePatch
import dev.jason.gboardpatches.patches.gboard.features.calculator.gboardCalculatorFeatureMarkerPatch
import dev.jason.gboardpatches.patches.gboard.features.calculator.gboardCalculatorLifecyclePatch
import dev.jason.gboardpatches.patches.gboard.features.clipboard.gboardClipboardAdapterTrimPatch
import dev.jason.gboardpatches.patches.gboard.features.clipboard.gboardClipboardColumnCountPatch
import dev.jason.gboardpatches.patches.gboard.features.clipboard.gboardClipboardFeatureMarkerPatch
import dev.jason.gboardpatches.patches.gboard.features.clipboard.gboardClipboardItemBindPatch
import dev.jason.gboardpatches.patches.gboard.features.clipboard.gboardClipboardLoaderPatch
import dev.jason.gboardpatches.patches.gboard.features.clipboard.gboardClipboardPrunePatch
import dev.jason.gboardpatches.patches.gboard.features.cursortrackpad.gboardCursorTrackpadFeatureMarkerPatch
import dev.jason.gboardpatches.patches.gboard.features.cursortrackpad.gboardCursorTrackpadFlagValuePatch
import dev.jason.gboardpatches.patches.gboard.features.cursortrackpad.gboardCursorTrackpadPreferencePatch
import dev.jason.gboardpatches.patches.gboard.features.editingaccesspoints.gboardEditingAccessPointsFeatureMarkerPatch
import dev.jason.gboardpatches.patches.gboard.features.editingaccesspoints.gboardEditingAccessPointsLifecyclePatch
import dev.jason.gboardpatches.patches.gboard.features.manualincognito.gboardManualIncognitoFeatureMarkerPatch
import dev.jason.gboardpatches.patches.gboard.features.manualincognito.gboardManualIncognitoLifecyclePatch
import dev.jason.gboardpatches.patches.gboard.features.manualincognito.gboardManualIncognitoPolicyPatch
import dev.jason.gboardpatches.patches.gboard.features.webclipboard.gboardWebClipboardAssetsPatch
import dev.jason.gboardpatches.patches.gboard.features.webclipboard.gboardWebClipboardCapturePatch
import dev.jason.gboardpatches.patches.gboard.features.webclipboard.gboardWebClipboardFeatureMarkerPatch
import dev.jason.gboardpatches.patches.gboard.features.webclipboard.gboardWebClipboardManifestPatch
import dev.jason.gboardpatches.patches.gboard.shared.accesspoint.gboardAccessPointContributions1803Patch
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesSettingsPatch
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

private fun gboardPublicResourcePatch(
    featureId: String,
    name: String,
    description: String,
    default: Boolean,
    block: ResourcePatchBuilder.() -> Unit,
) = resourcePatch(
    name = name,
    description = description,
    default = default,
) {
    block()
}

@Suppress("unused")
val gboardClipboardEnhancementsPatch = gboardPublicResourcePatch(
    featureId = "clipboard_enhancements",
    name = "Clipboard Enhancements",
    description = "Enhance clipboard retention time, item count limit, preview lines, countdown/creation time labels, order index, and column count.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardAboutPageResourcePatch,
        gboardPatchesSettingsPatch,
        gboardClipboardFeatureMarkerPatch,
        gboardClipboardLoaderPatch,
        gboardClipboardPrunePatch,
        gboardClipboardColumnCountPatch,
        gboardClipboardAdapterTrimPatch,
        gboardClipboardItemBindPatch
    )
}

@Suppress("unused")
val gboardWebClipboardPatch = gboardPublicResourcePatch(
    featureId = "web_clipboard",
    name = "Web Clipboard",
    description = "Add the phone-hosted Web Clipboard with browser sync, pairing, and a Quick Settings Tile.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardWebClipboardFeatureMarkerPatch,
        gboardWebClipboardManifestPatch,
        gboardWebClipboardAssetsPatch,
        gboardWebClipboardCapturePatch
    )
}

@Suppress("unused")
val gboardEditingAccessPointsPatch = gboardPublicResourcePatch(
    featureId = "editing_access_points",
    name = "Toolbar Editing Buttons",
    description = "Add Select All, Copy, Cut and Paste to the menu.",
    default = true
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(
        gboardPatchesSettingsPatch,
        gboardEditingAccessPointsFeatureMarkerPatch,
        gboardEditingAccessPointsLifecyclePatch,
        gboardAccessPointContributions1803Patch,
    )
}

@Suppress("unused")
val gboardCursorTrackpadPatch = gboardPublicResourcePatch(
    featureId = "enable_cursor_trackpad_mode",
    name = "Cursor trackpad mode",
    description = "Long-press the space bar to use cursor trackpad and lock modes.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardCursorTrackpadFeatureMarkerPatch,
        gboardCursorTrackpadFlagValuePatch,
        gboardCursorTrackpadPreferencePatch,
    )
}

@Suppress("unused")
val gboardManualIncognitoModePatch = gboardPublicResourcePatch(
    featureId = "incognito_mode_toggle",
    name = "Incognito Mode Toggle",
    description = "Add an Incognito toggle to the Access Point toolbar and configure clipboard and voice typing availability while Incognito mode is active.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardManualIncognitoFeatureMarkerPatch,
        gboardManualIncognitoLifecyclePatch,
        gboardManualIncognitoPolicyPatch,
        gboardAccessPointContributions1803Patch,
    )
}

@Suppress("unused")
val gboardSimpleCalculatorPatch = gboardPublicResourcePatch(
    featureId = "simple_calculator",
    name = "Simple Calculator",
    description = "Type an expression and show a scrollable equation plus its answer in Gboard's suggestion row.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    dependsOn(
        gboardPatchesSettingsPatch,
        gboardCalculatorFeatureMarkerPatch,
        gboardCalculatorLifecyclePatch,
    )
}
