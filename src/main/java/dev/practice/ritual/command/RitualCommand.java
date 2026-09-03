package dev.practice.ritual.command;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.ritual.GriffinRarity;
import dev.practice.ritual.ritual.RitualManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class RitualCommand implements CommandExecutor, TabCompleter {
    private final RitualPlugin plugin;

    public RitualCommand(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) {
            player.sendMessage("§e/ritual <start|stop|reset|give|griffin|rates|status|warp>");
            return true;
        }
        RitualManager mgr = plugin.rituals();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                mgr.start(player);
                mgr.save(player);
            }
            case "stop" -> {
                mgr.stop(player);
                mgr.save(player);
            }
            case "reset" -> {
                mgr.reset(player);
                mgr.save(player);
            }
            case "give", "kit" -> {
                plugin.rituals().giveKit(player);
                player.sendMessage("§aGave spade, Aspect Of The Void, Daedalus Blade, Healing Melon, Mana Fruit, and SkyBlock Menu.");
            }
            case "griffin" -> {
                mgr.session(player).griffin = GriffinRarity.MYTHIC;
                if (args.length >= 2 && !args[1].equalsIgnoreCase("MYTHIC")) {
                    player.sendMessage("§cGriffin is locked to §dMYTHIC§c. Lower tiers are disabled.");
                    return true;
                }
                player.sendMessage("§eGriffin: §dMYTHIC §7(Empyrean — locked)");
            }
            case "rates" -> {
                player.sendMessage("§eUse §a/setmagicfind <n> §eand §a/settracking <n>§e.");
                player.sendMessage("§7Current: mf=" + SetStatCommand.fmt(mgr.session(player).stats.magicFind)
                        + " tracking=" + SetStatCommand.fmt(mgr.session(player).stats.tracking));
            }
            case "status" -> {
                RitualManager.PlayerSession s = mgr.session(player);
                long starts = s.burrows.stream().filter(b -> b.index == 1 && !b.dug).count();
                int undug = mgr.undugCount(s);
                player.sendMessage("§eactive=" + s.active + " griffin=" + s.griffin
                        + " undug=" + undug + "/" + mgr.startCap()
                        + " starts=" + starts
                        + " mf=" + SetStatCommand.fmt(s.stats.magicFind)
                        + " tracking=" + SetStatCommand.fmt(s.stats.tracking)
                        + " dmg=" + SetStatCommand.fmt(s.stats.damage));
            }
            case "warp" -> {
                if (args.length < 2) {
                    player.sendMessage("§e/ritual warp <hub|castle|wizard|crypt|stonks|da|museum|taylor>");
                    return true;
                }
                plugin.warps().warp(player, args[1]);
            }
            default -> player.sendMessage("§cUnknown subcommand.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("start", "stop", "reset", "give", "griffin", "rates", "status", "warp")
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("griffin")) {
            return Stream.of("MYTHIC")
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("warp")) {
            String p = args[1].toLowerCase(Locale.ROOT);
            return Stream.of("hub", "castle", "wizard", "crypt", "stonks", "da", "museum", "taylor")
                    .filter(s -> s.startsWith(p)).toList();
        }
        return List.of();
    }
}
