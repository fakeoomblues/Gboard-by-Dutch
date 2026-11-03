package patches.maps.gearslip;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public final class NativeControlLabelsTest {
    private static final int SOUND = 0x7f990001;
    private final Context base = RuntimeEnvironment.getApplication();
    private final Resources resources = new Resources(base.getAssets(), base.getResources().getDisplayMetrics(), base.getResources().getConfiguration()) {
        @Override public String getResourceEntryName(int id) {
            return id == SOUND ? "map_buttons_view_mute_container" : super.getResourceEntryName(id);
        }
    };
    private final Context context = new ContextWrapper(base) {
        @Override public Resources getResources() { return resources; }
    };
    @Test public void aGeneratedNativeButtonResolvesItsRoleThroughDeepCardWrappers() {
        FrameLayout role = new FrameLayout(context); role.setId(SOUND);
        FrameLayout parent = role;
        for (int i = 0; i < 7; i++) {
            FrameLayout wrapper = new FrameLayout(context); wrapper.setId(View.generateViewId());
            parent.addView(wrapper); parent = wrapper;
        }
        ImageView button = new ImageView(context); button.setId(View.generateViewId()); parent.addView(button);
        assertEquals("Sound", new NativeControlLabels(context).action(button));
        button.setContentDescription("Unmute guidance");
        assertEquals("Unmute guidance", new NativeControlLabels(context).action(button));
    }
    @Test public void generatedIdsWithoutAKnownRoleDoNotBecomeAnonymousMenuActions() {
        ImageView button = new ImageView(context); button.setId(View.generateViewId());
        assertNull(new NativeControlLabels(context).action(button));
    }
}
