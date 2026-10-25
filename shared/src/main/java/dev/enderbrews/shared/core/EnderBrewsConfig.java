package dev.enderbrews.shared.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Server-side settings, read from {@code enderbrews.toml}. Every option has a default. */
public final class EnderBrewsConfig {

    public static final String FILE_NAME = "enderbrews.toml";

    private int threshold = 8;
    private Map<String, Integer> thresholdOverrides = Map.of();
    private boolean countShulkerBoxes = true;
    private boolean countSplashAndLingering = false;
    private List<String> extraPotionItems = List.of();
    private boolean higherLevelCountsForLower = true;
    private int maxLevel = 0;
    private int maxEndlessEffects = 0;
    private boolean allowHarmfulEffects = false;
    private List<String> blacklist = List.of("minecraft:infested", "minecraft:oozing", "minecraft:weaving", "minecraft:wind_charged");
    private List<String> whitelist = List.of();
    private List<String> alwaysAllowed = List.of();
    private boolean showParticles = false;
    private boolean showIcon = true;
    private boolean removeOnLogout = true;
    private boolean applyInSpectator = false;
    private boolean allowPause = true;
    private int checkIntervalTicks = 20;

    public static EnderBrewsConfig defaults() {
        return new EnderBrewsConfig();
    }

    /** Loads the config from {@code file}, writing the default file first if it does not exist. */
    public static EnderBrewsConfig load(Path file) throws IOException {
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, defaultFileContents(), StandardCharsets.UTF_8);
            return defaults();
        }
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    static EnderBrewsConfig parse(String text) {
        Map<String, Object> values = SimpleToml.parse(text);
        EnderBrewsConfig config = new EnderBrewsConfig();
        config.threshold = Math.max(1, readInt(values, "threshold", config.threshold));
        config.thresholdOverrides = readOverrides(values);
        config.countShulkerBoxes = readBool(values, "count_shulker_boxes", config.countShulkerBoxes);
        config.countSplashAndLingering = readBool(values, "count_splash_and_lingering", config.countSplashAndLingering);
        config.extraPotionItems = readIds(values, "extra_potion_items", config.extraPotionItems);
        config.higherLevelCountsForLower = readBool(values, "higher_level_counts_for_lower", config.higherLevelCountsForLower);
        config.maxLevel = Math.max(0, readInt(values, "max_level", config.maxLevel));
        config.maxEndlessEffects = Math.max(0, readInt(values, "max_endless_effects", config.maxEndlessEffects));
        config.allowHarmfulEffects = readBool(values, "allow_harmful_effects", config.allowHarmfulEffects);
        config.blacklist = readIds(values, "blacklist", config.blacklist);
        config.whitelist = readIds(values, "whitelist", config.whitelist);
        config.alwaysAllowed = readIds(values, "always_allowed", config.alwaysAllowed);
        config.showParticles = readBool(values, "show_particles", config.showParticles);
        config.showIcon = readBool(values, "show_icon", config.showIcon);
        config.removeOnLogout = readBool(values, "remove_on_logout", config.removeOnLogout);
        config.applyInSpectator = readBool(values, "apply_in_spectator", config.applyInSpectator);
        config.allowPause = readBool(values, "allow_pause", config.allowPause);
        config.checkIntervalTicks = Math.max(1, readInt(values, "check_interval_ticks", config.checkIntervalTicks));
        return config;
    }

    /** Adds the {@code minecraft:} namespace to ids written without one; keeps {@code #tag} markers. */
    public static String normalizeId(String id) {
        String trimmed = id.trim().toLowerCase(Locale.ROOT);
        boolean tag = trimmed.startsWith("#");
        String bare = tag ? trimmed.substring(1) : trimmed;
        if (!bare.contains(":")) {
            bare = "minecraft:" + bare;
        }
        return tag ? "#" + bare : bare;
    }

    public int thresholdFor(String effectId) {
        return thresholdOverrides.getOrDefault(effectId, threshold);
    }

    public int threshold() {
        return threshold;
    }

    public boolean countShulkerBoxes() {
        return countShulkerBoxes;
    }

    public boolean countSplashAndLingering() {
        return countSplashAndLingering;
    }

    public List<String> extraPotionItems() {
        return extraPotionItems;
    }

    public boolean higherLevelCountsForLower() {
        return higherLevelCountsForLower;
    }

    /** Highest level an endless effect may have (1 = level I); 0 means no limit. */
    public int maxLevel() {
        return maxLevel;
    }

    /** How many endless effects a player may have at once; 0 means no limit. */
    public int maxEndlessEffects() {
        return maxEndlessEffects;
    }

    public boolean allowHarmfulEffects() {
        return allowHarmfulEffects;
    }

    public List<String> blacklist() {
        return blacklist;
    }

    public List<String> whitelist() {
        return whitelist;
    }

    public List<String> alwaysAllowed() {
        return alwaysAllowed;
    }

    public boolean showParticles() {
        return showParticles;
    }

    public boolean showIcon() {
        return showIcon;
    }

    public boolean removeOnLogout() {
        return removeOnLogout;
    }

    public boolean applyInSpectator() {
        return applyInSpectator;
    }

    public boolean allowPause() {
        return allowPause;
    }

    public int checkIntervalTicks() {
        return checkIntervalTicks;
    }

    private static int readInt(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Integer i) {
            return i;
        }
        throw new IllegalArgumentException("'" + key + "' must be a whole number");
    }

    private static boolean readBool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        throw new IllegalArgumentException("'" + key + "' must be true or false");
    }

    private static List<String> readIds(Map<String, Object> values, String key, List<String> fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("'" + key + "' must be a list like [\"minecraft:speed\"]");
        }
        List<String> ids = new ArrayList<>();
        for (Object item : list) {
            ids.add(normalizeId((String) item));
        }
        return List.copyOf(ids);
    }

    private static Map<String, Integer> readOverrides(Map<String, Object> values) {
        Map<String, Integer> overrides = new LinkedHashMap<>();
        for (String entry : readIds(values, "threshold_overrides", List.of())) {
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("'threshold_overrides' entries look like \"minecraft:regeneration=32\", got " + entry);
            }
            try {
                overrides.put(normalizeId(entry.substring(0, eq)), Math.max(1, Integer.parseInt(entry.substring(eq + 1).trim())));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'threshold_overrides': '" + entry + "' needs a number after '='");
            }
        }
        return Map.copyOf(overrides);
    }

    static String defaultFileContents() {
        return """
                # Ender Brews config.
                # Keep enough potions of the same kind in your Ender Chest and their effect becomes endless.
                # Reload without restarting: /enderbrews reload
                # Ids look like "minecraft:speed". Where noted, "#namespace:tag" works too (mod only, not on Bukkit).

                # How many potions of one effect and level make it endless (1 or more).
                threshold = 8

                # Own threshold for some effects, e.g. ["minecraft:regeneration=32", "minecraft:strength=16"].
                threshold_overrides = []

                # Also count potions inside shulker boxes that sit in the Ender Chest.
                count_shulker_boxes = true

                # Also count splash and lingering potions (by default only drinkable potions count).
                count_splash_and_lingering = false

                # Extra items that hold potion effects and should count, e.g. potions from other mods.
                # Item ids or "#namespace:tag".
                extra_potion_items = []

                # A stronger potion also counts for the weaker levels of the same effect:
                # 4x Strength II + 4x Strength I = endless Strength I (with threshold 8).
                higher_level_counts_for_lower = true

                # Highest level an endless effect can have (1 = level I, 2 = level II, ...). 0 = no limit.
                max_level = 0

                # How many endless effects one player can have at once. 0 = no limit.
                max_endless_effects = 0

                # Allow harmful effects (poison, weakness, slowness...) to become endless.
                allow_harmful_effects = false

                # Effects that can never become endless. Effect ids or "#namespace:tag".
                blacklist = ["minecraft:infested", "minecraft:oozing", "minecraft:weaving", "minecraft:wind_charged"]

                # If not empty, ONLY these effects can become endless. Effect ids or "#namespace:tag".
                whitelist = []

                # Effects allowed even though the game calls them harmful. Effect ids or "#namespace:tag".
                always_allowed = []

                # Show potion particles around players with endless effects.
                show_particles = false

                # Show the effect icon (with "∞") on screen.
                show_icon = true

                # Take endless effects away when a player leaves and give them back on join.
                # Keeps worlds clean if the mod is ever removed.
                remove_on_logout = true

                # Give endless effects to players in spectator mode.
                apply_in_spectator = false

                # Let players pause their own endless effects with /enderbrews.
                allow_pause = true

                # How often (in ticks, 20 = 1 second) Ender Chests are counted again.
                check_interval_ticks = 20
                """;
    }
}
