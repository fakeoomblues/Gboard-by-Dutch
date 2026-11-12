package dev.jason.gboardpatches.extension.rambler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GboardRamblerSilenceRuntimeTest {
    @Test
    public void readsTheChosenSeconds() {
        assertEquals(3L, GboardRamblerSilenceRuntime.silenceSeconds("3"));
        assertEquals(10L, GboardRamblerSilenceRuntime.silenceSeconds(" 10 "));
    }

    @Test
    public void staysOffForOffMissingOrUnexpectedValues() {
        for (String stored : new String[] {null, "", "0", "-5", "61", "five", "5.5"}) {
            assertEquals(String.valueOf(stored), 0L, GboardRamblerSilenceRuntime.silenceSeconds(stored));
        }
    }
}
