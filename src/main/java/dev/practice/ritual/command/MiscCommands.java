package dev.practice.ritual.command;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.economy.SellPrices;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class MiscCommands implements CommandExecutor, TabCompleter {
    private final RitualPlugin plugin;

    public MiscCommands(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("pc")) {
            if (!(sender instanceof Player player)) return true;
            plugin.parties().chat(player, String.join(" ", args));
            return true;
        }
        if (name.equals("p") || name.equals("party")) {
            if (!(sender instanceof Player player)) return true;
            return party(player, args);
        }
        if (name.equals("trades")) {
            if (sender instanceof Player p) plugin.trades().open(p);
            return true;
        }
        if (name.equals("items")) {
            if (sender instanceof Player p) plugin.itemsGui().open(p);
            return true;
        }
        if (name.equals("anvil")) {
            if (sender instanceof Player p) plugin.anvil().open(p);
            return true;
        }
        if (name.equals("togglebreak")) {
            if (!(sender instanceof Player player)) return true;
            if (!player.isOp()) {
                player.sendMessage("§cOps only.");
                return true;
            }
            var s = plugin.rituals().session(player);
            s.stats.breakBlocks = !s.stats.breakBlocks;
            player.sendMessage(s.stats.breakBlocks ? "§aBlock breaking ON." : "§cBlock breaking OFF.");
            return true;
        }
        if (name.equals("togglefakelag")) {
            if (!sender.isOp()) {
                sender.sendMessage("§cOps only.");
                return true;
            }
            boolean on = plugin.toggleFakeLag();
            sender.sendMessage(on ? "§eFake lag ON (~18 TPS)." : "§aFake lag OFF.");
            return true;
        }
        if (name.equals("togglechance") || name.equals("chancemsg") || name.equals("toggledropchance")) {
            if (!(sender instanceof Player player)) return true;
            var s = plugin.rituals().session(player);
            s.stats.chanceMessages = !s.stats.chanceMessages;
            plugin.rituals().save(player);
            player.sendMessage(s.stats.chanceMessages
                    ? "§aChance messages ON."
                    : "§cChance messages OFF.");
            return true;
        }
        if (name.equals("compactor") || name.equals("autocompactor") || name.equals("togglecompactor")) {
            if (!(sender instanceof Player player)) return true;
            var s = plugin.rituals().session(player);
            s.stats.autoCompactor = !s.stats.autoCompactor;
            plugin.rituals().save(player);
            if (s.stats.autoCompactor) {
                int n = ItemFactory.compact(player);
                player.sendMessage("§aAuto Compactor ON. §7(160 claws → ench claw, 160 ench gold → ench gold block)"
                        + (n > 0 ? " §eCompacted " + n + "." : ""));
            } else {
                player.sendMessage("§cAuto Compactor OFF.");
            }
            return true;
        }
        if (name.equals("purse")) {
            if (!(sender instanceof Player player)) return true;
            player.sendMessage("§6Purse: " + SellPrices.coins(plugin.rituals().session(player).stats.purse) + " coins");
            return true;
        }
        return true;
    }

    private boolean party(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("§e/p <player> §7invite  §e/p accept §7 §e/p leave §7 §e/p list");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "accept" -> plugin.parties().accept(player);
            case "leave", "disband" -> plugin.parties().leave(player);
            case "list" -> {
                var p = plugin.parties().partyOf(player.getUniqueId());
                if (p == null) {
                    player.sendMessage("§cNot in a party.");
                    return true;
                }
                StringBuilder sb = new StringBuilder("§9Party: ");
                for (var id : p.members) {
                    Player m = Bukkit.getPlayer(id);
                    sb.append(m == null ? "§7offline" : "§e" + m.getName()).append("§7, ");
                }
                player.sendMessage(sb.toString());
            }
            default -> {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage("§cYou cannot invite yourself.");
                    return true;
                }
                plugin.parties().invite(player, target);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if ((name.equals("p") || name.equals("party")) && args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            List<String> out = Stream.concat(
                    Stream.of("accept", "leave", "list"),
                    Bukkit.getOnlinePlayers().stream().map(Player::getName)
            ).filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p)).toList();
            return out;
        }
        return List.of();
    }
}
