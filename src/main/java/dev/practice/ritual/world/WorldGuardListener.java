package dev.practice.ritual.world;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.ritual.ParticleEmitter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class WorldGuardListener implements Listener {
    private final RitualPlugin plugin;

    public WorldGuardListener(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
        if (event.getEntity() instanceof Player && event.getCause() != EntityDamageEvent.DamageCause.CUSTOM
                && event.getCause() != EntityDamageEvent.DamageCause.VOID
                && event.getCause() != EntityDamageEvent.DamageCause.SUICIDE) {
            // All vanilla damage is routed through our SkyBlock stats (aura) or cancelled.
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                    || event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL
                    || event.getCause() == EntityDamageEvent.DamageCause.CONTACT
                    || event.getCause() == EntityDamageEvent.DamageCause.CRAMMING
                    || event.getCause() == EntityDamageEvent.DamageCause.DROWNING
                    || event.getCause() == EntityDamageEvent.DamageCause.STARVATION) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        event.setCancelled(true);
        if (event.getEntity() instanceof Player p) p.setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWither(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getModifiedType() != PotionEffectType.WITHER) return;
        EntityPotionEffectEvent.Action action = event.getAction();
        if (action == EntityPotionEffectEvent.Action.ADDED
                || action == EntityPotionEffectEvent.Action.CHANGED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (ItemFactory.isAote(hand) || ItemFactory.isDaedalus(hand) || ItemFactory.isStaff(hand)) {
            event.setCancelled(true);
            return;
        }
        if (event.getBlock().getType() == Material.GRASS_BLOCK) {
            event.setDropItems(false);
            var burrow = plugin.rituals().burrowAt(player, event.getBlock());
            if (burrow != null) {
                event.setCancelled(true);
                return;
            }
            ParticleEmitter.emitRemoved(player, event.getBlock().getLocation());
            if (ItemFactory.isSpade(hand) || !player.isOp() || !plugin.rituals().session(player).stats.breakBlocks) {
                event.setCancelled(true);
            }
            return;
        }
        if (!player.isOp() || !plugin.rituals().session(player).stats.breakBlocks) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTrample(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null) {
            Material t = event.getClickedBlock().getType();
            if (t == Material.FARMLAND || t.name().contains("CROP") || t == Material.WHEAT
                    || t.name().endsWith("_CROP")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityTrample(EntityChangeBlockEvent event) {
        Material t = event.getBlock().getType();
        if (t == Material.FARMLAND || t == Material.WHEAT) event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (ItemFactory.isMenu(stack)) event.setCancelled(true);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (ItemFactory.isMenu(event.getOffHandItem()) || ItemFactory.isMenu(event.getMainHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInv(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // Slot 8 is the SkyBlock menu. Never let it leave that slot or get overwritten.
        if (event.getHotbarButton() == 8) {
            event.setCancelled(true);
            plugin.rituals().ensureMenu(player);
            return;
        }
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getInventory())
                && event.getSlot() == 8) {
            event.setCancelled(true);
            plugin.rituals().ensureMenu(player);
            return;
        }
        if (ItemFactory.isMenu(event.getCurrentItem()) || ItemFactory.isMenu(event.getCursor())) {
            event.setCancelled(true);
            plugin.rituals().ensureMenu(player);
        }
    }

    @EventHandler
    public void onDragInv(InventoryDragEvent event) {
        for (ItemStack s : event.getNewItems().values()) {
            if (ItemFactory.isMenu(s)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
