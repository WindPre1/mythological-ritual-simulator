package dev.practice.ritual;

import dev.practice.ritual.command.MiscCommands;
import dev.practice.ritual.command.RitualCommand;
import dev.practice.ritual.command.SetStatCommand;
import dev.practice.ritual.command.WarpCommand;
import dev.practice.ritual.craft.AnvilGui;
import dev.practice.ritual.economy.ItemsGui;
import dev.practice.ritual.economy.TradesGui;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.item.ItemListener;
import dev.practice.ritual.mob.MobListener;
import dev.practice.ritual.party.PartyManager;
import dev.practice.ritual.ritual.ParticleTask;
import dev.practice.ritual.ritual.RitualManager;
import dev.practice.ritual.scoreboard.SkyblockBoard;
import dev.practice.ritual.world.WorldGuardListener;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public final class RitualPlugin extends JavaPlugin implements Listener {
    private static RitualPlugin instance;
    private RitualManager ritualManager;
    private ItemFactory items;
    private SkyblockBoard board;
    private WarpCommand warps;
    private PartyManager parties;
    private TradesGui trades;
    private ItemsGui itemsGui;
    private AnvilGui anvil;
    private boolean fakeLag;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getConfig().set("start-burrows", 7);
        saveConfig();
        this.items = new ItemFactory(this);
        this.ritualManager = new RitualManager(this);
        this.board = new SkyblockBoard(this);
        this.parties = new PartyManager(this);
        this.trades = new TradesGui(this);
        this.itemsGui = new ItemsGui(this);
        this.anvil = new AnvilGui(this);

        World world = getServer().getWorld(getConfig().getString("world", "world"));
        if (world != null && !getConfig().getBoolean("natural-regeneration", false)) {
            world.setGameRule(GameRule.NATURAL_REGENERATION, false);
        }

        RitualCommand ritual = new RitualCommand(this);
        getCommand("ritual").setExecutor(ritual);
        getCommand("ritual").setTabCompleter(ritual);
        this.warps = new WarpCommand(this);
        bind("warp", warps, warps);
        bind("hubwarp", warps, warps);

        bindStat("setmagicfind", SetStatCommand.Kind.MAGIC_FIND);
        bindStat("settracking", SetStatCommand.Kind.TRACKING);
        bindStat("sethealth", SetStatCommand.Kind.HEALTH);
        bindStat("setdefense", SetStatCommand.Kind.DEFENSE);
        bindStat("setmana", SetStatCommand.Kind.MANA);
        bindStat("setdamage", SetStatCommand.Kind.DAMAGE);

        MiscCommands misc = new MiscCommands(this);
        for (String c : new String[]{"p", "party", "pc", "trades", "togglebreak",
                "togglefakelag", "togglechance", "compactor", "purse", "items", "anvil"}) {
            if (getCommand(c) != null) {
                getCommand(c).setExecutor(misc);
                getCommand(c).setTabCompleter(misc);
            }
        }

        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new MobListener(this), this);
        getServer().getPluginManager().registerEvents(ritualManager, this);
        getServer().getPluginManager().registerEvents(board, this);
        getServer().getPluginManager().registerEvents(warps, this);
        getServer().getPluginManager().registerEvents(trades, this);
        getServer().getPluginManager().registerEvents(itemsGui, this);
        getServer().getPluginManager().registerEvents(anvil, this);
        getServer().getPluginManager().registerEvents(new WorldGuardListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.practice.ritual.world.DianaMayor(this), this);
        getServer().getPluginManager().registerEvents(parties, this);
        getServer().getPluginManager().registerEvents(this, this);

        new ParticleTask(this).runTaskTimer(this, 5L, 8L);
        new dev.practice.ritual.mob.MobAI(this).runTaskTimer(this, 10L, 2L);
        getServer().getScheduler().runTaskTimer(this, ritualManager::tickRegen, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, ritualManager::tickMobAura, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, board::tick, 20L, 10L);
        getServer().getScheduler().runTaskTimer(this, this::fakeLagTick, 1L, 1L);

        for (Player p : getServer().getOnlinePlayers()) {
            board.apply(p);
            ritualManager.applyVanilla(p);
            ritualManager.ensureMenu(p);
        }

        getServer().getScheduler().runTaskLater(this, () ->
                dev.practice.ritual.world.DianaMayor.spawn(this), 40L);
        try {
            dev.practice.ritual.mob.MythoSkins.folder(this);
        } catch (Throwable ignored) {
        }
        getLogger().info("RitualPractice 2.1.22 enabled.");
    }

    private void bind(String name, org.bukkit.command.CommandExecutor exec, org.bukkit.command.TabCompleter tab) {
        var cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(exec);
            cmd.setTabCompleter(tab);
            cmd.setPermission(null);
        }
    }

    private void bindStat(String name, SetStatCommand.Kind kind) {
        SetStatCommand cmd = new SetStatCommand(this, kind);
        bind(name, cmd, cmd);
    }

    private void fakeLagTick() {
        if (!fakeLag) return;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // Sleep on the main thread simulates Hypixel hitch without burning CPU.
        // Target ~14 TPS: frequent small stalls + occasional 100–200ms spikes.
        try {
            if (rng.nextDouble() < 0.08) {
                Thread.sleep(90 + rng.nextInt(130));
            } else if (rng.nextDouble() < 0.42) {
                Thread.sleep(18 + rng.nextInt(28));
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean toggleFakeLag() {
        fakeLag = !fakeLag;
        return fakeLag;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            ritualManager.session(p);
            ritualManager.applyVanilla(p);
            ritualManager.ensureMenu(p);
            board.apply(p);
            if (!p.getInventory().contains(org.bukkit.Material.GOLDEN_SHOVEL)) {
                p.getInventory().addItem(items.deificSpade(), items.aote(),
                        items.daedalusBlade(ritualManager.session(p).stats.damage),
                        items.melon(), items.manaFruit(), items.fireFreezeStaff(),
                        items.crownOfAvarice());
            } else {
                boolean hasBlade = false;
                boolean hasStaff = false;
                boolean hasCrown = ItemFactory.isAvarice(p.getInventory().getHelmet());
                for (org.bukkit.inventory.ItemStack stack : p.getInventory().getContents()) {
                    if (ItemFactory.isDaedalus(stack)) hasBlade = true;
                    if (ItemFactory.isStaff(stack)) hasStaff = true;
                    if (ItemFactory.isAvarice(stack)) hasCrown = true;
                }
                if (!hasBlade) {
                    p.getInventory().addItem(items.daedalusBlade(ritualManager.session(p).stats.damage));
                }
                if (!hasStaff) {
                    p.getInventory().addItem(items.fireFreezeStaff());
                }
                if (!hasCrown) {
                    p.getInventory().addItem(items.crownOfAvarice());
                }
                ItemFactory.refreshDaedalus(p, ritualManager.session(p).stats.damage);
                ItemFactory.refreshSpades(p);
            }
        }, 15L);
    }

    @Override
    public void onDisable() {
        for (Player p : getServer().getOnlinePlayers()) {
            ritualManager.save(p);
        }
    }

    public static RitualPlugin get() {
        return instance;
    }

    public RitualManager rituals() {
        return ritualManager;
    }

    public ItemFactory items() {
        return items;
    }

    public SkyblockBoard board() {
        return board;
    }

    public WarpCommand warps() {
        return warps;
    }

    public PartyManager parties() {
        return parties;
    }

    public TradesGui trades() {
        return trades;
    }

    public ItemsGui itemsGui() {
        return itemsGui;
    }

    public AnvilGui anvil() {
        return anvil;
    }

    public NamespacedKey getKey(String path) {
        return new NamespacedKey(this, path);
    }
}
