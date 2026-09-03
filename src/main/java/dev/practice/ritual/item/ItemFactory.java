package dev.practice.ritual.item;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.economy.SellPrices;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ItemFactory {
    public static final String KIND_SPADE = "spade";
    public static final String KIND_AOTE = "aote";
    public static final String KIND_BLADE = "daedalus";
    public static final String KIND_MENU = "menu";
    public static final String KIND_MELON = "melon";
    public static final String KIND_MANA = "mana";
    public static final String KIND_SHURIKEN = "shuriken";
    public static final String KIND_STAFF = "firefreeze";
    public static final String KIND_AVARICE = "avarice";
    public static final String KIND_FISH = "four_eyed_fish";
    public static final String KIND_LOOTING = "looting_book";
    public static final String KIND_CORE = "mythos_core";
    public static final String KIND_MYTHOS = "mythos";
    public static final long AVARICE_CAP = 1_000_000_000L;

    private final RitualPlugin plugin;

    public ItemFactory(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack deificSpade() {
        ItemStack item = new ItemStack(Material.GOLDEN_SHOVEL);
        item.editMeta(meta -> {
            meta.displayName(legacy("§6Erudite Deific Spade").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    noItalic(Component.text("Use to reveal and dig up ", NamedTextColor.GRAY)
                            .append(Component.text("Griffin", NamedTextColor.GOLD))),
                    noItalic(Component.text("Burrows", NamedTextColor.GOLD)
                            .append(Component.text(" in the hub while Diana's", NamedTextColor.GRAY))),
                    noItalic(Component.text("Mythological Ritual", NamedTextColor.LIGHT_PURPLE)
                            .append(Component.text(" is active.", NamedTextColor.GRAY))),
                    Component.empty(),
                    noItalic(Component.text("Maximum Burrow Chain Length: ", NamedTextColor.GRAY)
                            .append(Component.text("10", NamedTextColor.GREEN))),
                    Component.empty(),
                    noItalic(Component.text("Burrows dug with this item may contain:", NamedTextColor.GRAY)),
                    noItalic(Component.text(" ⚫ ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("Monsters", NamedTextColor.RED))),
                    noItalic(Component.text(" ⚫ ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("Griffin Feathers", NamedTextColor.BLUE))),
                    noItalic(Component.text(" ⚫ ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("Coins", NamedTextColor.GOLD))),
                    noItalic(Component.text(" ⚫ ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("Mythos Fragments", NamedTextColor.DARK_PURPLE))),
                    noItalic(Component.text(" ⚫ ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("Braided Griffin Feathers", NamedTextColor.LIGHT_PURPLE))),
                    Component.empty(),
                    noItalic(Component.text("Ability: Echo ", NamedTextColor.GOLD)
                            .append(Component.text("RIGHT CLICK", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))),
                    noItalic(Component.text("Show the way to the next or nearby Griffin", NamedTextColor.GRAY)),
                    noItalic(Component.text("burrow.", NamedTextColor.GRAY)),
                    noItalic(Component.text("Mana Cost: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("10", NamedTextColor.AQUA))),
                    noItalic(Component.text("Cooldown: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("1s", NamedTextColor.GREEN))),
                    Component.empty(),
                    noItalic(Component.text("Erudite Bonus", NamedTextColor.GOLD)),
                    noItalic(Component.text("Griffin Burrow", NamedTextColor.GOLD)
                            .append(Component.text(" chains always consist", NamedTextColor.GRAY))),
                    noItalic(Component.text("of ", NamedTextColor.GRAY)
                            .append(Component.text("2", NamedTextColor.GREEN))
                            .append(Component.text(" additional burrows.", NamedTextColor.GRAY))),
                    Component.empty(),
                    noItalic(Component.text("LEGENDARY", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            ));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.EFFICIENCY, 5, true);
            mark(meta, KIND_SPADE, "DEIFIC_SPADE");
        });
        return SkyblockNbt.withId(item, "DEIFIC_SPADE");
    }

    public ItemStack aote() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        item.editMeta(meta -> {
            meta.displayName(legacy("§5Aspect Of The Void").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    noItalic(Component.text("Ultimate Wise V", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD)),
                    noItalic(Component.text("Reduces the ability mana cost of this", NamedTextColor.GRAY)),
                    noItalic(Component.text("item by 50%.", NamedTextColor.GRAY)),
                    Component.empty(),
                    noItalic(Component.text("Ability: Instant Transmission ", NamedTextColor.GOLD)
                            .append(Component.text("RIGHT CLICK", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))),
                    noItalic(Component.text("Teleport 12 blocks ahead of you.", NamedTextColor.GRAY)),
                    noItalic(Component.text("Mana Cost: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("25", NamedTextColor.AQUA))),
                    Component.empty(),
                    noItalic(Component.text("Ability: Ether Transmission ", NamedTextColor.GOLD)
                            .append(Component.text("SNEAK RIGHT CLICK", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))),
                    noItalic(Component.text("Teleport to your targeted block up", NamedTextColor.GRAY)),
                    noItalic(Component.text("to 61 blocks away.", NamedTextColor.GRAY)),
                    noItalic(Component.text("Mana Cost: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("25", NamedTextColor.AQUA))),
                    Component.empty(),
                    noItalic(Component.text("EPIC SWORD", NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
            ));
            mark(meta, KIND_AOTE, "ASPECT_OF_THE_VOID");
        });
        return SkyblockNbt.withId(item, "ASPECT_OF_THE_VOID");
    }

    public ItemStack daedalusBlade(double damage) {
        ItemStack item = new ItemStack(Material.GOLDEN_SWORD);
        item.editMeta(meta -> {
            meta.displayName(legacy("§6Daedalus Blade").decoration(TextDecoration.ITALIC, false));
            meta.lore(bladeLore(damage, 0));
            mark(meta, KIND_BLADE, "DAEDALUS_BLADE");
        });
        return SkyblockNbt.withId(item, "DAEDALUS_BLADE");
    }

    public static List<Component> bladeLore(double damage) {
        return bladeLore(damage, 0);
    }

    public static List<Component> bladeLore(double damage, ItemStack blade) {
        return bladeLore(damage, lootingBooks(blade));
    }

    public static List<Component> bladeLore(double damage, int books) {
        String dmg = dev.practice.ritual.command.SetStatCommand.fmt(damage);
        int level = lootingLevelFromBooks(books);
        List<Component> lore = new ArrayList<>();
        lore.add(noItalic(Component.text("Damage: +" + dmg, NamedTextColor.GRAY)));
        lore.add(Component.empty());
        lore.add(noItalic(Component.text("Deals ", NamedTextColor.GRAY)
                .append(Component.text("+" + dmg, NamedTextColor.RED))
                .append(Component.text(" damage per hit to", NamedTextColor.GRAY))));
        lore.add(noItalic(Component.text("Mythological Creatures", NamedTextColor.DARK_GREEN)
                .append(Component.text(".", NamedTextColor.GRAY))));
        if (books > 0 || level > 0) {
            lore.add(Component.empty());
            lore.add(noItalic(Component.text("Looting " + roman(Math.max(1, level)), NamedTextColor.BLUE)));
            lore.add(noItalic(Component.text("Drop rates ×", NamedTextColor.GRAY)
                    .append(Component.text(String.format(java.util.Locale.US, "%.2f", lootingMultiplier(level)), NamedTextColor.GREEN))
                    .append(Component.text("  (+0.15 per level, max 1.75)", NamedTextColor.DARK_GRAY))));
            if (level >= 5) {
                lore.add(noItalic(Component.text("Maxed.", NamedTextColor.GREEN)));
            }
            lore.add(noItalic(Component.text("1 book = +1 level. Does not apply to lootshare.", NamedTextColor.DARK_GRAY)));
        }
        lore.add(Component.empty());
        lore.add(noItalic(Component.text("The only weapon that can damage", NamedTextColor.GRAY)));
        lore.add(noItalic(Component.text("Griffin Burrow defenders.", NamedTextColor.GRAY)));
        lore.add(Component.empty());
        lore.add(noItalic(Component.text("LEGENDARY SWORD", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
        return lore;
    }

    public static void refreshDaedalus(org.bukkit.entity.Player player, double damage) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (!isDaedalus(stack)) continue;
            stack.editMeta(meta -> meta.lore(bladeLore(damage, stack)));
        }
    }

    public static void refreshSpades(org.bukkit.entity.Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (!isSpade(stack)) continue;
            if (stack.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.EFFICIENCY) >= 5) continue;
            stack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, 5);
            stack.editMeta(meta -> meta.addItemFlags(ItemFlag.HIDE_ENCHANTS));
        }
    }

    public ItemStack melon() {
        ItemStack item = new ItemStack(Material.MELON_SLICE);
        item.editMeta(meta -> {
            meta.displayName(legacy("§aHealing Melon").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    noItalic(Component.text("Ability: Chomp ", NamedTextColor.GOLD)
                            .append(Component.text("RIGHT CLICK", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))),
                    noItalic(Component.text("Heal ", NamedTextColor.GRAY)
                            .append(Component.text("50%", NamedTextColor.GREEN))
                            .append(Component.text(" of your max health.", NamedTextColor.GRAY))),
                    noItalic(Component.text("Mana Cost: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("200", NamedTextColor.AQUA))),
                    noItalic(Component.text("Cooldown: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("5s", NamedTextColor.GREEN))),
                    Component.empty(),
                    noItalic(Component.text("UNCOMMON", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
            ));
            mark(meta, KIND_MELON, "HEALING_MELON");
        });
        return item;
    }

    public ItemStack manaFruit() {
        ItemStack item = new ItemStack(Material.GLOW_BERRIES);
        item.editMeta(meta -> {
            meta.displayName(legacy("§bMana Fruit").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    noItalic(Component.text("Ability: Siphon ", NamedTextColor.GOLD)
                            .append(Component.text("RIGHT CLICK", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))),
                    noItalic(Component.text("Restore ", NamedTextColor.GRAY)
                            .append(Component.text("50%", NamedTextColor.AQUA))
                            .append(Component.text(" of your max mana.", NamedTextColor.GRAY))),
                    noItalic(Component.text("Costs ", NamedTextColor.GRAY)
                            .append(Component.text("50%", NamedTextColor.RED))
                            .append(Component.text(" of your current health.", NamedTextColor.GRAY))),
                    noItalic(Component.text("Cooldown: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("5s", NamedTextColor.GREEN))),
                    Component.empty(),
                    noItalic(Component.text("RARE", NamedTextColor.BLUE).decorate(TextDecoration.BOLD))
            ));
            mark(meta, KIND_MANA, "MANA_FRUIT");
        });
        return item;
    }

    public ItemStack shuriken() {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        item.editMeta(meta -> {
            meta.displayName(legacy("§9Extremely Real Shuriken").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    noItalic(Component.text("Ability: Tag ", NamedTextColor.GOLD)
                            .append(Component.text("RIGHT CLICK", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))),
                    noItalic(Component.text("Throw at an enemy to tag them.", NamedTextColor.GRAY)),
                    noItalic(Component.text("Tagged enemies grant ", NamedTextColor.GRAY)
                            .append(Component.text("+5% ✯ Magic Find", NamedTextColor.AQUA))),
                    noItalic(Component.text("when killed and show §6✯ §7on their name.", NamedTextColor.GRAY)),
                    noItalic(Component.text("Cooldown: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("1s", NamedTextColor.GREEN))),
                    Component.empty(),
                    noItalic(Component.text("RARE", NamedTextColor.BLUE).decorate(TextDecoration.BOLD))
            ));
            mark(meta, KIND_SHURIKEN, "EXTREMELY_REAL_SHURIKEN");
            try {
                meta.setMaxStackSize(64);
            } catch (Throwable ignored) {
            }
        });
        return item;
    }

    public ItemStack fireFreezeStaff() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        item.editMeta(meta -> {
            meta.displayName(legacy("§5Fire Freeze Staff").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    noItalic(Component.text("Ability: Fire Freeze ", NamedTextColor.GOLD)
                            .append(Component.text("RIGHT CLICK", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))),
                    noItalic(Component.text("Create a 5-block ring. A second ring", NamedTextColor.GRAY)),
                    noItalic(Component.text("shrinks over 5s; mobs still inside are", NamedTextColor.GRAY)),
                    noItalic(Component.text("frozen for 10s (can't move, can still hit).", NamedTextColor.GRAY)),
                    noItalic(Component.text("Mana Cost: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("500", NamedTextColor.AQUA))),
                    noItalic(Component.text("Cooldown: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("10s", NamedTextColor.GREEN))),
                    Component.empty(),
                    noItalic(Component.text("EPIC", NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
            ));
            mark(meta, KIND_STAFF, "FIRE_FREEZE_STAFF");
        });
        return item;
    }

    public ItemStack crownOfAvarice() {
        ItemStack item = new ItemStack(Material.GOLDEN_HELMET);
        item.editMeta(meta -> {
            meta.displayName(legacy("§6Crown of Avarice").decoration(TextDecoration.ITALIC, false));
            meta.lore(avariceLore(0));
            mark(meta, KIND_AVARICE, "CROWN_OF_AVARICE");
            meta.getPersistentDataContainer().set(plugin.getKey("avarice-coins"), PersistentDataType.LONG, 0L);
        });
        return item;
    }

    public static List<Component> avariceLore(long tracked) {
        boolean full = tracked >= AVARICE_CAP;
        List<Component> lore = new ArrayList<>();
        lore.add(noItalic(Component.text("Coins collected: ", NamedTextColor.GRAY)
                .append(Component.text(SellPrices.coins(tracked) + " / " + SellPrices.coins(AVARICE_CAP), NamedTextColor.GOLD))));
        lore.add(Component.empty());
        if (full) {
            lore.add(noItalic(Component.text("FULL", NamedTextColor.RED).decorate(TextDecoration.BOLD)));
            lore.add(noItalic(Component.text("Perk is now ", NamedTextColor.GRAY)
                    .append(Component.text("2x", NamedTextColor.GOLD))
                    .append(Component.text(" and coins go to your purse.", NamedTextColor.GRAY))));
        } else {
            lore.add(noItalic(Component.text("While worn, coins from Griffin", NamedTextColor.GRAY)));
            lore.add(noItalic(Component.text("Burrows", NamedTextColor.GOLD)
                    .append(Component.text(" and ", NamedTextColor.GRAY))
                    .append(Component.text("Mythological", NamedTextColor.DARK_GREEN))));
            lore.add(noItalic(Component.text("Creatures", NamedTextColor.DARK_GREEN)
                    .append(Component.text(" are absorbed into this", NamedTextColor.GRAY))));
            lore.add(noItalic(Component.text("Crown at ", NamedTextColor.GRAY)
                    .append(Component.text("5x", NamedTextColor.GOLD))
                    .append(Component.text(" and not added to your purse.", NamedTextColor.GRAY))));
            lore.add(noItalic(Component.text("At 1B coins the perk drops to ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("2x", NamedTextColor.GOLD))
                    .append(Component.text(" and stops absorbing.", NamedTextColor.DARK_GRAY))));
        }
        lore.add(Component.empty());
        lore.add(noItalic(Component.text("LEGENDARY HELMET", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
        return lore;
    }

    public static boolean isWearingAvarice(org.bukkit.entity.Player player) {
        return isAvarice(player.getInventory().getHelmet());
    }

    public static long avariceCoins(org.bukkit.entity.Player player) {
        ItemStack helm = player.getInventory().getHelmet();
        if (!isAvarice(helm) || !helm.hasItemMeta()) return 0;
        Long v = helm.getItemMeta().getPersistentDataContainer()
                .get(RitualPlugin.get().getKey("avarice-coins"), PersistentDataType.LONG);
        return v == null ? 0 : v;
    }

    /** Add already-multiplied coins onto the worn crown, clamped at 1B. */
    public static long addAvarice(org.bukkit.entity.Player player, long stored) {
        if (stored <= 0) return avariceCoins(player);
        ItemStack helm = player.getInventory().getHelmet();
        if (!isAvarice(helm)) return 0;
        final long[] total = {0};
        helm.editMeta(meta -> {
            Long prev = meta.getPersistentDataContainer().get(RitualPlugin.get().getKey("avarice-coins"), PersistentDataType.LONG);
            total[0] = Math.min(AVARICE_CAP, (prev == null ? 0L : prev) + stored);
            meta.getPersistentDataContainer().set(RitualPlugin.get().getKey("avarice-coins"), PersistentDataType.LONG, total[0]);
            meta.lore(avariceLore(total[0]));
        });
        return total[0];
    }

    /** @deprecated use addAvarice after applying the 5x yourself */
    public static long absorbAvarice(org.bukkit.entity.Player player, long amount) {
        return addAvarice(player, amount * 5L);
    }

    public ItemStack menu() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        item.editMeta(meta -> {
            meta.displayName(legacy("§aSkyBlock Menu §7(Click)").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    noItalic(Component.text("View your trades and sell", NamedTextColor.GRAY)),
                    noItalic(Component.text("Mythological Ritual drops.", NamedTextColor.GRAY)),
                    Component.empty(),
                    noItalic(Component.text("Click to open!", NamedTextColor.YELLOW))
            ));
            mark(meta, KIND_MENU, "SKYBLOCK_MENU");
        });
        return item;
    }

    public static ItemStack dropItem(SellPrices.Entry e, int amount) {
        ItemStack item = new ItemStack(e.material(), Math.max(1, amount));
        item.editMeta(meta -> {
            applyHypixelDrop(meta, e.id());
            meta.setUnbreakable(false);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(RitualPlugin.get().getKey("drop-id"),
                    PersistentDataType.STRING, e.id());
        });
        return item;
    }

    /** Hypixel SkyBlock display name + lore (NEU repo / in-game). */
    private static void applyHypixelDrop(ItemMeta meta, String id) {
        var ser = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();
        record Skin(String name, String[] lore, boolean glint) {}
        Skin skin = switch (id) {
            case "ANCIENT_CLAW" -> new Skin("§9Ancient Claw", new String[]{
                    "§7The claw of an ancient beast,",
                    "§7hardened by time and countless",
                    "§7battles.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§9§lRARE"
            }, false);
            case "ENCHANTED_GOLD" -> new Skin("§aEnchanted Gold Ingot", new String[]{
                    "§8Brewing Ingredient",
                    "§8Collection Item",
                    "",
                    "§a§lUNCOMMON"
            }, true);
            case "ENCHANTED_ANCIENT_CLAW" -> new Skin("§5Enchanted Ancient Claw", new String[]{
                    "§7The refined claw of an ancient",
                    "§7beast, enchanted with bygone magic",
                    "§7and honed through countless wars.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§5§lEPIC"
            }, true);
            case "ENCHANTED_GOLD_BLOCK" -> new Skin("§9Enchanted Gold Block", new String[]{
                    "§8Brewing Ingredient",
                    "§8Collection Item",
                    "",
                    "§9§lRARE"
            }, true);
            case "GRIFFIN_FEATHER" -> new Skin("§9Griffin Feather", new String[]{
                    "§7The feather of a mythical creature",
                    "§7that was buried for centuries. It still",
                    "§7retains its pleasant scent and",
                    "§7fluffiness despite the damage.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§9§lRARE"
            }, false);
            case "BRAIDED_GRIFFIN_FEATHER" -> new Skin("§5Braided Griffin Feather", new String[]{
                    "§7The exquisite feather of a mythical",
                    "§7creature. Its woven strands shimmer",
                    "§7with an inimitable grace and majesty.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§5§lEPIC"
            }, false);
            case "MYTHOS_FRAGMENT" -> new Skin("§5Mythos Fragment", new String[]{
                    "§7A shard of power left behind by a",
                    "§7Mythological creature.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§5§lEPIC"
            }, false);
            case "HILT" -> new Skin("§9Hilt of Revelations", new String[]{
                    "§7A rusted hilt, dropped by the Minos",
                    "§7Hunter.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§9§lRARE"
            }, false);
            case "PLUSHIE" -> new Skin("§5Crochet Tiger Plushie", new String[]{
                    "§8Consumed on use",
                    "",
                    "§aPet Items §7can boost pets in many",
                    "§7powerful ways! A pet can only hold",
                    "§7one §aPet Item§7, but you can §eswap §7it at",
                    "§7any time!",
                    "§8The pet must be visible to apply the item!",
                    "",
                    "§7Grants §e+35⚔ Attack Speed§7.",
                    "",
                    "§eRight-click on your summoned pet to",
                    "§egive it this item!",
                    "",
                    "§5§lEPIC PET ITEM"
            }, false);
            case "SOUVENIR" -> new Skin("§5Washed-up Souvenir", new String[]{
                    "§8Consumed on use",
                    "",
                    "§aPet Items §7can boost pets in many",
                    "§7powerful ways! A pet can only hold",
                    "§7one §aPet Item§7, but you can §eswap §7it at",
                    "§7any time!",
                    "§8The pet must be visible to apply the item!",
                    "",
                    "§7Grants §3+5α Sea Creature Chance§7.",
                    "",
                    "§eRight-click on your summoned pet to",
                    "§egive it this item!",
                    "",
                    "§5§lEPIC PET ITEM"
            }, false);
            case "CRETAN_URN" -> new Skin("§9Cretan Urn", new String[]{
                    "§7An ornate urn recovered from the",
                    "§7Cretan Bull.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§9§lRARE"
            }, false);
            case "SHELMET" -> new Skin("§9Dwarf Turtle Shelmet", new String[]{
                    "§8Consumed on use",
                    "",
                    "§aPet Items §7can boost pets in many",
                    "§7powerful ways! A pet can only hold",
                    "§7one §aPet Item§7, but you can §eswap §7it at",
                    "§7any time!",
                    "§8The pet must be visible to apply the item!",
                    "",
                    "§eRight-click on your summoned pet to",
                    "§egive it this item!",
                    "",
                    "§9§lRARE PET ITEM"
            }, false);
            case "REMEDIES" -> new Skin("§9Antique Remedies", new String[]{
                    "§8Consumed on use",
                    "",
                    "§aPet Items §7can boost pets in many",
                    "§7powerful ways! A pet can only hold",
                    "§7one §aPet Item§7, but you can §eswap §7it at",
                    "§7any time!",
                    "§8The pet must be visible to apply the item!",
                    "",
                    "§eRight-click on your summoned pet to",
                    "§egive it this item!",
                    "",
                    "§9§lRARE PET ITEM"
            }, false);
            case "DAEDALUS_STICK" -> new Skin("§6Daedalus Stick", new String[]{
                    "§7Drops rare off of Minotaurs from",
                    "§7Diana's Mythological Ritual.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§6§lLEGENDARY"
            }, false);
            case "MINOS_RELIC" -> new Skin("§5Minos Relic", new String[]{
                    "§8Consumed on use",
                    "",
                    "§aPet Items §7can boost pets in many",
                    "§7powerful ways! A pet can only hold",
                    "§7one §aPet Item§7, but you can §eswap §7it at",
                    "§7any time!",
                    "§8The pet must be visible to apply the item!",
                    "",
                    "§7Increases all pet stats by §a33.3%§7.",
                    "",
                    "§eRight-click on your summoned pet to",
                    "§egive it this item!",
                    "",
                    "§5§lEPIC PET ITEM"
            }, false);
            case "BRAIN_FOOD" -> new Skin("§aBrain Food", new String[]{
                    "§8Consumed on use",
                    "",
                    "§aPet Items §7can boost pets in many",
                    "§7powerful ways! A pet can only hold",
                    "§7one §aPet Item§7, but you can §eswap §7it at",
                    "§7any time!",
                    "§8The pet must be visible to apply the item!",
                    "",
                    "§eRight-click on your summoned pet to",
                    "§egive it this item!",
                    "",
                    "§a§lUNCOMMON PET ITEM"
            }, false);
            case "CHIMERA" -> new Skin("§fEnchanted Book", new String[]{
                    "§9Chimera I",
                    "§7Copies the stats of your active",
                    "§7pet.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§9§lRARE"
            }, true);
            case "STINGER" -> new Skin("§6Fateful Stinger", new String[]{
                    "§8Combinable in Anvil",
                    "",
                    "§7When applied to a weapon, increases",
                    "§7its §9Venomous §7level by §a1§7!",
                    "§8Can be applied once.",
                    "§8Requires Venomous VI!",
                    "",
                    "§8§oThe sting of the Manticore is fatal to",
                    "§8§oall but the Elephant.",
                    "",
                    "§6§lLEGENDARY"
            }, false);
            case "MANTICORE" -> new Skin("§6Manti-core", new String[]{
                    "§7An array of cells from different",
                    "§7creatures mashed together in some",
                    "§7crude, unnatural arrangement.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§6§lLEGENDARY"
            }, false);
            case "SHIMMERING_WOOL" -> new Skin("§dShimmering Wool", new String[]{
                    "§7Wool shorn from King Minos' golden",
                    "§7fleece. It still hums with greed.",
                    "",
                    "§eRight-click to view recipes!",
                    "",
                    "§d§lMYTHIC"
            }, false);
            case "CROWN" -> new Skin("§6Crown of Greed", new String[]{
                    "§7Health: §c+100",
                    "§7Defense: §a+50",
                    "",
                    "§6Ability: Indulgence ",
                    "§7Gain §a2x §6Coins §7from §eGriffin Burrows §7and",
                    "§b+10✯ Magic Find §7on §2Mythological §7mobs,",
                    "§7but you take §c1.25x §7damage from them.",
                    "",
                    "§8This item can be reforged!",
                    "§6§lLEGENDARY HELMET"
            }, false);
            default -> null;
        };
        if (skin == null) {
            SellPrices.Entry e = SellPrices.byId(id);
            String name = e == null ? id : e.display();
            meta.displayName(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            return;
        }
        meta.displayName(ser.deserialize(skin.name).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : skin.lore) {
            lore.add(ser.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        if (skin.glint) meta.setEnchantmentGlintOverride(true);
    }

    public static final int CLAW_COMPACT = 160;
    public static final int GOLD_COMPACT = 160;

    /** 160 claws → 1 ench claw; 160 ench gold → 1 ench gold block. */
    public static int compact(org.bukkit.entity.Player player) {
        int made = 0;
        made += compactId(player, "ANCIENT_CLAW", "ENCHANTED_ANCIENT_CLAW", CLAW_COMPACT);
        made += compactId(player, "ENCHANTED_GOLD", "ENCHANTED_GOLD_BLOCK", GOLD_COMPACT);
        return made;
    }

    private static int compactId(org.bukkit.entity.Player player, String fromId, String toId, int per) {
        int have = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (fromId.equals(idOf(stack))) have += stack.getAmount();
        }
        int make = have / per;
        if (make <= 0) return 0;
        int take = make * per;
        for (int slot = 0; slot < player.getInventory().getSize() && take > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!fromId.equals(idOf(stack))) continue;
            int n = Math.min(take, stack.getAmount());
            if (n >= stack.getAmount()) player.getInventory().setItem(slot, null);
            else stack.setAmount(stack.getAmount() - n);
            take -= n;
        }
        SellPrices.Entry to = SellPrices.byId(toId);
        if (to != null) {
            int left = make;
            while (left > 0) {
                int n = Math.min(64, left);
                var leftover = player.getInventory().addItem(dropItem(to, n));
                leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
                left -= n;
            }
        }
        return make;
    }

    public static boolean isSpade(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (KIND_SPADE.equals(kind(stack))) return true;
        Component name = stack.getItemMeta() != null ? stack.getItemMeta().displayName() : null;
        if (name != null && net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(name).contains("Spade")) {
            return true;
        }
        return false;
    }

    public static boolean isAote(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (KIND_AOTE.equals(kind(stack))) return true;
        Component name = stack.getItemMeta() != null ? stack.getItemMeta().displayName() : null;
        if (name != null) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(name);
            if (plain.toLowerCase().contains("aspect of the void") || plain.contains("/tp")) return true;
        }
        return false;
    }

    public static boolean isDaedalus(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (KIND_BLADE.equals(kind(stack))) return true;
        Component name = stack.getItemMeta() != null ? stack.getItemMeta().displayName() : null;
        if (name != null) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(name);
            if (plain.toLowerCase().contains("daedalus blade")) return true;
        }
        return false;
    }

    public static boolean isMenu(ItemStack stack) {
        return stack != null && KIND_MENU.equals(kind(stack));
    }

    public static boolean isMelon(ItemStack stack) {
        return stack != null && KIND_MELON.equals(kind(stack));
    }

    public static boolean isMana(ItemStack stack) {
        return stack != null && KIND_MANA.equals(kind(stack));
    }

    public static boolean isShuriken(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (KIND_SHURIKEN.equals(kind(stack))) return true;
        Component name = stack.getItemMeta() != null ? stack.getItemMeta().displayName() : null;
        if (name != null) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(name);
            if (plain.toLowerCase().contains("shuriken")) return true;
        }
        return false;
    }

    public static boolean isStaff(ItemStack stack) {
        return stack != null && KIND_STAFF.equals(kind(stack));
    }

    public static boolean isAvarice(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (KIND_AVARICE.equals(kind(stack))) return true;
        Component name = stack.getItemMeta() != null ? stack.getItemMeta().displayName() : null;
        if (name != null) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(name);
            if (plain.toLowerCase().contains("crown of avarice")) return true;
        }
        return false;
    }

    public ItemStack fourEyedFish() {
        ItemStack item = new ItemStack(Material.TROPICAL_FISH);
        item.editMeta(meta -> {
            meta.displayName(legacy("§5Four-Eyed Fish").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    noItalic(Component.text("Pet Item", NamedTextColor.DARK_GRAY)),
                    Component.empty(),
                    noItalic(Component.text("While in your inventory:", NamedTextColor.GRAY)),
                    noItalic(Component.text("+2,000 coins", NamedTextColor.GOLD)
                            .append(Component.text(" every Griffin Burrow", NamedTextColor.GRAY))),
                    noItalic(Component.text("you finish. Avarice absorbs.", NamedTextColor.GRAY)),
                    Component.empty(),
                    noItalic(Component.text("EPIC", NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD))
            ));
            mark(meta, KIND_FISH, "FOUR_EYED_FISH");
        });
        return item;
    }

    public ItemStack lootingBook() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        item.editMeta(meta -> {
            meta.displayName(legacy("§9Looting Enchant").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    noItalic(Component.text("Fuse with a Daedalus Blade in /anvil.", NamedTextColor.GRAY)),
                    noItalic(Component.text("1 book = +1 Looting level (max V).", NamedTextColor.GRAY)),
                    noItalic(Component.text("+0.15x drop rates per Looting level.", NamedTextColor.GREEN)),
                    noItalic(Component.text("Max Looting V = ×1.75.", NamedTextColor.GREEN)),
                    noItalic(Component.text("Does not apply to lootshare.", NamedTextColor.RED)),
                    Component.empty(),
                    noItalic(Component.text("RARE", NamedTextColor.BLUE).decorate(TextDecoration.BOLD))
            ));
            mark(meta, KIND_LOOTING, "LOOTING_BOOK");
        });
        return item;
    }

    public ItemStack byId(String id) {
        if (id == null) return null;
        return switch (id) {
            case "DEIFIC_SPADE" -> deificSpade();
            case "ASPECT_OF_THE_VOID" -> aote();
            case "DAEDALUS_BLADE" -> daedalusBlade(1_000_000);
            case "HEALING_MELON" -> melon();
            case "MANA_FRUIT" -> manaFruit();
            case "FIRE_FREEZE_STAFF" -> fireFreezeStaff();
            case "CROWN_OF_AVARICE" -> crownOfAvarice();
            case "EXTREMELY_REAL_SHURIKEN", "SHURIKEN" -> shuriken();
            case "FOUR_EYED_FISH" -> fourEyedFish();
            case "LOOTING_BOOK" -> lootingBook();
            default -> {
                SellPrices.Entry e = SellPrices.byId(id);
                yield e == null ? null : dropItem(e, 1);
            }
        };
    }

    public static String idOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return "";
        var pdc = stack.getItemMeta().getPersistentDataContainer();
        String drop = pdc.get(RitualPlugin.get().getKey("drop-id"), PersistentDataType.STRING);
        if (drop != null) return drop;
        String id = pdc.get(RitualPlugin.get().getKey("id"), PersistentDataType.STRING);
        return id == null ? "" : id;
    }

    public static boolean isLootingBook(ItemStack stack) {
        return stack != null && KIND_LOOTING.equals(kind(stack));
    }

    public static boolean isFish(ItemStack stack) {
        return stack != null && KIND_FISH.equals(kind(stack));
    }

    public static boolean isCore(ItemStack stack) {
        return stack != null && KIND_CORE.equals(kind(stack));
    }

    public static boolean isMythos(ItemStack stack) {
        return stack != null && KIND_MYTHOS.equals(kind(stack));
    }

    public static boolean isMythosEquip(ItemStack stack) {
        if (!isMythos(stack)) return false;
        String id = idOf(stack);
        return id.startsWith("MYTHOS_NECKLACE") || id.equals("MYTHOS_CLOAK")
                || id.equals("MYTHOS_BELT") || id.equals("MYTHOS_BRACELET");
    }

    public static int equipSlotOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return -1;
        Integer v = stack.getItemMeta().getPersistentDataContainer()
                .get(RitualPlugin.get().getKey("equip-slot"), PersistentDataType.INTEGER);
        if (v != null) return v;
        return switch (idOf(stack)) {
            case "MYTHOS_NECKLACE" -> 0;
            case "MYTHOS_CLOAK" -> 1;
            case "MYTHOS_BELT" -> 2;
            case "MYTHOS_BRACELET" -> 3;
            default -> -1;
        };
    }

    public static int lootingOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        var pdc = stack.getItemMeta().getPersistentDataContainer();
        Integer lvl = pdc.get(RitualPlugin.get().getKey("looting"), PersistentDataType.INTEGER);
        if (lvl != null) return Math.max(0, Math.min(5, lvl));
        Integer books = pdc.get(RitualPlugin.get().getKey("looting-books"), PersistentDataType.INTEGER);
        return books == null ? 0 : Math.max(0, Math.min(5, books));
    }

    public static int lootingBooks(ItemStack stack) {
        return lootingOf(stack);
    }

    public static boolean isLootingMax(ItemStack stack) {
        return lootingOf(stack) >= 5;
    }

    public static boolean addLootingBook(ItemStack stack) {
        if (stack == null) return false;
        int level = lootingOf(stack);
        if (level >= 5) return false;
        setLooting(stack, level + 1);
        return true;
    }

    public static void setLooting(ItemStack stack, int level) {
        if (stack == null) return;
        int next = Math.max(0, Math.min(5, level));
        stack.editMeta(meta -> {
            var pdc = meta.getPersistentDataContainer();
            pdc.set(RitualPlugin.get().getKey("looting-books"), PersistentDataType.INTEGER, next);
            pdc.set(RitualPlugin.get().getKey("looting"), PersistentDataType.INTEGER, next);
        });
    }

    public static int lootingLevelFromBooks(int books) {
        return Math.max(0, Math.min(5, books));
    }

    public static int lootingLevel(org.bukkit.entity.Player player) {
        int best = 0;
        for (ItemStack s : player.getInventory().getContents()) {
            if (isDaedalus(s)) best = Math.max(best, lootingOf(s));
        }
        return best;
    }

    /** Additive +0.15 per Looting level. L5 = 1.75x. */
    public static double lootingMultiplier(int level) {
        if (level <= 0) return 1.0;
        return 1.0 + 0.15 * Math.min(5, level);
    }

    public static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    public static boolean hasFourEyedFish(org.bukkit.entity.Player player) {
        for (ItemStack s : player.getInventory().getContents()) {
            if (isFish(s)) return true;
        }
        if (isFish(player.getItemOnCursor())) return true;
        return false;
    }

    public static String kind(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return "";
        String v = stack.getItemMeta().getPersistentDataContainer()
                .get(RitualPlugin.get().getKey("kind"), PersistentDataType.STRING);
        return v == null ? "" : v;
    }

    private void mark(ItemMeta meta, String kind, String sbId) {
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        var pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getKey("kind"), PersistentDataType.STRING, kind);
        pdc.set(plugin.getKey("id"), PersistentDataType.STRING, sbId);
    }

    private static Component noItalic(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static Component legacy(String s) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(s);
    }
}
