package dev.practice.ritual.mob;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.ritual.Burrow;
import dev.practice.ritual.ritual.GriffinRarity;
import dev.practice.ritual.ritual.MythoKind;
import dev.practice.ritual.ritual.RitualManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

public final class MobFactory {

    public static LivingEntity spawn(RitualPlugin plugin, Player player, Burrow burrow, MythoKind kind) {
        return spawn(plugin, player, burrow, kind, plugin.rituals().session(player).griffin);
    }

    public static LivingEntity spawn(RitualPlugin plugin, Player player, Burrow burrow, MythoKind kind, GriffinRarity griffin) {
        Location loc = burrow.center().add(0, 1, 0);
        LivingEntity entity = spawnOne(plugin, loc, kind, player, griffin, 1.0);
        RitualManager.PlayerSession s = plugin.rituals().session(player);
        s.pendingMob = entity;
        s.pendingMobs = 1;
        s.harpyShred = 0;
        s.minoBleed = 0;
        s.kingRage = 0;
        s.kingHealCut = 0;
        s.stingUntil = 0;
        s.healDisabledUntil = 0;
        s.sphinxInfuriated = false;
        s.sphinxAnswer = null;
        s.nymphWater = false;
        s.nymphCells.clear();

        if (kind == MythoKind.LYNX) {
            LivingEntity twin = spawnOne(plugin, loc.clone().add(1, 0, 0), kind, player, griffin, 1.0);
            twin.getPersistentDataContainer().set(plugin.getKey("lynx-active"), PersistentDataType.BOOLEAN, false);
            entity.getPersistentDataContainer().set(plugin.getKey("lynx-active"), PersistentDataType.BOOLEAN, true);
            entity.getPersistentDataContainer().set(plugin.getKey("lynx-twin"), PersistentDataType.STRING, twin.getUniqueId().toString());
            twin.getPersistentDataContainer().set(plugin.getKey("lynx-twin"), PersistentDataType.STRING, entity.getUniqueId().toString());
            s.pendingMobs = 2;
        }
        if (kind == MythoKind.KING) {
            entity.getPersistentDataContainer().set(plugin.getKey("king-shield"), PersistentDataType.INTEGER, 75);
        }
        return entity;
    }

    public static LivingEntity spawnMinion(RitualPlugin plugin, Player player, Location loc, MythoKind kind, GriffinRarity griffin, double hpScale) {
        LivingEntity entity = spawnOne(plugin, loc, kind, player, griffin, hpScale);
        entity.getPersistentDataContainer().set(plugin.getKey("king-minion"), PersistentDataType.BOOLEAN, true);
        plugin.rituals().session(player).pendingMobs++;
        return entity;
    }

    private static LivingEntity spawnOne(RitualPlugin plugin, Location loc, MythoKind kind, Player player, GriffinRarity griffin, double hpScale) {
        EntityType type = kind.mannequin ? EntityType.MANNEQUIN : kind.type;
        LivingEntity entity;
        Class<? extends org.bukkit.entity.Entity> raw = type.getEntityClass();
        if (raw != null && LivingEntity.class.isAssignableFrom(raw)) {
            @SuppressWarnings("unchecked")
            Class<? extends LivingEntity> cls = (Class<? extends LivingEntity>) raw;
            entity = loc.getWorld().spawn(loc, cls);
        } else {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        }
        configure(plugin, entity, kind, player, griffin, hpScale);
        spawnHologram(plugin, entity, kind, griffin, kind.health(griffin) * hpScale, player);
        return entity;
    }

    private static void configure(RitualPlugin plugin, LivingEntity entity, MythoKind kind, Player player, GriffinRarity griffin, double hpScale) {
        entity.customName(null);
        entity.setCustomNameVisible(false);
        entity.setRemoveWhenFarAway(false);
        entity.setPersistent(true);
        entity.setCanPickupItems(false);
        entity.setSilent(false);
        entity.setInvulnerable(false);
        entity.setCollidable(true);
        entity.setGravity(true);

        if (entity instanceof Ageable ageable) {
            ageable.setAdult();
            ageable.setAgeLock(true);
        }
        if (entity instanceof Zombie zombie) {
            zombie.setBaby(false);
        }

        zero(entity, Attribute.ARMOR);
        zero(entity, Attribute.ARMOR_TOUGHNESS);
        try {
            if (entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
                entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(kind == MythoKind.GAIA || kind == MythoKind.KING ? 1.0 : 0.6);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (entity.getAttribute(Attribute.STEP_HEIGHT) != null) {
                entity.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(1.25);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (entity.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                double speed = switch (kind) {
                    case HUNTER -> 0.42;
                    case HARPY -> 0.24;
                    case CHAMPION, INQUISITOR -> 0.34;
                    case MANTICORE -> 0.44;
                    case BULL -> 0.30;
                    case LYNX -> 0.32;
                    case KING -> 0.22;
                    case NYMPH -> 0.10;
                    default -> 0.16;
                };
                entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
            }
        } catch (Throwable ignored) {
        }
        if (kind == MythoKind.HARPY) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, PotionEffect.INFINITE_DURATION, 0, true, false));
        }
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20);
        }
        entity.setHealth(20);
        entity.setMaximumNoDamageTicks(0);
        entity.setNoDamageTicks(0);

        if (entity instanceof Mannequin mannequin) {
            mannequin.setImmovable(false);
            try {
                mannequin.setDescription(null);
            } catch (Throwable ignored) {
            }
            applyMythoSkin(plugin, mannequin, kind);
            plugin.getServer().getScheduler().runTask(plugin, () -> applyMythoSkin(plugin, mannequin, kind));
        }

        if (entity instanceof IronGolem golem) {
            golem.setPlayerCreated(true);
        }
        if (entity instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }
        if (entity instanceof Mob mob) {
            mob.setTarget(player);
            mob.setAware(true);
        }

        double hp = kind.health(griffin) * hpScale;
        var pdc = entity.getPersistentDataContainer();
        pdc.set(plugin.getKey("mytho"), PersistentDataType.STRING, kind.name());
        pdc.set(plugin.getKey("owner"), PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(plugin.getKey("griffin"), PersistentDataType.STRING, griffin.name());
        pdc.set(plugin.getKey("sb-hp"), PersistentDataType.DOUBLE, hp);
        pdc.set(plugin.getKey("sb-max"), PersistentDataType.DOUBLE, hp);
        pdc.set(plugin.getKey("hitters"), PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(plugin.getKey("spawn-at"), PersistentDataType.LONG, System.currentTimeMillis());
        pdc.set(plugin.getKey("dmg-mult"), PersistentDataType.DOUBLE, 1.0);
    }

    private static void zero(LivingEntity entity, Attribute attr) {
        try {
            if (entity.getAttribute(attr) != null) {
                entity.getAttribute(attr).setBaseValue(0);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Kept for shuriken/arrow callers. Mobs themselves are globally visible. */
    public static void revealToOwner(RitualPlugin plugin, org.bukkit.entity.Entity entity, Player owner) {
        try {
            entity.setVisibleByDefault(true);
        } catch (Throwable ignored) {
        }
    }

    private static void spawnHologram(RitualPlugin plugin, LivingEntity entity, MythoKind kind, GriffinRarity griffin, double hp, Player owner) {
        Location at = entity.getLocation().add(0, hologramOffset(kind), 0);
        ArmorStand stand = entity.getWorld().spawn(at, ArmorStand.class, h -> {
            h.setInvisible(true);
            h.setMarker(true);
            h.setGravity(false);
            h.setInvulnerable(true);
            h.setCollidable(false);
            h.setSmall(true);
            h.setCustomNameVisible(true);
            int hits = kind == MythoKind.KING ? 75 : -1;
            h.customName(hologramName(kind, griffin, hp, hp, hits, false));
            h.getPersistentDataContainer().set(plugin.getKey("hologram"), PersistentDataType.BOOLEAN, true);
        });
        entity.getPersistentDataContainer().set(plugin.getKey("hologram"), PersistentDataType.STRING, stand.getUniqueId().toString());
    }

    public static void removeHologram(RitualPlugin plugin, LivingEntity entity) {
        String id = entity.getPersistentDataContainer().get(plugin.getKey("hologram"), PersistentDataType.STRING);
        if (id == null) return;
        try {
            Entity e = plugin.getServer().getEntity(java.util.UUID.fromString(id));
            if (e != null) e.remove();
        } catch (IllegalArgumentException ignored) {
        }
        entity.getPersistentDataContainer().remove(plugin.getKey("hologram"));
    }

    public static double hologramOffset(MythoKind kind) {
        return kind.mannequin || kind.rare() ? 2.15 : 2.0;
    }

    public static Component hologramName(MythoKind kind, GriffinRarity griffin, double hp, double max) {
        return hologramName(kind, griffin, hp, max, -1, false);
    }

    public static Component hologramName(MythoKind kind, GriffinRarity griffin, double hp, double max, int kingHits) {
        return hologramName(kind, griffin, hp, max, kingHits, false);
    }

    public static Component hologramName(MythoKind kind, GriffinRarity griffin, double hp, double max, int kingHits, boolean tagged) {
        String tag = tagged ? " §6✯" : "";
        if (kind == MythoKind.KING && kingHits > 0) {
            return LegacyComponentSerializer.legacySection()
                    .deserialize("§6King Minos §7- §5" + kingHits + " Hits" + tag);
        }
        String shown = compact(hp);
        String cap = compact(max);
        String raw = "§e[Lv" + levelFor(kind) + "] §2" + griffin.prefix + " §2 " + kind.display
                + " §a" + shown + "§f/" + cap + "§c❤" + tag;
        return LegacyComponentSerializer.legacySection().deserialize(raw);
    }

    public static String compact(double n) {
        if (n >= 1_000_000) {
            double m = n / 1_000_000.0;
            if (Math.abs(m - Math.rint(m)) < 0.05) return String.format(Locale.US, "%.0fM", m);
            return String.format(Locale.US, "%.1fM", m);
        }
        if (n >= 10_000) {
            double k = n / 1_000.0;
            if (Math.abs(k - Math.rint(k)) < 0.05) return String.format(Locale.US, "%.0fK", k);
            return String.format(Locale.US, "%.1fK", k);
        }
        return String.format(Locale.US, "%,.0f", n);
    }

    private static int levelFor(MythoKind kind) {
        return switch (kind) {
            case HUNTER -> 250;
            case LYNX -> 320;
            case NYMPH, BULL -> 350;
            case HARPY -> 400;
            case GAIA -> 490;
            case MINOTAUR -> 400;
            case CHAMPION -> 600;
            case INQUISITOR, SPHINX -> 1250;
            case MANTICORE -> 1500;
            case KING -> 2000;
        };
    }

    /**
     * Loads skins from {@code skins-folder} (Windows MythoSkins dir) or plugin data folder.
     */
    private static void applyMythoSkin(RitualPlugin plugin, Mannequin mannequin, MythoKind kind) {
        MythoSkins.apply(plugin, mannequin, kind);
    }
}
