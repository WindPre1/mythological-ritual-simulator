package dev.practice.ritual.ritual;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public final class RitualSounds {

    private RitualSounds() {}

    public static boolean enabled() {
        return dev.practice.ritual.RitualPlugin.get().getConfig().getBoolean("sounds", true);
    }

    public static void pling(Player player, float pitch) {
        if (!enabled()) return;
        float p = Math.max(0.5f, Math.min(2.0f, pitch));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 0.85f, p);
    }

    public static void ding(Player player) {
        if (!enabled()) return;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.7f, 1.4f);
    }

    public static void rng(Player player, boolean jackpot) {
        if (!enabled()) return;
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.4f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 0.7f, 1.2f);
        if (jackpot) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.7f, 1.2f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 0.6f);
        }
    }

    public static void mobSpawn(Player player, Location at) {
        if (!enabled()) return;
        player.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    public static void fuse(Player player, Location at) {
        if (!enabled()) return;
        player.playSound(at, Sound.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.0f, 0.85f);
        player.playSound(at, Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    public static void treasure(Player player, Location at) {
        if (!enabled()) return;
        player.playSound(at, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 1.6f);
        player.playSound(at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.8f, 1.3f);
    }

    public static void chainDone(Player player, Location at) {
        if (!enabled()) return;
        player.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.2f, 0.7f);
        player.playSound(at, Sound.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.0f, 0.7f);
    }

    public static void dig(Player player, Location at, BurrowType type, MythoKind kind, boolean chainDone) {
        if (!enabled()) return;
        fuse(player, at);
        if (type == BurrowType.MOB) mobSpawn(player, at);
        if (type == BurrowType.TREASURE) treasure(player, at);
        if (chainDone) chainDone(player, at);
    }
}
