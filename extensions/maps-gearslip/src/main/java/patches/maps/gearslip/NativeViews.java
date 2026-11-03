package patches.maps.gearslip;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

/** Resolve mixed Maps/Android Auto resource owners without assuming the renamed package. */
final class NativeViews {
    static String name(View view) {
        int id = view.getId();
        if (id == View.NO_ID || (id >>> 24) == 0) return null;
        try { return view.getResources().getResourceEntryName(id); }
        catch (Resources.NotFoundException missing) { return null; }
    }
    static View find(View root, String name) {
        if (name.equals(name(root))) return root;
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View found = find(((ViewGroup) root).getChildAt(i), name);
            if (found != null) return found;
        }
        return null;
    }
}
