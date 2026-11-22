package dev.jason.gboardpatches.extension.hiddenflags;

import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

import dev.jason.gboardpatches.extension.flagsettings.GboardBooleanFlagSettings;

/** Per-flag preference for the hidden-flag overrides. */
public final class GboardHiddenFlagSettings {
    private static final String KEY_PREFIX = "hidden_flag.";
    /** Present only after a real toggle, so a value written by merely opening the screen is ignored. */
    private static final String EXPLICIT_PREFIX = "hidden_flag_explicit.";

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

    private static String explicitKey(String flagName) {
        return EXPLICIT_PREFIX + flagName;
    }

    /** True when the user actually toggled this row (as opposed to the screen being opened). */
    public static boolean isExplicit(SharedPreferences preferences, String flagName) {
        if (preferences == null || flagName == null) {
            return false;
        }
        try {
            return preferences.contains(explicitKey(flagName));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Records that this row was toggled. A legacy {@code true} value also counts as explicit: it can
     * only have come from a toggle or a replay, while a bare {@code false} could have been written by
     * the old screen, so it carries no meaning and is left untouched (treated as "not configured").
     */
    public static boolean markExplicit(SharedPreferences preferences, String flagName, boolean enabled) {
        if (preferences == null || flagName == null) {
            return false;
        }
        try {
            return preferences.edit().putBoolean(explicitKey(flagName), enabled).commit();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Whether this flag should be replayed at startup, with its stored value. */
    public static boolean hasStoredChoice(SharedPreferences preferences, String flagName) {
        return isExplicit(preferences, flagName) || readEnabled(preferences, flagName);
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
