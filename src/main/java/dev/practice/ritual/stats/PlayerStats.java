package dev.practice.ritual.stats;

import org.bukkit.configuration.ConfigurationSection;

public final class PlayerStats {
    public double health = 5000;
    public double maxHealth = 5000;
    public double defense = 1000;
    public double mana = 1000;
    public double maxMana = 1000;
    public double magicFind = 400;
    public double tracking = 50;
    public double damage = 1_000_000;
    public long purse = 0;
    public boolean breakBlocks = false;
    public long lastMelon;
    public long lastManaFruit;
    public long lastManaTick;
    public long lastFreeze;
    public boolean chanceMessages = true;
    public boolean autoCompactor = false;

    /** 32-bit signed int max. Every /set stat uses this. */
    public static final double CAP = Integer.MAX_VALUE;
    public static final double CAP_HEALTH = CAP;
    public static final double CAP_DEFENSE = CAP;
    public static final double CAP_MANA = CAP;
    public static final double CAP_MF = CAP;
    public static final double CAP_TRACKING = CAP;
    public static final double CAP_DAMAGE = CAP;

    public void clamp() {
        maxHealth = Math.min(Math.max(1, maxHealth), CAP);
        health = Math.min(health, maxHealth);
        defense = Math.min(Math.max(0, defense), CAP);
        maxMana = Math.min(Math.max(1, maxMana), CAP);
        mana = Math.min(mana, maxMana);
        magicFind = Math.min(Math.max(0, magicFind), CAP);
        tracking = Math.min(Math.max(0, tracking), CAP);
        damage = Math.min(Math.max(0, damage), CAP_DAMAGE);
    }

    public void clamp(boolean op) {
        clamp();
    }

    public boolean trySpendMana(double cost) {
        if (mana < cost) return false;
        mana -= cost;
        return true;
    }

    public void regenMana(double dtSeconds) {
        mana = Math.min(maxMana, mana + maxMana * 0.04 * dtSeconds);
    }

    public void regenHealth() {
        health = Math.min(maxHealth, health + maxHealth * 0.02);
    }

    /** SkyBlock: taken = raw * 100 / (def + 100) */
    public double taken(double raw) {
        return raw * 100.0 / (defense + 100.0);
    }

    public void save(ConfigurationSection sec) {
        sec.set("health", health);
        sec.set("max-health", maxHealth);
        sec.set("defense", defense);
        sec.set("mana", mana);
        sec.set("max-mana", maxMana);
        sec.set("magic-find", magicFind);
        sec.set("tracking", tracking);
        sec.set("damage", damage);
        sec.set("purse", purse);
        sec.set("chance-messages", chanceMessages);
        sec.set("auto-compactor", autoCompactor);
    }

    public void load(ConfigurationSection sec) {
        if (sec == null) return;
        health = sec.getDouble("health", health);
        maxHealth = sec.getDouble("max-health", maxHealth);
        defense = sec.getDouble("defense", defense);
        mana = sec.getDouble("mana", mana);
        maxMana = sec.getDouble("max-mana", maxMana);
        magicFind = sec.getDouble("magic-find", magicFind);
        tracking = sec.getDouble("tracking", tracking);
        damage = sec.getDouble("damage", damage);
        purse = sec.getLong("purse", purse);
        chanceMessages = sec.getBoolean("chance-messages", true);
        autoCompactor = sec.getBoolean("auto-compactor", false);
    }
}
