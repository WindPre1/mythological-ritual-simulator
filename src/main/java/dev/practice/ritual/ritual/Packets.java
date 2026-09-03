package dev.practice.ritual.ritual;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Always send with force=true so SBO's packet listener receives the fingerprint
 * even when the player is away / particle quality is low.
 */
public final class Packets {
    private Packets() {}

    public static void particle(Player player, Particle particle, Location at,
                                int count, double ox, double oy, double oz, double speed) {
        particle(player, particle, at, count, ox, oy, oz, speed, null);
    }

    public static <T> void particle(Player player, Particle particle, Location at,
                                    int count, double ox, double oy, double oz, double speed, T data) {
        if (data != null) {
            player.spawnParticle(particle, at, count, ox, oy, oz, speed, data, true);
        } else {
            player.spawnParticle(particle, at, count, ox, oy, oz, speed, null, true);
        }
    }

    /**
     * SBO ArrowGuessBurrow.isRelevant:
     *   particle.type == DUST && count == 0 && maxSpeed == 1.0f && DustParticleOptions
     * Range is read from packet xDist/yDist/zDist, NOT from the dust color.
     */
    public static void dust(Player player, Location at, float ox, float oy, float oz, Color color) {
        Particle.DustOptions data = new Particle.DustOptions(color, 1.0f);
        player.spawnParticle(Particle.DUST, at, 0, ox, oy, oz, 1.0, data, true);
    }
}
