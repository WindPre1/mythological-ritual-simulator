package dev.practice.ritual.economy;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.ritual.RitualSounds;
import dev.practice.ritual.stats.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class TradesGui implements Listener {
    public static final String TITLE = "Trades";
    private final RitualPlugin plugin;

    public TradesGui(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, Component.text(TITLE));
        int slot = 0;
        for (SellPrices.Entry e : SellPrices.all().values()) {
            if (slot >= 54) break;
            ItemStack icon = new ItemStack(e.material());
            icon.editMeta(meta -> {
                meta.displayName(Component.text(e.display(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.text("Sell price: ", NamedTextColor.GRAY)
                                .append(Component.text(SellPrices.coins(e.price()) + " coins", NamedTextColor.GOLD))
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("Click to sell all in your inventory.", NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                ));
                meta.getPersistentDataContainer().set(plugin.getKey("sell-id"), PersistentDataType.STRING, e.id());
            });
            inv.setItem(slot++, icon);
        }
        dev.practice.ritual.gui.GuiUtil.fillBlanks(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title() == null) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!TITLE.equals(title)) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            if (ItemFactory.isShuriken(clicked)) return;
            SellPrices.Entry match = matchStack(clicked);
            if (match == null) return;
            int amount = clicked.getAmount();
            event.setCurrentItem(null);
            long gained = (long) amount * match.price();
            plugin.rituals().session(player).stats.purse += gained;
            player.sendMessage("§aSold §e" + amount + "x " + match.display()
                    + " §afor §6" + SellPrices.coins(gained) + " coins§a.");
            RitualSounds.ding(player);
            plugin.rituals().save(player);
            return;
        }

        String buyId = clicked.getItemMeta().getPersistentDataContainer()
                .get(plugin.getKey("buy-id"), PersistentDataType.STRING);
        if (buyId != null) return;

        String id = clicked.getItemMeta().getPersistentDataContainer()
                .get(plugin.getKey("sell-id"), PersistentDataType.STRING);
        if (id == null) return;
        SellPrices.Entry entry = SellPrices.byId(id);
        if (entry == null) return;
        sellAll(player, entry);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (TITLE.equals(title)) event.setCancelled(true);
    }

    private void sellAll(Player player, SellPrices.Entry entry) {
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;
            if (ItemFactory.isMenu(stack) || ItemFactory.isSpade(stack) || ItemFactory.isAote(stack)
                    || ItemFactory.isDaedalus(stack)
                    || ItemFactory.isMelon(stack) || ItemFactory.isMana(stack)
                    || ItemFactory.isShuriken(stack) || ItemFactory.isStaff(stack)
                    || ItemFactory.isAvarice(stack)
                    || ItemFactory.isFish(stack) || ItemFactory.isLootingBook(stack)
                    || ItemFactory.isCore(stack) || ItemFactory.isMythos(stack)) continue;
            SellPrices.Entry match = matchStack(stack);
            if (match != null && match.id().equals(entry.id())) {
                count += stack.getAmount();
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
        if (count == 0) {
            player.sendMessage("§cYou don't have any " + entry.display() + ".");
            return;
        }
        long gained = (long) count * entry.price();
        PlayerStats s = plugin.rituals().session(player).stats;
        s.purse += gained;
        player.sendMessage("§aSold §e" + count + "x " + entry.display()
                + " §afor §6" + SellPrices.coins(gained) + " coins§a.");
        RitualSounds.ding(player);
        plugin.rituals().save(player);
    }

    public static SellPrices.Entry matchStack(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        var meta = stack.getItemMeta();
        String id = meta.getPersistentDataContainer()
                .get(RitualPlugin.get().getKey("drop-id"), PersistentDataType.STRING);
        if (id != null) return SellPrices.byId(id);
        if (meta.displayName() != null) {
            String name = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            return SellPrices.byDisplay(name);
        }
        return null;
    }
}
