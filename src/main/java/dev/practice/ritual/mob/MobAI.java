package dev.practice.ritual.mob;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.ritual.GriffinRarity;
import dev.practice.ritual.ritual.MythoKind;
import dev.practice.ritual.ritual.RitualManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hypixel Mythological Ritual AI (wiki, 2026):
 * Hunter   — walks at the player (Revenant-fast). Slight knockback on melee.
 * Lynx     — angry-villager particles on the active cat; wrong target heals 5%.
 * Nymph    — slow walk; at 50% HP a 5x5 water grid they move faster in.
 * Bull     — rams with true knockback, then stands still 5s (still contact-damages).
 * Harpy    — JUMPS (~10 blocks) and slow-falls; arrows while airborne; stops at 50% HP.
 *            Arrow hits shred Intelligence/Vitality (mana + healing) up to 50%.
 * Gaia     — staged hit-shield 6/7/8, iron-cross lightning, throw, no iframes.
 * Minotaur — slow axe every 1.5s; Bleed +1 melee / +3 axe; 75×bleed every 2s;
 *            bleed resets after 5s idle; at 27 runs away until 0.
 * Champion — +3%/s damage to 600% then despawn (~200s); bow 8–16; lightning if
 *            it can't close to 10 blocks (jump-dodgeable).
 * Inquisitor — +6%/s to 600% despawn (~100s); lightning (jump-dodgeable);
 *            10% stat shred per player hit, max 40%.
 * Sphinx   — riddle on spawn; correct = treasure, wrong = +50% HP + bleed.
 * Manticore — fast melee; Fatal Sting (green particles → arrow barrage →
 *            heal-lock + death timer).
 * King     — 75-hit golden-fleece shield, rage meter, ram, rod trap, lightning,
 *            bow if out of range, 2 Champions every 12.5% HP, 10 min enrage.
 */
public final class MobAI extends BukkitRunnable {
    private final RitualPlugin plugin;

    public MobAI(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            RitualManager.PlayerSession s = plugin.rituals().session(player);
            if (s.stingUntil > 0 && now >= s.stingUntil && s.pendingMobs > 0) {
                player.sendMessage("§cFATAL STING! Time ran out.");
                s.lastHitKind = MythoKind.MANTICORE;
                s.stats.health = 0;
                s.stingUntil = 0;
                player.setHealth(0);
                continue;
            }
            if (s.pendingMobs <= 0) continue;
            maybeRescueVoid(plugin, s.pendingMob);
            if (s.pendingMob != null && !s.pendingMob.isDead()) {
                String twinId = s.pendingMob.getPersistentDataContainer()
                        .get(plugin.getKey("lynx-twin"), PersistentDataType.STRING);
                if (twinId != null) {
                    try {
                        Entity twin = plugin.getServer().getEntity(java.util.UUID.fromString(twinId));
                        if (twin instanceof LivingEntity living) maybeRescueVoid(plugin, living);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            for (Entity e : player.getNearbyEntities(64, 40, 64)) {
                if (!(e instanceof LivingEntity living) || living.isDead()) continue;
                String kn = living.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
                if (kn == null) continue;
                String owner = living.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
                if (owner != null && !owner.equals(player.getUniqueId().toString())) continue;
                MythoKind kind;
                try {
                    kind = MythoKind.valueOf(kn);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                tick(player, s, living, kind, now);
            }
        }
    }

    private void tick(Player player, RitualManager.PlayerSession s, LivingEntity mob, MythoKind kind, long now) {
        Long spawn = mob.getPersistentDataContainer().get(plugin.getKey("spawn-at"), PersistentDataType.LONG);
        if (spawn == null) {
            spawn = now;
            mob.getPersistentDataContainer().set(plugin.getKey("spawn-at"), PersistentDataType.LONG, now);
        }
        double lived = (now - spawn) / 1000.0;
        double dmgMult = 1.0;

        if (mob instanceof Mob m) {
            if (frozen(mob, now)) {
                m.setAware(false);
                freezeInPlace(mob);
                player.spawnParticle(Particle.SNOWFLAKE, mob.getLocation().add(0, 1.1, 0), 3, 0.25, 0.4, 0.25, 0);
            } else {
                m.setTarget(player);
                m.setAware(true);
                try {
                    m.getPathfinder().stopPathfinding();
                } catch (Throwable ignored) {
                }
            }
        }

        switch (kind) {
            case HARPY -> harpy(player, s, mob, now);
            case GAIA -> gaia(player, s, mob, now);
            case HUNTER -> hunter(player, s, mob, now);
            case LYNX -> lynx(player, s, mob, now);
            case BULL -> bull(player, s, mob, now);
            case MINOTAUR -> minotaur(player, s, mob, now);
            case CHAMPION -> {
                dmgMult = Math.min(7.0, 1.0 + 0.03 * lived);
                if (lived >= 200) {
                    despawn(s, mob, "§cThe Minos Champion grew too strong and vanished.");
                    return;
                }
                champion(player, s, mob, now);
            }
            case INQUISITOR -> {
                dmgMult = Math.min(7.0, 1.0 + 0.06 * lived);
                if (lived >= 100) {
                    despawn(s, mob, "§cThe Minos Inquisitor vanished. Griffin Burrow chain failed.");
                    return;
                }
                inquisitor(player, s, mob, now);
            }
            case SPHINX -> sphinx(player, s, mob, now);
            case MANTICORE -> manticore(player, s, mob, now);
            case KING -> {
                if (lived >= 600) {
                    Location g = groundUnder(player.getLocation());
                    player.getWorld().strikeLightningEffect(g.clone().add(0.5, 1, 0.5));
                    s.stats.health = 0;
                    s.lastHitKind = MythoKind.KING;
                    player.setHealth(0);
                    despawn(s, mob, "§cKing Minos' 10-minute timer expired.");
                    return;
                }
                king(player, s, mob, now);
            }
            case NYMPH -> nymph(player, s, mob, now);
            default -> circle(mob, player, 0.18);
        }

        mob.getPersistentDataContainer().set(plugin.getKey("dmg-mult"), PersistentDataType.DOUBLE, dmgMult);
    }

    public static double damageMult(RitualPlugin plugin, LivingEntity mob) {
        if (mob == null) return 1.0;
        Double m = mob.getPersistentDataContainer().get(plugin.getKey("dmg-mult"), PersistentDataType.DOUBLE);
        return m == null ? 1.0 : m;
    }

    public static void onArrowHit(RitualPlugin plugin, Player player, LivingEntity shooter, MythoKind kind) {
        RitualManager.PlayerSession s = plugin.rituals().session(player);
        switch (kind) {
            case HARPY -> {
                hurt(plugin, player, s, kind, 0.85, false);
                s.harpyShred = Math.min(0.50, s.harpyShred + 0.05);
            }
            case CHAMPION, INQUISITOR, KING -> hurt(plugin, player, s, kind, 0.9, false);
            case MINOTAUR -> {
                s.minoBleed += 3;
                s.minoBleedLastInc = System.currentTimeMillis();
                hurt(plugin, player, s, kind, 0.8, false);
            }
            case MANTICORE -> {
                boolean already = s.stingUntil > System.currentTimeMillis();
                Long spawnAt = shooter == null ? null
                        : shooter.getPersistentDataContainer().get(plugin.getKey("spawn-at"), PersistentDataType.LONG);
                if (spawnAt != null && System.currentTimeMillis() - spawnAt < 2000) return;
                hurt(plugin, player, s, kind, already ? 0.15 : 0.45, false);
                if (!already) {
                    s.healDisabledUntil = System.currentTimeMillis() + 45_000;
                    s.stingUntil = System.currentTimeMillis() + 45_000;
                    player.sendMessage("§c§lFATAL STING! §cYou were fatally stung by a Manticore! Kill it before the time runs out or die!");
                }
            }
            default -> hurt(plugin, player, s, kind, 0.7, false);
        }
    }

    /* ---------- kinds ---------- */

    private void hunter(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        circle(mob, player, 0.42);
        melee(player, s, mob, MythoKind.HUNTER, 2.6, 400, now, 1.0, false);
    }

    private void lynx(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        unstuck(mob);
        chase(mob, player, 0.38);
        Boolean active = mob.getPersistentDataContainer().get(plugin.getKey("lynx-active"), PersistentDataType.BOOLEAN);
        if (Boolean.TRUE.equals(active)) {
            mob.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, mob.getLocation().add(0, 1.1, 0), 2, 0.25, 0.2, 0.25, 0);
        }
        melee(player, s, mob, MythoKind.LYNX, 2.4, 500, now, 1.0, false);
    }

    private void harpy(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        if (frozen(mob, now)) {
            freezeInPlace(mob);
            melee(player, s, mob, MythoKind.HARPY, 2.5, 500, now, 1.0, false);
            return;
        }
        if (!mob.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, PotionEffect.INFINITE_DURATION, 0, true, false));
        }
        mob.setGravity(true);
        Double hp = mob.getPersistentDataContainer().get(plugin.getKey("sb-hp"), PersistentDataType.DOUBLE);
        Double max = mob.getPersistentDataContainer().get(plugin.getKey("sb-max"), PersistentDataType.DOUBLE);
        boolean canHop = hp != null && max != null && max > 0 && hp / max > 0.5;
        Integer phase = mob.getPersistentDataContainer().get(plugin.getKey("harpy-phase"), PersistentDataType.INTEGER);
        if (phase == null) phase = 0; // 0 ground, 1 rising, 2 falling
        Double originY = mob.getPersistentDataContainer().get(plugin.getKey("hop-y"), PersistentDataType.DOUBLE);
        Vector toPlayer = player.getLocation().toVector().subtract(mob.getLocation().toVector());
        double driftX = 0, driftZ = 0;
        if (toPlayer.lengthSquared() > 1) {
            toPlayer.setY(0).normalize();
            driftX = toPlayer.getX() * 0.18;
            driftZ = toPlayer.getZ() * 0.18;
        }

        if (phase == 1) {
            // Rise is driven by the 1-tick hop runnable (10 blocks / 5 ticks).
            shootMaybe(mob, player, now);
            return;
        }
        if (phase == 2) {
            Location loc = mob.getLocation().add(driftX, -0.18, driftZ);
            boolean landed = !loc.clone().subtract(0, 0.2, 0).getBlock().isPassable();
            if (landed || (originY != null && loc.getY() <= originY + 0.2)) {
                if (originY != null) loc.setY(originY);
                mob.getPersistentDataContainer().set(plugin.getKey("harpy-phase"), PersistentDataType.INTEGER, 0);
                mob.getPersistentDataContainer().set(plugin.getKey("harpy-hop"), PersistentDataType.LONG, now);
            }
            loc.setYaw(mob.getLocation().getYaw());
            loc.setPitch(mob.getLocation().getPitch());
            mob.teleport(loc);
            shootMaybe(mob, player, now);
            return;
        }

        if (canHop) {
            Long lastHop = mob.getPersistentDataContainer().get(plugin.getKey("harpy-hop"), PersistentDataType.LONG);
            if (lastHop == null || now - lastHop > 900) {
                startHarpyHop(mob);
            }
            return;
        }
        circle(mob, player, 0.26);
        melee(player, s, mob, MythoKind.HARPY, 2.5, 500, now, 1.0, false);
    }

    /** 10 blocks up in 5 ticks. */
    private void startHarpyHop(LivingEntity mob) {
        mob.getPersistentDataContainer().set(plugin.getKey("harpy-phase"), PersistentDataType.INTEGER, 1);
        mob.getPersistentDataContainer().set(plugin.getKey("hop-y"), PersistentDataType.DOUBLE, mob.getLocation().getY());
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 1.1f);
        mob.getWorld().spawnParticle(Particle.CLOUD, mob.getLocation(), 10, 0.3, 0.1, 0.3, 0.02);
        final double originY = mob.getLocation().getY();
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!mob.isValid() || mob.isDead()) {
                    cancel();
                    return;
                }
                Integer phase = mob.getPersistentDataContainer().get(plugin.getKey("harpy-phase"), PersistentDataType.INTEGER);
                if (phase == null || phase != 1) {
                    cancel();
                    return;
                }
                Long until = mob.getPersistentDataContainer().get(plugin.getKey("frozen-until"), PersistentDataType.LONG);
                if (until != null && System.currentTimeMillis() < until) {
                    mob.setVelocity(new Vector(0, 0, 0));
                    return;
                }
                ticks++;
                Location loc = mob.getLocation().clone();
                loc.setY(originY + 2.0 * ticks);
                if (ticks >= 5) {
                    loc.setY(originY + 10);
                    mob.getPersistentDataContainer().set(plugin.getKey("harpy-phase"), PersistentDataType.INTEGER, 2);
                    cancel();
                }
                loc.setYaw(mob.getLocation().getYaw());
                loc.setPitch(mob.getLocation().getPitch());
                mob.teleport(loc);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void shootMaybe(LivingEntity mob, Player player, long now) {
        Long lastArrow = mob.getPersistentDataContainer().get(plugin.getKey("harpy-arrow"), PersistentDataType.LONG);
        if (lastArrow == null || now - lastArrow > 700) {
            mob.getPersistentDataContainer().set(plugin.getKey("harpy-arrow"), PersistentDataType.LONG, now);
            shoot(mob, player, MythoKind.HARPY, 1.35, 1.4f);
        }
    }

    private void gaia(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        circle(mob, player, 0.20);
        melee(player, s, mob, MythoKind.GAIA, 3.4, 700, now, 1.0, false);

        double dist = player.getLocation().distance(mob.getLocation());
        Long lastThrow = mob.getPersistentDataContainer().get(plugin.getKey("gaia-throw"), PersistentDataType.LONG);
        if (dist < 4.5 && (lastThrow == null || now - lastThrow > 7000)) {
            mob.getPersistentDataContainer().set(plugin.getKey("gaia-throw"), PersistentDataType.LONG, now);
            Vector up = player.getLocation().toVector().subtract(mob.getLocation().toVector());
            if (up.lengthSquared() > 0.01) up.normalize().multiply(0.55); else up = new Vector(0, 0, 0);
            up.setY(1.15);
            player.setVelocity(up);
            hurt(plugin, player, s, MythoKind.GAIA, 1.35, false);
        }

        Long last = mob.getPersistentDataContainer().get(plugin.getKey("gaia-bolt"), PersistentDataType.LONG);
        if (last != null && now - last < 2000) return;
        mob.getPersistentDataContainer().set(plugin.getKey("gaia-bolt"), PersistentDataType.LONG, now);
        ironCrossLightning(player, mob, s, MythoKind.GAIA, 16L);
    }

    private void bull(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        if (frozen(mob, now)) {
            freezeInPlace(mob);
            melee(player, s, mob, MythoKind.BULL, 2.6, 600, now, 0.7, false);
            return;
        }
        Long stunUntil = mob.getPersistentDataContainer().get(plugin.getKey("bull-stun"), PersistentDataType.LONG);
        if (stunUntil != null && now < stunUntil) {
            mob.setVelocity(new Vector(0, mob.getVelocity().getY(), 0));
            melee(player, s, mob, MythoKind.BULL, 2.6, 600, now, 0.7, false);
            return;
        }
        Long spawnAt = mob.getPersistentDataContainer().get(plugin.getKey("spawn-at"), PersistentDataType.LONG);
        if (spawnAt != null && now - spawnAt < 1000) {
            circle(mob, player, 0.20);
            melee(player, s, mob, MythoKind.BULL, 2.6, 600, now, 0.7, false);
            return;
        }
        Long last = mob.getPersistentDataContainer().get(plugin.getKey("bull-charge"), PersistentDataType.LONG);
        if (last == null || now - last > 1500) {
            mob.getPersistentDataContainer().set(plugin.getKey("bull-charge"), PersistentDataType.LONG, now);
            Vector dir = player.getLocation().toVector().subtract(mob.getLocation().toVector());
            if (dir.lengthSquared() > 0.01) {
                dir.setY(0).normalize().multiply(1.35).setY(0.18);
                mob.setVelocity(dir);
                mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_COW_HURT, 1f, 0.55f);
            }
        }
        if (player.getLocation().distanceSquared(mob.getLocation()) < 6.25) {
            Long hitAt = mob.getPersistentDataContainer().get(plugin.getKey("bull-hit"), PersistentDataType.LONG);
            if (hitAt == null || now - hitAt > 1500) {
                mob.getPersistentDataContainer().set(plugin.getKey("bull-hit"), PersistentDataType.LONG, now);
                mob.getPersistentDataContainer().set(plugin.getKey("bull-stun"), PersistentDataType.LONG, now + 5000);
                Vector kb = player.getLocation().toVector().subtract(mob.getLocation().toVector());
                if (kb.lengthSquared() > 0.01) kb.normalize().multiply(1.4).setY(0.55);
                else kb = new Vector(0, 0.55, 0);
                player.setVelocity(kb);
                hurt(plugin, player, s, MythoKind.BULL, 1.4, false);
            }
        }
    }

    private void minotaur(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        if (frozen(mob, now)) {
            freezeInPlace(mob);
        }
        if (s.minoBleed > 0 && now - s.minoBleedLastInc > 5000) {
            s.minoBleed = 0;
        }
        if (s.minoBleed >= 27) {
            if (!frozen(mob, now)) {
                Vector away = mob.getLocation().toVector().subtract(player.getLocation().toVector());
                if (away.lengthSquared() > 0.01) {
                    away.setY(0).normalize().multiply(0.28);
                    mob.setVelocity(new Vector(away.getX(), mob.getVelocity().getY(), away.getZ()));
                }
            }
            if (s.minoBleedPulse == 0 || now - s.minoBleedPulse > 2000) {
                s.minoBleedPulse = now;
                s.minoBleed = Math.max(0, s.minoBleed - 3);
            }
            return;
        }
        circle(mob, player, 0.22);
        if (melee(player, s, mob, MythoKind.MINOTAUR, 2.8, 500, now, 1.0, false)) {
            s.minoBleed += 1;
            s.minoBleedLastInc = now;
        }
        Long lastAxe = mob.getPersistentDataContainer().get(plugin.getKey("axe"), PersistentDataType.LONG);
        if (lastAxe == null || now - lastAxe > 1500) {
            mob.getPersistentDataContainer().set(plugin.getKey("axe"), PersistentDataType.LONG, now);
            shoot(mob, player, MythoKind.MINOTAUR, 0.55, 0.4f);
            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f);
        }
        if (s.minoBleed > 0 && (s.minoBleedPulse == 0 || now - s.minoBleedPulse > 2000)) {
            s.minoBleedPulse = now;
            double raw = 75.0 * s.minoBleed;
            s.stats.health -= s.stats.taken(raw);
            player.playHurtAnimation(0f);
            applyHealth(player, s);
        }
    }

    private void champion(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        circle(mob, player, 0.34);
        double dist = player.getLocation().distance(mob.getLocation());
        if (dist <= 3.2) {
            melee(player, s, mob, MythoKind.CHAMPION, 3.2, 450, now, 1.0, false);
        } else if (dist > 8 && dist < 22) {
            Long last = mob.getPersistentDataContainer().get(plugin.getKey("champ-bow"), PersistentDataType.LONG);
            if (last == null || now - last > 900) {
                mob.getPersistentDataContainer().set(plugin.getKey("champ-bow"), PersistentDataType.LONG, now);
                shoot(mob, player, MythoKind.CHAMPION, 1.7, 1.2f);
            }
        }
        if (dist > 10) {
            Long last = mob.getPersistentDataContainer().get(plugin.getKey("champ-bolt"), PersistentDataType.LONG);
            if (last == null || now - last > 5000) {
                mob.getPersistentDataContainer().set(plugin.getKey("champ-bolt"), PersistentDataType.LONG, now);
                ironCrossLightning(player, mob, s, MythoKind.CHAMPION, 16L);
            }
        }
    }

    private void inquisitor(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        move(player, s, mob, now, 0.32, true);
        melee(player, s, mob, MythoKind.INQUISITOR, 2.8, 500, now, 1.0, false);
        Long last = mob.getPersistentDataContainer().get(plugin.getKey("inq-bolt"), PersistentDataType.LONG);
        if (last != null && now - last < 10_000) return;
        mob.getPersistentDataContainer().set(plugin.getKey("inq-bolt"), PersistentDataType.LONG, now);
        ironCrossLightning(player, mob, s, MythoKind.INQUISITOR, 12L);
    }

    private void sphinx(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        move(player, s, mob, now, 0.18, true);
        melee(player, s, mob, MythoKind.SPHINX, 2.8, 700, now, 1.0, false);
        if (s.sphinxInfuriated) {
            Long last = mob.getPersistentDataContainer().get(plugin.getKey("sphinx-bleed"), PersistentDataType.LONG);
            if (last == null || now - last > 2500) {
                mob.getPersistentDataContainer().set(plugin.getKey("sphinx-bleed"), PersistentDataType.LONG, now);
                if (player.getLocation().distanceSquared(mob.getLocation()) < 144) {
                    player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 14, 0.4, 0.5, 0.4, 0.02);
                    hurt(plugin, player, s, MythoKind.SPHINX, 0.55, false);
                }
            }
        }
    }

    private void manticore(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        move(player, s, mob, now, 0.40, true);
        Long spawn = mob.getPersistentDataContainer().get(plugin.getKey("spawn-at"), PersistentDataType.LONG);
        if (spawn != null && now - spawn < 2000) return;
        melee(player, s, mob, MythoKind.MANTICORE, 2.8, 500, now, 0.4, false);
        Long last = mob.getPersistentDataContainer().get(plugin.getKey("sting"), PersistentDataType.LONG);
        if (last != null && now - last < 8000) {
            if (now - last < 2500) {
                player.spawnParticle(Particle.HAPPY_VILLAGER, mob.getLocation().add(0, 1.8, 0), 8, 0.3, 0.3, 0.3, 0);
            }
            return;
        }
        if (player.getLocation().distance(mob.getLocation()) > 22) return;
        mob.getPersistentDataContainer().set(plugin.getKey("sting"), PersistentDataType.LONG, now);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (mob.isDead() || !player.isOnline()) return;
            for (int i = 0; i < 7; i++) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> shoot(mob, player, MythoKind.MANTICORE, 2.0, 0.8f), i);
            }
        }, 30L);
    }

    private void king(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        Integer shield = mob.getPersistentDataContainer().get(plugin.getKey("king-shield"), PersistentDataType.INTEGER);
        move(player, s, mob, now, shield != null && shield > 0 ? 0.18 : 0.24, true);
        double dist = player.getLocation().distance(mob.getLocation());
        // 150 true damage per hit (bypasses defense) + 2500 raw DPS while in melee range.
        // MobAI ticks every 2 ticks → 10 Hz, so 250 raw per tick = 2500 DPS.
        if (dist <= 3.6) {
            s.lastHitKind = MythoKind.KING;
            hurtAmount(plugin, player, s, 250.0, false, false);
            Long lastTrue = mob.getPersistentDataContainer().get(plugin.getKey("king-true"), PersistentDataType.LONG);
            if (lastTrue == null || now - lastTrue >= 500) {
                mob.getPersistentDataContainer().set(plugin.getKey("king-true"), PersistentDataType.LONG, now);
                hurtAmount(plugin, player, s, 150.0, true, true);
            }
        }

        Long lastRod = mob.getPersistentDataContainer().get(plugin.getKey("king-rod"), PersistentDataType.LONG);
        if (dist < 18 && dist > 3 && (lastRod == null || now - lastRod > 10000)) {
            mob.getPersistentDataContainer().set(plugin.getKey("king-rod"), PersistentDataType.LONG, now);
            Vector pull = mob.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.55).setY(0.15);
            player.setVelocity(pull);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 2, true, false));
        }

        if (dist > 14) {
            Long lastBolt = mob.getPersistentDataContainer().get(plugin.getKey("king-bolt"), PersistentDataType.LONG);
            if (lastBolt == null || now - lastBolt > 8000) {
                mob.getPersistentDataContainer().set(plugin.getKey("king-bolt"), PersistentDataType.LONG, now);
                ironCrossLightning(player, mob, s, MythoKind.KING, 16L);
            }
        }
    }

    private void nymph(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now) {
        Double hp = mob.getPersistentDataContainer().get(plugin.getKey("sb-hp"), PersistentDataType.DOUBLE);
        Double max = mob.getPersistentDataContainer().get(plugin.getKey("sb-max"), PersistentDataType.DOUBLE);
        boolean enraged = hp != null && max != null && max > 0 && hp / max <= 0.5;
        if (enraged && !s.nymphWater) {
            s.nymphWater = true;
            paintWater(player, s, mob.getLocation());
        }
        boolean inWater = s.nymphWater && inWaterGrid(s, mob.getLocation());
        circle(mob, player, inWater ? 0.40 : 0.12);
        melee(player, s, mob, MythoKind.NYMPH, 2.5, 550, now, 1.0, false);
    }

    /* ---------- helpers ---------- */

    private boolean frozen(LivingEntity mob, long now) {
        Long until = mob.getPersistentDataContainer().get(plugin.getKey("frozen-until"), PersistentDataType.LONG);
        return until != null && now < until;
    }

    /** Walk slowly out to orbit (~3.2) after spawn instead of snapping into the strafe. */
    private boolean emerge(LivingEntity mob, Player player, long now) {
        if (Boolean.TRUE.equals(mob.getPersistentDataContainer().get(plugin.getKey("emerged"), PersistentDataType.BOOLEAN))) {
            return false;
        }
        Long spawn = mob.getPersistentDataContainer().get(plugin.getKey("spawn-at"), PersistentDataType.LONG);
        Vector to = player.getLocation().toVector().subtract(mob.getLocation().toVector());
        double dist = Math.sqrt(to.getX() * to.getX() + to.getZ() * to.getZ());
        if (dist >= 3.15 || (spawn != null && now - spawn > 2200)) {
            mob.getPersistentDataContainer().set(plugin.getKey("emerged"), PersistentDataType.BOOLEAN, true);
            return false;
        }
        walkOut(mob, player, 0.09);
        return true;
    }

    private void walkOut(LivingEntity mob, Player player, double speed) {
        if (mob instanceof Mob m) m.setTarget(player);
        Vector away = mob.getLocation().toVector().subtract(player.getLocation().toVector());
        away.setY(0);
        if (away.lengthSquared() < 0.04) {
            int h = mob.getUniqueId().hashCode();
            double a = (h & 0xffff) / 65535.0 * Math.PI * 2;
            away = new Vector(Math.cos(a), 0, Math.sin(a));
        } else {
            away.normalize();
        }
        int side = (mob.getUniqueId().hashCode() & 1) == 0 ? 1 : -1;
        Vector perp = new Vector(-away.getZ(), 0, away.getX()).multiply(side * 0.25);
        Vector move = away.multiply(speed).add(perp.multiply(speed));
        applyWalk(mob, move);
    }

    private void freezeInPlace(LivingEntity mob) {
        mob.setVelocity(new Vector(0, Math.min(0, mob.getVelocity().getY()), 0));
    }

    private void move(Player player, RitualManager.PlayerSession s, LivingEntity mob, long now, double speed, boolean rareStrafe) {
        if (frozen(mob, now)) {
            freezeInPlace(mob);
            return;
        }
        circle(mob, player, speed);
    }

    /** Catch up in a straight line only if far; otherwise orbit so they aren't a free melee piñata. */
    private void circle(LivingEntity mob, Player player, double speed) {
        if (frozen(mob, System.currentTimeMillis())) {
            freezeInPlace(mob);
            return;
        }
        if (emerge(mob, player, System.currentTimeMillis())) return;
        Vector to = player.getLocation().toVector().subtract(mob.getLocation().toVector());
        double dist = Math.sqrt(to.getX() * to.getX() + to.getZ() * to.getZ());
        if (dist > 10) chase(mob, player, speed);
        else strafe(mob, player, speed);
    }

    private void strafe(LivingEntity mob, Player player, double speed) {
        if (mob instanceof Mob m) m.setTarget(player);
        Vector to = player.getLocation().toVector().subtract(mob.getLocation().toVector());
        double dist = Math.sqrt(to.getX() * to.getX() + to.getZ() * to.getZ());
        if (dist > 80) return;
        if (dist < 0.05) {
            dist = 0.05;
            to.setX(1);
            to.setZ(0);
        }
        Vector fwd = new Vector(to.getX(), 0, to.getZ()).normalize();
        Vector perp = new Vector(-fwd.getZ(), 0, fwd.getX());
        int side = (mob.getUniqueId().hashCode() & 1) == 0 ? 1 : -1;
        perp.multiply(side);
        double desired = 3.2;
        Vector move;
        if (dist > desired + 2) {
            move = fwd.clone().multiply(speed * 0.7).add(perp.multiply(speed * 0.9));
        } else if (dist < desired - 1.2) {
            move = fwd.clone().multiply(-speed * 0.55).add(perp.multiply(speed));
        } else {
            // Orbit with a slight inward drift so melee still connects as they pass.
            move = perp.multiply(speed * 1.05).add(fwd.multiply(speed * 0.12));
        }
        applyWalk(mob, move);
    }

    private void chase(LivingEntity mob, Player player, double speed) {
        if (frozen(mob, System.currentTimeMillis())) {
            freezeInPlace(mob);
            return;
        }
        if (mob instanceof Mob m) m.setTarget(player);
        Vector dir = player.getLocation().toVector().subtract(mob.getLocation().toVector());
        double d2 = dir.lengthSquared();
        if (d2 < 0.6 || d2 > 80 * 80) return;
        dir.setY(0);
        if (dir.lengthSquared() < 0.01) return;
        dir.normalize();
        applyWalk(mob, dir.clone().multiply(speed));
    }

    /** Don't walk into full cubes. If already inside one, warp to nearby air. */
    private void applyWalk(LivingEntity mob, Vector move) {
        unstuck(mob);
        double y = mob.getVelocity().getY();
        Vector horiz = new Vector(move.getX(), 0, move.getZ());
        if (horiz.lengthSquared() > 1e-6) {
            Location ahead = mob.getLocation().clone().add(horiz.clone().normalize().multiply(0.7));
            Block wall = ahead.getBlock();
            Block step = ahead.clone().add(0, 1, 0).getBlock();
            Block head = ahead.clone().add(0, 2, 0).getBlock();
            if (!passableColumn(ahead)) {
                Vector slide = new Vector(-horiz.getZ(), 0, horiz.getX());
                Location left = mob.getLocation().clone().add(slide.clone().normalize().multiply(0.7));
                Location right = mob.getLocation().clone().add(slide.clone().multiply(-1).normalize().multiply(0.7));
                if (passableColumn(left)) horiz = slide.normalize().multiply(horiz.length());
                else if (passableColumn(right)) horiz = slide.multiply(-1).normalize().multiply(horiz.length());
                else horiz = new Vector(0, 0, 0);
            } else if (!wall.isPassable() && step.isPassable() && head.isPassable() && grounded(mob) && y <= 0.05) {
                y = 0.45;
            }
        }
        mob.setVelocity(new Vector(horiz.getX(), y, horiz.getZ()));
    }

    private static boolean passableColumn(Location feet) {
        Block wall = feet.getBlock();
        Block head = feet.clone().add(0, 1, 0).getBlock();
        return wall.isPassable() && head.isPassable();
    }

    private void unstuck(LivingEntity mob) {
        Location loc = mob.getLocation();
        if (passableColumn(loc)) return;
        org.bukkit.World w = loc.getWorld();
        if (w == null) return;
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        for (int r = 1; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        Location tryAt = new Location(w, bx + dx + 0.5, by + dy, bz + dz + 0.5);
                        if (passableColumn(tryAt)) {
                            mob.teleport(tryAt);
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean melee(Player player, RitualManager.PlayerSession s, LivingEntity mob,
                          MythoKind kind, double reach, long cdMs, long now, double mult, boolean trueDmg) {
        if (player.getLocation().distanceSquared(mob.getLocation()) > 16.0) return false;
        Long last = mob.getPersistentDataContainer().get(plugin.getKey("melee-at"), PersistentDataType.LONG);
        if (last != null && now - last < cdMs) return false;
        mob.getPersistentDataContainer().set(plugin.getKey("melee-at"), PersistentDataType.LONG, now);
        s.lastMobHitPlayer = now;
        hurt(plugin, player, s, kind, mult * damageMult(plugin, mob), trueDmg);
        return true;
    }

    private void shoot(LivingEntity from, Player to, MythoKind kind, double speed, float spread) {
        Location eye = from.getLocation().add(0, 1.4, 0);
        Vector dir = to.getEyeLocation().toVector().subtract(eye.toVector());
        if (dir.lengthSquared() < 0.01) return;
        Arrow arrow = from.getWorld().spawnArrow(eye, dir.normalize(), (float) speed, spread);
        arrow.setShooter(from);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.getPersistentDataContainer().set(plugin.getKey("mytho-shot"), PersistentDataType.STRING, kind.name());
        String ownerId = from.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
        if (ownerId != null) {
            Player owner = plugin.getServer().getPlayer(java.util.UUID.fromString(ownerId));
            if (owner != null) MobFactory.revealToOwner(plugin, arrow, owner);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, arrow::remove, 50L);
    }

    private void ironCrossLightning(Player owner, LivingEntity mob, RitualManager.PlayerSession s, MythoKind kind, long delay) {
        Location origin = mob == null || mob.isDead() ? owner.getLocation() : mob.getLocation();
        Player focus = randomPlayerWithin(origin, 10.0, owner);
        Location ground = groundUnder(focus.getLocation());
        Location base = ground.getBlock().getLocation();
        int[][] offs = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        List<Location> cells = new ArrayList<>();
        List<Player> watchers = playersWithin(origin, 24.0);
        for (int[] o : offs) {
            Location at = base.clone().add(o[0], 0, o[1]);
            cells.add(at);
            for (Player p : watchers) p.sendBlockChange(at, Material.IRON_BLOCK.createBlockData());
        }
        for (Player p : watchers) p.playSound(base, Sound.BLOCK_ANVIL_PLACE, 0.45f, 0.5f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Location at : cells) {
                for (Player p : watchers) {
                    if (p.isOnline()) p.sendBlockChange(at, at.getBlock().getBlockData());
                }
            }
            if (!focus.isOnline()) return;
            Location strike = base.clone().add(0.5, 1.0, 0.5);
            if (strike.getWorld() != null) strike.getWorld().strikeLightningEffect(strike);
            Location feet = focus.getLocation();
            boolean onCross = false;
            for (Location at : cells) {
                if (feet.getBlockX() == at.getBlockX() && feet.getBlockZ() == at.getBlockZ()) {
                    onCross = true;
                    break;
                }
            }
            if (grounded(focus) && onCross) {
                RitualManager.PlayerSession hitS = plugin.rituals().session(focus);
                hitS.lastHitKind = kind;
                hurtAmount(plugin, focus, hitS, 15_000.0, false, true);
                if (kind.rare()) splashRare(plugin, focus, kind, 15_000.0, false);
            }
        }, delay);
    }

    static Player randomPlayerWithin(Location origin, double radius, Player fallback) {
        List<Player> list = playersWithin(origin, radius);
        if (list.isEmpty()) return fallback;
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    static List<Player> playersWithin(Location origin, double radius) {
        List<Player> out = new ArrayList<>();
        if (origin.getWorld() == null) return out;
        double r2 = radius * radius;
        for (Player p : origin.getWorld().getPlayers()) {
            if (!p.isOnline() || p.isDead()) continue;
            if (p.getLocation().distanceSquared(origin) <= r2) out.add(p);
        }
        return out;
    }

    static void splashRare(RitualPlugin plugin, Player origin, MythoKind kind, double raw, boolean trueDmg) {
        if (!kind.rare()) return;
        for (Player p : playersWithin(origin.getLocation(), 4.0)) {
            if (p.getUniqueId().equals(origin.getUniqueId())) continue;
            RitualManager.PlayerSession os = plugin.rituals().session(p);
            os.lastHitKind = kind;
            hurtAmount(plugin, p, os, raw, trueDmg, true);
        }
    }

    static Location groundUnder(Location loc) {
        org.bukkit.World w = loc.getWorld();
        if (w == null) return loc;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int y = loc.getBlockY();
        int min = w.getMinHeight() + 1;
        while (y > min && w.getBlockAt(x, y, z).isPassable()) {
            y--;
        }
        return new Location(w, x, y, z);
    }

    private void paintWater(Player player, RitualManager.PlayerSession s, Location origin) {
        s.nymphCells.clear();
        Block originBlock = origin.getBlock();
        int y = originBlock.getY();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block b = origin.getWorld().getBlockAt(originBlock.getX() + dx, y, originBlock.getZ() + dz);
                if (!b.getType().isAir() && !b.isPassable()) {
                    b = b.getRelative(0, 1, 0);
                }
                Location at = b.getLocation();
                s.nymphCells.add(at);
                player.sendBlockChange(at, Material.WATER.createBlockData());
            }
        }
    }

    public static void clearNymphWater(Player player, RitualManager.PlayerSession s) {
        for (Location at : s.nymphCells) {
            player.sendBlockChange(at, at.getBlock().getBlockData());
        }
        s.nymphCells.clear();
        s.nymphWater = false;
    }

    private boolean inWaterGrid(RitualManager.PlayerSession s, Location loc) {
        for (Location at : s.nymphCells) {
            if (at.getBlockX() == loc.getBlockX() && at.getBlockZ() == loc.getBlockZ()
                    && Math.abs(at.getBlockY() - loc.getBlockY()) <= 1) return true;
        }
        return false;
    }

    static boolean grounded(Entity e) {
        if (e.isOnGround()) return true;
        Location below = e.getLocation().clone().subtract(0, 0.15, 0);
        return !below.getBlock().isPassable();
    }

    static void hurt(RitualPlugin plugin, Player player, RitualManager.PlayerSession s, MythoKind kind, double mult, boolean trueDmg) {
        s.lastHitKind = kind;
        s.lastMobHitPlayer = System.currentTimeMillis();
        double raw = kind.damage(s.griffin) * mult;
        hurtAmount(plugin, player, s, raw, trueDmg, true);
        if (kind.rare()) splashRare(plugin, player, kind, raw, trueDmg);
    }

    static void hurtAmount(RitualPlugin plugin, Player player, RitualManager.PlayerSession s,
                           double raw, boolean trueDmg, boolean animate) {
        if (s.stats.health <= 0) return;
        if (s.inqShredUntil > System.currentTimeMillis()) {
            raw *= (1.0 + s.inqShred);
        }
        double taken = trueDmg ? raw : s.stats.taken(raw);
        s.stats.health -= taken;
        if (animate) {
            player.playHurtAnimation(0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.7f, 1.0f);
        }
        applyHealth(player, s);
    }

    static void applyHealth(Player player, RitualManager.PlayerSession s) {
        if (s.stats.health <= 0) {
            s.stats.health = 0;
            player.setHealth(0);
        } else {
            RitualPlugin.get().rituals().applyVanilla(player);
        }
    }

    /** 3 seconds of hovering in the void, then snap next to the owner. */
    public static final long VOID_RESCUE_TICKS = 60L;

    static void maybeRescueVoid(RitualPlugin plugin, LivingEntity mob) {
        if (mob == null || mob.isDead() || mob.getWorld() == null) return;
        if (mob.getLocation().getY() >= mob.getWorld().getMinHeight()) return;
        mob.setVelocity(new Vector(0, 0, 0));
        mob.setFallDistance(0);
        scheduleVoidRescue(plugin, mob);
    }

    public static void scheduleVoidRescue(RitualPlugin plugin, LivingEntity mob) {
        if (mob == null || mob.isDead()) return;
        mob.setVelocity(new Vector(0, 0, 0));
        mob.setFallDistance(0);
        if (Boolean.TRUE.equals(mob.getPersistentDataContainer()
                .get(plugin.getKey("void-rescue"), PersistentDataType.BOOLEAN))) {
            return;
        }
        mob.getPersistentDataContainer().set(plugin.getKey("void-rescue"), PersistentDataType.BOOLEAN, true);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (mob.isDead() || !mob.isValid()) return;
            mob.getPersistentDataContainer().remove(plugin.getKey("void-rescue"));
            String ownerId = mob.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
            if (ownerId == null) return;
            Player owner;
            try {
                owner = plugin.getServer().getPlayer(java.util.UUID.fromString(ownerId));
            } catch (IllegalArgumentException e) {
                return;
            }
            if (owner == null || !owner.isOnline()) return;
            teleportToOwner(plugin, owner, mob);
        }, VOID_RESCUE_TICKS);
    }

    static void teleportToOwner(RitualPlugin plugin, Player owner, LivingEntity mob) {
        Location dest = owner.getLocation().clone();
        Vector dir = dest.getDirection();
        dir.setY(0);
        if (dir.lengthSquared() < 1e-6) dir = new Vector(1, 0, 0);
        dir.normalize().multiply(1.5);
        Location tryLoc = dest.clone().add(dir);
        tryLoc.setX(Math.floor(tryLoc.getX()) + 0.5);
        tryLoc.setY(Math.floor(owner.getLocation().getY()));
        tryLoc.setZ(Math.floor(tryLoc.getZ()) + 0.5);
        if (tryLoc.getBlock().isPassable() && tryLoc.clone().add(0, 1, 0).getBlock().isPassable()) {
            dest = tryLoc;
        } else {
            dest.setX(Math.floor(owner.getLocation().getX()) + 0.5);
            dest.setY(Math.floor(owner.getLocation().getY()));
            dest.setZ(Math.floor(owner.getLocation().getZ()) + 0.5);
        }
        dest.setYaw(mob.getLocation().getYaw());
        dest.setPitch(mob.getLocation().getPitch());
        mob.teleport(dest);
        mob.setVelocity(new Vector(0, 0, 0));
        mob.setFallDistance(0);
        String holId = mob.getPersistentDataContainer().get(plugin.getKey("hologram"), PersistentDataType.STRING);
        if (holId == null) return;
        try {
            Entity h = plugin.getServer().getEntity(java.util.UUID.fromString(holId));
            if (h != null && !h.isDead()) {
                String kn = mob.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
                MythoKind kind = MythoKind.HUNTER;
                if (kn != null) {
                    try {
                        kind = MythoKind.valueOf(kn);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                h.teleport(mob.getLocation().add(0, MobFactory.hologramOffset(kind), 0));
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void despawn(RitualManager.PlayerSession s, LivingEntity mob, String msg) {
        MobFactory.removeHologram(plugin, mob);
        mob.getPersistentDataContainer().set(plugin.getKey("resolved"), PersistentDataType.BOOLEAN, true);
        s.pendingMobs = Math.max(0, s.pendingMobs - 1);
        if (s.pendingMobs == 0) s.pendingMob = null;
        mob.remove();
        Player p = plugin.getServer().getPlayer(s.id);
        if (p != null) {
            if (s.pendingMobs == 0) {
                plugin.rituals().failActiveFight(p);
            }
            if (msg != null && !msg.isEmpty()) p.sendMessage(msg);
        }
    }
}
