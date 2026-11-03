package patches.maps.gearslip;

import static org.junit.Assert.*;
import android.graphics.Rect;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public final class ControlPlacementTest {
    private void unobscured(ControlPlacement.Plan plan, int w, int h, List<Rect> covers) {
        assertNotNull(plan);
        Rect canvas = new Rect(0, 0, w, h);
        for (int i = 0; i < plan.targets.size(); i++) {
            Rect target = plan.targets.get(i);
            assertTrue(canvas.contains(target));
            for (Rect cover : covers) assertFalse(Rect.intersects(target, cover));
            for (int j = i + 1; j < plan.targets.size(); j++) assertFalse(Rect.intersects(target, plan.targets.get(j)));
        }
    }
    @Test public void compactRightUsesTwoColumnsBelowMeasuredCard() {
        Rect player = new Rect(920, 0, 1280, 180);
        List<Rect> covers = Arrays.asList(player, new Rect(0, 396, 1280, 480));
        ControlPlacement.Plan plan = ControlPlacement.arrange(1280, 480, player, covers, 64, 8, 6);
        unobscured(plan, 1280, 480, covers);
        assertTrue(plan.belowPlayer); assertEquals(0, plan.hidden); assertEquals(6, plan.targets.size());
        for (Rect target : plan.targets) assertTrue(new Rect(920, 180, 1280, 396).contains(target));
        assertEquals(2, plan.targets.stream().map(r -> r.left).distinct().count());
    }
    @Test public void compactLeftMirrorsThePlacement() {
        Rect player = new Rect(0, 0, 360, 180);
        List<Rect> covers = Arrays.asList(player, new Rect(0, 396, 1280, 480));
        ControlPlacement.Plan plan = ControlPlacement.arrange(1280, 480, player, covers, 64, 8, 6);
        unobscured(plan, 1280, 480, covers); assertTrue(plan.belowPlayer);
        for (Rect target : plan.targets) assertTrue(new Rect(0, 180, 360, 396).contains(target));
    }
    @Test public void expandedCardUsesAnotherUnobscuredRegion() {
        Rect player = new Rect(640, 0, 1280, 380);
        List<Rect> covers = Arrays.asList(player, new Rect(0, 396, 1280, 480));
        ControlPlacement.Plan plan = ControlPlacement.arrange(1280, 480, player, covers, 64, 8, 6);
        unobscured(plan, 1280, 480, covers); assertFalse(plan.belowPlayer); assertEquals(0, plan.hidden);
        for (Rect target : plan.targets) assertTrue(target.right <= 640);
    }
    @Test public void shortSpaceReservesAReachableOverflowTarget() {
        List<Rect> covers = Collections.singletonList(new Rect(0, 100, 300, 200));
        ControlPlacement.Plan plan = ControlPlacement.arrange(300, 200, null, covers, 64, 8, 6);
        unobscured(plan, 300, 200, covers);
        assertEquals(2, plan.targets.size()); assertEquals(5, plan.hidden);
    }
    @Test public void measuredExtraCoverCannotHideAControlBelowThePlayer() {
        Rect player = new Rect(920, 0, 1280, 180);
        List<Rect> covers = Arrays.asList(player, new Rect(920, 180, 1280, 350), new Rect(0, 396, 1280, 480));
        ControlPlacement.Plan plan = ControlPlacement.arrange(1280, 480, player, covers, 64, 8, 6);
        unobscured(plan, 1280, 480, covers); assertFalse(plan.belowPlayer); assertEquals(0, plan.hidden);
    }
    @Test public void largerTargetsRelocateInsteadOfShrinkingOrClipping() {
        Rect player = new Rect(920, 0, 1280, 180);
        List<Rect> covers = Arrays.asList(player, new Rect(0, 396, 1280, 480));
        ControlPlacement.Plan plan = ControlPlacement.arrange(1280, 480, player, covers, 96, 12, 6);
        unobscured(plan, 1280, 480, covers); assertFalse(plan.belowPlayer); assertEquals(0, plan.hidden);
        for (Rect target : plan.targets) { assertEquals(96, target.width()); assertEquals(96, target.height()); }
    }
    @Test public void completelyCoveredSurfaceReportsUnavailableSpace() {
        List<Rect> covers = Collections.singletonList(new Rect(0, 0, 800, 480));
        ControlPlacement.Plan plan = ControlPlacement.arrange(800, 480, null, covers, 64, 8, 6);
        assertTrue(plan.targets.isEmpty()); assertEquals(6, plan.hidden);
    }
    @Test public void exactNativeBlockHeightDoesNotRequireUnusedRows() {
        Rect player = new Rect(920, 0, 1280, 250);
        List<Rect> covers = Arrays.asList(player, new Rect(0, 396, 1280, 480));
        assertEquals(new Rect(1144, 250, 1280, 390), ControlPlacement.belowBlock(1280, 480, player, covers, 136, 140));
    }
    @Test public void overflowAnchorAvoidsBothHostAndNativeChrome() {
        Rect player = new Rect(640, 0, 1280, 390);
        List<Rect> covers = Arrays.asList(player, new Rect(0, 396, 1280, 480), new Rect(0, 0, 640, 96), new Rect(0, 300, 96, 396));
        Rect anchor = ControlPlacement.overflowAnchor(1280, 480, player, covers, 64);
        assertNotNull(anchor);
        for (Rect cover : covers) assertFalse(Rect.intersects(anchor, cover));
        assertEquals(64, anchor.width()); assertEquals(64, anchor.height());
    }
    @Test public void overflowPanelKeepsWholeRowsAndStaysOutsideHostCovers() {
        Rect player = new Rect(640, 0, 1280, 390);
        List<Rect> covers = Arrays.asList(player, new Rect(0, 396, 1280, 480));
        Rect panel = ControlPlacement.menuPanel(1280, 480, covers, new Rect(0, 332, 64, 396), 64, 9);
        assertNotNull(panel); assertEquals(0, panel.height() % 64);
        for (Rect cover : covers) assertFalse(Rect.intersects(panel, cover));
    }
    @Test public void aNarrowUncoveredRegionStillAllowsFullSizeScrollableActions() {
        List<Rect> covers = Arrays.asList(new Rect(170, 0, 800, 480), new Rect(0, 396, 800, 480));
        Rect panel = ControlPlacement.menuPanel(800, 480, covers, new Rect(0, 300, 96, 396), 96, 5);
        assertNotNull(panel); assertEquals(170, panel.width()); assertTrue(panel.height() >= 192);
        for (Rect cover : covers) assertFalse(Rect.intersects(panel, cover));
    }
}
