package patches.maps.gearslip;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Owns only temporary layout and overflow UI; native parents and action handlers remain intact. */
final class NativeControlLayout implements ViewTreeObserver.OnGlobalLayoutListener, ViewTreeObserver.OnPreDrawListener {
    private final ViewGroup decor;
    private final int controlsId;
    private final NativeControlLabels labels;
    private final Map<ViewGroup, boolean[]> clips = new IdentityHashMap<>();
    private LinearLayout controls;
    private View firstView, secondView;
    private FrameLayout.LayoutParams original;
    private LinearLayout.LayoutParams first, second;
    private int orientation, unit, firstVisibility, secondVisibility;
    private float translationX, translationY;
    private Rect destination;
    private int requestedWidth, requestedHeight;
    private Rect requestedPlayer;
    private List<Rect> requestedCovers;
    private ViewParent originalParent;
    private boolean listening, overflow;
    private Button more;
    private NativeActionMenu menu;
    private long actionEpoch;
    private View overflowFailedOwner;

    NativeControlLayout(ViewGroup decor, int controlsId) { this(decor, controlsId, decor.getContext()); }
    NativeControlLayout(ViewGroup decor, int controlsId, Context app) {
        this.decor = decor; this.controlsId = controlsId; labels = new NativeControlLabels(app);
    }
    boolean owns(View root) { return root == decor; }
    boolean apply(int width, int height, Rect player, List<Rect> covers) {
        close();
        if (player == null || player.isEmpty()) return true;
        requestedWidth = width; requestedHeight = height; requestedPlayer = new Rect(player);
        requestedCovers = new ArrayList<>();
        for (Rect cover : covers) requestedCovers.add(new Rect(cover));
        if (!install()) {
            LinearLayout hidden = candidate();
            if (hidden == null || hidden.isShown() || !possible(hidden)) {
                requestedPlayer = null; requestedCovers = null; return false;
            }
        }
        decor.getViewTreeObserver().addOnGlobalLayoutListener(this);
        decor.getViewTreeObserver().addOnPreDrawListener(this); listening = true;
        onGlobalLayout();
        return true;
    }
    private LinearLayout candidate() {
        View found = decor.findViewById(controlsId);
        if (!(found instanceof LinearLayout)) return null;
        LinearLayout group = (LinearLayout) found;
        if (group.getChildCount() != 2 || !(group.getLayoutParams() instanceof FrameLayout.LayoutParams) ||
                !(group.getChildAt(0) instanceof ViewGroup) || !(group.getChildAt(1) instanceof ViewGroup) ||
                !(group.getChildAt(0).getLayoutParams() instanceof LinearLayout.LayoutParams) ||
                !(group.getChildAt(1).getLayoutParams() instanceof LinearLayout.LayoutParams)) return null;
        return group;
    }
    private boolean size(LinearLayout group) {
        unit = group.getLayoutParams().width > 0 ? group.getLayoutParams().width : group.getWidth();
        return unit > 0 && unit <= 32768;
    }
    private List<Rect> obstacles() {
        List<Rect> all = new ArrayList<>();
        for (Rect cover : requestedCovers) all.add(new Rect(cover));
        all.add(new Rect(requestedPlayer)); return all;
    }
    private Rect grid(LinearLayout group) {
        if (!size(group)) return null;
        LinearLayout.LayoutParams a = controls == group ? first : (LinearLayout.LayoutParams) group.getChildAt(0).getLayoutParams();
        LinearLayout.LayoutParams b = controls == group ? second : (LinearLayout.LayoutParams) group.getChildAt(1).getLayoutParams();
        int needed = Math.max(unit, requiredHeight(group, a, b));
        return ControlPlacement.belowBlock(requestedWidth, requestedHeight, requestedPlayer, obstacles(),
                unit * 2 + Math.max(1, unit / 8) + group.getPaddingLeft() + group.getPaddingRight(), needed);
    }
    private Rect anchor(LinearLayout group) {
        if (!size(group)) return null;
        return ControlPlacement.overflowAnchor(requestedWidth, requestedHeight, requestedPlayer, anchorObstacles(group), unit);
    }
    private List<Rect> anchorObstacles(LinearLayout group) {
        List<Rect> all = obstacles();
        for (String name : new String[]{"car_mic_button", "main_suggested_destinations_search_box", "nav_card_view", "status_card_view", "speed_limit_and_watermark_view", "map_buttons_view_speed_limit", "recenter_button_container"}) {
            View view = NativeViews.find(decor, name);
            Rect bounds = new Rect();
            if (view != null && !inside(view, group) && view.isShown() && view.getAlpha() > 0 && view.getGlobalVisibleRect(bounds)) {
                int[] origin = new int[2]; decor.getLocationOnScreen(origin); bounds.offset(-origin[0], -origin[1]);
                all.add(bounds);
            }
        }
        return all;
    }
    private boolean possible(LinearLayout group) {
        Rect target = grid(group);
        if (target != null) return true;
        target = anchor(group);
        return target != null && ControlPlacement.menuPanel(requestedWidth, requestedHeight, obstacles(), target, unit, 1) != null;
    }
    private boolean install() {
        LinearLayout group = candidate();
        if (group == null || !group.isShown() || !group.isEnabled()) return false;
        Rect bounds = grid(group);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams((LinearLayout.LayoutParams) group.getChildAt(0).getLayoutParams());
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams((LinearLayout.LayoutParams) group.getChildAt(1).getLayoutParams());
        boolean collapse = bounds == null || !fits(group, a, b, bounds);
        if (collapse) {
            if (overflowFailedOwner == group) return false;
            bounds = anchor(group);
            List<Entry> actions = entries(group);
            if (bounds == null || actions == null || actions.isEmpty() ||
                    ControlPlacement.menuPanel(requestedWidth, requestedHeight, obstacles(), bounds, unit, actions.size()) == null) return false;
        }
        controls = group; firstView = group.getChildAt(0); secondView = group.getChildAt(1); originalParent = group.getParent();
        first = a; second = b; firstVisibility = firstView.getVisibility(); secondVisibility = secondView.getVisibility();
        destination = bounds; overflow = collapse;
        original = new FrameLayout.LayoutParams((FrameLayout.LayoutParams) group.getLayoutParams());
        orientation = group.getOrientation(); translationX = group.getTranslationX(); translationY = group.getTranslationY();
        for (ViewParent parent = group.getParent(); parent instanceof ViewGroup; parent = parent.getParent()) {
            ViewGroup ancestor = (ViewGroup) parent;
            clips.put(ancestor, new boolean[]{ancestor.getClipChildren(), ancestor.getClipToPadding()});
            if (ancestor == decor) break;
        }
        long epoch = ++actionEpoch;
        if (overflow) {
            menu = new NativeActionMenu(decor, labels.more());
            more = menu.trigger(unit, () -> openMenu(epoch, group));
            controls.addView(more, new LinearLayout.LayoutParams(unit, unit));
        }
        return true;
    }
    private static int minimumHeight(ViewGroup group) {
        int height = group.getPaddingTop() + group.getPaddingBottom();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            if (child.getMeasuredHeight() == 0 && hasVisibleAction(child)) return 32768;
            height += child.getMeasuredHeight();
            if (child.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                height += margins.topMargin + margins.bottomMargin;
            }
        }
        return height;
    }
    private static boolean fits(LinearLayout group, LinearLayout.LayoutParams a, LinearLayout.LayoutParams b, Rect bounds) {
        return requiredHeight(group, a, b) <= bounds.height();
    }
    private static int requiredHeight(LinearLayout group, LinearLayout.LayoutParams a, LinearLayout.LayoutParams b) {
        int required = Math.max(minimumHeight((ViewGroup) group.getChildAt(0)) + a.topMargin + a.bottomMargin,
                minimumHeight((ViewGroup) group.getChildAt(1)) + b.topMargin + b.bottomMargin);
        return required + group.getPaddingTop() + group.getPaddingBottom();
    }
    private static boolean hasVisibleAction(View view) {
        if (view.getVisibility() != View.VISIBLE) return false;
        if (view.isClickable()) return true;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
            if (hasVisibleAction(((ViewGroup) view).getChildAt(i))) return true;
        return false;
    }
    @Override public void onGlobalLayout() {
        if (requestedPlayer == null) return;
        if (controls != null && (!controls.isShown() || !treeCurrent())) restoreLayout();
        if (controls != null && !overflow && !fits(controls, first, second, destination)) restoreLayout();
        if (controls != null && overflow && grid(controls) != null) restoreLayout();
        if (controls == null && !install()) return;
        if (overflow) {
            List<Rect> occupied = anchorObstacles(controls);
            boolean overlap = false;
            for (Rect cover : occupied) if (Rect.intersects(destination, cover)) { overlap = true; break; }
            if (overlap) {
                Rect next = ControlPlacement.overflowAnchor(requestedWidth, requestedHeight, requestedPlayer, occupied, unit);
                if (next == null) { restoreLayout(); return; }
                destination = next;
            }
        }
        for (ViewGroup ancestor : clips.keySet()) { ancestor.setClipChildren(false); ancestor.setClipToPadding(false); }
        if (controls.getOrientation() != LinearLayout.HORIZONTAL) controls.setOrientation(LinearLayout.HORIZONTAL);
        ViewGroup.LayoutParams params = controls.getLayoutParams();
        if (params.width != destination.width() || params.height != destination.height()) {
            FrameLayout.LayoutParams next = new FrameLayout.LayoutParams(original);
            next.width = destination.width(); next.height = destination.height(); controls.setLayoutParams(next);
        }
        if (overflow) {
            if (firstVisibility == View.VISIBLE && firstView.getVisibility() == View.VISIBLE) firstView.setVisibility(View.GONE);
            if (secondVisibility == View.VISIBLE && secondView.getVisibility() == View.VISIBLE) secondView.setVisibility(View.GONE);
        } else { column(firstView, first, false); column(secondView, second, true); }
        int[] position = new int[2], origin = new int[2];
        controls.getLocationInWindow(position); decor.getLocationInWindow(origin);
        float dx = destination.left - (position[0] - origin[0]), dy = destination.top - (position[1] - origin[1]);
        if (dx != 0) controls.setTranslationX(controls.getTranslationX() + dx);
        if (dy != 0) controls.setTranslationY(controls.getTranslationY() + dy);
    }
    @Override public boolean onPreDraw() {
        if (menu != null) {
            if (!active(actionEpoch, controls)) menu.dismiss();
            else menu.refresh();
        }
        return true;
    }
    private boolean treeCurrent() {
        return decor.findViewById(controlsId) == controls && controls.getChildCount() == (overflow ? 3 : 2) &&
                controls.getChildAt(0) == firstView && controls.getChildAt(1) == secondView &&
                (!overflow || controls.getChildAt(2) == more) &&
                controls.getLayoutParams() instanceof FrameLayout.LayoutParams &&
                firstView.getLayoutParams() instanceof LinearLayout.LayoutParams && secondView.getLayoutParams() instanceof LinearLayout.LayoutParams && sameAncestors();
    }
    private boolean sameAncestors() {
        int count = 0;
        for (ViewParent parent = controls.getParent(); parent instanceof ViewGroup; parent = parent.getParent()) {
            if (!clips.containsKey(parent)) return false;
            count++;
            if (parent == decor) return count == clips.size();
        }
        return false;
    }
    private void column(View view, LinearLayout.LayoutParams base, boolean end) {
        LinearLayout.LayoutParams current = (LinearLayout.LayoutParams) view.getLayoutParams();
        int left = base.leftMargin + (end ? Math.max(1, unit / 8) : 0);
        if (current.width == unit && current.height == ViewGroup.LayoutParams.MATCH_PARENT && current.weight == 0 && current.leftMargin == left) return;
        LinearLayout.LayoutParams next = new LinearLayout.LayoutParams(base);
        next.width = unit; next.height = ViewGroup.LayoutParams.MATCH_PARENT; next.weight = 0; next.leftMargin = left;
        view.setLayoutParams(next);
    }
    boolean click(float x, float y, long time) {
        if (menu != null && menu.click(x, y, time)) return true;
        if (controls == null || !controls.isShown() || controls.getAlpha() <= 0 || !treeCurrent()) return false;
        int[] position = new int[2], origin = new int[2];
        controls.getLocationInWindow(position); decor.getLocationInWindow(origin);
        float localX = x - position[0] + origin[0], localY = y - position[1] + origin[1];
        if (localX < 0 || localY < 0 || localX >= controls.getWidth() || localY >= controls.getHeight()) return false;
        return touch(controls, localX, localY, time);
    }
    private static boolean touch(View target, float x, float y, long time) {
        MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(time, time + 1, MotionEvent.ACTION_UP, x, y, 0);
        down.setSource(0x1002); up.setSource(0x1002);
        try { boolean handled = target.dispatchTouchEvent(down); target.dispatchTouchEvent(up); return handled; }
        finally { down.recycle(); up.recycle(); }
    }
    boolean back() { return menu != null && menu.dismiss(); }
    boolean scroll(float distanceY) { return menu != null && menu.scroll(distanceY); }
    boolean fling(float velocityY) { return menu != null && menu.fling(velocityY); }
    private static final class Entry {
        final View view; final CharSequence label;
        Entry(View view, CharSequence label) { this.view = view; this.label = label; }
    }
    private List<Entry> entries(LinearLayout group) {
        List<Entry> entries = new ArrayList<>();
        if (!collect(group.getChildAt(0), entries) || !collect(group.getChildAt(1), entries)) return null;
        return entries.size() <= 32 ? entries : null;
    }
    private boolean ownedHidden(View view) {
        return overflow && view.getVisibility() == View.GONE &&
                ((view == firstView && firstVisibility == View.VISIBLE) || (view == secondView && secondVisibility == View.VISIBLE));
    }
    private boolean collect(View view, List<Entry> entries) {
        if ((view.getVisibility() != View.VISIBLE && !ownedHidden(view)) || !view.isEnabled()) return true;
        int before = entries.size();
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
            if (!collect(((ViewGroup) view).getChildAt(i), entries)) return false;
        if (entries.size() == before && view.isClickable()) {
            CharSequence label = labels.action(view);
            if (label == null) {
                Log.w("MapsNativeCar", "Native overflow action has no known label: " + view.getClass().getName() + " role=" + NativeViews.name(view));
                return false;
            }
            entries.add(new Entry(view, label));
        }
        return true;
    }
    private boolean active(long epoch, LinearLayout owner) {
        return epoch == actionEpoch && requestedPlayer != null && overflow && controls == owner && owner != null &&
                owner.isShown() && owner.isEnabled() && treeCurrent();
    }
    private boolean available(View source) {
        View node = source;
        while (node != controls) {
            if (node == null || !node.isEnabled() || (node.getVisibility() != View.VISIBLE && !ownedHidden(node))) return false;
            node = node.getParent() instanceof View ? (View) node.getParent() : null;
        }
        return controls != null;
    }
    private static boolean inside(View source, View owner) {
        for (View node = source; node != null; node = node.getParent() instanceof View ? (View) node.getParent() : null)
            if (node == owner) return true;
        return false;
    }
    private void openMenu(long epoch, LinearLayout owner) {
        if (!active(epoch, owner)) return;
        List<Entry> entries = entries(owner);
        if (entries == null || entries.isEmpty()) { unavailableMenu(); return; }
        Rect panel = ControlPlacement.menuPanel(requestedWidth, requestedHeight, obstacles(), destination, unit, entries.size());
        List<NativeActionMenu.Action> actions = new ArrayList<>();
        for (Entry entry : entries) actions.add(new NativeActionMenu.Action(entry.view, entry.label,
                () -> active(epoch, owner) && available(entry.view), () -> activate(entry.view, epoch, owner)));
        try { if (!menu.show(actions, panel, unit)) unavailableMenu(); }
        catch (RuntimeException unavailable) {
            Log.w("MapsNativeCar", "Native control overflow is unavailable", unavailable); unavailableMenu();
        }
    }
    private void unavailableMenu() { overflowFailedOwner = controls; restoreLayout(); }
    private void activate(View source, long epoch, LinearLayout owner) {
        if (!active(epoch, owner) || !available(source)) return;
        View a = firstView, b = secondView;
        boolean showA = ownedHidden(a), showB = ownedHidden(b);
        if (showA) a.setVisibility(View.VISIBLE);
        if (showB) b.setVisibility(View.VISIBLE);
        try {
            if (source.hasOnClickListeners()) source.performClick();
            else {
                int width = source.getWidth(), height = source.getHeight();
                if (width > 0 && height > 0) touch(source, width / 2f, height / 2f, SystemClock.uptimeMillis());
            }
        } finally {
            if (active(epoch, owner)) {
                if (showA && a.getVisibility() == View.VISIBLE) a.setVisibility(View.GONE);
                if (showB && b.getVisibility() == View.VISIBLE) b.setVisibility(View.GONE);
            }
        }
    }
    void close() {
        if (listening && decor.getViewTreeObserver().isAlive()) {
            decor.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            decor.getViewTreeObserver().removeOnPreDrawListener(this);
        }
        listening = false; requestedPlayer = null; requestedCovers = null; overflowFailedOwner = null; restoreLayout();
    }
    private void restoreLayout() {
        actionEpoch++;
        if (menu != null) menu.dismiss();
        if (controls != null) {
            if (more != null && more.getParent() == controls) controls.removeView(more);
            controls.setOrientation(orientation);
            if ((controls.getParent() == originalParent || controls.getParent() == null) && controls.getLayoutParams() instanceof FrameLayout.LayoutParams)
                controls.setLayoutParams(original);
            if (firstView.getParent() == controls || firstView.getParent() == null) {
                if (!overflow) firstView.setLayoutParams(first);
                if (overflow && firstView.getVisibility() == View.GONE) firstView.setVisibility(firstVisibility);
            }
            if (secondView.getParent() == controls || secondView.getParent() == null) {
                if (!overflow) secondView.setLayoutParams(second);
                if (overflow && secondView.getVisibility() == View.GONE) secondView.setVisibility(secondVisibility);
            }
            controls.setTranslationX(translationX); controls.setTranslationY(translationY);
        }
        for (Map.Entry<ViewGroup, boolean[]> entry : clips.entrySet()) {
            entry.getKey().setClipChildren(entry.getValue()[0]); entry.getKey().setClipToPadding(entry.getValue()[1]);
        }
        clips.clear(); controls = null; firstView = secondView = null; originalParent = null; destination = null;
        overflow = false; more = null; menu = null;
    }
}
