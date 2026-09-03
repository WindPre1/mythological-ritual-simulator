package dev.practice.ritual.ritual;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public enum GriffinRarity {
    COMMON("Celestial", List.of(MythoKind.HUNTER, MythoKind.LYNX)),
    UNCOMMON("Blessed", List.of(MythoKind.HUNTER, MythoKind.LYNX, MythoKind.NYMPH, MythoKind.BULL)),
    RARE("Stalwart", List.of(MythoKind.HUNTER, MythoKind.LYNX, MythoKind.NYMPH, MythoKind.BULL,
            MythoKind.HARPY, MythoKind.GAIA)),
    EPIC("Venerable", List.of(MythoKind.HUNTER, MythoKind.LYNX, MythoKind.NYMPH, MythoKind.BULL,
            MythoKind.HARPY, MythoKind.GAIA, MythoKind.MINOTAUR, MythoKind.CHAMPION)),
    LEGENDARY("Exalted", List.of(MythoKind.HUNTER, MythoKind.LYNX, MythoKind.NYMPH, MythoKind.BULL,
            MythoKind.HARPY, MythoKind.GAIA, MythoKind.MINOTAUR, MythoKind.CHAMPION,
            MythoKind.INQUISITOR, MythoKind.SPHINX)),
    MYTHIC("Empyrean", List.of(MythoKind.values()));

    public final String prefix;
    private final List<MythoKind> pool;

    GriffinRarity(String prefix, List<MythoKind> pool) {
        this.prefix = prefix;
        this.pool = pool;
    }

    public List<MythoKind> pool() {
        return pool;
    }

    /**
     * Weighted roll using Hypixel wiki weights.
     * Elusive mobs (inq/sphinx/manti/king) can be scaled by {@code elusiveMult}.
     */
    public MythoKind roll(double elusiveMult) {
        List<MythoKind> pool = MYTHIC.pool;
        int total = 0;
        int[] w = new int[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            MythoKind k = pool.get(i);
            int weight = k.weight;
            if (k.elusive() && elusiveMult > 1.0) {
                weight = Math.max(1, (int) Math.round(weight * elusiveMult));
            }
            w[i] = weight;
            total += weight;
        }
        if (total <= 0) return pool.get(0);
        int r = ThreadLocalRandom.current().nextInt(total);
        for (int i = 0; i < pool.size(); i++) {
            r -= w[i];
            if (r < 0) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    public MythoKind roll() {
        return roll(1.0);
    }
}
