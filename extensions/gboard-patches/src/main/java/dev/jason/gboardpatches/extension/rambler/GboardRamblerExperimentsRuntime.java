package dev.jason.gboardpatches.extension.rambler;

/**
 * Supplies the server flags behind Rambler features Gboard 18.6.3 ships but keeps switched off.
 *
 * <p>Each experimental Rambler patch selects one of these hooks, so a feature only changes when
 * its patch is applied. Every hook keeps Gboard's own value for any other flag or value type.
 */
public final class GboardRamblerExperimentsRuntime {
    /** Sends the text saved under Rambler's custom instructions with every dictation. */
    public static final String CUSTOM_INSTRUCTION_FLAG = "agentic_dictation_enable_custom_instruction";
    /** Sends the text after the cursor too, not only the text before it. */
    public static final String CURSOR_AWARENESS_FLAG = "jetson_enable_cursor_awareness";
    /** With text selected, sends only the selection, so spoken edits apply to it. */
    public static final String SELECTED_TEXT_ONLY_FLAG = "jetson_send_only_selected_text_if_present";
    /** Selects the edited text again after a spoken edit. */
    public static final String RESELECT_AFTER_EDIT_FLAG = "jetson_reselect_after_edit";
    /** Sends the app's name, package, input type, action and field hint as context. */
    public static final String APP_CONTEXT_FLAG = "agentic_dictation_enable_app_info_context";
    /** Whole-utterance pattern that fires the field's Send action. */
    public static final String SEND_COMMAND_FLAG = "send_command_regex";

    /**
     * Gboard's own English pattern ({@code (?i)^[^a-z0-9]*sen[dt][^a-z0-9]*$}) plus the Dutch
     * verbs for sending. Like the original, it only matches when that word is all that was said.
     */
    public static final String SEND_COMMAND_PATTERN =
            "(?i)^[^a-z0-9]*(sen[dt]|verstuur|versturen|verzend|verzenden)[^a-z0-9]*$";

    private GboardRamblerExperimentsRuntime() {
    }

    /** Flag-family hook for Rambler custom instructions. */
    public static Object applyCustomInstructionFlag(String flagName, Object originalResult) {
        return CUSTOM_INSTRUCTION_FLAG.equals(flagName) ? enabled(originalResult) : originalResult;
    }

    /** Flag-family hook for Rambler cursor-aware editing. */
    public static Object applyCursorEditingFlag(String flagName, Object originalResult) {
        if (CURSOR_AWARENESS_FLAG.equals(flagName) || SELECTED_TEXT_ONLY_FLAG.equals(flagName)
                || RESELECT_AFTER_EDIT_FLAG.equals(flagName)) {
            return enabled(originalResult);
        }
        return originalResult;
    }

    /** Flag-family hook for Rambler app context. */
    public static Object applyAppContextFlag(String flagName, Object originalResult) {
        return APP_CONTEXT_FLAG.equals(flagName) ? enabled(originalResult) : originalResult;
    }

    /** Flag-family hook for the Dutch send command. */
    public static Object applySendCommandFlag(String flagName, Object originalResult) {
        return SEND_COMMAND_FLAG.equals(flagName) && originalResult instanceof String
                ? SEND_COMMAND_PATTERN
                : originalResult;
    }

    private static Object enabled(Object originalResult) {
        return originalResult instanceof Boolean ? Boolean.TRUE : originalResult;
    }
}
