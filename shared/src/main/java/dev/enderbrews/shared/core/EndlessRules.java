package dev.enderbrews.shared.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/** Turns a {@link PotionTally} into the list of effects a player has or is working towards. */
public final class EndlessRules {

    private static final Comparator<EffectStatus> DISPLAY_ORDER = Comparator
            .comparing(EffectStatus::state)
            .thenComparing(Comparator.comparingInt(EffectStatus::count).reversed())
            .thenComparing(EffectStatus::effectId);

    private EndlessRules() {
    }

    public static List<EffectStatus> evaluate(PotionTally tally, EnderBrewsConfig config, EffectInfo info) {
        List<EffectStatus> result = new ArrayList<>();
        for (Map.Entry<String, NavigableMap<Integer, Integer>> entry : tally.counts().entrySet()) {
            result.add(evaluateEffect(entry.getKey(), entry.getValue(), config, info));
        }
        applyLimit(result, config.maxEndlessEffects());
        result.sort(DISPLAY_ORDER);
        return result;
    }

    private static EffectStatus evaluateEffect(String effectId, NavigableMap<Integer, Integer> levels,
                                               EnderBrewsConfig config, EffectInfo info) {
        int threshold = config.thresholdFor(effectId);
        boolean blocked = !isAllowed(effectId, config, info);

        // Walk from the strongest level down. With higher_level_counts_for_lower the counts add up,
        // so the first level that reaches the threshold is the strongest one the player earned.
        int running = 0;
        int bestProgressAmplifier = levels.lastKey();
        int bestProgressCount = 0;
        for (Map.Entry<Integer, Integer> level : levels.descendingMap().entrySet()) {
            int count = config.higherLevelCountsForLower() ? (running += level.getValue()) : level.getValue();
            if (count >= threshold) {
                int amplifier = config.maxLevel() > 0 ? Math.min(level.getKey(), config.maxLevel() - 1) : level.getKey();
                return new EffectStatus(effectId, amplifier, count, threshold,
                        blocked ? EffectStatus.State.BLOCKED : EffectStatus.State.ACTIVE);
            }
            if (count > bestProgressCount) {
                bestProgressCount = count;
                bestProgressAmplifier = level.getKey();
            }
        }
        return new EffectStatus(effectId, bestProgressAmplifier, bestProgressCount, threshold,
                blocked ? EffectStatus.State.BLOCKED : EffectStatus.State.PROGRESS);
    }

    static boolean isAllowed(String effectId, EnderBrewsConfig config, EffectInfo info) {
        if (!info.exists(effectId) || info.isInstant(effectId)) {
            return false;
        }
        if (!config.whitelist().isEmpty() && config.whitelist().stream().noneMatch(e -> info.matches(effectId, e))) {
            return false;
        }
        if (config.blacklist().stream().anyMatch(e -> info.matches(effectId, e))) {
            return false;
        }
        return !info.isHarmful(effectId)
                || config.allowHarmfulEffects()
                || config.alwaysAllowed().stream().anyMatch(e -> info.matches(effectId, e));
    }

    private static void applyLimit(List<EffectStatus> statuses, int maxEndless) {
        if (maxEndless <= 0) {
            return;
        }
        List<EffectStatus> active = statuses.stream()
                .filter(s -> s.state() == EffectStatus.State.ACTIVE)
                .sorted(DISPLAY_ORDER)
                .toList();
        for (EffectStatus over : active.subList(Math.min(maxEndless, active.size()), active.size())) {
            statuses.set(statuses.indexOf(over), new EffectStatus(over.effectId(), over.amplifier(), over.count(),
                    over.threshold(), EffectStatus.State.LIMITED));
        }
    }
}
