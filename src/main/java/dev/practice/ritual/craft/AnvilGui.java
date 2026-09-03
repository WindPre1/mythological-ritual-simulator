package dev.practice.ritual.craft;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.gui.GuiUtil;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.ritual.RitualSounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class AnvilGui implements Listener {
    private static final int LEFT = 11;
    private static final int RIGHT = 15;
    private static final int RESULT = 22;
    private final RitualPlugin plugin;

    public AnvilGui(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Anvil"));
        holder.inv = inv;
        GuiUtil.fillBlanks(inv);
        inv.setItem(LEFT, null);
        inv.setItem(RIGHT, null);
        inv.setItem(RESULT, hint());
        inv.setItem(4, pane("§6Daedalus Blade + Looting Book"));
        player.openInventory(inv);
    }

    private ItemStack hint() {
        ItemStack item = new ItemStack(org.bukkit.Material.ANVIL);
        item.editMeta(meta -> {
            meta.displayName(GuiUtil.noItalic("§eFuse Looting"));
            meta.lore(java.util.List.of(
                    GuiUtil.noItalic(Component.text("Left: Daedalus Blade", NamedTextColor.GRAY)),
                    GuiUtil.noItalic(Component.text("Right: Looting Enchant book", NamedTextColor.GRAY)),
                    GuiUtil.noItalic(Component.text("1 book = +1 Looting level (max V).", NamedTextColor.DARK_GRAY)),
                    GuiUtil.noItalic(Component.text("+0.15x odds per level (max ×1.75).", NamedTextColor.DARK_GRAY)),
                    GuiUtil.noItalic(Component.text("Does not apply to lootshare.", NamedTextColor.RED))
            ));
        });
        return item;
    }

    private ItemStack pane(String name) {
        ItemStack item = new ItemStack(org.bukkit.Material.ORANGE_STAINED_GLASS_PANE);
        item.editMeta(meta -> meta.displayName(GuiUtil.noItalic(name)));
        return item;
    }

    private void refresh(Inventory inv, Player player) {
        ItemStack left = inv.getItem(LEFT);
        ItemStack right = inv.getItem(RIGHT);
        if (!ItemFactory.isDaedalus(left) || !ItemFactory.isLootingBook(right)) {
            inv.setItem(RESULT, hint());
            return;
        }
        if (ItemFactory.isLootingMax(left)) {
            ItemStack full = hint();
            full.editMeta(meta -> meta.displayName(GuiUtil.noItalic("§cAlready Looting V")));
            inv.setItem(RESULT, full);
            return;
        }
        ItemStack out = left.clone();
        ItemFactory.addLootingBook(out);
        double dmg = plugin.rituals().session(player).stats.damage;
        out.editMeta(meta -> meta.lore(ItemFactory.bladeLore(dmg, out)));
        inv.setItem(RESULT, out);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();

        if (event.getClickedInventory() != top) {
            if (event.getClick() == ClickType.DOUBLE_CLICK || event.getClick() == ClickType.NUMBER_KEY) {
                event.setCancelled(true);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top, player));
            return;
        }

        int slot = event.getRawSlot();
        if (slot == LEFT || slot == RIGHT) {
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top, player));
            return;
        }
        event.setCancelled(true);
        if (slot != RESULT) return;
        ItemStack left = top.getItem(LEFT);
        ItemStack right = top.getItem(RIGHT);
        if (!ItemFactory.isDaedalus(left) || !ItemFactory.isLootingBook(right)) return;
        if (ItemFactory.isLootingMax(left)) {
            player.sendMessage("§cThat blade is already Looting V.");
            return;
        }
        int before = ItemFactory.lootingOf(left);
        ItemFactory.addLootingBook(left);
        int after = ItemFactory.lootingOf(left);
        double dmg = plugin.rituals().session(player).stats.damage;
        left.editMeta(meta -> meta.lore(ItemFactory.bladeLore(dmg, left)));
        var leftover = player.getInventory().addItem(left.clone());
        leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
        top.setItem(LEFT, null);
        int books = right.getAmount();
        if (books <= 1) top.setItem(RIGHT, null);
        else right.setAmount(books - 1);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        RitualSounds.ding(player);
        player.sendMessage("§aFused §eLooting " + ItemFactory.roman(after) + " §aonto Daedalus Blade.");
        refresh(top, player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        Inventory top = event.getView().getTopInventory();
        int size = top.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < size && slot != LEFT && slot != RIGHT) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getWhoClicked() instanceof Player p) {
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top, p));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        for (int slot : new int[]{LEFT, RIGHT}) {
            ItemStack s = event.getInventory().getItem(slot);
            if (GuiUtil.empty(s) || GuiUtil.isFiller(s)) continue;
            var leftover = player.getInventory().addItem(s);
            leftover.values().forEach(st -> player.getWorld().dropItemNaturally(player.getLocation(), st));
            event.getInventory().setItem(slot, null);
        }
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }
}
