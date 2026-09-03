package dev.practice.ritual.mob;

import com.destroystokyo.paper.profile.ProfileProperty;
import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.ritual.MythoKind;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/**
 * Applies mannequin skins via a static ResolvableProfile textures property.
 * {@code setSkin(url)} alone is not enough on 1.21.9+ mannequins.
 */
public final class MythoSkins {
    private MythoSkins() {}

    public static File folder(RitualPlugin plugin) {
        String cfg = plugin.getConfig().getString("skins-folder", "");
        if (cfg != null && !cfg.isBlank()) {
            File f = new File(cfg);
            if (f.isDirectory()) return f;
        }
        File data = new File(plugin.getDataFolder(), "MythoSkins");
        if (!data.exists()) data.mkdirs();
        extractBundled(plugin, data);
        return data;
    }

    public static void apply(RitualPlugin plugin, Mannequin mannequin, MythoKind kind) {
        File dir = folder(plugin);
        String hash = readHash(dir, kind);
        if (hash == null) hash = fallbackHash(kind);
        if (hash == null) return;
        if (!setHash(mannequin, kind, hash) && plugin != null) {
            plugin.getLogger().warning("Failed to apply mytho skin for " + kind.name());
        }
    }

    private static void extractBundled(RitualPlugin plugin, File data) {
        String[] names = {
                "README.txt", "NYMPH.txt", "HARPY.txt", "MINOTAUR.txt", "CHAMPION.txt",
                "INQUISITOR.txt", "SPHINX.txt", "MANTICORE.txt", "KING.txt"
        };
        for (String name : names) {
            File dest = new File(data, name);
            if (dest.exists()) continue;
            try {
                plugin.saveResource("MythoSkins/" + name, false);
            } catch (Exception ignored) {
            }
        }
    }

    private static String readHash(File dir, MythoKind kind) {
        File txt = find(dir, kind, ".txt");
        if (txt != null) {
            String hash = parseHash(read(txt));
            if (hash != null) return hash;
        }
        File url = find(dir, kind, ".url");
        if (url != null) {
            String hash = parseHash(read(url));
            if (hash != null) return hash;
        }
        return null;
    }

    private static String read(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String parseHash(String raw) {
        if (raw == null) return null;
        String hash = raw.trim()
                .replace("http://textures.minecraft.net/texture/", "")
                .replace("https://textures.minecraft.net/texture/", "")
                .replaceAll("\\s+", "");
        if (hash.length() >= 32 && hash.length() <= 64 && hash.matches("[0-9a-fA-F]+")) {
            return hash.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static File find(File dir, MythoKind kind, String ext) {
        if (dir == null || !dir.isDirectory()) return null;
        String display = kind.display.replace(" ", "");
        String under = kind.display.replace(" ", "_");
        String[] names = {
                kind.name() + ext,
                kind.name().toLowerCase(Locale.ROOT) + ext,
                display + ext,
                under + ext,
                kind.display + ext
        };
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (String name : names) {
            for (File f : files) {
                if (f.getName().equalsIgnoreCase(name)) return f;
            }
        }
        return null;
    }

    public static String fallbackHash(MythoKind kind) {
        return switch (kind) {
            case NYMPH -> "83b31f1b501a69d7ca82e8c57f470adffa8ddd0e85cdf2d831a9c4a9dd82c5d9";
            case HARPY -> "865b45a954e9e14170d8cea0f33d4bd063dce6bbbe76e0db97cb22136af1f848";
            case MINOTAUR -> "12e3135c0166091439252da5322dc928c8891c2e2c7ee5a74f8b60bb93d78dff";
            case CHAMPION -> "cc3425d48f70a631485fd92690397167d4428358de89c06fe5250920a9e29262";
            case INQUISITOR -> "f35fb8cf2a5b026da93a1d6caa47f6de7f9adfe34cae0a290b373eab62cca730";
            case SPHINX -> "2372f8d9d0fa23b9b9db789100a2cb3e39c2f7759c4029b83022ab0ab9f85356";
            case MANTICORE -> "d5d16f94a89b3e38105fd1eb801f4ac2b6693a0244c081aef90680da7cf2c9ff";
            case KING -> "a40b6cf72f68f3e6cb1c5cc79c9a5d9d6bf31670a8c5807c61df35f565f56912";
            default -> null;
        };
    }

    private static boolean setHash(Mannequin mannequin, MythoKind kind, String hash) {
        String url = "http://textures.minecraft.net/texture/" + hash;
        String name = kind.display.replace(" ", "");
        if (name.length() > 16) name = name.substring(0, 16);
        UUID uuid = UUID.nameUUIDFromBytes(("mytho-" + kind.name()).getBytes(StandardCharsets.UTF_8));
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        String value = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        ProfileProperty textures = new ProfileProperty("textures", value);

        try {
            ResolvableProfile built = ResolvableProfile.resolvableProfile()
                    .name(name)
                    .uuid(uuid)
                    .addProperty(textures)
                    .build();
            mannequin.setProfile(built);
            showLayers(mannequin);
            return true;
        } catch (Throwable ignored) {
        }
        try {
            com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(uuid, name);
            profile.setProperty(textures);
            try {
                profile.getTextures().setSkin(java.net.URI.create(url).toURL());
            } catch (Throwable ignored) {
            }
            mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
            showLayers(mannequin);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void showLayers(Mannequin mannequin) {
        try {
            var parts = mannequin.getSkinParts();
            parts.setCapeEnabled(false);
            parts.setJacketEnabled(true);
            parts.setLeftSleeveEnabled(true);
            parts.setRightSleeveEnabled(true);
            parts.setLeftPantsEnabled(true);
            parts.setRightPantsEnabled(true);
            parts.setHatsEnabled(true);
            mannequin.setSkinParts(parts);
        } catch (Throwable ignored) {
        }
    }
}
