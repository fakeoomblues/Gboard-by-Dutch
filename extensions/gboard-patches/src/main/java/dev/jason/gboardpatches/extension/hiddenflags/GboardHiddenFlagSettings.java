package dev.jason.gboardpatches.extension.hiddenflags;

import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

import dev.jason.gboardpatches.extension.flagsettings.GboardBooleanFlagSettings;

/** Per-flag preference for the hidden-flag overrides. */
public final class GboardHiddenFlagSettings {
    private static final String KEY_PREFIX = "hidden_flag.";

    private GboardHiddenFlagSettings() {
    }

    public static String preferenceKey(String flagName) {
        return KEY_PREFIX + flagName;
    }

    public static boolean readEnabled(SharedPreferences preferences, String flagName) {
        try {
            return GboardBooleanFlagSettings.readEnabled(
                    preferences, preferenceKey(flagName), false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void ensureDefault(SharedPreferences preferences, String flagName) {
        try {
            GboardBooleanFlagSettings.ensureDefault(
                    preferences, preferenceKey(flagName), false);
        } catch (Throwable ignored) {
            // The screen falls back to its warning state.
        }
    }

    public static boolean writeEnabled(
            SharedPreferences preferences, String flagName, boolean enabled) {
        try {
            return GboardBooleanFlagSettings.writeEnabled(
                    preferences, preferenceKey(flagName), enabled);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Set<String> enabledNames(SharedPreferences preferences) {
        Set<String> names = new LinkedHashSet<>();
        if (preferences == null) {
            return names;
        }
        for (GboardHiddenFlagCatalog.Entry entry : GboardHiddenFlagCatalog.entries()) {
            if (readEnabled(preferences, entry.flagName)) {
                names.add(entry.flagName);
            }
        }
        return names;
    }
}
