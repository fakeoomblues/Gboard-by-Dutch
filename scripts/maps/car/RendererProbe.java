package nl.johninnl.mapsprojectionprobe;

import android.app.Instrumentation;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.text.TextUtils;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Executes the renderer class from the patched Maps DEX, with no copied renderer implementation. */
public final class RendererProbe extends Instrumentation {
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final Object frameLock = new Object();
    private Bitmap frame;
    private int frames;
    private Object renderer;
    private ImageReader reader;
    private ImageReader replacementReader;
    private volatile boolean closing;
    private final AtomicReference<IBinder> input = new AtomicReference<>();
    private final AtomicInteger hides = new AtomicInteger();
    private HandlerThread imageThread;
    private Bundle arguments;
    private final StringBuilder trace = new StringBuilder();
    public static final class HarnessService extends Service {
        void initialize(Context context) { attachBaseContext(context); }
        @Override public android.os.IBinder onBind(Intent intent) { return null; }
    }
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); this.arguments = arguments; start(); }
    private interface Work { void run() throws Exception; }
    private void main(Work work) {
        runOnMainSync(() -> { try { work.run(); } catch (Throwable error) { failure.compareAndSet(null, error); } });
    }
    private Object field(Object object, String name) throws Exception {
        Field value = object.getClass().getDeclaredField(name); value.setAccessible(true); return value.get(object);
    }
    private void invoke(String method, Class<?>[] parameters, Object... values) throws Exception {
        renderer.getClass().getMethod(method, parameters).invoke(renderer, values);
    }
    private void openOverflow() throws Exception {
        Object layout = field(renderer, "controlLayout");
        View more = (View) field(layout, "more");
        android.graphics.Rect at = new android.graphics.Rect();
        if (more == null || !more.isShown() || !more.getGlobalVisibleRect(at) || at.width() <= 0 || at.height() <= 0)
            throw new AssertionError("Expanded fixture has no reachable More target");
        invoke("click", new Class<?>[]{float.class, float.class}, (float) at.centerX(), (float) at.centerY());
    }
    private Object openMenu() throws Exception {
        Object layout = field(renderer, "controlLayout");
        Object menu = field(layout, "menu");
        if (menu == null || field(menu, "overlay") == null) throw new AssertionError("More did not open its action panel");
        return menu;
    }
    private void overflowState(String phase, Object menu) throws Exception {
        Object layout = field(renderer, "controlLayout");
        View owner = (View) field(layout, "controls");
        trace.append("overflow ").append(phase).append(" epoch=").append(field(layout, "actionEpoch"))
                .append(" ownerShown=").append(owner != null && owner.isShown())
                .append(" overlay=").append(menu != null && field(menu, "overlay") != null).append('\n');
        if (menu != null) for (Object action : (java.util.List<?>) field(menu, "actions")) {
            View source = (View) field(action, "source");
            java.util.function.BooleanSupplier current = (java.util.function.BooleanSupplier) field(action, "current");
            trace.append("overflow action ").append(field(action, "label"))
                    .append(" current=").append(current.getAsBoolean()).append(" enabled=").append(source.isEnabled())
                    .append(" listener=").append(source.hasOnClickListeners()).append('\n');
        }
    }
    private boolean settings(View view) {
        for (View node = view; node != null; node = node.getParent() instanceof View ? (View) node.getParent() : null) {
            try {
                String name = node.getResources().getResourceEntryName(node.getId());
                if (name.equals("map_buttons_view_settings_button") || name.equals("one_bar_settings_button_view")) return true;
            } catch (android.content.res.Resources.NotFoundException ignored) {}
        }
        return false;
    }
    private void overflowCycle() throws Exception {
        main(this::openOverflow); Thread.sleep(500); capture("overflow-open");
        main(() -> { openMenu(); invoke("back", new Class<?>[0]); });
        main(() -> {
            Object layout = field(renderer, "controlLayout");
            if (field(field(layout, "menu"), "overlay") != null) throw new AssertionError("Back retained the overflow panel");
            openOverflow();
        });
        Thread.sleep(500);
        Object[] activatedMenu = {null};
        main(() -> {
            Object menu = openMenu();
            overflowState("before-row", menu);
            java.util.List<?> actions = (java.util.List<?>) field(menu, "actions");
            java.util.List<?> buttons = (java.util.List<?>) field(menu, "buttons");
            for (int i = 0; i < actions.size(); i++) {
                if (!settings((View) field(actions.get(i), "source"))) continue;
                View row = (View) buttons.get(i); android.graphics.Rect at = new android.graphics.Rect();
                if (!row.isEnabled() || !row.getGlobalVisibleRect(at)) throw new AssertionError("Settings overflow row is unreachable");
                invoke("click", new Class<?>[]{float.class, float.class}, (float) at.centerX(), (float) at.centerY());
                activatedMenu[0] = menu;
                return;
            }
            throw new AssertionError("Current native Settings action is missing from overflow");
        });
        // View posts its click callback after ACTION_UP. Observe completion on a
        // later loop turn rather than asserting inside the touch dispatch call.
        Thread.sleep(1500);
        main(() -> {
            overflowState("after-row", activatedMenu[0]);
            if (activatedMenu[0] == null || field(activatedMenu[0], "overlay") != null)
                throw new AssertionError("Native action retained the overflow panel");
            trace.append("PASS More open/Back/reopen and delegated native Settings tap; inspect action frame\n");
        });
        capture("overflow-settings");
    }
    private View decor() throws Exception {
        Object controller = field(renderer, "controller");
        if (controller == null) throw new AssertionError("Native controller was not connected");
        Object presentation = field(controller, "k");
        Window window = (Window) presentation.getClass().getMethod("f").invoke(presentation);
        Object activity = field(controller, "J");
        trace.append("originalActivity=").append(activity.getClass().getName()).append('\n');
        return window.getDecorView();
    }
    private void text(View view, StringBuilder out) {
        if (view instanceof TextView && ((TextView) view).getText().length() != 0)
            out.append(((TextView) view).getText()).append(" | ");
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) text(((ViewGroup) view).getChildAt(i), out);
    }
    private void bounds(View view, StringBuilder out) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view.getId() != View.NO_ID) {
            String name;
            try { name = view.getResources().getResourceEntryName(view.getId()); }
            catch (android.content.res.Resources.NotFoundException unknown) { name = Integer.toHexString(view.getId()); }
            if (arguments.getString("inventory", "false").equals("true") || name.contains("map_buttons") || name.contains("one_bar") || name.contains("recenter")) {
                android.graphics.Rect rect = new android.graphics.Rect();
                boolean visible = view.getGlobalVisibleRect(rect);
                out.append(name).append(" bounds=").append(rect).append(" visible=").append(visible)
                        .append(" clickable=").append(view.isClickable()).append(" class=").append(view.getClass().getName())
                        .append(" parent=").append(view.getParent() == null ? "null" : view.getParent().getClass().getName())
                        .append(" padding=").append(view.getPaddingLeft()).append(',').append(view.getPaddingTop()).append(',')
                        .append(view.getPaddingRight()).append(',').append(view.getPaddingBottom())
                        .append(" scroll=").append(view.getScrollX()).append(',').append(view.getScrollY())
                        .append(" clip=").append(view.getClipBounds()).append(" outline=").append(view.getClipToOutline()).append('\n');
                if (arguments.getString("inventory", "false").equals("true") && name.equals("map_buttons_view_container")) {
                    for (android.view.ViewParent parent = view.getParent(); parent instanceof View; parent = parent.getParent()) {
                        View ancestor = (View) parent; android.graphics.Rect a = new android.graphics.Rect();
                        ancestor.getGlobalVisibleRect(a);
                        out.append("ancestor ").append(ancestor.getClass().getName()).append(" bounds=").append(a)
                                .append(" params=").append(ancestor.getLayoutParams() == null ? "null" : ancestor.getLayoutParams().getClass().getName())
                                .append(" layer=").append(ancestor.getLayerType()).append(" clipBounds=").append(ancestor.getClipBounds());
                        if (ancestor instanceof ViewGroup) out.append(" clipChildren=").append(((ViewGroup) ancestor).getClipChildren());
                        try {
                            Object layout = field(renderer, "controlLayout");
                            out.append(" recorded=").append(layout != null && ((java.util.Map<?, ?>) field(layout, "clips")).containsKey(ancestor));
                        } catch (Exception unavailable) { out.append(" recorded=unknown"); }
                        out.append('\n');
                    }
                }
                if (arguments.getString("inventory", "false").equals("true") && name.equals("map_buttons_view_zoom_in")) {
                    View node = view;
                    for (int depth = 0; depth < 9 && node != null; depth++) {
                        out.append("zoom node ").append(node.getClass().getName()).append(" size=").append(node.getWidth()).append('x').append(node.getHeight())
                                .append(" layer=").append(node.getLayerType()).append(" clip=").append(node.getClipBounds())
                                .append(" scale=").append(node.getScaleX()).append(',').append(node.getScaleY()).append(" alpha=").append(node.getAlpha())
                                .append(" padding=").append(node.getPaddingLeft()).append(',').append(node.getPaddingTop()).append(',')
                                .append(node.getPaddingRight()).append(',').append(node.getPaddingBottom())
                                .append(" scroll=").append(node.getScrollX()).append(',').append(node.getScrollY())
                                .append(" outline=").append(node.getClipToOutline());
                        if (node.getBackground() != null) out.append(" bg=").append(node.getBackground().getClass().getName()).append(':').append(node.getBackground().getBounds());
                        if (node.getOutlineProvider() != null) {
                            android.graphics.Outline outline = new android.graphics.Outline();
                            node.getOutlineProvider().getOutline(node, outline);
                            android.graphics.Rect outlineRect = new android.graphics.Rect();
                            out.append(" outlineRect=").append(outline.getRect(outlineRect) ? outlineRect : "path");
                        }
                        out.append('\n');
                        node = node.getParent() instanceof View ? (View) node.getParent() : null;
                    }
                }
            }
        }
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
            bounds(((ViewGroup) view).getChildAt(i), out);
    }
    private void outlines(View view) {
        if (view == null) return;
        view.invalidateOutline(); view.invalidate();
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) outlines(((ViewGroup) view).getChildAt(i));
    }
    private int[] zoomPaint() throws Exception {
        AtomicReference<android.graphics.Rect> target = new AtomicReference<>();
        main(() -> {
            View root = decor();
            View zoom = root.findViewById(root.getResources().getIdentifier("map_buttons_view_zoom_in", "id", getTargetContext().getPackageName()));
            android.graphics.Rect bounds = new android.graphics.Rect();
            if (zoom == null || !zoom.getGlobalVisibleRect(bounds)) throw new AssertionError("Native zoom target disappeared");
            target.set(bounds);
        });
        if (failure.get() != null) throw new AssertionError("Cannot sample native zoom paint", failure.get());
        android.graphics.Rect bounds = target.get();
        synchronized (frameLock) {
            if (frame == null) throw new AssertionError("Missing native frame");
            return new int[]{frame.getPixel(bounds.centerX(), bounds.centerY()),
                    frame.getPixel(bounds.left + bounds.width() * 3 / 4, bounds.centerY()),
                    frame.getPixel(bounds.left + bounds.width() * 7 / 8, bounds.centerY())};
        }
    }
    private void sameZoomPaint(int[] before, int[] after) {
        for (int i = 0; i < before.length; i++) for (int shift : new int[]{0, 8, 16})
            if (Math.abs(((before[i] >> shift) & 255) - ((after[i] >> shift) & 255)) > 20)
                throw new AssertionError("Native zoom paint did not survive search at sample " + i);
        trace.append("PASS native zoom glyph/background paint samples after search\n");
    }
    private void capture(String phase) throws Exception {
        File output = new File(getTargetContext().getFilesDir(), "native-renderer-" + phase + ".png");
        synchronized (frameLock) {
            if (frame == null) throw new AssertionError("Original Maps renderer produced no frames");
            try (FileOutputStream file = new FileOutputStream(output)) { frame.compress(Bitmap.CompressFormat.PNG, 100, file); }
            trace.append("frames=").append(frames).append(" screenshot=").append(output).append('\n');
        }
        main(() -> text(decor(), trace));
        main(() -> bounds(decor(), trace));
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Class<?> type = getTargetContext().getClassLoader().loadClass("patches.maps.gearslip.ProjectionRenderer");
            String action = arguments.getString("action", "map");
            int width = Integer.parseInt(arguments.getString("width", "1280"));
            int height = Integer.parseInt(arguments.getString("height", "720"));
            int dpi = Integer.parseInt(arguments.getString("dpi", "160"));
            main(() -> {
                HarnessService service = new HarnessService(); service.initialize(getTargetContext());
                renderer = type.getConstructor(Service.class, Consumer.class).newInstance(service, (Consumer<Throwable>) failure::set);
                Class<?> keyboard = getTargetContext().getClassLoader().loadClass("patches.maps.gearslip.ProjectionRenderer$KeyboardListener");
                Object listener = Proxy.newProxyInstance(getTargetContext().getClassLoader(), new Class<?>[]{keyboard}, (proxy, method, values) -> {
                    if (method.getName().equals("show")) input.set((IBinder) values[0]);
                    else if (method.getName().equals("hide")) hides.incrementAndGet();
                    return null;
                });
                type.getMethod("setKeyboardListener", keyboard).invoke(renderer, listener);
                imageThread = new HandlerThread("native-map-probe-images"); imageThread.start();
                reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
                reader.setOnImageAvailableListener(source -> {
                    if (closing) return;
                    try (Image image = source.acquireLatestImage()) {
                        if (image == null) return;
                        Image.Plane plane = image.getPlanes()[0];
                        Bitmap padded = Bitmap.createBitmap(plane.getRowStride() / plane.getPixelStride(), image.getHeight(), Bitmap.Config.ARGB_8888);
                        padded.copyPixelsFromBuffer(plane.getBuffer());
                        Bitmap next = Bitmap.createBitmap(padded, 0, 0, image.getWidth(), image.getHeight());
                        if (next != padded) padded.recycle();
                        synchronized (frameLock) { if (frame != null) frame.recycle(); frame = next; frames++; }
                    } catch (Throwable error) { if (!closing) failure.compareAndSet(null, error); }
                }, new Handler(imageThread.getLooper()));
                invoke("create", new Class<?>[]{Intent.class, Configuration.class}, new Intent(Intent.ACTION_MAIN), getTargetContext().getResources().getConfiguration());
                invoke("resume", new Class<?>[0]);
                invoke("surfaceAvailable", new Class<?>[]{Surface.class, int.class, int.class, int.class}, reader.getSurface(), width, height, dpi);
            });
            for (int i = 0; i < 12 && failure.get() == null; i++) {
                Thread.sleep(500);
                // Keep instrumentation observable while the renderer initializes.
                Bundle status = new Bundle(); status.putString("phase", "waiting-frame"); sendStatus(0, status);
            }
            if (failure.get() != null) throw new AssertionError("Native renderer failed", failure.get());
            if ("true".equals(arguments.getString("reveal"))) {
                main(() -> invoke("click", new Class<?>[]{float.class, float.class}, width / 2f, height * 0.6f));
                Thread.sleep(500);
            }
            capture("map");
            if ("areas".equals(action)) {
                Object activity = field(field(renderer, "controller"), "J");
                String[] coordinates = arguments.getString("area", "0,0,920,396").split(",");
                if (coordinates.length != 4) throw new IllegalArgumentException("area requires left,top,right,bottom");
                android.graphics.Rect area = new android.graphics.Rect(Integer.parseInt(coordinates[0]),
                        Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]), Integer.parseInt(coordinates[3]));
                main(() -> invoke("visibleArea", new Class<?>[]{android.graphics.Rect.class}, area));
                String grid = arguments.getString("player");
                if (grid != null) {
                    String[] p = grid.split(",");
                    android.graphics.Rect player = new android.graphics.Rect(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                            Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                    String[] b = arguments.getString("bar", "0,396,1280,480").split(",");
                    java.util.List<android.graphics.Rect> covers = java.util.Collections.singletonList(new android.graphics.Rect(
                            Integer.parseInt(b[0]), Integer.parseInt(b[1]), Integer.parseInt(b[2]), Integer.parseInt(b[3])));
                    main(() -> {
                        View root = decor();
                        int nativeId = root.getResources().getIdentifier("map_buttons_view_container", "id", getTargetContext().getPackageName());
                        ViewGroup nativeGroup = root.findViewById(nativeId);
                        if (nativeGroup == null) throw new AssertionError("This native layout has no recognized control group");
                        trace.append("grid candidate class=").append(nativeGroup.getClass().getName())
                                .append(" children=").append(nativeGroup.getChildCount())
                                .append(" params=").append(nativeGroup.getLayoutParams().getClass().getName())
                                .append(" width=").append(nativeGroup.getWidth()).append('\n');
                        java.lang.reflect.Method minimum = getTargetContext().getClassLoader().loadClass("patches.maps.gearslip.NativeControlLayout")
                                .getDeclaredMethod("minimumHeight", ViewGroup.class); minimum.setAccessible(true);
                        for (int i = 0; i < nativeGroup.getChildCount(); i++) {
                            View child = nativeGroup.getChildAt(i);
                            trace.append("grid child ").append(i).append(" class=").append(child.getClass().getName())
                                    .append(" params=").append(child.getLayoutParams().getClass().getName())
                                    .append(" minimum=").append(child instanceof ViewGroup ? minimum.invoke(null, child) : -1).append('\n');
                        }
                        Object accepted = renderer.getClass().getMethod("applyOcclusions", android.graphics.Rect.class, java.util.List.class)
                                .invoke(renderer, player, covers);
                        trace.append("native grid accepted=").append(accepted).append('\n');
                        if (!Boolean.TRUE.equals(accepted)) throw new AssertionError("Native grid did not accept the measured compact fixture");
                    });
                    if (failure.get() != null) throw new AssertionError("Native grid fixture failed before interaction", failure.get());
                }
                Thread.sleep(1500); capture("areas");
                if ("true".equals(arguments.getString("overflow_cycle"))) overflowCycle();
                if (field(field(renderer, "controller"), "J") != activity)
                    throw new AssertionError("Area update recreated the native activity");
                if ("true".equals(arguments.getString("search_cycle"))) {
                    int[] paintBefore = "true".equals(arguments.getString("paint_check")) ? zoomPaint() : null;
                    main(() -> {
                        View root = decor();
                        int searchId = root.getResources().getIdentifier("main_suggested_destinations_search_box", "id", getTargetContext().getPackageName());
                        View search = root.findViewById(searchId);
                        android.graphics.Rect target = new android.graphics.Rect();
                        if (search == null || !search.getGlobalVisibleRect(target) || !search.isClickable())
                            throw new AssertionError("Native search entry is not reachable");
                        invoke("click", new Class<?>[]{float.class, float.class}, (float) target.centerX(), (float) target.centerY());
                    });
                    Thread.sleep(1500); capture("grid-search");
                    main(() -> invoke("back", new Class<?>[0]));
                    Thread.sleep(1500); capture("grid-returned");
                    if (paintBefore != null) sameZoomPaint(paintBefore, zoomPaint());
                    if ("true".equals(arguments.getString("redraw_check"))) {
                        Thread.sleep(2000); capture("grid-returned-late");
                        main(() -> decor().invalidate());
                        Thread.sleep(500); capture("grid-returned-redrawn");
                        main(() -> {
                            View root = decor();
                            int id = root.getResources().getIdentifier("map_buttons_view_container", "id", getTargetContext().getPackageName());
                            outlines(root.findViewById(id));
                        });
                        Thread.sleep(500); capture("grid-returned-outlines");
                        if (grid != null) {
                            String[] p = grid.split(",");
                            android.graphics.Rect player = new android.graphics.Rect(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                            main(() -> renderer.getClass().getMethod("applyOcclusions", android.graphics.Rect.class, java.util.List.class)
                                    .invoke(renderer, player, java.util.Collections.singletonList(new android.graphics.Rect(0, 396, width, height))));
                            Thread.sleep(1000); capture("grid-returned-reapplied");
                        }
                    }
                    if (field(field(renderer, "controller"), "J") != activity)
                        throw new AssertionError("Search cycle recreated the native activity");
                }
                String tap = arguments.getString("tap");
                if (tap != null) {
                    main(() -> {
                        View root = decor();
                        int id = root.getResources().getIdentifier(tap, "id", getTargetContext().getPackageName());
                        View button = id == 0 ? null : root.findViewById(id);
                        android.graphics.Rect target = new android.graphics.Rect();
                        if (button == null || !button.getGlobalVisibleRect(target) || !button.isClickable())
                            throw new AssertionError("Requested native control is not clickable: " + tap);
                        trace.append("tap ").append(tap).append(" at ").append(target.centerX()).append(',').append(target.centerY()).append('\n');
                        invoke("click", new Class<?>[]{float.class, float.class}, (float) target.centerX(), (float) target.centerY());
                    });
                    Thread.sleep(1500); capture("areas-tapped");
                }
                main(() -> {
                    if (grid != null) renderer.getClass().getMethod("applyOcclusions", android.graphics.Rect.class, java.util.List.class)
                            .invoke(renderer, null, java.util.Collections.emptyList());
                    invoke("visibleArea", new Class<?>[]{android.graphics.Rect.class}, new android.graphics.Rect(0, 0, width, height));
                });
                Thread.sleep(1500); capture("restored");
                if (field(field(renderer, "controller"), "J") != activity)
                    throw new AssertionError("Area restoration recreated the native activity");
                trace.append("PASS activity retained across area update/restoration; inspect bounds and frames\n");
            } else if ("search".equals(action)) {
                main(() -> invoke("click", new Class<?>[]{float.class, float.class}, 280f, 56f));
                Thread.sleep(2000); capture("search");
            } else if ("surface".equals(action)) {
                main(() -> invoke("click", new Class<?>[]{float.class, float.class}, 280f, 56f));
                Thread.sleep(1500);
                main(() -> invoke("click", new Class<?>[]{float.class, float.class}, 400f, 56f));
                Thread.sleep(1000);
                if (input.get() == null) throw new AssertionError("Maps did not request its native input connection");
                Object before = field(field(renderer, "controller"), "J");
                int hideCount = hides.get();
                main(() -> {
                    invoke("surfaceDestroyed", new Class<?>[0]);
                    replacementReader = ImageReader.newInstance(1280, 720, PixelFormat.RGBA_8888, 3);
                    replacementReader.setOnImageAvailableListener(source -> {
                        try (Image image = source.acquireLatestImage()) {} catch (IllegalStateException closed) {}
                    }, new Handler(imageThread.getLooper()));
                    invoke("surfaceAvailable", new Class<?>[]{Surface.class, int.class, int.class, int.class}, replacementReader.getSurface(), 1280, 720, 160);
                });
                Thread.sleep(1000);
                if (field(field(renderer, "controller"), "J") != before) throw new AssertionError("Surface replacement recreated the native editor's activity");
                if (hides.get() != hideCount) throw new AssertionError("Surface replacement dismissed the native keyboard");
                main(() -> {
                    Parcel request = Parcel.obtain(), reply = Parcel.obtain();
                    try {
                        request.writeInterfaceToken("com.google.android.gms.car.input.IProxyInputConnection");
                        request.writeInt(4096); request.writeInt(0);
                        if (!input.get().transact(1, request, reply, 0)) throw new AssertionError("Native input connection was lost");
                        reply.readException();
                        CharSequence value = reply.readInt() == 0 ? null : TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(reply);
                        if (value == null) throw new AssertionError("Native editor stopped answering after surface replacement");
                    } finally { request.recycle(); reply.recycle(); }
                });
                trace.append("PASS 4 emitted-DEX native editor/surface replacement checks\n");
            } else if ("route".equals(action) || "start".equals(action)) {
                main(() -> invoke("newIntent", new Class<?>[]{Intent.class}, new Intent(Intent.ACTION_VIEW,
                        Uri.parse("google.navigation:q=52.3676,4.9041&mode=d"))));
                Thread.sleep(8000); capture("route");
                if ("start".equals(action)) {
                    main(() -> invoke("click", new Class<?>[]{float.class, float.class}, 218f, 382f));
                    Thread.sleep(5000); capture("guidance");
                }
            }
            if (failure.get() != null) throw new AssertionError("Native renderer interaction failed", failure.get());
            result.putString("result", "PASS emitted-DEX renderer smoke; inspect screenshots for UI acceptance");
        } catch (Throwable error) {
            result.putString("result", "FAIL " + android.util.Log.getStackTraceString(error));
        } finally {
            main(() -> { closing = true; if (renderer != null) invoke("close", new Class<?>[0]); if (reader != null) reader.close(); if (replacementReader != null) replacementReader.close(); if (imageThread != null) imageThread.quitSafely(); });
            if (imageThread != null) {
                try { imageThread.join(2000); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); failure.compareAndSet(null, interrupted); }
                if (imageThread.isAlive()) failure.compareAndSet(null, new AssertionError("Image thread did not stop"));
            }
            synchronized (frameLock) { if (frame != null) frame.recycle(); frame = null; }
            if (failure.get() != null) result.putString("result", "FAIL " + android.util.Log.getStackTraceString(failure.get()));
            result.putString("trace", trace.toString()); finish(-1, result);
        }
    }
}
