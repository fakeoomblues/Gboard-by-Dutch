package dev.jason.gboardpatches.extension.rambler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class GboardRamblerExtrasRuntimeTest {
    @Test
    public void forcesTheThreeOptionalPaths() {
        assertEquals(Boolean.TRUE, GboardRamblerExtrasRuntime.applyFlagValue(
                "enable_rambler_al_toolbar_overflow_menu", Boolean.FALSE));
        assertEquals(Boolean.TRUE, GboardRamblerExtrasRuntime.applyFlagValue(
                "rambler_preserve_rewritten_text_on_revert", Boolean.FALSE));
        assertEquals(Boolean.TRUE, GboardRamblerExtrasRuntime.applyFlagValue(
                "jetson_lite_enable_edit_history", Boolean.FALSE));
    }

    @Test
    public void leavesOtherFlagsAndTypesAlone() {
        Object stock = Boolean.FALSE;
        assertSame(stock, GboardRamblerExtrasRuntime.applyFlagValue("enable_nga", stock));
        Object text = "1";
        assertSame(text, GboardRamblerExtrasRuntime.applyFlagValue(
                "rambler_preserve_rewritten_text_on_revert", text));
        assertSame(stock, GboardRamblerExtrasRuntime.applyFlagValue(null, stock));
    }
}
