package org.ungoogled.waze;

import java.util.Locale;

/** Native observations only; no Activity, GPS, wall-clock or overlay dependency. */
final class NavigationState {
    static final long ROAD_TTL_MS = 30_000, ZONE_TTL_MS = 90_000;
    private String roadStatus = "unknown", zoneStatus = "unknown", unit = "", source = "", segment = "";
    private long roadAt = -1, zoneAt = -1, startAt, session;
    private double limitKmh = Double.NaN;
    private int rawLimit = -1, firstLeft, minimumLeft, left, percent;

    void reset(boolean enabled) {
        roadStatus = zoneStatus = enabled ? "unknown" : "disabled";
        roadAt = zoneAt = -1; limitKmh = Double.NaN; rawLimit = -1;
        source = segment = "";
    }

    void speed(String units, int limit, long at) {
        if (at < roadAt) return;
        roadAt = at; rawLimit = limit;
        unit = units == null ? "" : units.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        double factor = unit.equals("mph") || unit.equals("mi/h") || unit.equals("imperial") ? 1.609344 :
                unit.equals("km/h") || unit.equals("kmh") || unit.equals("kph") || unit.equals("metric") ? 1 : Double.NaN;
        limitKmh = limit > 0 ? limit * factor : Double.NaN;
        roadStatus = Double.isNaN(limitKmh) ? "unknown" : "valid";
    }

    void legacy(int progress, int remaining, long at) {
        if (progress < 0 || progress > 100 || remaining < 0) return;
        update("legacy", "", remaining, progress, at);
    }

    void insights(String id, int remaining, long at) {
        if (remaining < 0) return;
        update("insights", id == null ? "" : id, remaining, -1, at);
    }

    private void update(String feed, String id, int remaining, int progress, long at) {
        expire(at);
        if (at < zoneAt || (zoneStatus.equals("active") && !source.equals(feed))) return;
        if (remaining == 0 || progress == 100) { clear(feed, at); return; }
        boolean identityChanged = !id.equals(segment);
        boolean resetDistance = zoneStatus.equals("active") && remaining > left + Math.max(100, firstLeft / 5);
        boolean resetProgress = zoneStatus.equals("active") && progress >= 0 && percent >= 0 && progress + 5 < percent;
        if (!zoneStatus.equals("active") || identityChanged || resetDistance || resetProgress) {
            session++; startAt = at; firstLeft = minimumLeft = remaining; source = feed; segment = id;
        }
        zoneStatus = "active"; zoneAt = at; left = remaining;
        minimumLeft = Math.min(minimumLeft, remaining); percent = progress;
    }

    void clear(String feed, long at) {
        if (at < zoneAt || (zoneStatus.equals("active") && !source.equals(feed))) return;
        zoneStatus = "inactive"; zoneAt = at; source = feed;
    }

    private void expire(long now) {
        if (roadAt >= 0 && now - roadAt > ROAD_TTL_MS && !roadStatus.equals("disabled")) roadStatus = "stale";
        if (zoneAt >= 0 && now - zoneAt > ZONE_TTL_MS && zoneStatus.equals("active")) zoneStatus = "stale";
    }

    Snapshot snapshot(long now) {
        expire(now);
        boolean active = zoneStatus.equals("active");
        int observed = active ? firstLeft - minimumLeft : -1;
        long elapsed = zoneAt - startAt;
        double avg = active && observed >= 30 && elapsed >= 3000 ? observed * 3600.0 / elapsed : Double.NaN;
        double progress = !active ? Double.NaN : percent >= 0 ? percent :
                Math.max(0, Math.min(100, 100.0 * (firstLeft - left) / firstLeft));
        return new Snapshot(roadStatus, zoneStatus, roadAt, zoneAt,
                roadStatus.equals("valid") ? limitKmh : Double.NaN,
                roadStatus.equals("valid") ? rawLimit : -1, unit, source, segment,
                session, active ? startAt : -1, active ? left : -1, observed, progress,
                active && percent < 0, avg);
    }

    static final class Snapshot {
        final String roadStatus, zoneStatus, unit, source, segment;
        final long roadAt, zoneAt, zoneSession, observationStart;
        final int rawLimit, remaining, observedMeters;
        final double limitKmh, progress, averageKmh;
        final boolean progressEstimated;
        Snapshot(String roadStatus, String zoneStatus, long roadAt, long zoneAt, double limitKmh,
                 int rawLimit, String unit, String source, String segment, long zoneSession,
                 long observationStart, int remaining, int observedMeters, double progress,
                 boolean progressEstimated, double averageKmh) {
            this.roadStatus = roadStatus; this.zoneStatus = zoneStatus; this.roadAt = roadAt; this.zoneAt = zoneAt;
            this.limitKmh = limitKmh; this.rawLimit = rawLimit; this.unit = unit; this.source = source;
            this.segment = segment; this.zoneSession = zoneSession; this.observationStart = observationStart;
            this.remaining = remaining; this.observedMeters = observedMeters; this.progress = progress;
            this.progressEstimated = progressEstimated; this.averageKmh = averageKmh;
        }
    }
}
