package dev.practice.ritual.economy;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SellPrices {
    public record Entry(String id, String display, Material material, long price) {}

    private static final Map<String, Entry> BY_ID = new LinkedHashMap<>();

    static {
        add("GRIFFIN_FEATHER", "Griffin Feather", Material.FEATHER, 200_000);
        add("BRAIDED_GRIFFIN_FEATHER", "Braided Griffin Feather", Material.FEATHER, 40_000_000);
        add("CHIMERA", "Enchanted Book (Chimera 1)", Material.ENCHANTED_BOOK, 30_000_000);
        add("MANTICORE", "Manti-core", Material.BLAZE_ROD, 30_000_000);
        add("STINGER", "Fateful Stinger", Material.ARROW, 5_000_000);
        add("CRETAN_URN", "Cretan Urn", Material.DECORATED_POT, 250_000);
        add("SHELMET", "Dwarf Turtle Shelmet", Material.TURTLE_HELMET, 250_000);
        add("PLUSHIE", "Crochet Tiger Plushie", Material.TOTEM_OF_UNDYING, 250_000);
        add("REMEDIES", "Antique Remedies", Material.GLISTERING_MELON_SLICE, 250_000);
        add("MYTHOS_FRAGMENT", "Mythos Fragment", Material.PRISMARINE_SHARD, 25_000);
        add("HILT", "Hilt of Revelations", Material.IRON_SWORD, 150_000);
        add("SHIMMERING_WOOL", "Shimmering Wool", Material.WHITE_WOOL, 50_000_000);
        add("CROWN", "Crown of Greed", Material.GOLDEN_HELMET, 1_000_000);
        add("DAEDALUS_STICK", "Daedalus Stick", Material.STICK, 2_500_000);
        add("MINOS_RELIC", "Minos Relic", Material.PRISMARINE_CRYSTALS, 30_000_000);
        add("BRAIN_FOOD", "Brain Food", Material.GOLDEN_APPLE, 2_000_000);
        add("SOUVENIR", "Washed-up Souvenir", Material.NAUTILUS_SHELL, 250_000);
        add("ANCIENT_CLAW", "Ancient Claw", Material.FLINT, 500);
        add("ENCHANTED_ANCIENT_CLAW", "Enchanted Ancient Claw", Material.PRISMARINE_CRYSTALS, 80_000);
        add("ENCHANTED_GOLD", "Enchanted Gold Ingot", Material.GOLD_INGOT, 1_200);
        add("ENCHANTED_GOLD_BLOCK", "Enchanted Gold Block", Material.GOLD_BLOCK, 192_000);
    }

    private static void add(String id, String display, Material mat, long price) {
        BY_ID.put(id, new Entry(id, display, mat, price));
    }

    public static Entry byId(String id) {
        return BY_ID.get(id);
    }

    public static Entry byDisplay(String name) {
        if (name == null) return null;
        String n = name.replaceAll("§[0-9a-fk-orx]", "").trim().toLowerCase(Locale.ROOT);
        for (Entry e : BY_ID.values()) {
            if (e.display.toLowerCase(Locale.ROOT).equals(n)) return e;
        }
        if (n.contains("chimera")) return BY_ID.get("CHIMERA");
        if (n.contains("braided")) return BY_ID.get("BRAIDED_GRIFFIN_FEATHER");
        if (n.contains("griffin feather")) return BY_ID.get("GRIFFIN_FEATHER");
        if (n.contains("manti")) return BY_ID.get("MANTICORE");
        if (n.contains("stinger")) return BY_ID.get("STINGER");
        if (n.contains("shelmet")) return BY_ID.get("SHELMET");
        if (n.contains("plushie")) return BY_ID.get("PLUSHIE");
        if (n.contains("remed")) return BY_ID.get("REMEDIES");
        if (n.contains("urn")) return BY_ID.get("CRETAN_URN");
        if (n.contains("hilt")) return BY_ID.get("HILT");
        if (n.contains("wool")) return BY_ID.get("SHIMMERING_WOOL");
        if (n.contains("crown")) return BY_ID.get("CROWN");
        if (n.contains("daedalus")) return BY_ID.get("DAEDALUS_STICK");
        if (n.contains("relic")) return BY_ID.get("MINOS_RELIC");
        if (n.contains("brain")) return BY_ID.get("BRAIN_FOOD");
        if (n.contains("souvenir")) return BY_ID.get("SOUVENIR");
        if (n.contains("mythos")) return BY_ID.get("MYTHOS_FRAGMENT");
        if (n.contains("enchanted ancient")) return BY_ID.get("ENCHANTED_ANCIENT_CLAW");
        if (n.contains("ancient claw")) return BY_ID.get("ANCIENT_CLAW");
        if (n.contains("enchanted gold block")) return BY_ID.get("ENCHANTED_GOLD_BLOCK");
        if (n.contains("enchanted gold")) return BY_ID.get("ENCHANTED_GOLD");
        return null;
    }

    public static Map<String, Entry> all() {
        return BY_ID;
    }

    public static String coins(long n) {
        return String.format(Locale.US, "%,d", n);
    }
}
