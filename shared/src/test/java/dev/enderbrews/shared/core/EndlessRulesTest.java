package dev.enderbrews.shared.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EndlessRulesTest {

    /** A tiny stand-in for the registry: poison and weakness are harmful, healing is instant. */
    private static final EffectInfo EFFECTS = new EffectInfo() {
        private final Set<String> known = Set.of("minecraft:strength", "minecraft:speed", "minecraft:poison",
                "minecraft:weakness", "minecraft:instant_health", "minecraft:resistance", "minecraft:slowness",
                "minecraft:oozing", "minecraft:regeneration");

        @Override
        public boolean exists(String effectId) {
            return known.contains(effectId);
        }

        @Override
        public boolean isHarmful(String effectId) {
            return Set.of("minecraft:poison", "minecraft:weakness", "minecraft:slowness", "minecraft:oozing").contains(effectId);
        }

        @Override
        public boolean isInstant(String effectId) {
            return effectId.equals("minecraft:instant_health");
        }

        @Override
        public boolean matches(String effectId, String entry) {
            return entry.equals(effectId);
        }
    };

    private static PotionTally tally(Object... effectAmplifierCount) {
        PotionTally tally = new PotionTally();
        for (int i = 0; i < effectAmplifierCount.length; i += 3) {
            for (int n = 0; n < (int) effectAmplifierCount[i + 2]; n++) {
                tally.addPotion(Map.of((String) effectAmplifierCount[i], (int) effectAmplifierCount[i + 1]));
            }
        }
        return tally;
    }

    private static EffectStatus only(List<EffectStatus> statuses) {
        assertEquals(1, statuses.size());
        return statuses.get(0);
    }

    @Test
    void reachingTheThresholdMakesTheEffectEndless() {
        EffectStatus status = only(EndlessRules.evaluate(tally("minecraft:strength", 0, 8), EnderBrewsConfig.defaults(), EFFECTS));
        assertEquals(EffectStatus.State.ACTIVE, status.state());
        assertEquals(0, status.amplifier());
    }

    @Test
    void belowTheThresholdShowsProgress() {
        EffectStatus status = only(EndlessRules.evaluate(tally("minecraft:strength", 0, 5), EnderBrewsConfig.defaults(), EFFECTS));
        assertEquals(EffectStatus.State.PROGRESS, status.state());
        assertEquals(5, status.count());
        assertEquals(8, status.threshold());
    }

    @Test
    void strongerPotionsCountForWeakerLevels() {
        EffectStatus status = only(EndlessRules.evaluate(tally("minecraft:strength", 1, 4, "minecraft:strength", 0, 4),
                EnderBrewsConfig.defaults(), EFFECTS));
        assertEquals(EffectStatus.State.ACTIVE, status.state());
        assertEquals(0, status.amplifier());
    }

    @Test
    void theStrongestEarnedLevelWins() {
        EffectStatus status = only(EndlessRules.evaluate(tally("minecraft:strength", 1, 8, "minecraft:strength", 0, 8),
                EnderBrewsConfig.defaults(), EFFECTS));
        assertEquals(1, status.amplifier());
    }

    @Test
    void levelsCountSeparatelyWhenConfigured() {
        EnderBrewsConfig config = EnderBrewsConfig.parse("higher_level_counts_for_lower = false");
        EffectStatus status = only(EndlessRules.evaluate(tally("minecraft:strength", 1, 4, "minecraft:strength", 0, 4), config, EFFECTS));
        assertEquals(EffectStatus.State.PROGRESS, status.state());
        assertEquals(4, status.count());
    }

    @Test
    void harmfulInstantAndBlacklistedEffectsAreBlocked() {
        List<EffectStatus> statuses = EndlessRules.evaluate(tally("minecraft:poison", 0, 8, "minecraft:instant_health", 0, 8,
                "minecraft:oozing", 0, 8), EnderBrewsConfig.defaults(), EFFECTS);
        assertEquals(3, statuses.size());
        assertTrue(statuses.stream().allMatch(s -> s.state() == EffectStatus.State.BLOCKED));
    }

    @Test
    void turtleMasterOnlyGivesItsGoodHalf() {
        PotionTally tally = new PotionTally();
        for (int i = 0; i < 8; i++) {
            tally.addPotion(Map.of("minecraft:slowness", 3, "minecraft:resistance", 2));
        }
        List<EffectStatus> statuses = EndlessRules.evaluate(tally, EnderBrewsConfig.defaults(), EFFECTS);
        assertEquals(EffectStatus.State.ACTIVE, statuses.get(0).state());
        assertEquals("minecraft:resistance", statuses.get(0).effectId());
        assertEquals(EffectStatus.State.BLOCKED, statuses.get(1).state());
    }

    @Test
    void harmfulEffectsCanBeAllowed() {
        EnderBrewsConfig config = EnderBrewsConfig.parse("always_allowed = [\"weakness\"]");
        EffectStatus status = only(EndlessRules.evaluate(tally("minecraft:weakness", 0, 8), config, EFFECTS));
        assertEquals(EffectStatus.State.ACTIVE, status.state());
    }

    @Test
    void whitelistAllowsOnlyListedEffects() {
        EnderBrewsConfig config = EnderBrewsConfig.parse("whitelist = [\"minecraft:speed\"]");
        List<EffectStatus> statuses = EndlessRules.evaluate(tally("minecraft:speed", 0, 8, "minecraft:strength", 0, 8), config, EFFECTS);
        assertEquals("minecraft:speed", statuses.get(0).effectId());
        assertEquals(EffectStatus.State.ACTIVE, statuses.get(0).state());
        assertEquals(EffectStatus.State.BLOCKED, statuses.get(1).state());
    }

    @Test
    void thresholdOverridesApplyPerEffect() {
        EnderBrewsConfig config = EnderBrewsConfig.parse("threshold = 2\nthreshold_overrides = [\"minecraft:regeneration=32\"]");
        List<EffectStatus> statuses = EndlessRules.evaluate(tally("minecraft:regeneration", 0, 10, "minecraft:speed", 0, 2), config, EFFECTS);
        assertEquals("minecraft:speed", statuses.get(0).effectId());
        assertEquals(EffectStatus.State.ACTIVE, statuses.get(0).state());
        assertEquals(EffectStatus.State.PROGRESS, statuses.get(1).state());
        assertEquals(32, statuses.get(1).threshold());
    }

    @Test
    void maxLevelCapsTheLevel() {
        EnderBrewsConfig config = EnderBrewsConfig.parse("max_level = 1");
        EffectStatus status = only(EndlessRules.evaluate(tally("minecraft:strength", 1, 8), config, EFFECTS));
        assertEquals(EffectStatus.State.ACTIVE, status.state());
        assertEquals(0, status.amplifier());
    }

    @Test
    void maxEndlessEffectsKeepsTheBestStocked() {
        EnderBrewsConfig config = EnderBrewsConfig.parse("max_endless_effects = 1");
        List<EffectStatus> statuses = EndlessRules.evaluate(tally("minecraft:speed", 0, 8, "minecraft:strength", 0, 12), config, EFFECTS);
        assertEquals("minecraft:strength", statuses.get(0).effectId());
        assertEquals(EffectStatus.State.ACTIVE, statuses.get(0).state());
        assertEquals(EffectStatus.State.LIMITED, statuses.get(1).state());
    }

    @Test
    void thresholdOfOneWorks() {
        EnderBrewsConfig config = EnderBrewsConfig.parse("threshold = 1");
        assertEquals(EffectStatus.State.ACTIVE,
                only(EndlessRules.evaluate(tally("minecraft:speed", 0, 1), config, EFFECTS)).state());
    }

    @Test
    void defaultConfigFileParsesToTheDefaults() {
        EnderBrewsConfig config = EnderBrewsConfig.parse(EnderBrewsConfig.defaultFileContents());
        assertEquals(8, config.threshold());
        assertEquals(EnderBrewsConfig.defaults().blacklist(), config.blacklist());
        assertTrue(config.removeOnLogout());
    }

    @Test
    void brokenConfigIsReported() {
        assertThrows(IllegalArgumentException.class, () -> EnderBrewsConfig.parse("threshold = lots"));
        assertThrows(IllegalArgumentException.class, () -> EnderBrewsConfig.parse("blacklist = [\"a\""));
    }

    @Test
    void multiLineArraysAndTagsParse() {
        EnderBrewsConfig config = EnderBrewsConfig.parse("blacklist = [\n  \"minecraft:speed\", # fast\n  \"#c:bad\"\n]");
        assertEquals(List.of("minecraft:speed", "#c:bad"), config.blacklist());
    }
}
