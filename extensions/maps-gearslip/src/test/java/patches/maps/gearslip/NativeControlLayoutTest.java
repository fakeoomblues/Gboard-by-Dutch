package patches.maps.gearslip;

import static org.junit.Assert.*;
import android.app.Activity;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Button;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public final class NativeControlLayoutTest {
    private FrameLayout canvas, narrow;
    private LinearLayout controls, first, second;
    private NativeControlLayout layout;
    private int clicks;
    @After public void cleanup() { if (layout != null) layout.close(); }
    @Before public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        canvas = new FrameLayout(activity); activity.setContentView(canvas);
        narrow = new FrameLayout(activity); canvas.addView(narrow, new FrameLayout.LayoutParams(84, 396));
        controls = new LinearLayout(activity); controls.setId(42); controls.setOrientation(LinearLayout.VERTICAL);
        narrow.addView(controls, new FrameLayout.LayoutParams(64, 396));
        first = group(activity, 3); second = group(activity, 2);
        controls.addView(first, new LinearLayout.LayoutParams(64, 192));
        controls.addView(second, new LinearLayout.LayoutParams(64, 128));
        layout = new NativeControlLayout(canvas, 42); measure();
    }
    private LinearLayout group(Activity activity, int count) {
        LinearLayout group = new LinearLayout(activity); group.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < count; i++) {
            Button button = new Button(activity); button.setText("Native action");
            button.setOnClickListener(v -> clicks++); group.addView(button, new LinearLayout.LayoutParams(64, 64));
        }
        return group;
    }
    private void measure() {
        canvas.measure(View.MeasureSpec.makeMeasureSpec(1280, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        canvas.layout(0, 0, 1280, 480); canvas.getViewTreeObserver().dispatchOnGlobalLayout();
    }
    private boolean compact() {
        Rect player = new Rect(920, 0, 1280, 180);
        return layout.apply(1280, 480, player, Arrays.asList(new Rect(0, 396, 1280, 480)));
    }
    @Test public void reflowsTwoNativeGroupsWithoutReparentingThem() {
        assertTrue(compact()); measure();
        assertEquals(LinearLayout.HORIZONTAL, controls.getOrientation());
        assertSame(narrow, controls.getParent()); assertSame(controls, first.getParent()); assertSame(controls, second.getParent());
        assertTrue(controls.getWidth() > 64); assertFalse(narrow.getClipChildren());
    }
    @Test public void tapAtMovedBoundsReachesTheOriginalNativeAction() {
        assertTrue(compact()); measure();
        int[] c = new int[2], d = new int[2]; first.getChildAt(0).getLocationInWindow(c); canvas.getLocationInWindow(d);
        assertTrue(c[0] - d[0] >= 920); assertTrue(c[1] - d[1] >= 180);
        assertTrue(layout.click(c[0] - d[0] + 32, c[1] - d[1] + 32, SystemClock.uptimeMillis()));
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(1, clicks);
    }
    @Test public void closingRestoresNativeLayoutAndClipping() {
        assertTrue(compact()); measure(); layout.close(); measure();
        assertEquals(LinearLayout.VERTICAL, controls.getOrientation()); assertEquals(64, controls.getWidth());
        assertEquals(0f, controls.getTranslationX(), 0f); assertEquals(0f, controls.getTranslationY(), 0f);
        assertTrue(narrow.getClipChildren()); assertFalse(layout.click(1200, 200, SystemClock.uptimeMillis()));
    }
    @Test public void expandedCardUsesAnExplicitOverflowWithoutLeavingThePreviousGridActive() {
        assertTrue(compact()); measure();
        assertTrue(layout.apply(1280, 480, new Rect(640, 0, 1280, 390), Collections.emptyList())); measure(); measure();
        assertEquals(3, controls.getChildCount()); assertEquals(View.GONE, first.getVisibility()); assertEquals(View.GONE, second.getVisibility());
        assertTrue(controls.getChildAt(2).getContentDescription().toString().contains("controls"));
        layout.close(); measure();
        assertEquals(LinearLayout.VERTICAL, controls.getOrientation()); assertTrue(narrow.getClipChildren());
    }
    @Test public void emptyPlaceholderForANativeHiddenActionDoesNotPreventReflow() {
        FrameLayout placeholder = new FrameLayout(canvas.getContext());
        Button hidden = new Button(canvas.getContext()); hidden.setVisibility(View.GONE);
        placeholder.addView(hidden, new FrameLayout.LayoutParams(64, 64));
        first.addView(placeholder, new LinearLayout.LayoutParams(64, LinearLayout.LayoutParams.WRAP_CONTENT)); measure();
        assertTrue(compact());
    }
    private LinearLayout replacement() {
        LinearLayout next = new LinearLayout(canvas.getContext()); next.setId(42); next.setOrientation(LinearLayout.VERTICAL);
        next.addView(group((Activity) canvas.getContext(), 3), new LinearLayout.LayoutParams(64, 192));
        next.addView(group((Activity) canvas.getContext(), 2), new LinearLayout.LayoutParams(64, 128));
        narrow.addView(next, new FrameLayout.LayoutParams(64, 396));
        return next;
    }
    @Test public void replacingTheNativeTreeRetainsGeometryAndRoutesOnlyToTheNewActions() {
        int[] oldClicks = {0}; first.getChildAt(0).setOnClickListener(v -> oldClicks[0]++);
        assertTrue(compact()); measure(); narrow.removeView(controls);
        LinearLayout next = replacement(); measure(); measure();
        assertEquals(LinearLayout.HORIZONTAL, next.getOrientation());
        assertEquals(LinearLayout.VERTICAL, controls.getOrientation());
        assertEquals(64, controls.getLayoutParams().width); assertEquals(396, controls.getLayoutParams().height);
        int[] p = new int[2], origin = new int[2];
        ((LinearLayout) next.getChildAt(0)).getChildAt(0).getLocationInWindow(p); canvas.getLocationInWindow(origin);
        assertTrue(layout.click(p[0] - origin[0] + 32, p[1] - origin[1] + 32, SystemClock.uptimeMillis()));
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(1, clicks); assertEquals(0, oldClicks[0]);
    }
    @Test public void temporarilyAbsentControlsAreRestoredWhenTheNativeTreeReturns() {
        assertTrue(compact()); measure(); narrow.removeView(controls); measure();
        assertTrue(narrow.getClipChildren()); assertFalse(layout.click(1200, 200, SystemClock.uptimeMillis()));
        LinearLayout next = replacement(); measure(); measure();
        assertEquals(LinearLayout.HORIZONTAL, next.getOrientation());
    }
    @Test public void closingWhileTheTreeIsAbsentDoesNotReviveGeometryLater() {
        assertTrue(compact()); measure(); narrow.removeView(controls); measure(); layout.close();
        LinearLayout next = replacement(); measure(); measure();
        assertEquals(LinearLayout.VERTICAL, next.getOrientation()); assertTrue(narrow.getClipChildren());
    }
    @Test public void newlyVisibleActionsCannotBeClippedByAnOldGrid() {
        assertTrue(compact()); measure();
        Button extra = new Button(canvas.getContext()); extra.setText("New native action"); extra.setOnClickListener(v -> clicks++);
        first.addView(extra, new LinearLayout.LayoutParams(64, 64)); measure(); measure();
        assertEquals(3, controls.getChildCount()); assertEquals(View.GONE, first.getVisibility());
        first.removeView(extra); measure(); measure();
        assertEquals(2, controls.getChildCount()); assertEquals(LinearLayout.HORIZONTAL, controls.getOrientation());
    }
    @Test public void anUnlabeledNewActionRestoresNativeLayoutInsteadOfOfferingAnAnonymousRow() {
        assertTrue(compact()); measure();
        Button extra = new Button(canvas.getContext()); extra.setOnClickListener(v -> clicks++);
        first.addView(extra, new LinearLayout.LayoutParams(64, 64)); measure(); measure();
        assertEquals(2, controls.getChildCount()); assertEquals(LinearLayout.VERTICAL, controls.getOrientation());
        assertEquals(View.VISIBLE, first.getVisibility()); assertTrue(narrow.getClipChildren());
    }
    @Test public void reparentingAnAncestorRefreshesClippingOwnershipWithoutNewGeometry() {
        assertTrue(compact()); measure();
        canvas.removeView(narrow);
        FrameLayout inserted = new FrameLayout(canvas.getContext());
        canvas.addView(inserted, new FrameLayout.LayoutParams(84, 396));
        inserted.addView(narrow, new FrameLayout.LayoutParams(84, 396)); measure(); measure();
        assertFalse(inserted.getClipChildren());
        layout.close(); assertTrue(inserted.getClipChildren());
    }
    @Test public void nativeHideShowSuspendsAndRebuildsTheReflow() {
        assertTrue(compact()); measure(); controls.setVisibility(View.GONE); measure();
        assertEquals(LinearLayout.VERTICAL, controls.getOrientation()); assertTrue(narrow.getClipChildren());
        controls.setVisibility(View.VISIBLE); measure(); measure();
        assertEquals(LinearLayout.HORIZONTAL, controls.getOrientation()); assertFalse(narrow.getClipChildren());
    }
    @Test public void aRequestDuringNativeSearchWaitsWithoutForcingControlsVisible() {
        controls.setVisibility(View.GONE); measure();
        assertTrue(compact()); assertEquals(View.GONE, controls.getVisibility());
        assertEquals(LinearLayout.VERTICAL, controls.getOrientation()); assertTrue(narrow.getClipChildren());
        controls.setVisibility(View.VISIBLE); measure(); measure();
        assertEquals(LinearLayout.HORIZONTAL, controls.getOrientation());
    }
    private void shortPlayer() {
        assertTrue(layout.apply(1280, 480, new Rect(920, 0, 1280, 300), Arrays.asList(new Rect(0, 396, 1280, 480))));
        measure(); measure(); assertEquals(3, controls.getChildCount());
    }
    private View menuAction() {
        controls.getChildAt(2).performClick(); measure();
        View overlay = canvas.getChildAt(canvas.getChildCount() - 1);
        java.util.ArrayList<View> actions = new java.util.ArrayList<>();
        overlay.findViewsWithText(actions, "Native action", View.FIND_VIEWS_WITH_TEXT);
        assertEquals(5, actions.size()); return actions.get(0);
    }
    @Test public void overflowInvokesTheOriginalActionAndKeepsNativeParents() {
        shortPlayer(); View originalAction = first.getChildAt(0);
        View row = menuAction(); row.performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(1, clicks); assertSame(first, originalAction.getParent()); assertSame(controls, first.getParent());
        assertEquals(1, canvas.getChildCount());
    }
    @Test public void staleOverflowRowsCannotActAfterTheGeometryIsClosed() {
        shortPlayer(); View row = menuAction(); layout.close(); row.performClick();
        assertEquals(0, clicks); assertEquals(View.VISIBLE, first.getVisibility()); assertEquals(2, controls.getChildCount());
    }
    @Test public void nativeSearchRevokesAnOldOverflowEvenWhenTheSameViewsReturn() {
        shortPlayer(); View row = menuAction(); controls.setVisibility(View.GONE); measure();
        assertEquals(1, canvas.getChildCount()); controls.setVisibility(View.VISIBLE); measure(); measure();
        row.performClick(); assertEquals(0, clicks); assertEquals(3, controls.getChildCount());
    }
    @Test public void backClosesOnlyTheOverflowPanel() {
        shortPlayer(); menuAction(); assertTrue(layout.back()); assertFalse(layout.back());
        assertEquals(1, canvas.getChildCount()); assertEquals(3, controls.getChildCount()); assertEquals(0, clicks);
    }
    @Test public void nativeDisabledActionsAreNotOfferedInOverflow() {
        first.getChildAt(0).setEnabled(false); shortPlayer(); controls.getChildAt(2).performClick(); measure();
        java.util.ArrayList<View> actions = new java.util.ArrayList<>();
        canvas.getChildAt(canvas.getChildCount() - 1).findViewsWithText(actions, "Native action", View.FIND_VIEWS_WITH_TEXT);
        assertEquals(4, actions.size());
    }
    @Test public void overflowDoesNotOverwriteNativeLayoutParameterUpdates() {
        shortPlayer(); LinearLayout.LayoutParams updated = new LinearLayout.LayoutParams(64, 160); updated.topMargin = 7;
        second.setLayoutParams(updated); layout.close();
        assertSame(updated, second.getLayoutParams()); assertEquals(View.VISIBLE, second.getVisibility());
    }
    @Test public void aLargerCardCanReturnToTheCompactGridWithoutLosingActions() {
        shortPlayer(); assertTrue(compact()); measure(); measure();
        assertEquals(2, controls.getChildCount()); assertEquals(View.VISIBLE, first.getVisibility());
        assertEquals(View.VISIBLE, second.getVisibility()); assertEquals(LinearLayout.HORIZONTAL, controls.getOrientation());
    }
}
