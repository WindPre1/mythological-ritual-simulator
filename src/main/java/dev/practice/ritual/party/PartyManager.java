package dev.practice.ritual.party;

import dev.practice.ritual.RitualPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PartyManager implements Listener {
    public static final class Party {
        public final UUID leader;
        public final Set<UUID> members = new HashSet<>();

        Party(UUID leader) {
            this.leader = leader;
            members.add(leader);
        }
    }

    private final RitualPlugin plugin;
    private final Map<UUID, Party> byPlayer = new HashMap<>();
    private final Map<UUID, UUID> pending = new HashMap<>(); // invitee -> leader
    private final Map<UUID, Long> pendingAt = new HashMap<>();

    public PartyManager(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    public Party partyOf(UUID id) {
        return byPlayer.get(id);
    }

    public void invite(Player from, Player to) {
        Party p = byPlayer.get(from.getUniqueId());
        if (p == null) {
            p = new Party(from.getUniqueId());
            byPlayer.put(from.getUniqueId(), p);
        }
        if (!p.leader.equals(from.getUniqueId())) {
            from.sendMessage("§cOnly the party leader can invite.");
            return;
        }
        if (byPlayer.containsKey(to.getUniqueId())) {
            from.sendMessage("§cThat player is already in a party.");
            return;
        }
        pending.put(to.getUniqueId(), from.getUniqueId());
        pendingAt.put(to.getUniqueId(), System.currentTimeMillis());
        from.sendMessage("§aInvited §e" + to.getName() + " §ato the party.");
        to.sendMessage("§e" + from.getName() + " §ainvited you to a party. §e/p accept");
    }

    public void accept(Player player) {
        UUID leader = pending.remove(player.getUniqueId());
        pendingAt.remove(player.getUniqueId());
        if (leader == null) {
            player.sendMessage("§cNo pending invite.");
            return;
        }
        Party p = byPlayer.get(leader);
        if (p == null) {
            p = new Party(leader);
            byPlayer.put(leader, p);
        }
        p.members.add(player.getUniqueId());
        byPlayer.put(player.getUniqueId(), p);
        broadcast(p, "§e" + player.getName() + " §ajoined the party.");
    }

    public void leave(Player player) {
        Party p = byPlayer.remove(player.getUniqueId());
        if (p == null) {
            player.sendMessage("§cYou are not in a party.");
            return;
        }
        p.members.remove(player.getUniqueId());
        player.sendMessage("§cYou left the party.");
        if (p.leader.equals(player.getUniqueId()) || p.members.isEmpty()) {
            broadcast(p, "§cThe party was disbanded.");
            for (UUID id : p.members) byPlayer.remove(id);
        } else {
            broadcast(p, "§e" + player.getName() + " §cleft the party.");
        }
    }

    public void chat(Player from, String message) {
        Party p = byPlayer.get(from.getUniqueId());
        if (p == null) {
            from.sendMessage("§cYou are not in a party. §e/p <player>");
            return;
        }
        String legacy = "§9Party §8> §b" + from.getName() + "§f: " + message;
        for (UUID id : p.members) {
            Player m = Bukkit.getPlayer(id);
            if (m != null && m.isOnline()) m.sendMessage(legacy);
        }
    }

    /** /pc typed as a command or leaked into public chat. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null) return;
        String body = stripPc(raw);
        if (body == null) return;
        event.setCancelled(true);
        chat(event.getPlayer(), body);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        String raw = event.getMessage();
        if (raw == null) return;
        String body = stripPc(raw);
        if (body == null) {
            // If a public message already looks like party chat, keep it party-only.
            String plain = raw.replace("§", "").toLowerCase(Locale.ROOT);
            if (plain.startsWith("party ") && plain.contains(">")) {
                event.setCancelled(true);
                Party p = partyOf(event.getPlayer().getUniqueId());
                event.getRecipients().clear();
                if (p != null) {
                    for (UUID id : p.members) {
                        Player m = Bukkit.getPlayer(id);
                        if (m != null && m.isOnline()) event.getRecipients().add(m);
                    }
                }
            }
            return;
        }
        event.setCancelled(true);
        event.getRecipients().clear();
        Player from = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> chat(from, body));
    }

    /** @return party-chat body, or null if this is not a /pc message */
    private static String stripPc(String raw) {
        String t = raw.trim();
        String lower = t.toLowerCase(Locale.ROOT);
        String[] prefixes = {
                "/pc ", "/pc", "/pchat ", "/pchat", "/partychat ", "/partychat",
                "/minecraft:pc ", "/minecraft:pc",
                "/ritualpractice:pc ", "/ritualpractice:pc",
                "pc "
        };
        for (String prefix : prefixes) {
            if (lower.equals(prefix.trim())) return "";
            if (lower.startsWith(prefix)) return t.substring(prefix.length());
        }
        return null;
    }

    public void broadcast(Party p, String msg) {
        for (UUID id : p.members) {
            Player m = Bukkit.getPlayer(id);
            if (m != null && m.isOnline()) m.sendMessage("§9Party §8> " + msg);
        }
    }

    public void onQuit(UUID id) {
        pending.remove(id);
        leaveSilent(id);
    }

    private void leaveSilent(UUID id) {
        Party p = byPlayer.remove(id);
        if (p == null) return;
        p.members.remove(id);
        if (p.leader.equals(id) || p.members.isEmpty()) {
            for (UUID m : p.members) byPlayer.remove(m);
        }
    }
}
