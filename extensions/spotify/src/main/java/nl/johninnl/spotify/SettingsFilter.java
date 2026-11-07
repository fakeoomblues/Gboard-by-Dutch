package nl.johninnl.spotify;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Removes settings sections and rows that only lead to Premium or downloads. The screen model
 * stores sections as p.xt61 items (title resource id in field b) and rows in field e. Row lists are
 * immutable, so a changed section is rebuilt rather than mutated.
 */
@SuppressWarnings("unused")
public final class SettingsFilter {
    private static final Set<Integer> HIDDEN_SECTION_TITLES = new HashSet<>(Arrays.asList(
            0x7f13291b, // settings_section_advertisements_title
            0x7f13291a, // settings_section_advertisementcategories_title
            0x7f1328ea, // settings_page_advertisements_title
            0x7f132929, // settings_section_download_quality_title
            0x7f13292a, // settings_section_downloads_and_streaming_title
            0x7f132928, // settings_section_download_quality_note
            0x7f132932, // settings_section_offline_title
            0x7f13293d, // settings_section_storage_title
            0x7f132924, // settings_section_default_save_locations_title
            0x7f132927  // settings_section_dj_title
    ));

    private static final Set<Integer> HIDDEN_ROW_TITLES = new HashSet<>(Arrays.asList(
            0x7f131812, // lossless_streaming_quality_very_high
            0x7f131808, // lossless_streaming_quality_lossless
            0x7f131809, // lossless_streaming_quality_lossless_16
            0x7f13180b  // lossless_streaming_quality_lossless_24
    ));

    private static Class<?> sectionType;
    private static Class<?> rowListType;

    private SettingsFilter() {
    }

    /**
     * Injection point: the two section lists of the settings screen model.
     */
    public static List filterSections(List sections) {
        if (sections == null) return null;

        List<Object> visible = new ArrayList<>(sections.size());
        for (Object section : sections) {
            Integer title = integerField(section, "b");
            if (title != null && HIDDEN_SECTION_TITLES.contains(title)) continue;

            Object rewritten = rewriteRows(section);
            visible.add(rewritten);
        }
        return visible;
    }

    private static Object rewriteRows(Object section) {
        Object rows = field(section, "e");
        if (!(rows instanceof List)) return section;

        List<Object> filtered = new ArrayList<>();
        boolean changed = false;
        for (Object row : (List<?>) rows) {
            if (isHiddenRow(row)) {
                changed = true;
            } else {
                filtered.add(row);
            }
        }
        nl.johninnl.spotify.shared.Logger.info("SettingsFilter rows=" + ((List<?>) rows).size()
                + " hidden=" + (filtered.size() != ((List<?>) rows).size()));
        if (!changed) return section;

        Object rowList = rowListProxy(filtered);
        Object rebuilt = rowList == null ? null : newSection(section, rowList);
        nl.johninnl.spotify.shared.Logger.info("SettingsFilter rebuilt=" + (rebuilt != null));
        if (rebuilt != null) return rebuilt;
        return section;
    }

    private static Object newSection(Object section, Object rows) {
        try {
            if (sectionType == null) sectionType = section.getClass();
            for (Constructor<?> constructor : sectionType.getDeclaredConstructors()) {
                if (constructor.getParameterCount() != 6 || constructor.isSynthetic()) continue;
                constructor.setAccessible(true);
                return constructor.newInstance(
                        field(section, "a"), field(section, "b"), field(section, "c"),
                        field(section, "d"), rows, 0
                );
            }
        } catch (Throwable ignored) {
            // Keep the original section.
        }
        return null;
    }

    private static Object rowListProxy(List<Object> rows) {
        try {
            if (rowListType == null) rowListType = Class.forName("p.c380");
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("subList")) {
                    int from = (Integer) args[0];
                    int to = (Integer) args[1];
                    return new ArrayList<>(rows.subList(from, to));
                }
                return method.invoke(rows, args);
            };
            return Proxy.newProxyInstance(rowListType.getClassLoader(),
                    new Class<?>[]{rowListType, List.class}, handler);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isHiddenRow(Object row) {
        if (row == null) return false;

        String id = rowId(row);
        if (id != null) {
            String lower = id.toLowerCase(Locale.ROOT);
            if (lower.contains("advert") || lower.contains("download") ||
                    lower.contains("offline") || lower.contains("storage") ||
                    lower.contains("start_jam") || lower.contains("startjam") ||
                    lower.equals("very_high") || lower.contains("lossless") ||
                    lower.contains("_jam") || lower.equals("jam")) {
                return true;
            }
        }

        for (Class<?> type = row.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != int.class && field.getType() != Integer.class) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(row);
                    if (value instanceof Integer && HIDDEN_ROW_TITLES.contains(value)) return true;
                } catch (Throwable ignored) {
                    // Keep looking.
                }
            }
        }
        return false;
    }

    private static String rowId(Object row) {
        try {
            Object value = row.getClass().getMethod("getId").invoke(row);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object field(Object object, String name) {
        if (object == null) return null;
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
                // Try the superclass.
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer integerField(Object object, String name) {
        Object value = field(object, name);
        return value instanceof Integer ? (Integer) value : null;
    }
}
