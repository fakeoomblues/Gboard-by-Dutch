package dev.jason.gboardpatches.extension.rambler;

/**
 * Exposes Gboard's cloud Rambler voice typing on phones Google does not roll it out to.
 *
 * <p>Gboard only offers the Rambler/Standard choice in Voice settings when agentic dictation is
 * enabled and the activation type is the agentic one (2). A re-signed Gboard never receives
 * those server values, so they are supplied here. The two toolbar flags match the set confirmed
 * working on a device by other Rambler patches. Which recognizer runs stays the user's choice.
 */
public final class GboardRamblerCloudRuntime {
    public static final String ENABLE_AGENTIC_DICTATION_FLAG = "enable_agentic_dictation";
    public static final String ENABLE_RAMBLER_AL_TOOLBAR_FLAG = "enable_rambler_al_toolbar";
    public static final String ENABLE_RAMBLER_TOOLBAR_AT_CURSOR_POSITION_FLAG =
            "enable_rambler_toolbar_at_cursor_position";
    public static final String AD_ACTIVATION_TYPE_FLAG = "ad_activation_type";
    public static final long AGENTIC_ACTIVATION_TYPE = 2L;
    /**
     * 18.8.0 addition: after Rambler rewrites a span, put the cursor after the last change instead
     * of at the end of the text (kov reads it at two sites). Off by default in this beta.
     */
    public static final String JETSON_POSITION_INPUT_CURSOR_AFTER_DIFF_FLAG =
            "jetson_position_cursor_after_last_diff";
    /** Adds the overflow button to the floating Rambler (AL) toolbar. Off by default in this beta. */
    public static final String RAMBLER_AL_OVERFLOW_MENU_FLAG =
            "enable_rambler_al_toolbar_overflow_menu";
    /** Keeps the rewritten text when you revert from the full transcript to the cleaned-up text. */
    public static final String RAMBLER_PRESERVE_REWRITTEN_ON_REVERT_FLAG =
            "rambler_preserve_rewritten_text_on_revert";
    /** Passes edit history into a Jetson Lite request; only read while the Lite path is active. */
    public static final String JETSON_LITE_EDIT_HISTORY_FLAG = "jetson_lite_enable_edit_history";

    private GboardRamblerCloudRuntime() {
    }

    /** Flag-family hook: called with every flag name and Gboard's own value for it. */
    public static Object applyFlagValue(String flagName, Object originalResult) {
        if (flagName == null) {
            return originalResult;
        }
        switch (flagName) {
            case ENABLE_AGENTIC_DICTATION_FLAG:
            case ENABLE_RAMBLER_AL_TOOLBAR_FLAG:
            case ENABLE_RAMBLER_TOOLBAR_AT_CURSOR_POSITION_FLAG:
            case JETSON_POSITION_INPUT_CURSOR_AFTER_DIFF_FLAG:
            case RAMBLER_AL_OVERFLOW_MENU_FLAG:
            case RAMBLER_PRESERVE_REWRITTEN_ON_REVERT_FLAG:
            case JETSON_LITE_EDIT_HISTORY_FLAG:
                return originalResult instanceof Boolean ? Boolean.TRUE : originalResult;
            case AD_ACTIVATION_TYPE_FLAG:
                return originalResult instanceof Long
                        ? Long.valueOf(AGENTIC_ACTIVATION_TYPE)
                        : originalResult;
            default:
                return originalResult;
        }
    }
}
