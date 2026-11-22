package nl.johninnl.deezer.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Own only the exported session identity. Native positions, tracks and contexts remain native. */
public final class QueueHandles {
    private static final AtomicLong NEXT = new AtomicLong(0x100000000L);
    private static final ThreadLocal<Request> REQUEST = new ThreadLocal<>();
    private static final ThreadLocal<CommandFrame> COMMAND = new ThreadLocal<>();
    private static final int MAX_ROWS = 10000;
    private NativeQueue owner;
    private Object source, cursor, holder;
    private long epoch, sourceEpoch, mutationEpoch;
    private final Map<Long, Entry> handles = new HashMap<>();
    private final Map<Integer, Entry> positions = new HashMap<>();

    private static final class Entry {
        final long handle;
        final int position;
        final Object occurrence;
        Entry(long handle, int position, Object occurrence) {
            this.handle = handle; this.position = position; this.occurrence = occurrence;
        }
    }
    private static final class Request {
        final QueueHandles session;
        final NativeQueue owner;
        final Entry entry;
        boolean consumed;
        Object command;
        Request(QueueHandles session, NativeQueue owner, Entry entry) {
            this.session = session; this.owner = owner; this.entry = entry;
        }
    }
    private static final class CommandFrame {
        final Request request;
        CommandFrame(Request request) { this.request = request; }
    }

    public QueueHandles(NativeQueue owner) { this.owner = owner; }

    /** Each publication gets fresh IDs, including same-content reloads and A -> B -> A. */
    public List<Long> replace(Object producingSource, List<?> occurrences, List<Long> rawIds, long producingEpoch, long producingMutation) {
        NativeQueue nativeQueue = nativeOwner();
        if (nativeQueue == null) return null;
        synchronized (nativeQueue.queueLock()) {
            synchronized (this) {
                clear();
                if (owner != nativeQueue || producingSource == null ||
                        nativeQueue.queueSource() != producingSource || nativeQueue.queueSourceEpoch() != producingEpoch ||
                        producingMutation < 0 || QueueMutation.version() != producingMutation ||
                        nativeQueue.queueCursorStamp() != producingMutation || nativeQueue.queueSourceStamp() != producingMutation || occurrences == null ||
                        rawIds.size() != occurrences.size() || rawIds.isEmpty() || rawIds.size() > MAX_ROWS) return null;
                Object currentCursor = nativeQueue.queueCursor();
                long currentEpoch = nativeQueue.queueEpoch();
                if (currentCursor == null || currentEpoch < 0 || producingEpoch < 0) return null;
                List<Long> ids = new ArrayList<>(rawIds.size());
                for (int i = 0; i < rawIds.size(); i++) {
                    Long raw = rawIds.get(i);
                    if (raw == null || raw < 0 || raw > Integer.MAX_VALUE || positions.containsKey(raw.intValue())) {
                        clear(); return null;
                    }
                    int position = raw.intValue();
                    Object actual = nativeQueue.queueItem(position);
                    Object provided = occurrences.get(i);
                    if (actual == null || provided == null || nativeQueue.queuePosition(provided) != position ||
                            !nativeQueue.queueSame(provided, actual)) { clear(); return null; }
                    long id = NEXT.getAndIncrement();
                    if (id < 0x100000000L) { clear(); return null; }
                    Entry entry = new Entry(id, position, actual);
                    handles.put(id, entry); positions.put(position, entry); ids.add(id);
                }
                if (currentCursor != nativeQueue.queueCursor() || currentEpoch != nativeQueue.queueEpoch() ||
                        producingEpoch != nativeQueue.queueSourceEpoch() || producingSource != nativeQueue.queueSource() ||
                        QueueMutation.version() != producingMutation || nativeQueue.queueCursorStamp() != producingMutation ||
                        nativeQueue.queueSourceStamp() != producingMutation) { clear(); return null; }
                source = producingSource; cursor = currentCursor; epoch = currentEpoch;
                sourceEpoch = producingEpoch; mutationEpoch = producingMutation;
                holder = nativeQueue.queueHolder();
                return ids;
            }
        }
    }

    private synchronized NativeQueue nativeOwner() { return owner; }
    private boolean live(NativeQueue nativeQueue) {
        return owner == nativeQueue && cursor != null && source == nativeQueue.queueSource() &&
            cursor == nativeQueue.queueCursor() && epoch == nativeQueue.queueEpoch() &&
            sourceEpoch == nativeQueue.queueSourceEpoch() && QueueMutation.version() == mutationEpoch &&
            nativeQueue.queueCursorStamp() == mutationEpoch && nativeQueue.queueSourceStamp() == mutationEpoch && !handles.isEmpty();
    }
    public boolean ready() {
        NativeQueue nativeQueue = nativeOwner();
        if (nativeQueue == null) return false;
        synchronized (nativeQueue.queueLock()) {
            synchronized (this) { return live(nativeQueue) && nativeQueue.queueAllowed(); }
        }
    }
    public long active(long rawId) {
        NativeQueue nativeQueue = nativeOwner();
        if (nativeQueue == null || rawId < 0 || rawId > Integer.MAX_VALUE) return -1;
        synchronized (nativeQueue.queueLock()) {
            synchronized (this) {
                Entry entry = positions.get((int) rawId);
                Object current = nativeQueue.queueCurrent();
                return live(nativeQueue) && entry != null && current != null &&
                    nativeQueue.queueSame(entry.occurrence, current) ? entry.handle : -1;
            }
        }
    }

    /** Reject before native EventBus/pause; never hold a native lock across external callbacks. */
    public boolean select(long handle) {
        NativeQueue nativeQueue = nativeOwner();
        if (nativeQueue == null) return false;
        final Entry entry;
        try {
        synchronized (nativeQueue.queueLock()) {
            synchronized (this) {
                entry = handles.get(handle);
                if (!live(nativeQueue) || entry == null || !nativeQueue.queueAllowed() ||
                        !nativeQueue.queueSame(entry.occurrence, nativeQueue.queueItem(entry.position))) return false;
            }
        }
        } catch (RuntimeException failure) { invalidate(); return false; }
        Request previous = REQUEST.get();
        Request request = new Request(this, nativeQueue, entry);
        REQUEST.set(request);
        try {
            nativeQueue.queueDispatch(entry.position);
            return request.consumed;
        } finally {
            if (previous == null) REQUEST.remove(); else REQUEST.set(previous);
        }
    }

    /** Scope the actual native command; a reentrant independent command gets no selection guard. */
    public static void command(NativeQueue owner, Object command) {
        CommandFrame previous = COMMAND.get();
        Request selected = REQUEST.get();
        COMMAND.set(new CommandFrame(selected != null && selected.owner == owner && selected.command == command
            ? selected : null));
        try { owner.queueCommand(command); }
        finally { if (previous == null) COMMAND.remove(); else COMMAND.set(previous); }
    }

    /** Bind exactly the object invoked by the original queue callback, after its EventBus event. */
    public static void selectedCommand(NativeQueue owner, Object command) {
        Request selected = REQUEST.get();
        if (selected == null || selected.owner != owner) return;
        Object previous = selected.command;
        selected.command = command;
        try { command(owner, command); }
        finally { selected.command = previous; }
    }

    /** Hook ONLY the authoritative selected-row consumption, never the shared cursor getter. */
    public static boolean consume(Object actualHolder, Object actualRow) {
        CommandFrame frame = COMMAND.get();
        Request request = frame == null ? null : frame.request;
        if (request == null) return true;
        QueueHandles session = request.session;
        NativeQueue nativeQueue = request.owner;
        try {
        synchronized (nativeQueue.queueLock()) {
            synchronized (session) {
                if (!session.live(nativeQueue) || session.handles.get(request.entry.handle) != request.entry || session.holder != actualHolder || request.consumed ||
                        !nativeQueue.queueAllowed() || actualRow == null ||
                        !nativeQueue.queueSame(request.entry.occurrence, actualRow)) return false;
                request.consumed = true;
                return true;
            }
        }
        } catch (RuntimeException failure) { session.invalidate(); return false; }
    }

    private void clear() { handles.clear(); positions.clear(); source = null; cursor = null; holder = null; }
    public synchronized void invalidate() { clear(); }
    public synchronized void retire() { clear(); owner = null; }
}
