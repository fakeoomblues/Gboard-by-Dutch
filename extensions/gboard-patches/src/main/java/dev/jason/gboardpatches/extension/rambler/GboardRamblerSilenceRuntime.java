package dev.jason.gboardpatches.extension.rambler;

/**
 * Stops Rambler after a chosen number of seconds without new speech.
 *
 * <p>Gboard's voice input manager restarts a timeout on every transcript update for standard
 * voice typing and stops listening when it runs out. For Rambler it skips that restart, so the
 * same timer is a fixed five-minute session limit. With a silence time chosen under Voice typing
 * → Rambler, the patch sets that timer to the chosen seconds and restarts it on each Rambler
 * transcript update, so Rambler stops and keeps the text as when ✓ is tapped.
 */
public final class GboardRamblerSilenceRuntime {
    /** Preference written by the Stop after silence list under Rambler settings. */
    public static final String PREF_KEY = "morphe_rambler_silence_stop";
    /** Off unless the user picks a time. */
    public static final String DEFAULT_VALUE = "0";
    static final long MAX_SECONDS = 60L;

    private GboardRamblerSilenceRuntime() {
    }

    /** The stored choice in seconds; 0 (off) for anything missing or unexpected. */
    public static long silenceSeconds(String stored) {
        if (stored == null) {
            return 0L;
        }
        try {
            long seconds = Long.parseLong(stored.trim());
            return seconds > 0L && seconds <= MAX_SECONDS ? seconds : 0L;
        } catch (NumberFormatException invalid) {
            return 0L;
        }
    }
}
