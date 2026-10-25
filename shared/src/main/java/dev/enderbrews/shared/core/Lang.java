package dev.enderbrews.shared.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side translations. Players without the mod on their client have no language files for it,
 * so the server picks the text in the language the player's client reports. Effect names are not
 * translated here: they are sent as vanilla translation keys that every client already knows.
 *
 * <p>Files live at {@code /enderbrews/lang/<locale>.lang} with one {@code key=value} per line;
 * {@code %s} marks where an argument (often an effect name) goes.
 */
public final class Lang {

    private static final String FALLBACK = "en_us";
    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

    private Lang() {
    }

    public static String get(String locale, String key) {
        String normalized = locale == null ? FALLBACK : locale.toLowerCase(Locale.ROOT);
        String text = load(normalized).get(key);
        if (text == null) {
            text = load(FALLBACK).getOrDefault(key, key);
        }
        return text;
    }

    /** The text split around each {@code %s}, so callers can put rich components in between. */
    public static String[] parts(String locale, String key) {
        return get(locale, key).split("%s", -1);
    }

    private static Map<String, String> load(String locale) {
        return CACHE.computeIfAbsent(locale, Lang::read);
    }

    private static Map<String, String> read(String locale) {
        Map<String, String> entries = new HashMap<>();
        try (InputStream in = Lang.class.getResourceAsStream("/enderbrews/lang/" + locale + ".lang")) {
            if (in == null) {
                return entries;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq > 0 && !line.startsWith("#")) {
                    entries.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (IOException ignored) {
            // Missing or unreadable translations fall back to English.
        }
        return entries;
    }
}
