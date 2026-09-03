package dev.practice.ritual.ritual;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.economy.SellPrices;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.stats.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class DropTables {
    /** Chance a hitter (not the killer) actually rolls the killer's loot pool. MF does not affect this gate. */
    public static final double LOOTSHARE_ROLL = 0.20;

    private DropTables() {}

    public static double rolledChance(double basePercent, double magicFind) {
        return rolledChance(basePercent, magicFind, 1.0);
    }

    public static double rolledChance(double basePercent, double magicFind, double lootingMult) {
        return (basePercent / 100.0) * Math.max(1.0, lootingMult) * (1.0 + Math.max(0, magicFind) / 100.0);
    }

    public static void rollTreasure(Player player, PlayerStats stats) {
        // Deific Spade wiki weights (total 3221). Feathers are 62.71%, not ~every treasure.
        record Row(String id, String name, int coins, int weight) {}
        Row[] table = {
                new Row("GRIFFIN_FEATHER", "§9Griffin Feather", 0, 2020),
                new Row(null, null, 10_000, 500),
                new Row("MYTHOS_FRAGMENT", "§5Mythos Fragment", 0, 300),
                new Row(null, null, 25_000, 200),
                new Row(null, null, 50_000, 100),
                new Row(null, null, 100_000, 50),
                new Row(null, null, 250_000, 25),
                new Row(null, null, 500_000, 15),
                new Row(null, null, 1_000_000, 10),
                new Row("BRAIDED_GRIFFIN_FEATHER", "§dBraided Griffin Feather", 0, 1)
        };
        int total = 0;
        for (Row row : table) total += row.weight;
        int roll = ThreadLocalRandom.current().nextInt(total);
        Row pick = table[table.length - 1];
        for (Row row : table) {
            roll -= row.weight;
            if (roll < 0) {
                pick = row;
                break;
            }
        }
        if (pick.coins > 0) {
            grantCoins(player, stats, pick.coins, true);
            RitualSounds.rng(player, false);
            return;
        }
        dugTreasure(player, pick.id, pick.name, pick.weight, total);
    }

    public static void rollMobLoot(Player player, MythoKind kind, PlayerStats stats, boolean lootShare, String ownerName) {
        rollMobLoot(player, kind, stats, lootShare, ownerName, false);
    }

    public static void rollMobLoot(Player player, MythoKind kind, PlayerStats stats, boolean lootShare, String ownerName, boolean tagged) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (lootShare) {
            player.sendMessage("§e§lLOOT SHARE §fYou received loot for assisting " + ownerName);
            // Always announce. Only 20% of assists actually roll the killer's table.
            // Magic Find does not change the 20% gate — it applies to the table if this hits.
            if (rng.nextDouble() >= LOOTSHARE_ROLL) return;
        } else {
            int coins = kind.coins(RitualPlugin.get().rituals().session(player).griffin);
            grantCoins(player, stats, coins, false);
        }

        // Wiki Mythic griffin: guaranteed claws, 50% gold of the same count, 50% ench claws on elusive.
        // SBO tracks these ONLY from the [Sacks] hover (Pickuplog.kt → trackWithSacksMessage),
        // and only if it has already seen a Diana death hologram (dianaMobDiedRecently).
        double lootMult = lootShare ? 1.0 : ItemFactory.lootingMultiplier(ItemFactory.lootingLevel(player));
        int claws = lootingRange(kind.clawDrop(), lootMult, rng);
        int gold = rng.nextBoolean() ? lootingRange(kind.clawDrop(), lootMult, rng) : 0;
        int enchClaw = 0;
        if (kind.enchClawDrop() > 0 && rng.nextBoolean()) {
            enchClaw = lootingRange(kind.enchClawDrop(), lootMult, rng);
        }
        giveSilent(player, "ANCIENT_CLAW", claws);
        if (gold > 0) giveSilent(player, "ENCHANTED_GOLD", gold);
        if (enchClaw > 0) giveSilent(player, "ENCHANTED_ANCIENT_CLAW", enchClaw);
        if (RitualPlugin.get().rituals().session(player).stats.autoCompactor) {
            ItemFactory.compact(player);
        }
        // Delay so SBO can observe the 0 HP hologram first (trackWithSacksMessage gates on death).
        final int fClaws = claws, fGold = gold, fEnch = enchClaw;
        RitualPlugin.get().getServer().getScheduler().runTaskLater(RitualPlugin.get(),
                () -> sendSacks(player, fClaws, fGold, fEnch), 10L);

        double mf = stats.magicFind;
        if (tagged) mf *= 1.05;
        double uniqueLoot = lootShare ? 1.0 : lootMult;

        // Same unique table as the killer. Independent rolls (King wool + crown can both drop).
        // Looting does not apply to lootshare — Magic Find only.
        for (Drop d : extras(kind)) {
            double chance = d.useMf
                    ? rolledChance(d.basePercent, mf, uniqueLoot)
                    : d.basePercent / 100.0;
            if (rng.nextDouble() <= chance) {
                rareDrop(player, d.id, d.legacyName, mf, uniqueLoot, d.basePercent, lootShare);
            }
        }
    }

    /**
     * Hypixel sacks chat that SBO's Pickuplog.kt actually parses:
     * <pre>
     *   Pattern "(.*?) item(.*?) (.*?)" with "Sacks" in group 1
     *   then message.siblings: the sibling whose string contains " item"
     *   must have HoverEvent.ShowText whose plain text matches +N Name (Last 5s.)
     *   Names: Ancient Claw / Enchanted Gold Ingot / Enchanted Ancient Claw
     *   (Ingot is stripped before sackDrops.contains)
     * </pre>
     * Hover is on a single sibling that contains the whole visible line so Paper
     * wrapping still leaves a top-level sibling with both " item" and the tooltip.
     */
    static void sendSacks(Player player, int claws, int gold, int enchClaw) {
        if (!player.isOnline()) return;
        int total = 0;
        var ser = LegacyComponentSerializer.legacySection();
        Component hover = Component.empty();
        if (claws > 0) {
            hover = hover.append(ser.deserialize("§a+" + claws + " Ancient Claw (Last 5s.)"));
            total += claws;
        }
        if (gold > 0) {
            if (total > 0) hover = hover.append(Component.newline());
            hover = hover.append(ser.deserialize("§a+" + gold + " Enchanted Gold Ingot (Last 5s.)"));
            total += gold;
        }
        if (enchClaw > 0) {
            if (total > 0) hover = hover.append(Component.newline());
            hover = hover.append(ser.deserialize("§a+" + enchClaw + " Enchanted Ancient Claw (Last 5s.)"));
            total += enchClaw;
        }
        if (total <= 0) return;
        Component line = ser.deserialize("§6[Sacks] §a+" + total + " items§a. §8(Last 5s.)")
                .hoverEvent(HoverEvent.showText(hover));
        // Empty root → content lives in siblings, which is what SBO iterates.
        player.sendMessage(Component.empty().append(line));
    }

    public static void grantCoins(Player player, PlayerStats stats, long amount, boolean treasure) {
        grantCoins(player, stats, amount, treasure ? "treasure" : "mob");
    }

    public static void grantCoins(Player player, PlayerStats stats, long amount, String kind) {
        if (amount <= 0) return;
        long shown;
        if (ItemFactory.isWearingAvarice(player)) {
            long current = ItemFactory.avariceCoins(player);
            if (current >= ItemFactory.AVARICE_CAP) {
                shown = amount * 2L;
                stats.purse += shown;
            } else {
                long want = amount * 5L;
                long room = ItemFactory.AVARICE_CAP - current;
                long absorb = Math.min(want, room);
                ItemFactory.addAvarice(player, absorb);
                shown = absorb;
            }
        } else {
            stats.purse += amount;
            shown = amount;
        }
        // SBO coins: ^§6§lWow! §eYou dug out §6(.*?) coins§e!$
        player.sendMessage("§6§lWow! §eYou dug out §6" + SellPrices.coins(shown) + " coins§e!");
    }

    private static List<Drop> extras(MythoKind kind) {
        List<Drop> list = new ArrayList<>();
        switch (kind) {
            case MANTICORE -> {
                list.add(new Drop("STINGER", "§6Fateful Stinger", 0.5, true));
                list.add(new Drop("MANTICORE", "§6Manti-core", 0.2, true));
            }
            case KING -> {
                // Wiki: independent 0–1x each. Crown 2% Uncommon, Shimmering Wool 0.2% Rare.
                // Both on one King is rare-but-valid (looting 1.75 × MF), not a dual-drop bug.
                list.add(new Drop("SHIMMERING_WOOL", "§dShimmering Wool", 0.2, true));
                list.add(new Drop("CROWN", "§6Crown of Greed", 2.0, true));
            }
            case INQUISITOR -> list.add(new Drop("CHIMERA", chimeraLegacy(), 1.25, true));
            default -> list.add(new Drop(kind.dropId, mainLegacy(kind), kind.dropBasePercent, true));
        }
        return list;
    }

    private static String chimeraLegacy() {
        return "§fEnchanted Book (§d§lChimera 1§f)";
    }

    private static String mainLegacy(MythoKind kind) {
        return switch (kind) {
            case INQUISITOR -> chimeraLegacy();
            case KING -> "§6Crown of Greed";
            case MANTICORE -> "§6Manti-core";
            case SPHINX -> "§aBrain Food";
            case CHAMPION -> "§5Minos Relic";
            case MINOTAUR -> "§6Daedalus Stick";
            case GAIA -> "§9Dwarf Turtle Shelmet";
            case LYNX -> "§5Crochet Tiger Plushie";
            case HUNTER -> "§9Hilt of Revelations";
            case NYMPH -> "§5Washed-up Souvenir";
            case BULL -> "§9Cretan Urn";
            case HARPY -> "§9Antique Remedies";
        };
    }

    private record Drop(String id, String legacyName, double basePercent, boolean useMf) {}

    /**
     * SBO treasure tracker:
     * {@code ^§6§lRARE DROP! §eYou dug out a (.*?)§e!$}
     * then {@code group(1).drop(2)} strips the color code.
     */
    public static void dugTreasure(Player player, String id, String coloredName, int weight, int total) {
        String msg = "§6§lRARE DROP! §eYou dug out a " + coloredName + "§e!";
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg));
        RitualSounds.rng(player, id.equals("BRAIDED_GRIFFIN_FEATHER"));
        give(player, id, 1);
    }

    /** Uniform integer from wiki base up to floor(base * looting), inclusive. */
    static int lootingRange(int base, double lootMult, ThreadLocalRandom rng) {
        int lo = Math.max(0, base);
        int hi = Math.max(lo, (int) Math.floor(base * Math.max(1.0, lootMult)));
        if (hi <= lo) return lo;
        return lo + rng.nextInt(hi - lo + 1);
    }

    public static void rareDrop(Player player, String id, String legacyName, double magicFind,
                               double lootMult, double basePercent, boolean lootShare) {
        String mfBit = " §b(+§b" + fmtMf(magicFind) + " ✯ Magic Find)";
        String msg = "§6§lRARE DROP! " + legacyName + mfBit;
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg));
        if (RitualPlugin.get().rituals().session(player).stats.chanceMessages) {
            double finalPct = rolledChance(basePercent, magicFind, lootMult) * 100.0;
            int lootLvl = lootShare ? 0 : ItemFactory.lootingLevel(player);
            String lootBit = lootShare
                    ? "lootshare (no Looting)"
                    : (lootLvl <= 0 ? "no Looting" : "Looting " + ItemFactory.roman(lootLvl) + " (×" + fmtMult(lootMult) + ")");
            player.sendMessage("§7Chance: §e" + fmtPct(finalPct) + "% §8· base " + fmtPct(basePercent) + "% · "
                    + lootBit + " · " + fmtMf(magicFind) + " ✯");
        }
        RitualSounds.rng(player, id.equals("CHIMERA") || id.equals("BRAIDED_GRIFFIN_FEATHER")
                || id.equals("SHIMMERING_WOOL") || id.equals("MANTICORE"));
        give(player, id, 1);
    }

    private static String fmtPct(double pct) {
        if (pct >= 10) return String.format(Locale.US, "%.1f", pct);
        if (pct >= 1) return String.format(Locale.US, "%.2f", pct);
        if (pct >= 0.01) return String.format(Locale.US, "%.3f", pct);
        return String.format(Locale.US, "%.4f", pct);
    }

    private static String fmtMult(double m) {
        return String.format(Locale.US, "%.2f", m);
    }

    private static String fmtMf(double mf) {
        if (Math.abs(mf - Math.rint(mf)) < 1e-6) return String.format(Locale.US, "%.0f", mf);
        return String.format(Locale.US, "%.1f", mf);
    }

    public static void giveSilent(Player player, String id, int amount) {
        if (amount > 0) give(player, id, amount);
    }

    public static void give(Player player, String id, int amount) {
        SellPrices.Entry e = SellPrices.byId(id);
        if (e == null || amount <= 0) return;
        int left = amount;
        while (left > 0) {
            int n = Math.min(64, left);
            ItemStack item = ItemFactory.dropItem(e, n);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
            left -= n;
        }
    }

    public static ItemStack namedDrop(SellPrices.Entry e) {
        return ItemFactory.dropItem(e, 1);
    }
}
