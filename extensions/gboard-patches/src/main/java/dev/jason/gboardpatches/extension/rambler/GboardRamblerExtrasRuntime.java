package dev.jason.gboardpatches.extension.rambler;

/**
 * Optional Rambler toolbar and revert behaviour, kept out of the core Rambler patch so the feature
 * is visible in the patch list.
 *
 * <ul>
 *   <li>{@code enable_rambler_al_toolbar_overflow_menu} - the overflow button on the floating
 *       Rambler toolbar (kxm), off by default in this beta.</li>
 *   <li>{@code rambler_preserve_rewritten_text_on_revert} - keeps the rewritten text when reverting
 *       from the full transcript to the cleaned-up text.</li>
 *   <li>{@code jetson_lite_enable_edit_history} - passes edit history into a Jetson Lite request;
 *       only read while the Lite path is active.</li>
 * </ul>
 */
public final class GboardRamblerExtrasRuntime {
    public static final String OVERFLOW_MENU_FLAG = "enable_rambler_al_toolbar_overflow_menu";
    public static final String PRESERVE_REWRITTEN_ON_REVERT_FLAG =
            "rambler_preserve_rewritten_text_on_revert";
    public static final String LITE_EDIT_HISTORY_FLAG = "jetson_lite_enable_edit_history";

    private GboardRamblerExtrasRuntime() {
    }

    /** Flag-family hook for {@code FLAG_NAME_AND_RESULT}. */
    public static Object applyFlagValue(String flagName, Object stockResult) {
        if (flagName == null || !(stockResult instanceof Boolean)) {
            return stockResult;
        }
        switch (flagName) {
            case OVERFLOW_MENU_FLAG:
            case PRESERVE_REWRITTEN_ON_REVERT_FLAG:
            case LITE_EDIT_HISTORY_FLAG:
                return Boolean.TRUE;
            default:
                return stockResult;
        }
    }
}
