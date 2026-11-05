package patches.maps.gearslip;

import static org.junit.Assert.*;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public final class NativeControlsTest {
    private static final int GROUP = 0x7f990001, SETTINGS = 0x7f990002, SEARCH = 0x7f990003, EDITOR = 0x7f990004;
    private FrameLayout root, group;
    private ImageView button;
    private NativeControls controls;
    private Context context;
    private int clicks, updates;
    @Before public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Resources original = activity.getResources();
        Resources resources = new Resources(activity.getAssets(), original.getDisplayMetrics(), original.getConfiguration()) {
            @Override public String getResourceEntryName(int id) {
                if (id == GROUP) return "map_buttons_view_container";
                if (id == SETTINGS) return "map_buttons_view_settings_button";
                if (id == SEARCH) return "main_suggested_destinations_search_box";
                if (id == EDITOR) return "destination_input_keyboard_search_edit_text";
                return super.getResourceEntryName(id);
            }
        };
        context = new ContextWrapper(activity) { @Override public Resources getResources() { return resources; } };
        root = new FrameLayout(context); activity.setContentView(root);
        group = new FrameLayout(context); group.setId(GROUP);
        root.addView(group, new FrameLayout.LayoutParams(64, 200));
        button = button(); group.addView(button, new FrameLayout.LayoutParams(64, 64));
        measure();
    }
    private ImageView button() {
        ImageView view = new ImageView(context); view.setId(SETTINGS);
        view.setImageDrawable(new ColorDrawable(Color.BLUE)); view.setOnClickListener(v -> clicks++); return view;
    }
    private void measure() {
        root.measure(View.MeasureSpec.makeMeasureSpec(1280, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1280, 480); root.getViewTreeObserver().dispatchOnGlobalLayout();
    }
    private NativeControls.Control bind() {
        controls = new NativeControls(root, () -> updates++);
        assertEquals(1, controls.snapshot().size()); return controls.snapshot().get(0);
    }
    @After public void close() { if (controls != null) controls.close(); }
    @Test public void exportsNativeActionsWithoutReparentingOrResizingTheNativeMap() {
        NativeControls.Control control = bind();
        assertEquals(NativeControls.Role.SETTINGS, control.role); assertEquals("Settings", control.title);
        assertEquals(48, control.icon.getWidth()); assertSame(group, button.getParent());
        assertEquals(View.INVISIBLE, group.getVisibility()); assertEquals(64, group.getLayoutParams().width);
        assertEquals(1280, root.getWidth()); assertEquals(480, root.getHeight());
    }
    @Test public void aHostActionInvokesTheNativeHandlerAndKeepsItsDuplicateHidden() {
        NativeControls.Control control = bind(); controls.invoke(control);
        assertEquals(1, clicks); assertEquals(View.INVISIBLE, group.getVisibility());
    }
    @Test public void nativeControlPixelsCannotStealTouchesFromTheMapAfterExport() {
        int[] mapClicks = {0};
        View map = new View(context); map.setOnClickListener(v -> mapClicks[0]++);
        root.addView(map, 0, new FrameLayout.LayoutParams(1280, 480)); measure(); bind();
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 32, 32, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 1, MotionEvent.ACTION_UP, 32, 32, 0);
        try { root.dispatchTouchEvent(down); root.dispatchTouchEvent(up); }
        finally { down.recycle(); up.recycle(); }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(0, clicks); assertEquals(1, mapClicks[0]);
    }
    @Test public void replacedNativeViewsRevokeOldHostCallbacks() {
        NativeControls.Control old = bind(); group.removeView(button); button = button();
        group.addView(button, new FrameLayout.LayoutParams(64, 64)); measure();
        assertThrows(SecurityException.class, () -> controls.invoke(old));
        controls.invoke(controls.snapshot().get(0)); assertEquals(1, clicks);
    }
    @Test public void disabledNativeActionsDisappearAndNotifyTheHost() {
        NativeControls.Control old = bind(); int before = updates; button.setEnabled(false);
        root.getViewTreeObserver().dispatchOnPreDraw(); assertTrue(controls.snapshot().isEmpty()); assertTrue(updates > before);
        assertThrows(SecurityException.class, () -> controls.invoke(old));
        button.setEnabled(true); root.getViewTreeObserver().dispatchOnPreDraw(); assertEquals(1, controls.snapshot().size());
    }
    @Test public void aNativeIconChangeRefreshesTheHostModelWithoutChangingTheActionHandler() {
        NativeControls.Control control = bind(); android.graphics.Bitmap icon = control.icon; int before = updates;
        button.setImageDrawable(new ColorDrawable(Color.RED)); root.getViewTreeObserver().dispatchOnPreDraw();
        assertSame(control, controls.snapshot().get(0)); assertNotSame(icon, control.icon); assertTrue(updates > before);
        controls.invoke(control); assertEquals(1, clicks);
    }
    @Test public void aDisabledNativeOwnerCannotBeBypassedThroughACalAction() {
        NativeControls.Control control = bind(); group.setEnabled(false);
        assertThrows(SecurityException.class, () -> controls.invoke(control)); assertEquals(0, clicks);
        root.getViewTreeObserver().dispatchOnPreDraw(); assertTrue(controls.snapshot().isEmpty());
    }
    @Test public void searchFocusesTheActiveNativeEditorRatherThanAnOlderHiddenEditor() {
        int[] oldClicks = {0}, currentClicks = {0};
        View old = new View(context); old.setId(EDITOR); old.setVisibility(View.GONE); old.setOnClickListener(v -> oldClicks[0]++);
        View current = new View(context); current.setId(EDITOR); current.setVisibility(View.GONE); current.setOnClickListener(v -> currentClicks[0]++);
        root.addView(old, new FrameLayout.LayoutParams(100, 64)); root.addView(current, new FrameLayout.LayoutParams(100, 64));
        ImageView search = new ImageView(context); search.setId(SEARCH); search.setImageDrawable(new ColorDrawable(Color.BLUE));
        search.setOnClickListener(v -> current.setVisibility(View.VISIBLE)); root.addView(search, new FrameLayout.LayoutParams(64, 64));
        measure(); controls = new NativeControls(root, () -> updates++);
        NativeControls.Control action = controls.snapshot().stream().filter(control -> control.role == NativeControls.Role.SEARCH).findFirst().get();
        controls.invoke(action); measure(); root.getViewTreeObserver().dispatchOnPreDraw();
        assertEquals(0, oldClicks[0]); assertEquals(1, currentClicks[0]);
    }
    @Test public void nativeLayoutHidingDoesNotRemoveTheActionFromTheStandardHostInterface() {
        group.setVisibility(View.GONE); NativeControls.Control control = bind(); controls.invoke(control);
        assertEquals(1, clicks); assertEquals(View.GONE, group.getVisibility());
    }
    @Test public void closeRestoresOnlyTheVisibilityOwnedByTheAdapter() {
        bind(); controls.close(); assertEquals(View.VISIBLE, group.getVisibility());
        controls = null; bind(); group.setVisibility(View.GONE); controls.close();
        assertEquals(View.GONE, group.getVisibility());
    }
}
