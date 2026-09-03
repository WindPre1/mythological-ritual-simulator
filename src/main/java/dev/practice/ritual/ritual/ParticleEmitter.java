package dev.practice.ritual.ritual;

import dev.practice.ritual.RitualPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Packet fingerprints from SBO ParticleTypes.kt / PreciseGuessBurrow.kt / ArrowGuessBurrow.kt
 */
public final class ParticleEmitter {

    private ParticleEmitter() {}

    public static void emitClose(Player player, Burrow burrow) {
        Location at = burrow.particleOrigin();
        Packets.particle(player, Particle.ENCHANT, at, 5, 0.5, 0.4, 0.5, 0.05);
        Packets.particle(player, Particle.CRIT, at, 1, 0.05, 0.0, 0.05, 0.0);
        switch (burrow.type) {
            case START -> Packets.particle(player, Particle.ENCHANTED_HIT, at, 4, 0.5, 0.1, 0.5, 0.01);
            case MOB -> Packets.particle(player, Particle.CRIT, at, 3, 0.5, 0.1, 0.5, 0.01);
            case TREASURE -> Packets.particle(player, Particle.DRIPPING_LAVA, at, 2, 0.35, 0.1, 0.35, 0.01);
        }
    }

    /**
     * Hypixel Echo is a cubic bezier that SBO's PreciseGuessBurrow inverts:
     *   P0 = eye
     *   look pitch θ, particle pitch = atan2(sin(θ)-0.75, cos(θ))
     *   d = sqrt(24*sin(θ-π)+25)
     *   P1 = P0 + d * dir(modified pitch)
     *   P2 = P3 = burrow (x+0.5, y+1.0, z+0.5)
     *
     * SBO fits x(i),y(i),z(i) as cubics in particle index, then evaluates at
     * t = 3d / |P'(0)| which equals the last index when samples are uniform in u.
     * Then down(0.5).roundToBlock() lands on the grass.
     *
     * Consecutive samples must be <= 3 blocks (SBO drops farther ones).
     * At most 12 plings.
     */
    public static void emitEcho(RitualPlugin plugin, Player player, Location targetBlock) {
        Location p0 = player.getEyeLocation().clone();
        Location p3 = targetBlock.clone().add(0.5, 1.0, 0.5);

        double theta = Math.toRadians(player.getLocation().getPitch());
        double yaw = Math.toRadians(player.getLocation().getYaw());
        double modPitch = Math.atan2(Math.sin(theta) - 0.75, Math.cos(theta));
        double cp = Math.cos(modPitch);
        Vector dir = new Vector(-Math.sin(yaw) * cp, -Math.sin(modPitch), Math.cos(yaw) * cp);
        if (dir.lengthSquared() < 1e-8) dir = new Vector(0, -1, 0);
        dir.normalize();

        double d = Math.sqrt(24.0 * Math.sin(theta - Math.PI) + 25.0);
        if (Double.isNaN(d) || d < 0.5) d = 5.0;

        Vector p0v = p0.toVector();
        Vector p1v = p0v.clone().add(dir.clone().multiply(d));
        Vector p3v = p3.toVector();
        Vector p2v = p3v.clone();

        double dist = p0v.distance(p3v);
        int steps = Math.max(12, Math.min(80, (int) Math.ceil(Math.max(dist, 8) / 1.7)));
        // 120+ → 12 sounds, every 10 blocks closer → 1 less, within 10 → 1. Every 3 ticks.
        int maxPlings = Math.max(1, Math.min(12, (int) Math.ceil(dist / 10.0)));
        int beamTicks = Math.max(1, (maxPlings - 1) * 3 + 1);
        RitualManager.PlayerSession session = plugin.rituals().session(player);
        long lockMs = Math.max(1000L, beamTicks * 50L);
        session.echoBusyUntil = System.currentTimeMillis() + lockMs;
        session.lastEcho = System.currentTimeMillis();

        new BukkitRunnable() {
            int tick = 0;
            int emitted = 0;
            int plings = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    session.echoBusyUntil = 0;
                    cancel();
                    return;
                }
                int remainingTicks = Math.max(1, beamTicks - tick);
                int remainingPts = Math.max(0, steps - emitted);
                int n = Math.max(1, (int) Math.ceil(remainingPts / (double) remainingTicks));
                for (int k = 0; k < n && emitted < steps; k++, emitted++) {
                    double u = steps <= 1 ? 1.0 : emitted / (double) (steps - 1);
                    Location at = bezier(p0v, p1v, p2v, p3v, u).toLocation(p0.getWorld());
                    Packets.particle(player, Particle.DRIPPING_LAVA, at, 2, 0.0, 0.0, 0.0, -0.5);
                }

                if (plings < maxPlings && tick % 3 == 0) {
                    float t = plings / (float) Math.max(1, maxPlings - 1);
                    RitualSounds.pling(player, 0.5f + t * 1.5f);
                    plings++;
                }
                tick++;
                // Stop drawing once every echo sound has played.
                if (plings >= maxPlings) {
                    cancel();
                    return;
                }
                if (tick >= beamTicks) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static Vector bezier(Vector p0, Vector p1, Vector p2, Vector p3, double u) {
        double o = 1.0 - u;
        double o2 = o * o;
        double o3 = o2 * o;
        double u2 = u * u;
        double u3 = u2 * u;
        return p0.clone().multiply(o3)
                .add(p1.clone().multiply(3 * o2 * u))
                .add(p2.clone().multiply(3 * o * u2))
                .add(p3.clone().multiply(u3));
    }

    /**
     * SBO ArrowGuessBurrow (SkyHanni PR 4916), exact contract:
     *
     * Packet: DUST, count=0, maxSpeed=1.0, DustParticleOptions (color ignored).
     * Offsets xDist/yDist/zDist encode distance range:
     *   (0,128,0) → 0..117, (255,255,0) → 112..282, (255,0,0) → 281..600
     *
     * Collects particle XYZ. Builds a collinear shaft of SHAFT_LENGTH=20 points
     * with consecutive spacing ≤ 0.12 (EPSILON cross² < 1e-6).
     * line[1] must have 2 neighbors, line[18] must have 4 (tip cluster).
     * Ray = base.down(1.5) → tip.down(1.5); origin must sit inside HUB_BOUNDS.
     *
     * We spawn at block Y+2.0 so down(1.5) is the grass CENTER — the ray then
     * scores the true target at perp=0, which is how SBO picks among candidates.
     */
    public static void emitArrow(Player player, Location fromBlock, Location toBlock) {
        Location origin = fromBlock.clone().add(0.5, 2.0, 0.5);
        Location dest = toBlock.clone().add(0.5, 2.0, 0.5);
        Vector to = dest.toVector().subtract(origin.toVector());
        double dist = to.length();
        if (dist < 1) return;
        Vector dir = to.normalize();

        float ox, oy, oz;
        Color color;
        if (dist <= 117) {
            ox = 0f; oy = 128f; oz = 0f;
            color = Color.fromRGB(0, 128, 0);
        } else if (dist <= 282) {
            ox = 255f; oy = 255f; oz = 0f;
            color = Color.fromRGB(255, 255, 0);
        } else {
            ox = 255f; oy = 0f; oz = 0f;
            color = Color.fromRGB(255, 0, 0);
        }

        final int shaft = 20;
        final double spacing = 0.10;
        Location[] points = new Location[shaft];
        for (int i = 0; i < shaft; i++) {
            points[i] = origin.clone().add(dir.getX() * (i * spacing),
                    dir.getY() * (i * spacing),
                    dir.getZ() * (i * spacing));
            Packets.dust(player, points[i], ox, oy, oz, color);
        }

        Vector perp = dir.clone().crossProduct(new Vector(0, 1, 0));
        if (perp.lengthSquared() < 1e-8) perp = dir.clone().crossProduct(new Vector(1, 0, 0));
        perp.normalize().multiply(0.02);

        Location nearTip = points[shaft - 2];
        Packets.dust(player, nearTip.clone().add(perp), ox, oy, oz, color);
        Packets.dust(player, nearTip.clone().subtract(perp), ox, oy, oz, color);
    }

    /**
     * SBO PacketReceiveEvent close-burrow fingerprint:
     * {@code LARGE_SMOKE && maxSpeed == 0.01f && xDist == yDist == zDist == 0}.
     * Position is block-center +1y; SBO does {@code roundLocationToBlock().down()}.
     */
    public static void emitRemoved(Player player, Location at) {
        Packets.particle(player, Particle.LARGE_SMOKE, at.clone().add(0.5, 1.0, 0.5), 1, 0.0, 0.0, 0.0, 0.01);
    }
}
