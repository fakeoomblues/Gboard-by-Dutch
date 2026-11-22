package nl.johninnl.deezer.queue;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import java.util.ArrayList;
import java.util.List;

/** The only raw/native-ID to exported/opaque-ID boundary. */
public final class QueueSession {
    private QueueHandles handles;
    private MediaSessionCompat session;
    private PlaybackStateCompat rawState;
    private boolean transition;
    public QueueSession(NativeQueue owner, MediaSessionCompat session) {
        this.handles = new QueueHandles(owner); this.session = session;
    }
    public synchronized PlaybackStateCompat state(PlaybackStateCompat raw) {
        rawState = raw;
        if (raw == null || handles == null) return raw;
        boolean ready;
        long active;
        try { ready = !transition && handles.ready(); active = transition ? -1 : handles.active(raw.getActiveQueueItemId()); }
        catch (RuntimeException failure) { ready = false; active = -1; }
        return new PlaybackStateCompat.Builder(raw)
            .setActions((raw.getActions() & ~4096L) | (ready ? 4096L : 0L))
            .setActiveQueueItemId(active).build();
    }
    public synchronized void publish(List<?> raw) {
        if (session == null || handles == null) return;
        transition = true;
        handles.invalidate();
        @SuppressWarnings("unchecked")
        List<MediaSessionCompat.QueueItem> output = (List<MediaSessionCompat.QueueItem>) raw;
        try {
            if (rawState != null) session.setPlaybackState(state(rawState));
            try {
                QueueSource source = raw instanceof QueueSource ? (QueueSource) raw : null;
                List<Long> positions = new ArrayList<>();
                if (source != null) for (Object value : raw) positions.add(((MediaSessionCompat.QueueItem) value).getQueueId());
                List<Long> ids = source == null || !source.current() ? null : handles.replace(source.source, source.occurrences, positions, source.version, source.mutation);
                if (ids != null) {
                    List<MediaSessionCompat.QueueItem> mapped = new ArrayList<>();
                    for (int i = 0; i < raw.size(); i++) {
                        MediaSessionCompat.QueueItem item = (MediaSessionCompat.QueueItem) raw.get(i);
                        mapped.add(new MediaSessionCompat.QueueItem(item.getDescription(), ids.get(i)));
                    }
                    output = mapped;
                }
            } catch (RuntimeException failure) { handles.invalidate(); }
            try { session.setQueue(output); }
            catch (RuntimeException failure) { handles.invalidate(); throw failure; }
        } finally {
            transition = false;
            if (rawState != null) session.setPlaybackState(state(rawState));
        }
    }
    public void select(long id) {
        QueueHandles current;
        synchronized (this) { current = handles; if (transition) return; }
        if (current != null) current.select(id);
    }
    public synchronized void retire() {
        if (handles != null) handles.retire();
        handles = null; session = null; rawState = null;
    }
}
