package dev.jason.gboardpatches.extension.betafeatures;

import android.content.SharedPreferences;

import dev.jason.gboardpatches.extension.flagsettings.GboardBooleanFlagSettings;

/** Persistent switch for the 18.8 beta flags forced by {@link GboardBetaFlagRuntime}. */
public final class GboardBetaFlagSettings {
    public static final String PREF_KEY_ENABLED = "pref_beta_flags_enabled";
    public static final boolean DEFAULT_ENABLED = true;

    private GboardBetaFlagSettings() {
    }

    public static boolean readEnabled(SharedPreferences preferences) {
        try {
            return GboardBooleanFlagSettings.readEnabled(
                    preferences, PREF_KEY_ENABLED, DEFAULT_ENABLED);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void ensureDefault(SharedPreferences preferences) {
        try {
            GboardBooleanFlagSettings.ensureDefault(
                    preferences, PREF_KEY_ENABLED, DEFAULT_ENABLED);
        } catch (Throwable ignored) {
            // The feature screen falls back to its warning state.
        }
    }

    public static boolean writeEnabled(SharedPreferences preferences, boolean enabled) {
        try {
            return GboardBooleanFlagSettings.writeEnabled(
                    preferences, PREF_KEY_ENABLED, enabled);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
