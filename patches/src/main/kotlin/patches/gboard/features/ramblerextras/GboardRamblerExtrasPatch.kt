package patches.gboard.features.ramblerextras

import patches.gboard.shared.GboardFlagFamilyFeature
import patches.gboard.shared.gboardFlagFamilyFeaturePatch

internal val gboardRamblerExtrasFlagValuePatch = gboardFlagFamilyFeaturePatch(
    description = "Apply the Rambler toolbar-overflow, revert and Lite-history flags.",
    feature = GboardFlagFamilyFeature.RAMBLER_TOOLBAR_EXTRAS,
)
