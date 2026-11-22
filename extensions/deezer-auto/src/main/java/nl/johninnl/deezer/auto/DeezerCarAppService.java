package nl.johninnl.deezer.auto;

import android.content.ComponentName;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.media.MediaConstants;
import androidx.car.app.media.MediaPlaybackManager;
import androidx.car.app.media.model.MediaPlaybackTemplate;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.validation.HostValidator;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.List;

/** Optional template client of the existing authenticated service, never a playback service. */
public final class DeezerCarAppService extends CarAppService {
    @Override public void onCreate() {
        PinnedCarApi.assertPresent(getApplicationInfo().sourceDir, CarAppApiLevels.class.getClassLoader());
        super.onCreate();
    }

    @Override public HostValidator createHostValidator() {
        HostValidator.Builder builder = new HostValidator.Builder(this);
        // These arrays are linked into the target by the optional resource patch. Missing
        // resources fail closed, including debug builds; there is no allow-all fallback.
        for (String name : new String[]{"morphe_deezer_auto_hosts_allowlist_sample", "morphe_deezer_auto_extra_hosts"}) {
            int id = getResources().getIdentifier(name, "array", getPackageName());
            if (id != 0) builder.addAllowedHosts(id);
        }
        return builder.build();
    }

    @Override public Session onCreateSession() { return new MediaSessionClient(); }

    private static final class MediaSessionClient extends Session {
        private MediaBrowser browser;
        private MediaController controller;
        private int generation;
        private String error;
        private boolean destroyed;
        private boolean registered;
        private MediaController.Callback callback;

        @Override public Screen onCreateScreen(Intent intent) {
            getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override public void onDestroy(LifecycleOwner owner) {
                    destroyed = true;
                    close();
                }
            });
            connect();
            return MediaConstants.ACTION_SHOW_MEDIA_PLAYBACK.equals(intent.getAction())
                ? new PlayerScreen(getCarContext(), this) : new BrowseScreen(getCarContext(), this, null, "Deezer");
        }

        @Override public void onNewIntent(Intent intent) {
            if (MediaConstants.ACTION_SHOW_MEDIA_PLAYBACK.equals(intent.getAction())) showPlayer();
        }

        private void refresh() {
            if (!destroyed) {
                Screen top = getCarContext().getCarService(ScreenManager.class).getTop();
                if (top != null) top.invalidate();
            }
        }

        private void close() {
            generation++;
            registered = false;
            if (controller != null && callback != null) controller.unregisterCallback(callback);
            callback = null;
            controller = null;
            if (browser != null) browser.disconnect();
            browser = null;
        }

        private void fail(String message) {
            close();
            error = message;
            refresh();
        }

        private void connect() {
            close();
            error = null;
            int expected = generation;
            CarContext context = getCarContext();
            browser = new MediaBrowser(context, new ComponentName(context.getPackageName(),
                "com.deezer.feature.mediabrowser.DeezerMediaBrowserService"), new MediaBrowser.ConnectionCallback() {
                @Override public void onConnected() {
                    if (destroyed || expected != generation) return;
                    try {
                        controller = new MediaController(context, browser.getSessionToken());
                        if (!context.getPackageName().equals(controller.getPackageName())) {
                            throw new SecurityException("Media session belongs to another application");
                        }
                        context.getCarService(MediaPlaybackManager.class).registerMediaPlaybackToken(
                            MediaSessionCompat.Token.fromToken(browser.getSessionToken()));
                        callback = new MediaController.Callback() {
                            @Override public void onSessionDestroyed() {
                                if (expected == generation) fail("Deezer playback ended. Reconnect to continue.");
                            }
                            @Override public void onQueueChanged(List<MediaSession.QueueItem> queue) {
                                if (expected == generation) refresh();
                            }
                            @Override public void onPlaybackStateChanged(PlaybackState state) {
                                if (expected == generation) refresh();
                            }
                        };
                        controller.registerCallback(callback);
                        registered = true;
                        refresh();
                    } catch (RuntimeException failure) {
                        fail("Could not connect Deezer playback. Open Deezer on your phone, then reconnect.");
                    }
                }
                @Override public void onConnectionFailed() {
                    if (expected == generation) fail("Open Deezer on your phone and sign in, then reconnect.");
                }
                @Override public void onConnectionSuspended() {
                    if (expected == generation) fail("Deezer disconnected. Reconnect to continue.");
                }
            }, null);
            browser.connect();
        }

        private void showPlayer() {
            ScreenManager screens = getCarContext().getCarService(ScreenManager.class);
            for (Screen screen : screens.getScreenStack()) {
                if (screen instanceof PlayerScreen && ((PlayerScreen) screen).client == this) {
                    screens.push(screen);
                    return;
                }
            }
            screens.push(new PlayerScreen(getCarContext(), this));
        }

        private void showLibrary() {
            ScreenManager screens = getCarContext().getCarService(ScreenManager.class);
            for (Screen screen : screens.getScreenStack()) {
                if (screen instanceof BrowseScreen && ((BrowseScreen) screen).client == this && ((BrowseScreen) screen).parent == null) {
                    screens.push(screen);
                    return;
                }
            }
            screens.push(new BrowseScreen(getCarContext(), this, null, "Deezer"));
        }

        private void showQueue() {
            ScreenManager screens = getCarContext().getCarService(ScreenManager.class);
            for (Screen screen : screens.getScreenStack()) {
                if (screen instanceof QueueScreen && ((QueueScreen) screen).client == this) {
                    screens.push(screen);
                    return;
                }
            }
            screens.push(new QueueScreen(getCarContext(), this));
        }

        private Template pending(Screen owner) {
            final int expectedGeneration = generation;
            MessageTemplate.Builder template = new MessageTemplate.Builder(error == null ? "Connecting to Deezer…" : error)
                .setTitle("Deezer").setHeaderAction(Action.APP_ICON);
            if (error == null) template.setLoading(true);
            else template.addAction(new Action.Builder().setTitle("Reconnect").setOnClickListener(() -> {
                if (!destroyed && generation == expectedGeneration && owner.getScreenManager().getTop() == owner) connect();
            }).build());
            return template.build();
        }

        private boolean ready() { return registered && controller != null && browser != null && browser.isConnected(); }

        private PlaybackConnection snapshot() { return ready() ? new PlaybackConnection(this) : null; }
    }

    /** A rendered template may outlive a connection; its actions must never target a replacement. */
    private static final class PlaybackConnection {
        private final MediaSessionClient client;
        private final int generation;
        private final MediaBrowser browser;
        private final MediaController controller;

        PlaybackConnection(MediaSessionClient client) {
            this.client = client;
            generation = client.generation;
            browser = client.browser;
            controller = client.controller;
        }

        private boolean current() {
            return !client.destroyed && client.registered && client.generation == generation &&
                client.browser == browser && client.controller == controller && browser.isConnected();
        }

        private OnClickListener action(Screen owner, Runnable action) {
            return () -> {
                if (current() && owner.getScreenManager().getTop() == owner) action.run();
            };
        }
    }

    private static final class PlayerScreen extends Screen {
        private final MediaSessionClient client;
        PlayerScreen(CarContext context, MediaSessionClient client) { super(context); this.client = client; }
        @Override public Template onGetTemplate() {
            final PlaybackConnection origin = client.snapshot();
            if (origin == null) return client.pending(this);
            Header.Builder header = new Header.Builder().setTitle("Deezer").setStartHeaderAction(Action.BACK);
            header.addEndHeaderAction(new Action.Builder().setTitle("Library").setOnClickListener(origin.action(this, client::showLibrary)).build());
            List<MediaSession.QueueItem> queue = origin.controller.getQueue();
            if (queue != null && !queue.isEmpty()) header.addEndHeaderAction(new Action.Builder().setTitle("Queue")
                .setOnClickListener(origin.action(this, () -> {
                    List<MediaSession.QueueItem> liveQueue = origin.controller.getQueue();
                    if (liveQueue != null && !liveQueue.isEmpty()) client.showQueue();
                })).build());
            // The host draws metadata and controls from Deezer's actual session.
            return new MediaPlaybackTemplate.Builder().setHeader(header.build()).build();
        }
    }

    private static final class BrowseScreen extends Screen {
        private final MediaSessionClient client;
        private final String parent;
        private final String title;
        private final List<MediaBrowser.MediaItem> items = new ArrayList<>();
        private MediaBrowser subscribedBrowser;
        private String subscribedId;
        private boolean loading = true;
        private boolean failed;
        private int page;
        private MediaBrowser.SubscriptionCallback subscription;

        BrowseScreen(CarContext context, MediaSessionClient client, String parent, String title) {
            super(context); this.client = client; this.parent = parent; this.title = title;
            getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override public void onDestroy(LifecycleOwner owner) { unsubscribe(); }
            });
        }

        private void unsubscribe() {
            if (subscribedBrowser != null && subscribedBrowser.isConnected()) subscribedBrowser.unsubscribe(subscribedId, subscription);
            subscribedBrowser = null; subscribedId = null;
            subscription = null;
        }

        @Override public Template onGetTemplate() {
            final PlaybackConnection origin = client.snapshot();
            if (origin == null) return client.pending(this);
            if (subscribedBrowser != origin.browser) {
                unsubscribe(); loading = true; failed = false; items.clear(); page = 0;
                subscribedBrowser = origin.browser;
                subscribedId = parent == null ? origin.browser.getRoot() : parent;
                final MediaBrowser expectedBrowser = subscribedBrowser;
                final String expectedParent = subscribedId;
                final int expectedGeneration = client.generation;
                subscription = new MediaBrowser.SubscriptionCallback() {
                    private boolean current(String id) {
                        return origin.current() && subscription == this && client.generation == expectedGeneration &&
                            subscribedBrowser == expectedBrowser && client.browser == expectedBrowser &&
                            expectedParent.equals(id) && expectedParent.equals(subscribedId);
                    }
                    @Override public void onChildrenLoaded(String id, List<MediaBrowser.MediaItem> children) {
                        if (!current(id)) return;
                        items.clear(); items.addAll(children); loading = false; failed = false; page = 0; invalidate();
                    }
                    @Override public void onError(String id) {
                        if (!current(id)) return;
                        loading = false; failed = true; invalidate();
                    }
                };
                subscribedBrowser.subscribe(subscribedId, subscription);
            }
            Header header = new Header.Builder().setTitle(title)
                .setStartHeaderAction(parent == null ? Action.APP_ICON : Action.BACK)
                .addEndHeaderAction(new Action.Builder().setTitle("Player").setOnClickListener(origin.action(this, client::showPlayer)).build()).build();
            if (failed) return new MessageTemplate.Builder("Deezer could not load this list. Open Deezer on your phone to check your account.")
                .setHeader(header).addAction(new Action.Builder().setTitle("Retry").setOnClickListener(origin.action(this, () -> {
                    unsubscribe(); invalidate();
                })).build()).build();
            ListTemplate.Builder template = new ListTemplate.Builder().setHeader(header);
            if (loading) return template.setLoading(true).build();
            ItemList.Builder rows = new ItemList.Builder().setNoItemsMessage("No items available from Deezer");
            int limit = Math.max(3, getCarContext().getCarService(ConstraintManager.class).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST));
            int size = limit - 2; // Reserve navigation rows without truncating the provider's tree.
            final int renderedPage = page;
            int start = Math.min(page * size, items.size());
            if (page > 0) rows.addItem(new Row.Builder().setTitle("Previous page").setOnClickListener(origin.action(this, () -> {
                if (page == renderedPage) { page--; invalidate(); }
            })).build());
            for (int i = start; i < Math.min(start + size, items.size()); i++) {
                MediaBrowser.MediaItem item = items.get(i);
                String id = item.getMediaId();
                Row.Builder row = new Row.Builder().setTitle(label(item.getDescription().getTitle()));
                CharSequence subtitle = item.getDescription().getSubtitle();
                if (subtitle != null && subtitle.length() > 0) row.addText(subtitle);
                if (id != null && (item.isBrowsable() || item.isPlayable())) row.setBrowsable(item.isBrowsable()).setOnClickListener(origin.action(this, () -> {
                    if (item.isBrowsable()) getScreenManager().push(new BrowseScreen(getCarContext(), client, id, label(item.getDescription().getTitle())));
                    else {
                        Bundle extras = item.getDescription().getExtras();
                        origin.controller.getTransportControls().playFromMediaId(id, extras == null ? null : new Bundle(extras));
                        client.showPlayer();
                    }
                }));
                rows.addItem(row.build());
            }
            if (start + size < items.size()) rows.addItem(new Row.Builder().setTitle("Next page").setOnClickListener(origin.action(this, () -> {
                if (page == renderedPage) { page++; invalidate(); }
            })).build());
            return template.setSingleList(rows.build()).build();
        }
    }

    private static final class QueueScreen extends Screen {
        private final MediaSessionClient client;
        private int page;
        private int viewGeneration = -1;
        QueueScreen(CarContext context, MediaSessionClient client) { super(context); this.client = client; }
        @Override public Template onGetTemplate() {
            final PlaybackConnection origin = client.snapshot();
            if (origin == null) return client.pending(this);
            if (viewGeneration != origin.generation) { viewGeneration = origin.generation; page = 0; }
            List<MediaSession.QueueItem> queue = origin.controller.getQueue();
            PlaybackState state = origin.controller.getPlaybackState();
            ItemList.Builder rows = new ItemList.Builder().setNoItemsMessage("Deezer has no queue");
            int size = Math.max(1, getCarContext().getCarService(ConstraintManager.class).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST) - 2);
            if (queue != null) {
                page = Math.min(page, Math.max(0, (queue.size() - 1) / size));
                final int renderedPage = page;
                if (page > 0) rows.addItem(new Row.Builder().setTitle("Previous page").setOnClickListener(origin.action(this, () -> {
                    if (page == renderedPage) { page--; invalidate(); }
                })).build());
                for (int i = page * size; i < Math.min((page + 1) * size, queue.size()); i++) {
                    MediaSession.QueueItem item = queue.get(i);
                    Row.Builder row = new Row.Builder().setTitle(label(item.getDescription().getTitle()));
                    if (state != null && (state.getActions() & PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM) != 0) {
                        row.setOnClickListener(origin.action(this, () -> {
                            PlaybackState current = origin.controller.getPlaybackState();
                            if (current != null && (current.getActions() & PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM) != 0) {
                                origin.controller.getTransportControls().skipToQueueItem(item.getQueueId());
                            }
                        }));
                    }
                    rows.addItem(row.build());
                }
                if ((page + 1) * size < queue.size()) rows.addItem(new Row.Builder().setTitle("Next page").setOnClickListener(origin.action(this, () -> {
                    if (page == renderedPage) { page++; invalidate(); }
                })).build());
            }
            return new ListTemplate.Builder().setHeader(new Header.Builder().setTitle("Queue").setStartHeaderAction(Action.BACK).build())
                .setSingleList(rows.build()).build();
        }
    }

    private static String label(CharSequence title) { return title == null || title.length() == 0 ? "Untitled" : title.toString(); }
}
