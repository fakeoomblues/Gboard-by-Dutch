package dev.jason.gboardpatches.extension.betafeatures;

import android.content.Context;
import android.content.SharedPreferences;

import dev.jason.gboardpatches.extension.R;
import dev.jason.gboardpatches.extension.flagsettings.GboardBooleanFlagSettingsFeature;
import dev.jason.gboardpatches.extension.settings.GboardPatchesFeatureAvailability;
import dev.jason.gboardpatches.extension.settings.GboardSettingsText;

/** Home-page toggle for the 18.8 beta flags forced by {@link GboardBetaFlagRuntime}. */
public final class GboardBetaFlagSettingsFeature extends GboardBooleanFlagSettingsFeature {

    public GboardBetaFlagSettingsFeature(Context context) {
        super(
                GboardPatchesFeatureAvailability.FEATURE_BETA_FLAGS,
                text(context, R.string.gboard_patches_beta_flags_title),
                text(context, R.string.gboard_patches_beta_flags_entry_summary),
                text(context, R.string.gboard_patches_beta_flags_title),
                text(context, R.string.gboard_patches_header_badge),
                text(context, R.string.gboard_patches_beta_flags_error_title),
                text(context, R.string.gboard_patches_beta_flags_error_summary),
                text(context, R.string.gboard_patches_flag_patch_section_feature),
                null,
                new SettingsStore() {
                    @Override
                    public void ensureDefault(SharedPreferences preferences) {
                        GboardBetaFlagSettings.ensureDefault(preferences);
                    }

                    @Override
                    public boolean readEnabled(SharedPreferences preferences) {
                        return GboardBetaFlagSettings.readEnabled(preferences);
                    }

                    @Override
                    public boolean writeEnabled(
                            SharedPreferences preferences,
                            boolean enabled) {
                        return GboardBetaFlagSettings.writeEnabled(preferences, enabled);
                    }
                });
        declareRestartRequired(GboardBetaFlagSettings.PREF_KEY_ENABLED);
    }

    private static String text(Context context, int resourceId) {
        return GboardSettingsText.get(context, resourceId);
    }
}
