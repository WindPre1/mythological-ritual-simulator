package dev.practice.ritual.ritual;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.mob.MobFactory;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class ParticleTask extends org.bukkit.scheduler.BukkitRunnable {
    private final RitualPlugin plugin;

    public ParticleTask(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        double range = plugin.getConfig().getDouble("close-detect-range", 32);
        double rangeSq = range * range;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!ItemFactory.isSpade(player.getInventory().getItemInMainHand())) continue;
            RitualManager.PlayerSession s = plugin.rituals().session(player);
            if (!s.active) continue;
            for (Burrow burrow : s.burrows) {
                if (burrow.dug) continue;
                if (burrow.center().distanceSquared(player.getLocation()) > rangeSq) continue;
                ParticleEmitter.emitClose(player, burrow);
            }
            if (s.pendingMob != null && !s.pendingMob.isDead()) {
                Boolean active = s.pendingMob.getPersistentDataContainer()
                        .get(plugin.getKey("lynx-active"), PersistentDataType.BOOLEAN);
                if (Boolean.TRUE.equals(active)) {
                    player.spawnParticle(Particle.ANGRY_VILLAGER, s.pendingMob.getLocation().add(0, 1.2, 0), 2, 0.2, 0.2, 0.2, 0);
                }
                followHologram(s.pendingMob);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 48, 24, 48)) {
                if (!(e instanceof LivingEntity living)) continue;
                String ownerId = living.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
                if (ownerId != null && !ownerId.equals(player.getUniqueId().toString())) continue;
                Boolean active = living.getPersistentDataContainer()
                        .get(plugin.getKey("lynx-active"), PersistentDataType.BOOLEAN);
                if (Boolean.TRUE.equals(active)) {
                    player.spawnParticle(Particle.ANGRY_VILLAGER, living.getLocation().add(0, 1.1, 0), 2, 0.15, 0.2, 0.15, 0);
                }
                if (living.getPersistentDataContainer().has(plugin.getKey("mytho"), PersistentDataType.STRING)) {
                    followHologram(living);
                }
            }
        }
    }

    private void followHologram(LivingEntity living) {
        String id = living.getPersistentDataContainer().get(plugin.getKey("hologram"), PersistentDataType.STRING);
        if (id == null) return;
        Entity e;
        try {
            e = plugin.getServer().getEntity(UUID.fromString(id));
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (!(e instanceof ArmorStand stand) || stand.isDead()) return;
        String kindName = living.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
        MythoKind kind = MythoKind.HUNTER;
        if (kindName != null) {
            try {
                kind = MythoKind.valueOf(kindName);
            } catch (IllegalArgumentException ignored) {
            }
        }
        stand.teleport(living.getLocation().add(0, MobFactory.hologramOffset(kind), 0));
    }
}
