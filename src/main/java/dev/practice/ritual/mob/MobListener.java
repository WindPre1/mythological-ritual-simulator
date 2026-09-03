package dev.practice.ritual.mob;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.ritual.DropTables;
import dev.practice.ritual.ritual.GriffinRarity;
import dev.practice.ritual.ritual.MythoKind;
import dev.practice.ritual.ritual.RitualManager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MobListener implements Listener {
    private final RitualPlugin plugin;

    public MobListener(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand stand) {
            if (stand.getPersistentDataContainer().has(plugin.getKey("hologram"), PersistentDataType.BOOLEAN)) {
                event.setCancelled(true);
                return;
            }
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        String kindName = victim.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
        if (kindName == null) return;
        MythoKind kind = MythoKind.valueOf(kindName);

        if (!(event.getDamager() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if (!ItemFactory.isDaedalus(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            player.sendMessage("§cUse §6Daedalus Blade §cto attack mythological mobs.");
            return;
        }

        if (kind == MythoKind.KING) {
            Integer shield = victim.getPersistentDataContainer().get(plugin.getKey("king-shield"), PersistentDataType.INTEGER);
            if (shield != null && shield > 0) {
                event.setCancelled(true);
                shield -= 1;
                victim.getPersistentDataContainer().set(plugin.getKey("king-shield"), PersistentDataType.INTEGER, shield);
                victim.setNoDamageTicks(0);
                victim.playHurtAnimation(player.getYaw());
                if (shield <= 0) {
                    Double hp = victim.getPersistentDataContainer().get(plugin.getKey("sb-hp"), PersistentDataType.DOUBLE);
                    Double max = victim.getPersistentDataContainer().get(plugin.getKey("sb-max"), PersistentDataType.DOUBLE);
                    updateHologram(victim, kind, hp == null ? 0 : hp, max == null ? 1 : max);
                } else {
                    updateHologram(victim, kind, 0, 1);
                }
                RitualManager.PlayerSession ks = plugin.rituals().session(player);
                ks.kingRage = Math.min(1.0, ks.kingRage + 0.01);
                return;
            }
        }

        if (kind == MythoKind.GAIA) {
            // 1/6 chance the hit connects; no iframes (wiki shield).
            if (ThreadLocalRandom.current().nextInt(6) != 0) {
                event.setCancelled(true);
                player.playSound(victim.getLocation(), org.bukkit.Sound.ITEM_SHIELD_BLOCK, 0.7f, 0.8f);
                return;
            }
        } else {
            Long last = victim.getPersistentDataContainer().get(plugin.getKey("last-hit"), PersistentDataType.LONG);
            long now = System.currentTimeMillis();
            if (last != null && now - last < 250) {
                event.setCancelled(true);
                return;
            }
            victim.getPersistentDataContainer().set(plugin.getKey("last-hit"), PersistentDataType.LONG, now);
        }

        if (kind == MythoKind.LYNX) {
            Boolean active = victim.getPersistentDataContainer().get(plugin.getKey("lynx-active"), PersistentDataType.BOOLEAN);
            if (active == null || !active) {
                event.setCancelled(true);
                healLynx(victim, 0.05);
                String twinId = victim.getPersistentDataContainer().get(plugin.getKey("lynx-twin"), PersistentDataType.STRING);
                if (twinId != null) {
                    Entity twin = plugin.getServer().getEntity(UUID.fromString(twinId));
                    if (twin instanceof LivingEntity living) healLynx(living, 0.05);
                }
                return;
            }
        }

        event.setCancelled(true);
        markHitter(victim, player);

        RitualManager.PlayerSession sess = plugin.rituals().session(player);
        if (kind == MythoKind.INQUISITOR) {
            sess.inqShred = Math.min(0.40, sess.inqShred + 0.10);
            sess.inqShredUntil = System.currentTimeMillis() + 16_000;
        }
        if (kind == MythoKind.KING) {
            sess.kingRage = Math.min(1.0, sess.kingRage + 0.02);
        }

        double dmg = sess.stats.damage;
        if (sess.inqShredUntil > System.currentTimeMillis()) {
            dmg *= (1.0 - sess.inqShred);
        }
        Double hp = victim.getPersistentDataContainer().get(plugin.getKey("sb-hp"), PersistentDataType.DOUBLE);
        Double max = victim.getPersistentDataContainer().get(plugin.getKey("sb-max"), PersistentDataType.DOUBLE);
        if (hp == null) hp = 20.0;
        if (max == null) max = hp;
        hp = hp - dmg;
        victim.getPersistentDataContainer().set(plugin.getKey("sb-hp"), PersistentDataType.DOUBLE, hp);
        victim.setNoDamageTicks(0);
        victim.setMaximumNoDamageTicks(0);
        victim.playHurtAnimation(player.getYaw());
        victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_HURT, 1f, 1f);
        updateHologram(victim, kind, Math.max(0, hp), max);
        if (hp <= 0) {
            victim.setHealth(0);
        } else {
            victim.setHealth(Math.max(1, 20.0 * (hp / max)));
        }

        if (kind == MythoKind.LYNX && hp > 0) {
            victim.getPersistentDataContainer().set(plugin.getKey("lynx-active"), PersistentDataType.BOOLEAN, false);
            String twinId = victim.getPersistentDataContainer().get(plugin.getKey("lynx-twin"), PersistentDataType.STRING);
            if (twinId != null) {
                Entity twin = plugin.getServer().getEntity(UUID.fromString(twinId));
                if (twin instanceof LivingEntity living && !living.isDead()) {
                    living.getPersistentDataContainer().set(plugin.getKey("lynx-active"), PersistentDataType.BOOLEAN, true);
                }
            }
        }
    }

    private void healLynx(LivingEntity victim, double pct) {
        Double hp = victim.getPersistentDataContainer().get(plugin.getKey("sb-hp"), PersistentDataType.DOUBLE);
        Double max = victim.getPersistentDataContainer().get(plugin.getKey("sb-max"), PersistentDataType.DOUBLE);
        if (hp == null || max == null) return;
        hp = Math.min(max, hp + max * pct);
        victim.getPersistentDataContainer().set(plugin.getKey("sb-hp"), PersistentDataType.DOUBLE, hp);
        String kindName = victim.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
        if (kindName != null) {
            updateHologram(victim, MythoKind.valueOf(kindName), hp, max);
        }
    }

    private void markHitter(LivingEntity victim, Player player) {
        String raw = victim.getPersistentDataContainer().get(plugin.getKey("hitters"), PersistentDataType.STRING);
        Set<String> set = new HashSet<>();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split(",")) if (!s.isBlank()) set.add(s);
        }
        set.add(player.getUniqueId().toString());
        victim.getPersistentDataContainer().set(plugin.getKey("hitters"), PersistentDataType.STRING, String.join(",", set));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!victim.getPersistentDataContainer().has(plugin.getKey("mytho"), PersistentDataType.STRING)) return;
        if (event instanceof EntityDamageByEntityEvent) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            victim.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            victim.setFallDistance(0);
            MobAI.scheduleVoidRescue(plugin, victim);
            return;
        }
        // /kill and world-border can still remove the mob so the burrow is not stuck.
        if (cause == EntityDamageEvent.DamageCause.KILL
                || cause == EntityDamageEvent.DamageCause.WORLD_BORDER) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        String kindName = victim.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
        if (kindName == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        if (Boolean.TRUE.equals(victim.getPersistentDataContainer().get(plugin.getKey("resolved"), PersistentDataType.BOOLEAN))) {
            return;
        }
        victim.getPersistentDataContainer().set(plugin.getKey("resolved"), PersistentDataType.BOOLEAN, true);
        // Keep the 0 HP hologram for a few ticks so SBO DianaMobDetect can fire
        // (healthRegex ...§f/  and health <= 0) before the stand is removed.
        String holId = victim.getPersistentDataContainer().get(plugin.getKey("hologram"), PersistentDataType.STRING);
        if (holId != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                try {
                    org.bukkit.entity.Entity e = plugin.getServer().getEntity(UUID.fromString(holId));
                    if (e != null && !e.isDead()) e.remove();
                } catch (IllegalArgumentException ignored) {
                }
            }, 12L);
        }
        MythoKind kind = MythoKind.valueOf(kindName);

        String ownerId = victim.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
        Player owner = ownerId == null ? null : plugin.getServer().getPlayer(UUID.fromString(ownerId));
        RitualManager.PlayerSession s = owner == null ? null : plugin.rituals().session(owner);

        if (kind == MythoKind.LYNX) {
            String twinId = victim.getPersistentDataContainer().get(plugin.getKey("lynx-twin"), PersistentDataType.STRING);
            if (twinId != null) {
                Entity twin = plugin.getServer().getEntity(UUID.fromString(twinId));
                if (twin instanceof LivingEntity living && !living.isDead()) {
                    living.getPersistentDataContainer().set(plugin.getKey("lynx-active"), PersistentDataType.BOOLEAN, true);
                    if (s != null) {
                        s.pendingMobs = Math.max(1, s.pendingMobs - 1);
                        s.pendingMob = living;
                    }
                    return;
                }
            }
        }

        if (s != null) {
            s.pendingMobs = Math.max(0, s.pendingMobs - 1);
            if (s.pendingMobs == 0) s.pendingMob = null;
            if (kind == MythoKind.HARPY) s.harpyShred = 0;
            if (kind == MythoKind.MANTICORE) {
                s.stingUntil = 0;
                s.healDisabledUntil = 0;
            }
            if (kind == MythoKind.NYMPH && owner != null) {
                MobAI.clearNymphWater(owner, s);
            }
            if (kind == MythoKind.MINOTAUR) s.minoBleed = 0;
            if (kind == MythoKind.SPHINX) s.sphinxAnswer = null;
        }

        if (owner != null && owner.isOnline()) {
            EntityDamageEvent last = victim.getLastDamageCause();
            EntityDamageEvent.DamageCause lastCause = last == null ? null : last.getCause();
            boolean natural = lastCause == EntityDamageEvent.DamageCause.WORLD_BORDER;
            if (natural) {
                // World-border: free the burrow, no loot.
                return;
            }
            boolean tagged = Boolean.TRUE.equals(victim.getPersistentDataContainer()
                    .get(plugin.getKey("shuriken"), PersistentDataType.BOOLEAN));
            DropTables.rollMobLoot(owner, kind, plugin.rituals().session(owner).stats, false, owner.getName(), tagged);

            // Lootshare only for players who actually hit this mob — not nearby party spectators.
            Set<UUID> share = new HashSet<>();
            String hitters = victim.getPersistentDataContainer().get(plugin.getKey("hitters"), PersistentDataType.STRING);
            if (hitters != null) {
                for (String id : hitters.split(",")) {
                    if (id.isBlank() || id.equals(owner.getUniqueId().toString())) continue;
                    try {
                        share.add(UUID.fromString(id));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            boolean taggedShare = tagged;
            for (UUID id : share) {
                Player other = plugin.getServer().getPlayer(id);
                if (other == null || !other.isOnline()) continue;
                DropTables.rollMobLoot(other, kind, plugin.rituals().session(other).stats, true,
                        owner.getName(), taggedShare);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerHurt(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event.getDamager() instanceof org.bukkit.entity.Snowball) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof Arrow arrow) {
            String shot = arrow.getPersistentDataContainer().get(plugin.getKey("mytho-shot"), PersistentDataType.STRING);
            LivingEntity shooter = arrow.getShooter() instanceof LivingEntity living ? living : null;
            if (shot == null && shooter != null) {
                shot = shooter.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
            }
            if (shot != null) {
                event.setCancelled(true);
                arrow.remove();
                try {
                    MythoKind kind = MythoKind.valueOf(shot);
                    MobAI.onArrowHit(plugin, player, shooter, kind);
                } catch (IllegalArgumentException ignored) {
                }
                return;
            }
        }

        if (!(event.getDamager() instanceof LivingEntity damager)) return;
        String kindName = damager.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
        if (kindName == null) return;
        event.setCancelled(true);
    }

    private void updateHologram(LivingEntity victim, MythoKind kind, double hp, double max) {
        String id = victim.getPersistentDataContainer().get(plugin.getKey("hologram"), PersistentDataType.STRING);
        if (id == null) return;
        Entity e = plugin.getServer().getEntity(UUID.fromString(id));
        if (!(e instanceof ArmorStand stand) || stand.isDead()) return;
        GriffinRarity griffin = GriffinRarity.MYTHIC;
        String g = victim.getPersistentDataContainer().get(plugin.getKey("griffin"), PersistentDataType.STRING);
        if (g != null) {
            try {
                griffin = GriffinRarity.valueOf(g);
            } catch (IllegalArgumentException ignored) {
            }
        }
        Integer hits = victim.getPersistentDataContainer().get(plugin.getKey("king-shield"), PersistentDataType.INTEGER);
        boolean tagged = Boolean.TRUE.equals(victim.getPersistentDataContainer()
                .get(plugin.getKey("shuriken"), PersistentDataType.BOOLEAN));
        // Force the health line at 0 so SBO's healthRegex sees a Diana death.
        int shownHits = hp <= 0 ? -1 : (hits == null ? -1 : hits);
        stand.customName(MobFactory.hologramName(kind, griffin, hp, max, shownHits, tagged));
    }

    @EventHandler
    public void onRemove(org.bukkit.event.entity.EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!living.getPersistentDataContainer().has(plugin.getKey("mytho"), PersistentDataType.STRING)) return;
        String holId = living.getPersistentDataContainer().get(plugin.getKey("hologram"), PersistentDataType.STRING);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (holId != null) {
                try {
                    org.bukkit.entity.Entity e = plugin.getServer().getEntity(UUID.fromString(holId));
                    if (e != null && !e.isDead()) e.remove();
                } catch (IllegalArgumentException ignored) {
                }
            }
        }, 12L);
        if (Boolean.TRUE.equals(living.getPersistentDataContainer().get(plugin.getKey("resolved"), PersistentDataType.BOOLEAN))) {
            return;
        }
        living.getPersistentDataContainer().set(plugin.getKey("resolved"), PersistentDataType.BOOLEAN, true);
        // Natural despawn (void, unload) — free the burrow, no loot.
        String ownerId = living.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
        Player owner = ownerId == null ? null : plugin.getServer().getPlayer(UUID.fromString(ownerId));
        if (owner == null) return;
        RitualManager.PlayerSession s = plugin.rituals().session(owner);
        String kindName = living.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
        if ("LYNX".equals(kindName)) {
            String twinId = living.getPersistentDataContainer().get(plugin.getKey("lynx-twin"), PersistentDataType.STRING);
            if (twinId != null) {
                Entity twin = plugin.getServer().getEntity(UUID.fromString(twinId));
                if (twin instanceof LivingEntity other && !other.isDead()) {
                    s.pendingMobs = Math.max(1, s.pendingMobs - 1);
                    s.pendingMob = other;
                    return;
                }
            }
        }
        s.pendingMobs = Math.max(0, s.pendingMobs - 1);
        if (s.pendingMobs == 0) s.pendingMob = null;
    }
}
