package dev.practice.ritual.command;

import dev.practice.ritual.RitualPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WarpCommand implements CommandExecutor, TabCompleter, Listener {
    private static final Pattern WARP_TEXT = Pattern.compile(
            "^[/]?\\s*(?:warp|warps|hubwarp|rwarp|sbwarp)\\s+(\\S+)",
            Pattern.CASE_INSENSITIVE);

    private final RitualPlugin plugin;

    public WarpCommand(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) {
            player.sendMessage("§eWarps: " + String.join(", ", names()));
            return true;
        }
        warp(player, args[0]);
        return true;
    }

    public boolean warp(Player player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT).replace(",", "");
        if (name.equals("darkauction") || name.equals("dark_auction")) name = "da";
        var sec = plugin.getConfig().getConfigurationSection("warps." + name);
        if (sec == null) {
            player.sendMessage("§cUnknown warp. " + String.join(", ", names()));
            return false;
        }
        World world = player.getWorld();
        Location loc = new Location(world,
                sec.getDouble("x"),
                sec.getDouble("y"),
                sec.getDouble("z"),
                (float) sec.getDouble("yaw"),
                (float) sec.getDouble("pitch"));
        player.teleport(loc);
        player.sendMessage("§aWarped to §e" + name);
        return true;
    }

    /** SBO / some clients send `/warp wizard` as chat, not as a command. */
    public boolean tryWarpFromText(Player player, String text) {
        if (text == null) return false;
        String t = text.trim();
        Matcher m = WARP_TEXT.matcher(t);
        if (!m.find()) return false;
        warp(player, m.group(1));
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (tryWarpFromText(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (raw == null) return;
        String t = raw.trim();
        if (!WARP_TEXT.matcher(t).find()) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> tryWarpFromText(player, t));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String n : names()) if (n.startsWith(p)) out.add(n);
            return out;
        }
        return List.of();
    }

    private List<String> names() {
        var sec = plugin.getConfig().getConfigurationSection("warps");
        return sec == null ? List.of() : new ArrayList<>(sec.getKeys(false));
    }
}
