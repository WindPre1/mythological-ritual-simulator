package dev.practice.ritual.command;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.stats.PlayerStats;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class SetStatCommand implements CommandExecutor, TabCompleter {
    public enum Kind {
        MAGIC_FIND("Magic Find", s -> s.magicFind, (s, v) -> s.magicFind = v, PlayerStats.CAP_MF, "✯"),
        TRACKING("Tracking", s -> s.tracking, (s, v) -> s.tracking = v, PlayerStats.CAP_TRACKING, ""),
        HEALTH("Health", s -> s.maxHealth, (s, v) -> { s.maxHealth = v; s.health = v; }, PlayerStats.CAP_HEALTH, "❤"),
        DEFENSE("Defense", s -> s.defense, (s, v) -> s.defense = v, PlayerStats.CAP_DEFENSE, "❈"),
        MANA("Mana", s -> s.maxMana, (s, v) -> { s.maxMana = v; s.mana = Math.min(s.mana, v); }, PlayerStats.CAP_MANA, "✎"),
        DAMAGE("Damage", s -> s.damage, (s, v) -> s.damage = v, PlayerStats.CAP_DAMAGE, "");

        final String label;
        final Function<PlayerStats, Double> get;
        final BiConsumer<PlayerStats, Double> set;
        final double cap;
        final String suffix;

        Kind(String label, Function<PlayerStats, Double> get, BiConsumer<PlayerStats, Double> set, double cap, String suffix) {
            this.label = label;
            this.get = get;
            this.set = set;
            this.cap = cap;
            this.suffix = suffix;
        }
    }

    private final RitualPlugin plugin;
    private final Kind kind;

    public SetStatCommand(RitualPlugin plugin, Kind kind) {
        this.plugin = plugin;
        this.kind = kind;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        PlayerStats s = plugin.rituals().session(player).stats;
        if (args.length == 0) {
            player.sendMessage("§e" + kind.label + ": §a" + fmt(kind.get.apply(s))
                    + (kind.suffix.isEmpty() ? "" : " " + kind.suffix)
                    + " §7(cap " + fmt(kind.cap) + ")");
            return true;
        }
        double value;
        try {
            value = parseAmount(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cUsage: /" + label + " <number>  §7ex: 1.5m, 5.5k, 400, 10m");
            return true;
        }
        if (value < 0) value = 0;
        if (value > kind.cap) {
            value = kind.cap;
            player.sendMessage("§cHard cap is " + fmt(kind.cap) + ".");
        }
        kind.set.accept(s, value);
        s.clamp();
        plugin.rituals().applyVanilla(player);
        if (kind == Kind.DAMAGE) {
            dev.practice.ritual.item.ItemFactory.refreshDaedalus(player, s.damage);
        }
        plugin.rituals().save(player);
        player.sendMessage("§a" + kind.label + " set to §e" + fmt(kind.get.apply(s))
                + (kind.suffix.isEmpty() ? "" : " " + kind.suffix));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return switch (kind) {
            case MAGIC_FIND -> List.of("0", "400", "1.5k", "2.1b");
            case TRACKING -> List.of("0", "50", "1k", "2.1b");
            case HEALTH -> List.of("5k", "5.5k", "2.1b");
            case DEFENSE -> List.of("1k", "1.5k", "2.1b");
            case MANA -> List.of("1k", "5k", "2.1b");
            case DAMAGE -> List.of("1m", "10m", "2.1b");
        };
    }

    /** k = thousand, m = million, b = billion. */
    public static double parseAmount(String raw) {
        String s = raw.trim().replace(",", "").replace("_", "").replace(" ", "");
        if (s.isEmpty()) throw new NumberFormatException("empty");
        double mult = 1;
        char last = Character.toLowerCase(s.charAt(s.length() - 1));
        if (last == 'k' || last == 'm' || last == 'b') {
            mult = last == 'k' ? 1_000 : last == 'm' ? 1_000_000 : 1_000_000_000;
            s = s.substring(0, s.length() - 1);
        }
        return Double.parseDouble(s) * mult;
    }

    public static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.format(Locale.US, "%,.0f", v);
        return String.format(Locale.US, "%,.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
