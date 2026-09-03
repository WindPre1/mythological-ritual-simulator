package dev.practice.ritual.ritual;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public enum MythoKind {
    HUNTER("Minos Hunter", EntityType.ZOMBIE, false, false,
            new double[]{4_000, 15_000, 100_000, 350_000, 1_000_000, 1_750_000},
            new double[]{40, 125, 500, 1_250, 2_750, 4_000},
            2.0, 1000, Material.IRON_SWORD, "Hilt of Revelations", "HILT"),
    LYNX("Siamese Lynxes", EntityType.WOLF, false, false,
            new double[]{2_500, 12_500, 75_000, 250_000, 750_000, 1_250_000},
            new double[]{30, 75, 350, 1_000, 2_250, 3_000},
            0.4, 1000, Material.TOTEM_OF_UNDYING, "Crochet Tiger Plushie", "PLUSHIE"),
    NYMPH("Stranded Nymph", EntityType.MANNEQUIN, false, true,
            new double[]{20_000, 20_000, 125_000, 400_000, 1_250_000, 2_500_000},
            new double[]{100, 100, 250, 750, 1_750, 2_500},
            0.5, 800, Material.NAUTILUS_SHELL, "Washed-up Souvenir", "SOUVENIR"),
    BULL("Cretan Bull", EntityType.COW, false, false,
            new double[]{25_000, 25_000, 150_000, 500_000, 1_500_000, 3_000_000},
            new double[]{100, 100, 400, 800, 2_000, 2_750},
            0.5, 800, Material.DECORATED_POT, "Cretan Urn", "CRETAN_URN"),
    HARPY("Harpy", EntityType.MANNEQUIN, false, true,
            new double[]{200_000, 200_000, 200_000, 750_000, 2_000_000, 3_750_000},
            new double[]{750, 750, 750, 1_500, 3_750, 6_500},
            0.6, 600, Material.GLISTERING_MELON_SLICE, "Antique Remedies", "REMEDIES"),
    GAIA("Gaia Construct", EntityType.IRON_GOLEM, false, false,
            new double[]{175_000, 175_000, 175_000, 650_000, 1_500_000, 3_500_000},
            new double[]{600, 600, 600, 2_000, 3_500, 5_000},
            0.6, 600, Material.TURTLE_HELMET, "Dwarf Turtle Shelmet", "SHELMET"),
    MINOTAUR("Minotaur", EntityType.MANNEQUIN, false, true,
            new double[]{1_500_000, 1_500_000, 1_500_000, 1_500_000, 8_000_000, 15_500_000},
            new double[]{350, 350, 350, 350, 500, 1_000},
            0.08, 400, Material.STICK, "Daedalus Stick", "DAEDALUS_STICK"),
    CHAMPION("Minos Champion", EntityType.MANNEQUIN, false, true,
            new double[]{2_000_000, 2_000_000, 2_000_000, 2_000_000, 12_500_000, 25_000_000},
            new double[]{1_000, 1_000, 1_000, 1_000, 1_750, 2_500},
            0.04, 400, Material.PRISMARINE_CRYSTALS, "Minos Relic", "MINOS_RELIC"),
    INQUISITOR("Minos Inquisitor", EntityType.MANNEQUIN, true, true,
            new double[]{50_000_000, 50_000_000, 50_000_000, 50_000_000, 50_000_000, 80_000_000},
            new double[]{750, 750, 750, 750, 750, 1_000},
            1.25, 75, Material.ENCHANTED_BOOK, "Enchanted Book (Chimera 1)", "CHIMERA"),
    SPHINX("Sphinx", EntityType.MANNEQUIN, true, true,
            new double[]{40_000_000, 40_000_000, 40_000_000, 40_000_000, 40_000_000, 65_000_000},
            new double[]{4_000, 4_000, 4_000, 4_000, 4_000, 7_000},
            0.5, 75, Material.GOLDEN_APPLE, "Brain Food", "BRAIN_FOOD"),
    MANTICORE("Manticore", EntityType.MANNEQUIN, true, true,
            new double[]{125_000_000, 125_000_000, 125_000_000, 125_000_000, 125_000_000, 125_000_000},
            new double[]{8_000, 8_000, 8_000, 8_000, 8_000, 8_000},
            1.0, 15, Material.BLAZE_ROD, "Manti-core", "MANTICORE"),
    KING("King Minos", EntityType.MANNEQUIN, true, true,
            new double[]{100_000_000, 100_000_000, 100_000_000, 100_000_000, 100_000_000, 100_000_000},
            new double[]{1_200, 1_200, 1_200, 1_200, 1_200, 1_200},
            1.0, 15, Material.GOLDEN_HELMET, "Crown of Greed", "CROWN");

    public final String display;
    public final EntityType type;
    public final boolean rare;
    public final boolean mannequin;
    private final double[] health;
    private final double[] damage;
    public final double dropBasePercent;
    public final int weight;
    public final Material dropMaterial;
    public final String dropName;
    public final String dropId;

    MythoKind(String display, EntityType type, boolean rare, boolean mannequin,
              double[] health, double[] damage,
              double dropBasePercent, int weight,
              Material dropMaterial, String dropName, String dropId) {
        this.display = display;
        this.type = type;
        this.rare = rare;
        this.mannequin = mannequin;
        this.health = health;
        this.damage = damage;
        this.dropBasePercent = dropBasePercent;
        this.weight = weight;
        this.dropMaterial = dropMaterial;
        this.dropName = dropName;
        this.dropId = dropId;
    }

    public boolean rare() {
        return rare;
    }

    public boolean elusive() {
        return this == INQUISITOR || this == SPHINX || this == MANTICORE || this == KING;
    }

    /** Wiki Mythic/Empyrean griffin Ancient Claw count (100%). */
    public int clawDrop() {
        return switch (this) {
            case HUNTER, LYNX -> 8;
            case NYMPH, BULL -> 10;
            case HARPY, GAIA -> 12;
            case MINOTAUR, CHAMPION -> 14;
            case INQUISITOR, SPHINX -> 32;
            case MANTICORE, KING -> 48;
        };
    }

    /** Wiki Mythic Enchanted Ancient Claw count, 50% to drop this many (else 0). */
    public int enchClawDrop() {
        return switch (this) {
            case INQUISITOR, SPHINX -> 2;
            case MANTICORE, KING -> 4;
            default -> 0;
        };
    }

    public double health(GriffinRarity r) {
        return health[health.length - 1];
    }

    public double damage(GriffinRarity r) {
        return damage[damage.length - 1];
    }

    /** Always Empyrean / Mythic griffin coin table. */
    public int coins(GriffinRarity r) {
        return switch (this) {
            case HUNTER, LYNX -> 500;
            case NYMPH, BULL -> 550;
            case HARPY, GAIA -> 600;
            case MINOTAUR, CHAMPION -> 1500;
            case INQUISITOR, SPHINX -> 2500;
            case MANTICORE, KING -> 5000;
        };
    }

    /** Vanilla entity HP is unused; keep a small bar so they exist. */
    public double vanillaHp() {
        return 20;
    }
}
