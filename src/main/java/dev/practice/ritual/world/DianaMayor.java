package dev.practice.ritual.world;

import dev.practice.ritual.RitualPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public final class DianaMayor implements Listener {
    public static final double X = 6.5;
    public static final double Y = 79.0;
    public static final double Z = 19.5;

    private final RitualPlugin plugin;

    public DianaMayor(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    public static void spawn(RitualPlugin plugin) {
        World world = plugin.getServer().getWorld(plugin.getConfig().getString("world", "world"));
        if (world == null && !plugin.getServer().getWorlds().isEmpty()) {
            world = plugin.getServer().getWorlds().get(0);
        }
        if (world == null) return;

        for (Entity e : world.getEntities()) {
            if (e.getPersistentDataContainer().has(plugin.getKey("diana-mayor"), PersistentDataType.BOOLEAN)) {
                e.remove();
            }
        }

        Location loc = new Location(world, X, Y, Z, 180f, 0f);
        Mannequin npc = world.spawn(loc, Mannequin.class, m -> {
            m.setInvulnerable(true);
            m.setGravity(false);
            m.setCollidable(false);
            m.setSilent(true);
            m.setRemoveWhenFarAway(false);
            m.setPersistent(true);
            try {
                m.setImmovable(true);
            } catch (Throwable ignored) {
            }
            m.customName(Component.text("Diana", NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));
            m.setCustomNameVisible(true);
            m.getPersistentDataContainer().set(plugin.getKey("diana-mayor"), PersistentDataType.BOOLEAN, true);
        });

        Location holoAt = loc.clone().add(0, 2.15, 0);
        world.spawn(holoAt, ArmorStand.class, h -> {
            h.setInvisible(true);
            h.setMarker(true);
            h.setGravity(false);
            h.setInvulnerable(true);
            h.setCollidable(false);
            h.setSmall(true);
            h.setCustomNameVisible(true);
            h.customName(Component.text("Mayor", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            h.getPersistentDataContainer().set(plugin.getKey("diana-mayor"), PersistentDataType.BOOLEAN, true);
        });

        applySkin(npc);
    }

    private static void applySkin(Mannequin mannequin) {
        // Distinct from mytho skins; brunette NPC look.
        String hash = "2372f8d9d0fa23b9b9db789100a2cb3e39c2f7759c4029b83022ab0ab9f85356";
        try {
            com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(
                    java.util.UUID.nameUUIDFromBytes("diana-mayor".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    "Diana");
            profile.getTextures().setSkin(
                    java.net.URI.create("http://textures.minecraft.net/texture/" + hash).toURL());
            mannequin.setProfile(io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile(profile));
        } catch (Throwable ignored) {
            try {
                mannequin.setProfile(Mannequin.defaultProfile());
            } catch (Throwable ignored2) {
            }
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity e = event.getRightClicked();
        if (!e.getPersistentDataContainer().has(plugin.getKey("diana-mayor"), PersistentDataType.BOOLEAN)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.sendMessage("§e[NPC] §dDiana§f: I am the only mayor");
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer()
                .has(plugin.getKey("diana-mayor"), PersistentDataType.BOOLEAN)) {
            event.setCancelled(true);
        }
    }
}
