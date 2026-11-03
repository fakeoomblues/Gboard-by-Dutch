package patches.maps.gearslip;

import android.content.Context;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

/** Use native accessibility labels before falling back to the adapter's localized names. */
final class NativeControlLabels {
    private final Context app;
    NativeControlLabels(Context app) { this.app = app; }
    String more() { return string("morphe_maps_more_controls", "More controls"); }
    CharSequence action(View source) {
        CharSequence label = source.getContentDescription();
        if (blank(label) && source instanceof TextView) label = ((TextView) source).getText();
        if (blank(label)) {
            try {
                AccessibilityNodeInfo info = source.createAccessibilityNodeInfo();
                if (info != null) {
                    try { label = info.getContentDescription(); if (blank(label)) label = info.getText(); }
                    finally { info.recycle(); }
                }
            } catch (RuntimeException | LinkageError unavailable) { /* Use the known native role below. */ }
        }
        if (!blank(label)) return label.length() > 240 ? label.subSequence(0, 240) : label;
        View node = source;
        // Native widgets add generated-ID/card wrappers between their action and
        // named role. Walk the owning branch rather than assuming a fixed depth.
        while (node != null) {
            String name = NativeViews.name(node);
            if (name != null) switch (name) {
                case "map_buttons_view_settings_button": case "one_bar_settings_button_view": return string("morphe_maps_control_settings", "Settings");
                case "map_buttons_view_mute_container": return string("morphe_maps_control_sound", "Sound");
                case "map_buttons_view_compass": case "map_buttons_view_compass_container": return string("morphe_maps_control_compass", "Compass");
                case "map_buttons_view_zoom_in": return string("morphe_maps_control_zoom_in", "Zoom in");
                case "map_buttons_view_zoom_out": return string("morphe_maps_control_zoom_out", "Zoom out");
                case "recenter_button_container": return string("morphe_maps_control_recenter", "Recenter");
                case "map_buttons_view_map_interaction": case "map_buttons_view_map_interaction_button_card": return string("morphe_maps_control_pan", "Move map");
                case "map_buttons_view_report_incident_button": return string("morphe_maps_control_report", "Report incident");
                case "map_buttons_view_evcs_layer_map_filters_screen_container": return string("morphe_maps_control_charging", "Charging filters");
            }
            ViewParent parent = node.getParent(); node = parent instanceof View ? (View) parent : null;
        }
        return null; // Do not offer an anonymous or guessed action to the driver.
    }
    private String string(String name, String fallback) {
        int id = app.getResources().getIdentifier(name, "string", app.getPackageName());
        if (id == 0) id = app.getResources().getIdentifier(name, "string", "com.google.android.apps.maps");
        return id == 0 ? fallback : app.getString(id);
    }
    private static boolean blank(CharSequence text) { return text == null || text.toString().trim().isEmpty(); }
}
