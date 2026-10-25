package dev.enderbrews.shared.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the small TOML subset the config file uses: {@code key = value} pairs where a value is
 * a boolean, an integer, a quoted string or an array of quoted strings (which may span lines).
 * Tables, dates and the rest of TOML are not supported. Kept dependency-free so the same config
 * code works on every mod loader and on Bukkit.
 */
final class SimpleToml {

    private SimpleToml() {
    }

    static Map<String, Object> parse(String text) {
        Map<String, Object> values = new LinkedHashMap<>();
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = stripComment(lines[i]).trim();
            if (line.isEmpty()) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("line " + (i + 1) + ": expected 'key = value'");
            }
            String key = line.substring(0, eq).trim();
            StringBuilder value = new StringBuilder(line.substring(eq + 1).trim());
            if (value.toString().startsWith("[")) {
                while (!closesArray(value) && i + 1 < lines.length) {
                    value.append(' ').append(stripComment(lines[++i]).trim());
                }
                if (!closesArray(value)) {
                    throw new IllegalArgumentException("key '" + key + "': array is missing ']'");
                }
            }
            values.put(key, parseValue(key, value.toString().trim()));
        }
        return values;
    }

    private static Object parseValue(String key, String raw) {
        if (raw.equals("true") || raw.equals("false")) {
            return Boolean.parseBoolean(raw);
        }
        if (raw.startsWith("\"")) {
            return parseString(key, raw);
        }
        if (raw.startsWith("[")) {
            return parseArray(key, raw.substring(1, raw.lastIndexOf(']')));
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("key '" + key + "': cannot read value '" + raw + "'");
        }
    }

    private static List<String> parseArray(String key, String body) {
        List<String> items = new ArrayList<>();
        for (String part : body.split(",")) {
            String item = part.trim();
            if (!item.isEmpty()) {
                items.add(parseString(key, item));
            }
        }
        return items;
    }

    private static String parseString(String key, String raw) {
        if (raw.length() < 2 || !raw.startsWith("\"") || !raw.endsWith("\"")) {
            throw new IllegalArgumentException("key '" + key + "': expected a quoted string, got " + raw);
        }
        return raw.substring(1, raw.length() - 1);
    }

    private static boolean closesArray(CharSequence value) {
        return value.toString().trim().endsWith("]");
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == '#' && !quoted) {
                return line.substring(0, i);
            }
        }
        return line;
    }
}
