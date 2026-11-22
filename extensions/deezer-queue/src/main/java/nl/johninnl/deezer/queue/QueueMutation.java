package nl.johninnl.deezer.queue;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/** Invalidate identity before native SQL can change rows, including the loader-refresh interval. */
public final class QueueMutation {
    private static long epoch;
    private static int writers;
    static final String KEY = "nl.johninnl.deezer.queue.cursorEpoch";
    public static synchronized long version() { return writers == 0 && epoch >= 0 ? epoch : -1; }
    static synchronized void begin() { writers++; epoch++; }
    static synchronized void end() { epoch++; writers--; }
    public static long cursorVersion(Object value) {
        if (!(value instanceof Cursor)) return -1;
        try {
            Bundle extras = ((Cursor) value).getExtras();
            return extras == null ? -1 : extras.getLong(KEY, -1L);
        } catch (RuntimeException unavailable) { return -1; }
    }
    private static boolean rows(Uri uri) {
        if (uri == null || uri.getPathSegments().isEmpty()) return false;
        String path = uri.getPathSegments().get(0);
        return path.equals("tracks") || path.equals("tracks_insert") || path.equals("tracks_replace") ||
            path.equals("unique_tracks") || path.equals("contexts");
    }
    private static boolean identity(ContentValues values) {
        return values != null && identityKeys(values.keySet());
    }
    static boolean identityKeys(Iterable<String> keys) {
        for (String key : keys) {
            String name = key == null ? "" : key.toUpperCase(java.util.Locale.ROOT);
            // Only these two proven metadata columns are exempt. Unknown/rowid/case variants fence.
            if (!name.equals("BOOKMARK") && !name.equals("HEARD_STATUS")) return true;
        }
        return false;
    }
    public static Uri insert(NativeProvider provider, Uri uri, ContentValues values) throws Throwable {
        boolean change = rows(uri); if (change) begin();
        try { return provider.queueInsert(uri, values); } finally { if (change) end(); }
    }
    public static int bulkInsert(NativeProvider provider, Uri uri, ContentValues[] values) throws Throwable {
        boolean change = rows(uri); if (change) begin();
        try { return provider.queueBulkInsert(uri, values); } finally { if (change) end(); }
    }
    public static int delete(NativeProvider provider, Uri uri, String where, String[] args) throws Throwable {
        boolean change = rows(uri); if (change) begin();
        try { return provider.queueDelete(uri, where, args); } finally { if (change) end(); }
    }
    public static int update(NativeProvider provider, Uri uri, ContentValues values, String where, String[] args) throws Throwable {
        // Preserve only proven bookmark/heard metadata; all other track updates fence selection.
        boolean change = rows(uri) && identity(values); if (change) begin();
        try { return provider.queueUpdate(uri, values, where, args); } finally { if (change) end(); }
    }
    public static Bundle call(NativeProvider provider, String method, String argument, Bundle extras) throws Throwable {
        boolean change = "clear_all".equals(method) || "clear_from_position".equals(method) ||
            "unshuffle".equals(method) || "remove_keys".equals(method) || "clear".equals(method) ||
            "reorder".equals(method) || "shuffle".equals(method);
        if (change) begin();
        try { return provider.queueCall(method, argument, extras); } finally { if (change) end(); }
    }
    public static Cursor query(NativeProvider provider, Uri uri, String[] columns, String where, String[] args, String order) throws Throwable {
        long before = version();
        Cursor cursor = provider.queueQuery(uri, columns, where, args, order);
        if (cursor == null || !rows(uri)) return cursor;
        long after = version();
        return new QueueCursor(cursor, before >= 0 && before == after ? before : -1);
    }
}
