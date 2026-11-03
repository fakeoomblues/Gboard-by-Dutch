package patches.maps.gearslip;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Surface-pixel placement of native control targets around measured host covers. */
final class ControlPlacement {
    static final class Plan {
        final List<Rect> targets;
        final int hidden;
        final boolean belowPlayer;
        Plan(List<Rect> targets, int hidden, boolean belowPlayer) {
            this.targets = targets; this.hidden = hidden; this.belowPlayer = belowPlayer;
        }
    }
    static Plan arrange(int width, int height, Rect player, List<Rect> covers,
                        int target, int gap, int count) {
        if (width <= 0 || height <= 0 || width > 32768 || height > 32768 ||
                target <= 0 || target > 32768 || gap < 0 || gap > 32768 || count < 0 || count > 32 ||
                covers == null || covers.size() > 33) throw new IllegalArgumentException("Invalid control geometry");
        List<Rect> free = freeRegions(width, height, covers);
        boolean atLeft = player != null && player.centerX() < width / 2;
        Rect below = player == null || player.isEmpty() ? null : new Rect(player.left, player.bottom, player.right, height);
        Rect selected = null;
        boolean belowPlayer = false;
        int best = 0;
        // Prefer the measured card's lower edge only when all requested controls fit.
        if (below != null) for (Rect region : free) {
            Rect candidate = new Rect(region);
            if (candidate.intersect(below) && capacity(candidate, target, gap) >= count && count > 0) {
                selected = candidate; belowPlayer = true; break;
            }
        }
        if (selected == null) for (Rect region : free) {
            int available = capacity(region, target, gap);
            if (available > best) { selected = region; best = available; }
        }
        if (selected == null || count == 0) return new Plan(Collections.emptyList(), count, false);
        int columns = Math.min(Math.min(2, count), (selected.width() + gap) / (target + gap));
        int slots = Math.min(count, capacity(selected, target, gap));
        int hidden = slots < count ? count - slots + 1 : 0;
        List<Rect> targets = new ArrayList<>();
        int groupWidth = columns * target + (columns - 1) * gap;
        int x = atLeft ? selected.left : selected.right - groupWidth;
        for (int i = 0; i < slots; i++) {
            int left = x + (i % columns) * (target + gap);
            int top = selected.top + (i / columns) * (target + gap);
            targets.add(new Rect(left, top, left + target, top + target));
        }
        return new Plan(Collections.unmodifiableList(targets), hidden, belowPlayer);
    }
    private static List<Rect> freeRegions(int width, int height, List<Rect> covers) {
        if (width <= 0 || height <= 0 || width > 32768 || height > 32768 || covers == null || covers.size() > 64)
            throw new IllegalArgumentException("Invalid cover geometry");
        List<Rect> free = new ArrayList<>();
        free.add(new Rect(0, 0, width, height));
        for (Rect cover : covers) {
            if (cover == null || cover.isEmpty()) continue;
            List<Rect> next = new ArrayList<>();
            for (Rect region : free) {
                Rect overlap = new Rect(region);
                if (!overlap.intersect(cover)) { next.add(region); continue; }
                add(next, region.left, region.top, overlap.left, region.bottom);
                add(next, overlap.right, region.top, region.right, region.bottom);
                add(next, overlap.left, region.top, overlap.right, overlap.top);
                add(next, overlap.left, overlap.bottom, overlap.right, region.bottom);
            }
            free = next;
        }
        return free;
    }
    static Rect overflowAnchor(int width, int height, Rect player, List<Rect> covers, int target) {
        if (target <= 0 || target > 32768) throw new IllegalArgumentException("Invalid control size");
        List<Rect> free = freeRegions(width, height, covers);
        boolean cardLeft = player != null && player.centerX() < width / 2;
        Rect below = player == null ? null : new Rect(player.left, player.bottom, player.right, height);
        Rect selected = anchor(free, below, target, cardLeft, width, height);
        return selected != null ? selected : anchor(free, null, target, !cardLeft, width, height);
    }
    static Rect belowBlock(int width, int height, Rect player, List<Rect> covers, int blockWidth, int blockHeight) {
        if (player == null || player.isEmpty() || blockWidth <= 0 || blockHeight <= 0) return null;
        Rect below = new Rect(player.left, player.bottom, player.right, height);
        for (Rect free : freeRegions(width, height, covers)) {
            Rect region = new Rect(free);
            if (!region.intersect(below) || region.width() < blockWidth || region.height() < blockHeight) continue;
            int left = player.centerX() < width / 2 ? region.left : region.right - blockWidth;
            return new Rect(left, region.top, left + blockWidth, region.top + blockHeight);
        }
        return null;
    }
    private static Rect anchor(List<Rect> regions, Rect limit, int target, boolean preferLeft, int width, int height) {
        Rect selected = null;
        long best = Long.MAX_VALUE;
        for (Rect free : regions) {
            Rect region = new Rect(free);
            if (limit != null && !region.intersect(limit)) continue;
            if (region.width() < target || region.height() < target) continue;
            int left = preferLeft ? region.left : region.right - target;
            Rect candidate = new Rect(left, region.bottom - target, left + target, region.bottom);
            long score = (long) (height - candidate.bottom) * (width + 1L) + (preferLeft ? candidate.left : width - candidate.right);
            if (score < best) { selected = candidate; best = score; }
        }
        return selected;
    }
    static Rect menuPanel(int width, int height, List<Rect> covers, Rect anchor, int unit, int actions) {
        if (unit <= 0 || unit > 32768 || actions < 1 || actions > 32) throw new IllegalArgumentException("Invalid menu geometry");
        Rect best = null;
        long area = 0;
        for (Rect region : freeRegions(width, height, covers)) {
            if (region.width() < unit || region.height() < unit * 2) continue;
            long size = (long) region.width() * region.height();
            if (size > area) { best = region; area = size; }
        }
        if (best == null) return null;
        int panelWidth = Math.min(best.width(), unit * 5);
        int rows = Math.min(actions + 1, best.height() / unit);
        int left = Math.max(best.left, Math.min(anchor.left, best.right - panelWidth));
        int top = Math.max(best.top, Math.min(anchor.top, best.bottom - rows * unit));
        return new Rect(left, top, left + panelWidth, top + rows * unit);
    }
    private static int capacity(Rect region, int target, int gap) {
        return Math.min(2, (region.width() + gap) / (target + gap)) *
                Math.min(32, (region.height() + gap) / (target + gap));
    }
    private static void add(List<Rect> regions, int left, int top, int right, int bottom) {
        if (left < right && top < bottom) regions.add(new Rect(left, top, right, bottom));
    }
}
