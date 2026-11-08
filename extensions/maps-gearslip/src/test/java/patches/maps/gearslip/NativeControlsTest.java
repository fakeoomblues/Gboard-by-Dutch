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
import android.widget.RelativeLayout;
import android.widget.TextView;
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
    private static final int GROUP = 0x7f990001, SETTINGS = 0x7f990002, SEARCH = 0x7f990003, EDITOR = 0x7f990004, INPUT_SEARCH = 0x7f990005;
    private static final int NAV_CARD=0x7f990006,STATUS_CARD=0x7f990007,END=0x7f990008;
    private static final int ZOOM_IN=0x7f990009,ZOOM_OUT=0x7f99000a,ZOOM_WIDGET=0x7f99000b;
    private static final int MENU=0x7f99000c,PREVIEW=0x7f99000d;
    private static final int SEARCH_LABEL=0x7f99000e;
    private FrameLayout root, group;
    private ImageView button;
    private NativeControls controls;
    private Context context;
    private int clicks, updates;
    @Before public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Resources original = activity.getResources();
        Resources resources = new Resources(activity.getAssets(), original.getDisplayMetrics(), original.getConfiguration()) {
            @Override public int getIdentifier(String name, String type, String pkg) {
                if ("SEARCH".equals(name) && "string".equals(type)) return SEARCH_LABEL;
                return super.getIdentifier(name, type, pkg);
            }
            @Override public CharSequence getText(int id) {
                return id == SEARCH_LABEL ? "Zoeken" : super.getText(id);
            }
            @Override public String getResourceEntryName(int id) {
                if (id == GROUP) return "map_buttons_view_container";
                if (id == SETTINGS) return "map_buttons_view_settings_button";
                if (id == SEARCH) return "main_suggested_destinations_search_box";
                if (id == EDITOR) return "destination_input_keyboard_search_edit_text";
                if (id == INPUT_SEARCH) return "destination_input_search_box";
                if(id==NAV_CARD)return "nav_card_view";
                if(id==STATUS_CARD)return "status_card_view";
                if(id==END)return "status_panel_close_button";
                if(id==ZOOM_IN)return "map_buttons_view_zoom_in";
                if(id==ZOOM_OUT)return "map_buttons_view_zoom_out";
                if(id==ZOOM_WIDGET)return "zoom_widget_view";
                if(id==MENU)return "menu_frame_view";
                if(id==PREVIEW)return "place_details_card_scrollable";
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
    public static final class CardView extends FrameLayout {
        public CardView(Context context) { super(context); }
    }
    private FrameLayout compactSearch(CardView card, String label, int[] count) {
        FrameLayout owner=new FrameLayout(context);
        owner.setOnClickListener(v->{assertTrue("The native handler keeps its visible owner",v.isShown());count[0]++;});
        RelativeLayout content=new RelativeLayout(context);
        ImageView image=new ImageView(context);image.setImageDrawable(new ColorDrawable(Color.BLUE));
        FrameLayout icon=new FrameLayout(context);icon.addView(image,new FrameLayout.LayoutParams(48,48));
        TextView text=new TextView(context);text.setText(label);
        content.addView(icon,new RelativeLayout.LayoutParams(64,64));content.addView(text,new RelativeLayout.LayoutParams(180,48));
        owner.addView(content,new FrameLayout.LayoutParams(300,64));card.addView(owner,new FrameLayout.LayoutParams(300,64));
        return owner;
    }
    @Test public void idlessCompactSearchExportsTheNativeLocalizedHandlerAndHidesItsCard() {
        int[] count={0};CardView card=new CardView(context);FrameLayout owner=compactSearch(card,"Zoeken",count);
        ImageView account=new ImageView(context);account.setOnClickListener(v->count[0]+=100);owner.addView(account,new FrameLayout.LayoutParams(48,48));
        root.addView(card,new FrameLayout.LayoutParams(300,64));measure();controls=new NativeControls(root,()->updates++);
        NativeControls.Control search=controls.snapshot().stream().filter(c->c.role==NativeControls.Role.SEARCH).findFirst().get();
        assertSame(owner,search.source);assertEquals(View.INVISIBLE,card.getVisibility());controls.invoke(search);
        assertEquals(1,count[0]);assertEquals(View.INVISIBLE,card.getVisibility());
        controls.close();assertEquals(View.VISIBLE,card.getVisibility());
    }
    @Test public void compactSearchUsesTheRenderedCopyAndRetiresDetachedCallbacks() {
        int[] current={0},unused={0};CardView card=new CardView(context),offscreen=new CardView(context);
        FrameLayout owner=compactSearch(card,"Zoeken",current);compactSearch(offscreen,"Zoeken",unused);
        root.addView(card,new FrameLayout.LayoutParams(300,64));root.addView(offscreen,new FrameLayout.LayoutParams(0,0));measure();
        controls=new NativeControls(root,()->updates++);NativeControls.Control search=controls.snapshot().stream().filter(c->c.role==NativeControls.Role.SEARCH).findFirst().get();
        assertSame(owner,search.source);assertEquals(View.INVISIBLE,offscreen.getVisibility());controls.invoke(search);
        assertEquals(1,current[0]);assertEquals(0,unused[0]);root.removeView(card);measure();
        assertThrows(SecurityException.class,()->controls.invoke(search));
    }
    @Test public void unrelatedIdlessCardsAndLooseSearchLabelsDoNotBecomeSearchActions() {
        CardView card=new CardView(context);compactSearch(card,"Other native action",new int[]{0});root.addView(card,new FrameLayout.LayoutParams(300,64));
        FrameLayout plain=new FrameLayout(context);CardView detached=new CardView(context);FrameLayout loose=compactSearch(detached,"Zoeken",new int[]{0});
        detached.removeView(loose);plain.addView(loose);root.addView(plain,new FrameLayout.LayoutParams(300,64));measure();
        controls=new NativeControls(root,()->updates++);assertFalse(controls.snapshot().stream().anyMatch(c->c.role==NativeControls.Role.SEARCH));
        assertEquals(View.VISIBLE,card.getVisibility());assertEquals(View.VISIBLE,plain.getVisibility());
    }
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
    @Test public void logicalRolesUseTheLiveHandlerWhileRetiredAndUnavailableViewsStayRevoked() {
        NativeControls.Control old=bind();group.removeView(button);button=button();
        group.addView(button,new FrameLayout.LayoutParams(64,64));measure();
        assertThrows(SecurityException.class,()->controls.invoke(old));
        controls.invoke(NativeControls.Role.SETTINGS);assertEquals(1,clicks);
        button.setEnabled(false);root.getViewTreeObserver().dispatchOnPreDraw();
        assertThrows(SecurityException.class,()->controls.invoke(NativeControls.Role.SETTINGS));
        assertEquals(1,clicks);group.removeView(button);measure();
        assertThrows(SecurityException.class,()->controls.invoke(NativeControls.Role.SETTINGS));
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
    @Test public void namedSearchContainerKeepsItsRippleActionInsteadOfAnInteractiveChild() {
        int[] searchClicks = {0}, childClicks = {0};
        FrameLayout search = new FrameLayout(context); search.setId(SEARCH); search.setClickable(true);
        search.setOnTouchListener((v, event) -> { if (event.getAction() == MotionEvent.ACTION_UP) searchClicks[0]++; return true; });
        ImageView child = new ImageView(context); child.setImageDrawable(new ColorDrawable(Color.BLUE));
        child.setOnClickListener(v -> childClicks[0]++); search.addView(child, new FrameLayout.LayoutParams(48, 48));
        root.addView(search, new FrameLayout.LayoutParams(300, 64)); measure();
        controls = new NativeControls(root, () -> updates++);
        NativeControls.Control action = controls.snapshot().stream().filter(c -> c.role == NativeControls.Role.SEARCH).findFirst().get();
        assertSame(search, action.source); controls.invoke(action);
        assertEquals(1, searchClicks[0]); assertEquals(0, childClicks[0]);
    }
    @Test public void destinationInputSearchUsesTheNativeContainerAndIgnoresInvisibleChildren() {
        int[] searchClicks = {0}, childClicks = {0};
        FrameLayout search = new FrameLayout(context); search.setId(INPUT_SEARCH); search.setClickable(true);
        search.setOnTouchListener((v, event) -> { if (event.getAction() == MotionEvent.ACTION_UP) searchClicks[0]++; return true; });
        ImageView child = new ImageView(context); child.setImageDrawable(new ColorDrawable(Color.BLUE));
        child.setOnClickListener(v -> childClicks[0]++); child.setVisibility(View.INVISIBLE);
        search.addView(child, new FrameLayout.LayoutParams(48, 48)); root.addView(search, new FrameLayout.LayoutParams(300, 64)); measure();
        controls = new NativeControls(root, () -> updates++);
        NativeControls.Control action = controls.snapshot().stream().filter(c -> c.role == NativeControls.Role.SEARCH).findFirst().get();
        assertSame(search, action.source); controls.invoke(action);
        assertEquals(1, searchClicks[0]); assertEquals(0, childClicks[0]);
    }
    @Test public void nativeLayoutHidingDoesNotRemoveTheActionFromTheStandardHostInterface() {
        group.setVisibility(View.GONE); NativeControls.Control control = bind(); controls.invoke(control);
        assertEquals(1, clicks); assertEquals(View.GONE, group.getVisibility());
    }
    @Test public void frontmostDestinationSearchWinsOverTheOlderMapSearch() {
        int[] oldClicks={0}, currentClicks={0};
        ImageView old=new ImageView(context);old.setId(SEARCH);old.setImageDrawable(new ColorDrawable(Color.BLUE));old.setOnClickListener(v->oldClicks[0]++);
        ImageView current=new ImageView(context);current.setId(INPUT_SEARCH);current.setImageDrawable(new ColorDrawable(Color.RED));current.setOnClickListener(v->currentClicks[0]++);
        root.addView(old,new FrameLayout.LayoutParams(300,64));root.addView(current,new FrameLayout.LayoutParams(300,64));measure();
        controls=new NativeControls(root,()->updates++);
        NativeControls.Control action=controls.snapshot().stream().filter(c->c.role==NativeControls.Role.SEARCH).findFirst().get();
        assertSame(current,action.source);controls.invoke(action);assertEquals(0,oldClicks[0]);assertEquals(1,currentClicks[0]);
    }
    @Test public void editorInsideAnExportedSearchContainerReceivesItsOriginalVisibleClick() {
        int[] focused={0};FrameLayout search=new FrameLayout(context);search.setId(INPUT_SEARCH);
        android.widget.EditText editor=new android.widget.EditText(context);editor.setId(EDITOR);editor.setVisibility(View.GONE);
        editor.setOnClickListener(v->{assertTrue("Only adapter-owned hiding may be restored for native focus",v.isShown());assertTrue(v.requestFocus());focused[0]++;});
        ImageView icon=new ImageView(context);icon.setImageDrawable(new ColorDrawable(Color.BLUE));search.addView(icon,new FrameLayout.LayoutParams(48,48));search.addView(editor,new FrameLayout.LayoutParams(200,48));
        search.setOnClickListener(v->{editor.setVisibility(View.VISIBLE);editor.requestFocus();});root.addView(search,new FrameLayout.LayoutParams(300,64));measure();
        controls=new NativeControls(root,()->updates++);NativeControls.Control action=controls.snapshot().stream().filter(c->c.role==NativeControls.Role.SEARCH).findFirst().get();
        controls.invoke(action);assertEquals(View.VISIBLE,search.getVisibility());assertTrue(editor.isFocused());measure();root.getViewTreeObserver().dispatchOnPreDraw();
        assertEquals(1,focused[0]);assertTrue(editor.isFocused());assertEquals(View.VISIBLE,search.getVisibility());
        editor.setVisibility(View.GONE);measure();assertEquals(View.INVISIBLE,search.getVisibility());
    }
    @Test public void nativeHiddenAncestorPreventsEditorFocus() {
        int[] focused={0};FrameLayout nativeHidden=new FrameLayout(context);nativeHidden.setVisibility(View.GONE);
        View editor=new View(context);editor.setId(EDITOR);editor.setOnClickListener(v->focused[0]++);nativeHidden.addView(editor,new FrameLayout.LayoutParams(200,48));root.addView(nativeHidden);
        ImageView search=new ImageView(context);search.setId(SEARCH);search.setImageDrawable(new ColorDrawable(Color.BLUE));search.setOnClickListener(v->{});root.addView(search,new FrameLayout.LayoutParams(300,64));measure();
        controls=new NativeControls(root,()->updates++);NativeControls.Control action=controls.snapshot().stream().filter(c->c.role==NativeControls.Role.SEARCH).findFirst().get();
        controls.invoke(action);measure();root.getViewTreeObserver().dispatchOnPreDraw();assertEquals(0,focused[0]);assertEquals(View.GONE,nativeHidden.getVisibility());
    }
    @Test public void closeRestoresOnlyTheVisibilityOwnedByTheAdapter() {
        bind(); controls.close(); assertEquals(View.VISIBLE, group.getVisibility());
        controls = null; bind(); group.setVisibility(View.GONE); controls.close();
        assertEquals(View.GONE, group.getVisibility());
    }
    @Test public void hostOwnsGuidanceCardsWhileTheOriginalStopActionRemainsUsable(){
        FrameLayout nav=new FrameLayout(context),status=new FrameLayout(context);
        nav.setId(NAV_CARD);status.setId(STATUS_CARD);
        ImageView end=button();end.setId(END);status.addView(end,new FrameLayout.LayoutParams(64,64));
        root.addView(nav,new FrameLayout.LayoutParams(400,180));root.addView(status,new FrameLayout.LayoutParams(400,100));measure();
        controls=new NativeControls(root,()->updates++);controls.hostedNavigation(true);
        assertEquals(View.INVISIBLE,nav.getVisibility());assertEquals(View.INVISIBLE,status.getVisibility());assertEquals(View.VISIBLE,root.getVisibility());
        NativeControls.Control stop=controls.snapshot().stream().filter(c->c.role==NativeControls.Role.END).findFirst().get();
        controls.invoke(stop);assertEquals(1,clicks);assertEquals(View.INVISIBLE,status.getVisibility());
        controls.hostedNavigation(false);assertEquals(View.VISIBLE,nav.getVisibility());assertEquals(View.VISIBLE,status.getVisibility());
        controls.hostedNavigation(true);nav.setVisibility(View.GONE);controls.close();
        assertEquals("A native retirement is preserved",View.GONE,nav.getVisibility());assertEquals(View.VISIBLE,status.getVisibility());
    }
    @Test public void theSeparateNativeZoomWidgetYieldsOnlyToACompleteUsableHostPair(){
        ImageView plus=button(),minus=button();plus.setId(ZOOM_IN);minus.setId(ZOOM_OUT);
        group.addView(plus,new FrameLayout.LayoutParams(64,64));group.addView(minus,new FrameLayout.LayoutParams(64,64));
        FrameLayout widget=new FrameLayout(context);widget.setId(ZOOM_WIDGET);root.addView(widget,new FrameLayout.LayoutParams(64,128));measure();
        controls=new NativeControls(root,()->updates++);
        assertEquals("The independent native widget must not duplicate the host pair",View.INVISIBLE,widget.getVisibility());
        NativeControls.Control zoom=controls.snapshot().stream().filter(c->c.role==NativeControls.Role.ZOOM_IN).findFirst().get();
        controls.invoke(zoom);assertEquals(1,clicks);assertEquals(View.INVISIBLE,widget.getVisibility());
        minus.setEnabled(false);root.getViewTreeObserver().dispatchOnPreDraw();
        assertEquals("Keep native zoom when a complete host pair is unavailable",View.VISIBLE,widget.getVisibility());
        minus.setEnabled(true);root.getViewTreeObserver().dispatchOnPreDraw();assertEquals(View.INVISIBLE,widget.getVisibility());
        controls.close();assertEquals(View.VISIBLE,widget.getVisibility());
    }
    @Test public void nativeZoomRetirementIsNotUndoneByAdapterTeardown(){
        ImageView plus=button(),minus=button();plus.setId(ZOOM_IN);minus.setId(ZOOM_OUT);
        group.addView(plus,new FrameLayout.LayoutParams(64,64));group.addView(minus,new FrameLayout.LayoutParams(64,64));
        FrameLayout widget=new FrameLayout(context);widget.setId(ZOOM_WIDGET);root.addView(widget,new FrameLayout.LayoutParams(64,128));measure();
        controls=new NativeControls(root,()->updates++);assertEquals(View.INVISIBLE,widget.getVisibility());
        widget.setVisibility(View.GONE);controls.close();assertEquals(View.GONE,widget.getVisibility());
    }
    @Test public void aHostedPreviewAlsoSuppressesItsNativeCardBackgroundAndRestoresOnlyOwnedState()throws Exception{
        FrameLayout menu=new FrameLayout(context),card=new FrameLayout(context),preview=new FrameLayout(context);menu.setId(MENU);preview.setId(PREVIEW);card.setBackgroundColor(Color.WHITE);
        ImageView start=button();start.setId(View.NO_ID);preview.addView(start,new FrameLayout.LayoutParams(64,64));card.addView(preview,new FrameLayout.LayoutParams(400,240));menu.addView(card,new FrameLayout.LayoutParams(400,240));root.addView(menu,new FrameLayout.LayoutParams(400,240));measure();
        controls=new NativeControls(root,()->updates++);controls.hostedPanel(preview);
        assertEquals("The separate native card wrapper must not remain on the map",View.INVISIBLE,menu.getVisibility());assertEquals(View.VISIBLE,preview.getVisibility());assertEquals(View.VISIBLE,controls.nativeVisibility(menu));assertEquals(View.VISIBLE,root.getVisibility());
        controls.invoke(new NativeCarContent.Action(start,"Start","view",-1,true));assertEquals(1,clicks);assertEquals(View.INVISIBLE,menu.getVisibility());
        controls.hostedPanel(null);assertEquals(View.VISIBLE,menu.getVisibility());
        controls.hostedPanel(preview);menu.setVisibility(View.GONE);controls.hostedPanel(null);assertEquals(View.GONE,menu.getVisibility());
    }
    @Test public void anUnmeasuredResponsiveCopyCannotReplaceTheRenderedHandler(){
        FrameLayout unused=new FrameLayout(context);unused.setId(GROUP);ImageView duplicate=button();duplicate.setOnClickListener(v->clicks+=100);
        unused.addView(duplicate,new FrameLayout.LayoutParams(64,64));root.addView(unused,new FrameLayout.LayoutParams(0,0));measure();
        NativeControls.Control current=bind();assertSame(button,current.source);controls.invoke(current);assertEquals(1,clicks);
        assertEquals(View.INVISIBLE,group.getVisibility());assertEquals(View.INVISIBLE,unused.getVisibility());
    }
    @Test public void everyRenderedNativeCopyYieldsToTheHostAndRetiredHandlersAreRevoked(){
        FrameLayout front=new FrameLayout(context);front.setId(GROUP);ImageView current=button();current.setOnClickListener(v->clicks+=10);front.addView(current,new FrameLayout.LayoutParams(64,64));root.addView(front,new FrameLayout.LayoutParams(64,200));measure();
        NativeControls.Control selected=bind();assertSame(current,selected.source);assertEquals(View.INVISIBLE,front.getVisibility());assertEquals(View.INVISIBLE,group.getVisibility());
        controls.invoke(selected);assertEquals(10,clicks);
        front.setVisibility(View.GONE);root.getViewTreeObserver().dispatchOnPreDraw();
        assertThrows(SecurityException.class,()->controls.invoke(selected));NativeControls.Control replacement=controls.snapshot().get(0);assertSame(button,replacement.source);controls.invoke(replacement);assertEquals(11,clicks);
        controls.close();assertEquals(View.GONE,front.getVisibility());assertEquals(View.VISIBLE,group.getVisibility());
    }
    @Test public void nativeAutoHideDoesNotRevokeAnOtherwiseEligibleHostAction(){
        NativeControls.Control selected=bind();group.setAlpha(0);root.getViewTreeObserver().dispatchOnPreDraw();
        assertEquals(1,controls.snapshot().size());controls.invoke(selected);assertEquals(1,clicks);assertEquals(0,group.getAlpha(),0);
    }
}
