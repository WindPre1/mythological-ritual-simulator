package dev.practice.ritual.economy;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.gui.GuiUtil;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.ritual.RitualSounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Claim practice items. */
public final class ItemsGui implements Listener {
    private final RitualPlugin plugin;

    public ItemsGui(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Items"));
        holder.inv = inv;
        ItemFactory f = plugin.items();
        double dmg = plugin.rituals().session(player).stats.damage;
        put(inv, 10, "SPADE", f.deificSpade());
        put(inv, 11, "AOTE", f.aote());
        put(inv, 12, "BLADE", f.daedalusBlade(dmg));
        put(inv, 13, "MELON", f.melon());
        put(inv, 14, "MANA", f.manaFruit());
        put(inv, 15, "STAFF", f.fireFreezeStaff());
        put(inv, 16, "AVARICE", f.crownOfAvarice());
        put(inv, 19, "SHURIKEN", f.shuriken());
        put(inv, 20, "FOUR_EYED_FISH", f.fourEyedFish());
        put(inv, 21, "LOOTING_BOOK", f.lootingBook());
        GuiUtil.fillBlanks(inv);
        player.openInventory(inv);
    }

    private void put(Inventory inv, int slot, String id, ItemStack icon) {
        icon.editMeta(meta -> {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(GuiUtil.noItalic(Component.text("Click to claim 1.", NamedTextColor.YELLOW)));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(plugin.getKey("claim-id"), PersistentDataType.STRING, id);
        });
        inv.setItem(slot, icon);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String id = clicked.getItemMeta().getPersistentDataContainer()
                .get(plugin.getKey("claim-id"), PersistentDataType.STRING);
        if (id == null) return;
        ItemFactory f = plugin.items();
        double dmg = plugin.rituals().session(player).stats.damage;
        ItemStack give = switch (id) {
            case "SPADE" -> f.deificSpade();
            case "AOTE" -> f.aote();
            case "BLADE" -> f.daedalusBlade(dmg);
            case "MELON" -> f.melon();
            case "MANA" -> f.manaFruit();
            case "STAFF" -> f.fireFreezeStaff();
            case "AVARICE" -> f.crownOfAvarice();
            case "SHURIKEN" -> f.shuriken();
            case "FOUR_EYED_FISH" -> f.fourEyedFish();
            case "LOOTING_BOOK" -> f.lootingBook();
            default -> null;
        };
        if (give == null) return;
        var leftover = player.getInventory().addItem(give);
        leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
        RitualSounds.ding(player);
        player.sendMessage("§aClaimed.");
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }
}
