package patches.maps.gearslip;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BooleanSupplier;

/** An explicit overflow surface whose rows invoke current native action handlers. */
final class NativeActionMenu {
    static final class Action {
        final View source;
        final CharSequence label;
        final BooleanSupplier current;
        final Runnable activate;
        Action(View source, CharSequence label, BooleanSupplier current, Runnable activate) {
            this.source = source; this.label = label; this.current = current; this.activate = activate;
        }
        void invoke() { if (current.getAsBoolean() && source.isEnabled()) activate.run(); }
    }
    private final ViewGroup decor;
    private final String title;
    private FrameLayout overlay;
    private ScrollView scroll;
    private List<Action> actions = Collections.emptyList();
    private final List<Button> buttons = new ArrayList<>();
    NativeActionMenu(ViewGroup decor, String title) { this.decor = decor; this.title = title; }
    Button trigger(int unit, Runnable open) {
        Button button = button(unit, "⋯"); button.setContentDescription(title);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit * .6f);
        GradientDrawable shape = new GradientDrawable(); shape.setColor(background()); shape.setCornerRadius(unit / 2f);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(pressed()), new InsetDrawable(shape, Math.max(1, unit / 16)), null));
        button.setElevation(unit / 16f); button.setOnClickListener(v -> open.run()); return button;
    }
    boolean show(List<Action> actions, Rect panel, int unit) {
        dismiss();
        if (actions.isEmpty() || panel == null) return false;
        FrameLayout layer = new FrameLayout(decor.getContext()); layer.setClickable(true); layer.setOnClickListener(v -> dismiss());
        LinearLayout sheet = new LinearLayout(decor.getContext()); sheet.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable shape = new GradientDrawable(); shape.setColor(background()); shape.setCornerRadius(unit / 4f);
        sheet.setBackground(shape); sheet.setClipToOutline(true); sheet.setElevation(unit / 8f); sheet.setClickable(true);
        LinearLayout header = new LinearLayout(decor.getContext()); header.setOrientation(LinearLayout.HORIZONTAL);
        TextView heading = new TextView(decor.getContext()); heading.setText(title); heading.setTextColor(foreground());
        heading.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit * .375f); heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(unit / 4, 0, 0, 0); heading.setSingleLine(); heading.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(heading, new LinearLayout.LayoutParams(0, unit, 1));
        Button close = button(unit, "×"); close.setContentDescription(decor.getContext().getString(android.R.string.cancel));
        close.setOnClickListener(v -> dismiss()); header.addView(close, new LinearLayout.LayoutParams(unit, unit));
        sheet.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, unit));
        ScrollView body = new ScrollView(decor.getContext()); body.setFillViewport(false);
        LinearLayout rows = new LinearLayout(decor.getContext()); rows.setOrientation(LinearLayout.VERTICAL);
        for (Action action : actions) {
            Button row = button(unit, action.label); row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            row.setPadding(unit / 4, unit / 8, unit / 4, unit / 8);
            row.setMinHeight(unit); row.setMinimumHeight(unit); row.setSingleLine(false);
            row.setMaxLines(Integer.MAX_VALUE); row.setEllipsize(null);
            row.setOnClickListener(v -> { dismiss(); action.invoke(); });
            rows.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); buttons.add(row);
        }
        body.addView(rows, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sheet.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        FrameLayout.LayoutParams at = new FrameLayout.LayoutParams(panel.width(), panel.height(), Gravity.TOP | Gravity.LEFT); at.leftMargin = panel.left; at.topMargin = panel.top;
        layer.addView(sheet, at);
        overlay = layer; scroll = body; this.actions = new ArrayList<>(actions);
        FrameLayout.LayoutParams full = new FrameLayout.LayoutParams(decor.getWidth(), decor.getHeight(), Gravity.TOP | Gravity.LEFT);
        full.leftMargin = -decor.getPaddingLeft(); full.topMargin = -decor.getPaddingTop();
        decor.addView(layer, full);
        return true;
    }
    boolean click(float x, float y, long time) {
        if (overlay == null) return false;
        int[] p = new int[2], origin = new int[2]; overlay.getLocationInWindow(p); decor.getLocationInWindow(origin);
        MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x - p[0] + origin[0], y - p[1] + origin[1], 0);
        MotionEvent up = MotionEvent.obtain(time, time + 1, MotionEvent.ACTION_UP, x - p[0] + origin[0], y - p[1] + origin[1], 0);
        down.setSource(0x1002); up.setSource(0x1002);
        try { FrameLayout target = overlay; target.dispatchTouchEvent(down); target.dispatchTouchEvent(up); return true; }
        finally { down.recycle(); up.recycle(); }
    }
    boolean scroll(float distance) {
        if (scroll == null) return false;
        if (Float.isFinite(distance)) scroll.smoothScrollBy(0, Math.round(distance)); return true;
    }
    boolean fling(float velocity) {
        if (scroll == null) return false;
        if (Float.isFinite(velocity)) scroll.fling(-Math.round(velocity)); return true;
    }
    boolean dismiss() {
        if (overlay == null) return false;
        if (overlay.getParent() == decor) decor.removeView(overlay);
        overlay = null; scroll = null; actions = Collections.emptyList(); buttons.clear(); return true;
    }
    void refresh() {
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            buttons.get(i).setEnabled(action.current.getAsBoolean() && action.source.isEnabled());
        }
    }
    private Button button(int unit, CharSequence text) {
        Button button = new Button(decor.getContext()); button.setText(text); button.setAllCaps(false);
        button.setMinWidth(0); button.setMinimumWidth(0); button.setMinHeight(0); button.setMinimumHeight(0); button.setPadding(0, 0, 0, 0);
        button.setTextColor(foreground()); button.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit * .375f);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(pressed()), new ColorDrawable(background()), null));
        return button;
    }
    private boolean night() { return (decor.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES; }
    private int background() { return night() ? Color.rgb(40, 40, 40) : Color.WHITE; }
    private int foreground() { return night() ? Color.WHITE : Color.rgb(30, 30, 30); }
    private int pressed() { return night() ? 0xff606060 : 0xffdddddd; }
}
