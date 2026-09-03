package dev.practice.ritual.scoreboard;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.command.SetStatCommand;
import dev.practice.ritual.economy.SellPrices;
import dev.practice.ritual.stats.PlayerStats;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class SkyblockBoard implements Listener {
    private final RitualPlugin plugin;

    public SkyblockBoard(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player) {
        redraw(player);
        player.playerListName(Component.text("Area: Hub"));
        player.sendPlayerListHeaderAndFooter(
                Component.text("SKYBLOCK").color(NamedTextColor.GOLD)
                        .append(Component.newline())
                        .append(Component.text("Area: Hub", NamedTextColor.AQUA)),
                Component.text("Ritual Practice")
        );
    }

    public void redraw(Player player) {
        PlayerStats s = plugin.rituals().session(player).stats;
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("sb", Criteria.DUMMY, Component.text("SKYBLOCK", NamedTextColor.YELLOW));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        try {
            obj.numberFormat(NumberFormat.blank());
        } catch (Throwable ignored) {
        }
        obj.getScore(" §7Late Summer 27th").setScore(10);
        obj.getScore("§f ").setScore(9);
        obj.getScore("§f ⏣ §bHub").setScore(8);
        obj.getScore("§f  ").setScore(7);
        obj.getScore("§dMythological Ritual Practice").setScore(6);
        obj.getScore("§f   ").setScore(5);
        obj.getScore("§6Purse: " + SellPrices.coins(s.purse)).setScore(4);
        obj.getScore("§8 ").setScore(3);
        obj.getScore("§b✯ Magic Find: " + SetStatCommand.fmt(s.magicFind)).setScore(2);
        obj.getScore("§d❖ Tracking: " + SetStatCommand.fmt(s.tracking)).setScore(1);
        obj.getScore("§0 ").setScore(0);
        player.setScoreboard(board);
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playerListName(Component.text("Area: Hub"));
            redraw(player);
            actionBar(player);
        }
    }

    private void actionBar(Player player) {
        PlayerStats s = plugin.rituals().session(player).stats;
        String line = "§c" + compact(s.health) + "/" + compact(s.maxHealth) + "❤   §a"
                + compact(s.defense) + "❈ Defense   §b"
                + compact(s.mana) + "/" + compact(s.maxMana) + "✎ Mana";
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(line));
    }

    private static String compact(double n) {
        return String.format(java.util.Locale.US, "%,.0f", n);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> apply(event.getPlayer()), 10L);
    }
}
