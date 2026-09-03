package dev.practice.ritual.ritual;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.item.ItemFactory;
import dev.practice.ritual.mob.MobAI;
import dev.practice.ritual.mob.MobFactory;
import dev.practice.ritual.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RitualManager implements Listener {
    private final RitualPlugin plugin;
    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    public RitualManager(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerSession session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), id -> {
            PlayerSession s = new PlayerSession(id);
            s.griffin = GriffinRarity.MYTHIC;
            load(player, s);
            s.griffin = GriffinRarity.MYTHIC;
            return s;
        });
    }

    public void start(Player player) {
        PlayerSession s = session(player);
        s.active = true;
        s.activeChain = null;
        s.pendingMobs = 0;
        s.pendingMob = null;
        s.lastKind = null;
        s.burrows.clear();
        fillToCap(player, s);
        RitualSounds.ding(player);
        player.sendMessage("§6[Ritual] §eStarted. Griffin §d" + s.griffin
                + "§e, chain §a" + chainLength()
                + "§e, burrows §a" + undugCount(s) + "§7/" + startCap());
    }

    public void stop(Player player) {
        PlayerSession s = sessions.get(player.getUniqueId());
        if (s != null) {
            s.active = false;
            s.activeChain = null;
            s.burrows.clear();
            s.pendingMobs = 0;
            s.pendingMob = null;
        }
        player.sendMessage("§6[Ritual] §cStopped.");
    }

    public void reset(Player player) {
        PlayerSession s = session(player);
        s.burrows.clear();
        s.activeChain = null;
        s.pendingMobs = 0;
        s.pendingMob = null;
        s.lastKind = null;
        if (s.active) fillToCap(player, s);
        player.sendMessage("§6[Ritual] §eBurrows reset. §a" + undugCount(s) + "§e active.");
    }

    public int chainLength() {
        return plugin.getConfig().getInt("chain-length", 10);
    }

    public int startCap() {
        return 7;
    }

    public int undugCount(PlayerSession s) {
        int n = 0;
        for (Burrow b : s.burrows) {
            if (!b.dug) n++;
        }
        return n;
    }

    private void fillToCap(Player player, PlayerSession s) {
        int cap = startCap();
        enforceCap(player, s, cap);
        int guard = 0;
        while (undugCount(s) < cap && guard++ < 4000) {
            if (!spawnOneStart(player, s)) break;
        }
        enforceCap(player, s, cap);
    }

    /**
     * Hard cap on TOTAL undug burrows. Extra idle START burrows vanish one by one
     * with a chat message until we are at or under the cap. Never eats the burrow
     * you are currently fighting.
     */
    private void enforceCap(Player player, PlayerSession s, int cap) {
        int guard = 0;
        while (undugCount(s) > cap && guard++ < 64) {
            Burrow victim = pickDespawn(s);
            if (victim == null) break;
            s.burrows.remove(victim);
            ParticleEmitter.emitRemoved(player, victim.block.getLocation());
            player.sendMessage("§eA Griffin Burrow disappeared! §7(" + undugCount(s) + "/" + cap + ")");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
        }
    }

    /** Prefer idle START burrows that are not the active chain. */
    private Burrow pickDespawn(PlayerSession s) {
        Burrow fallback = null;
        for (Burrow b : s.burrows) {
            if (b.dug) continue;
            boolean inActive = s.activeChain != null && s.activeChain.equals(b.chainId);
            if (inActive && b.timesDug > 0) continue;
            if (inActive && b.index > 1) {
                if (fallback == null) fallback = b;
                continue;
            }
            if (b.index == 1) return b;
            if (fallback == null) fallback = b;
        }
        return fallback;
    }

    /**
     * SBO ArrowGuessBurrow.HUB_BOUNDS — exclusive min, inclusive max.
     * isInside: x > -283 && x <= 175, y > 60 && y <= 105, z > -208 && z <= 205
     *
     * Arrow particles spawn at block Y+2 so SBO's base.down(1.5) is the grass
     * CENTER. That origin must itself be inside HUB_BOUNDS, so we cap spawns
     * one block in from the max edges (x+0.5 <= 175, y+0.5 <= 105, z+0.5 <= 205).
     */
    public static final int HUB_MIN_X = -282;
    public static final int HUB_MAX_X = 174;
    public static final int HUB_MIN_Y = 61;
    public static final int HUB_MAX_Y = 104;
    public static final int HUB_MIN_Z = -207;
    public static final int HUB_MAX_Z = 204;

    private boolean spawnOneStart(Player player, PlayerSession s) {
        World world = player.getWorld();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double minSep = plugin.getConfig().getDouble("spawn.min-separation", 8);
        for (int attempt = 0; attempt < 1500; attempt++) {
            int x = rng.nextInt(HUB_MIN_X, HUB_MAX_X + 1);
            int z = rng.nextInt(HUB_MIN_Z, HUB_MAX_Z + 1);
            Block found = findGrass(world, x, z);
            if (found == null || occupied(s, found)) continue;
            if (tooClose(s, found.getLocation(), minSep)) continue;
            s.burrows.add(new Burrow(player.getUniqueId(), UUID.randomUUID(), 1, chainLength(), found, BurrowType.START));
            return true;
        }
        return false;
    }

    private boolean tooClose(PlayerSession s, Location loc, double min) {
        double minSq = min * min;
        for (Burrow b : s.burrows) {
            if (b.dug) continue;
            if (b.block.getLocation().distanceSquared(loc) < minSq) return true;
        }
        return false;
    }

    private Block findGrass(World world, int x, int z) {
        for (int y = HUB_MAX_Y; y >= HUB_MIN_Y; y--) {
            Block b = world.getBlockAt(x, y, z);
            if (isValidBurrowBlock(b)) return b;
        }
        return null;
    }

    private boolean occupied(PlayerSession s, Block b) {
        for (Burrow burrow : s.burrows) {
            if (!burrow.dug && burrow.block.equals(b)) return true;
        }
        return false;
    }

    public boolean isValidBurrowBlock(Block b) {
        if (b.getType() != Material.GRASS_BLOCK) return false;
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();
        if (x < HUB_MIN_X || x > HUB_MAX_X || z < HUB_MIN_Z || z > HUB_MAX_Z) return false;
        if (y < HUB_MIN_Y || y > HUB_MAX_Y) return false;
        return openSky(b);
    }

    /** Completely open sky — air only, no plants/leaves/fences. */
    private static boolean openSky(Block grass) {
        World w = grass.getWorld();
        int x = grass.getX();
        int z = grass.getZ();
        int max = w.getMaxHeight();
        for (int y = grass.getY() + 1; y < max; y++) {
            if (!w.getBlockAt(x, y, z).getType().isAir()) return false;
        }
        return true;
    }

    public Burrow burrowAt(Player player, Block block) {
        PlayerSession s = sessions.get(player.getUniqueId());
        if (s == null || !s.active) return null;
        for (Burrow b : s.burrows) {
            if (!b.dug && b.block.equals(block)) return b;
        }
        return null;
    }

    /**
     * Hypixel Echo:
     *  - no active chain → nearest undug START
     *  - after digging into a chain → the next undug burrow in that chain only
     */
    public Burrow echoTarget(Player player) {
        PlayerSession s = sessions.get(player.getUniqueId());
        if (s == null || !s.active) return null;

        if (s.activeChain != null) {
            Burrow next = null;
            for (Burrow b : s.burrows) {
                if (b.dug) continue;
                if (!s.activeChain.equals(b.chainId)) continue;
                if (next == null || b.index < next.index) next = b;
            }
            return next;
        }

        Burrow best = null;
        double bestD = Double.MAX_VALUE;
        Location loc = player.getLocation();
        for (Burrow b : s.burrows) {
            if (b.dug || b.index != 1) continue;
            double d = b.center().distanceSquared(loc);
            if (d < bestD) {
                bestD = d;
                best = b;
            }
        }
        return best;
    }

    public void echo(Player player) {
        PlayerSession s = session(player);
        long now = System.currentTimeMillis();
        if (now < s.echoBusyUntil) {
            player.sendMessage("§cWait for the Echo to finish.");
            return;
        }
        Burrow target = echoTarget(player);
        if (target == null) {
            player.sendMessage("§eNo nearby Griffin Burrow.");
            return;
        }
        if (!s.stats.trySpendMana(10)) {
            player.sendMessage("§cNot enough mana!");
            return;
        }
        ParticleEmitter.emitEcho(plugin, player, target.block.getLocation());
    }

    public boolean spadeBusy(Player player) {
        return System.currentTimeMillis() < session(player).echoBusyUntil;
    }

    public void dig(Player player, Burrow burrow) {
        PlayerSession s = session(player);
        long now = System.currentTimeMillis();
        if (now - s.lastDig < 1000) return;
        if (s.pendingMobs > 0) {
            player.sendMessage("§cDefeat the burrow defenders in order to dig it!");
            return;
        }
        s.lastDig = now;

        burrow.timesDug++;
        s.activeChain = burrow.chainId;
        boolean done = burrow.complete();

        if (!done) {
            if (burrow.type == BurrowType.MOB) {
                double elusive = 1.0 + s.stats.tracking / 100.0;
                MythoKind kind = s.griffin.roll(elusive);
                s.lastKind = kind;
                sendMobSpawnChat(player, kind);
                RitualSounds.fuse(player, burrow.center());
                dev.practice.ritual.mob.MobFactory.spawn(plugin, player, burrow, kind, s.griffin);
                RitualSounds.mobSpawn(player, burrow.center());
            } else if (burrow.type == BurrowType.TREASURE) {
                RitualSounds.fuse(player, burrow.center());
                DropTables.rollTreasure(player, s.stats);
                RitualSounds.treasure(player, burrow.center());
            } else {
                RitualSounds.fuse(player, burrow.center());
            }
            save(player);
            return;
        }

        burrow.dug = true;
        ParticleEmitter.emitRemoved(player, burrow.block.getLocation());

        if (ItemFactory.hasFourEyedFish(player)) {
            DropTables.grantCoins(player, s.stats, 2000, "fish");
        }

        boolean chainDone = burrow.index >= burrow.chainLength;
        if (chainDone) {
            player.sendMessage("§eYou finished the Griffin burrow chain!");
            RitualSounds.chainDone(player, burrow.center());
        }
        player.sendMessage("§eYou dug out a Griffin Burrow! §7(" + burrow.index + "/" + burrow.chainLength + ")");
        if (burrow.type == BurrowType.START) RitualSounds.ding(player);
        RitualSounds.dig(player, burrow.center(),
                burrow.type == BurrowType.START ? BurrowType.START : burrow.type, s.lastKind, false);

        Location from = burrow.block.getLocation();
        Location nextLoc = null;
        if (!chainDone) {
            Block next = pickNext(s, burrow.block);
            if (next != null) {
                BurrowType nextType = ThreadLocalRandom.current().nextDouble() < plugin.getConfig().getDouble("treasure-chance", 0.33)
                        ? BurrowType.TREASURE : BurrowType.MOB;
                Burrow nextB = new Burrow(player.getUniqueId(), burrow.chainId, burrow.index + 1, burrow.chainLength, next, nextType);
                s.burrows.add(nextB);
                nextLoc = next.getLocation();
            }
        } else {
            s.activeChain = null;
            s.lastKind = null;
        }

        fillToCap(player, s);
        save(player);

        if (nextLoc != null) {
            Location arrowFrom = from.clone();
            Location arrowTo = nextLoc.clone();
            // BurrowDugEvent clears SBO's particle set on the chat packet.
            // Send after chat is processed, then once more in case the first
            // burst raced the clear.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) ParticleEmitter.emitArrow(player, arrowFrom, arrowTo);
            }, 3L);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) ParticleEmitter.emitArrow(player, arrowFrom, arrowTo);
            }, 10L);
        }
    }

    /** Next chain burrow: random SBO-valid grass inside hub AABB. */
    private Block pickNext(PlayerSession s, Block from) {
        World world = from.getWorld();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double minSep = plugin.getConfig().getDouble("spawn.min-separation", 8);
        for (int attempt = 0; attempt < 1500; attempt++) {
            int x = rng.nextInt(HUB_MIN_X, HUB_MAX_X + 1);
            int z = rng.nextInt(HUB_MIN_Z, HUB_MAX_Z + 1);
            Block b = findGrass(world, x, z);
            if (b == null || occupied(s, b)) continue;
            if (tooClose(s, b.getLocation(), minSep)) continue;
            return b;
        }
        return null;
    }

    public void tickRegen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerSession s = sessions.get(player.getUniqueId());
            if (s == null) continue;
            s.stats.regenMana(1.0);
            s.regenTicks++;
            if (s.regenTicks >= 3) {
                s.regenTicks = 0;
                if (s.stats.health > 0 && s.healDisabledUntil < System.currentTimeMillis()) {
                    double heal = s.stats.maxHealth * 0.05 * (1.0 - s.harpyShred) * (1.0 - s.kingHealCut);
                    s.stats.health = Math.min(s.stats.maxHealth, s.stats.health + heal);
                }
            }
            applyVanilla(player);
            ensureMenu(player);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerSession s = sessions.get(player.getUniqueId());
            if (s == null || !s.active) continue;
            Iterator<Burrow> it = s.burrows.iterator();
            while (it.hasNext()) {
                if (it.next().dug) it.remove();
            }
            if (s.active) fillToCap(player, s);
        }
    }

    public void tickMobAura() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerSession s = sessions.get(player.getUniqueId());
            if (s == null || s.pendingMobs <= 0) continue;
            player.setFireTicks(0);
        }
    }

    /**
     * SBO DianaTracker: {@code (.*?) §eYou dug (.*?)§2(.*?)§e!(.*?)$}
     * Hypixel: {@code §c§lUh oh! §eYou dug out a §2Gaia Construct§e!}
     */
    private static final String[] SPAWN_BANG = {
            "Oh", "Uh oh", "Yikes", "Oi", "Good Grief", "Danger", "Woah"
    };

    public static void sendMobSpawnChat(Player player, MythoKind kind) {
        String bang = SPAWN_BANG[ThreadLocalRandom.current().nextInt(SPAWN_BANG.length)];
        String article = kind == MythoKind.LYNX ? "" : "a ";
        player.sendMessage("§c§l" + bang + "! §eYou dug out " + article + "§2" + kind.display + "§e!");
    }

    /** Natural despawn / timeout / player death: wipe owned mobs and drop the active chain. */
    public void failActiveFight(Player player) {
        PlayerSession s = session(player);
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity e : world.getEntities()) {
                if (!(e instanceof LivingEntity living)) continue;
                String owner = living.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
                if (owner == null || !owner.equals(player.getUniqueId().toString())) continue;
                if (!living.getPersistentDataContainer().has(plugin.getKey("mytho"), PersistentDataType.STRING)) continue;
                MobFactory.removeHologram(plugin, living);
                living.getPersistentDataContainer().set(plugin.getKey("resolved"), PersistentDataType.BOOLEAN, true);
                living.remove();
            }
        }
        s.pendingMobs = 0;
        s.pendingMob = null;
        s.lastKind = null;
        s.stingUntil = 0;
        s.healDisabledUntil = 0;
        s.harpyShred = 0;
        s.minoBleed = 0;
        if (s.nymphWater) {
            MobAI.clearNymphWater(player, s);
        }
        if (s.activeChain != null) {
            for (Burrow b : s.burrows) {
                if (!b.dug && s.activeChain.equals(b.chainId)) {
                    ParticleEmitter.emitRemoved(player, b.block.getLocation());
                    b.dug = true;
                }
            }
            s.activeChain = null;
        }
        fillToCap(player, s);
        save(player);
    }

    public void applyVanilla(Player player) {
        PlayerStats st = session(player).stats;
        try {
            if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20);
            }
            if (player.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.4);
            }
        } catch (Throwable ignored) {
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 5, true, false, false));
        player.setFoodLevel(20);
        player.setSaturation(20);
        if (st.health <= 0) return;
        double ratio = Math.max(0.05, Math.min(1.0, st.health / st.maxHealth));
        player.setHealth(Math.max(1.0, 20.0 * ratio));
    }

    public void ensureMenu(Player player) {
        var inv = player.getInventory();
        ItemStack slot = inv.getItem(8);
        if (ItemFactory.isMenu(slot)) return;
        if (slot != null && !slot.getType().isAir()) {
            inv.setItem(8, null);
            var leftover = inv.addItem(slot);
            leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
        }
        inv.setItem(8, plugin.items().menu());
    }

    public void giveKit(Player player) {
        var inv = player.getInventory();
        inv.addItem(plugin.items().deificSpade(), plugin.items().aote(),
                plugin.items().daedalusBlade(session(player).stats.damage),
                plugin.items().melon(), plugin.items().manaFruit(),
                plugin.items().fireFreezeStaff(), plugin.items().crownOfAvarice());
        ItemFactory.refreshDaedalus(player, session(player).stats.damage);
        ensureMenu(player);
        applyVanilla(player);
    }

    public void save(Player player) {
        PlayerSession s = sessions.get(player.getUniqueId());
        if (s == null) return;
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, player.getUniqueId() + ".yml");
        YamlConfiguration y = new YamlConfiguration();
        s.stats.save(y.createSection("stats"));
        y.set("active", s.active);
        y.set("griffin", s.griffin.name());
        y.set("active-chain", s.activeChain == null ? null : s.activeChain.toString());
        List<Map<String, Object>> list = new ArrayList<>();
        for (Burrow b : s.burrows) {
            if (b.dug) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("world", b.block.getWorld().getName());
            m.put("x", b.block.getX());
            m.put("y", b.block.getY());
            m.put("z", b.block.getZ());
            m.put("type", b.type.name());
            m.put("index", b.index);
            m.put("chain-length", b.chainLength);
            m.put("chain", b.chainId.toString());
            m.put("times", b.timesDug);
            list.add(m);
        }
        y.set("burrows", list);
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save " + player.getName() + ": " + e.getMessage());
        }
    }

    private void load(Player player, PlayerSession s) {
        File file = new File(plugin.getDataFolder(), "players/" + player.getUniqueId() + ".yml");
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        s.stats.load(y.getConfigurationSection("stats"));
        s.active = y.getBoolean("active", false);
        s.griffin = GriffinRarity.MYTHIC;
        String chain = y.getString("active-chain");
        if (chain != null) {
            try {
                s.activeChain = UUID.fromString(chain);
            } catch (IllegalArgumentException ignored) {
            }
        }
        s.burrows.clear();
        List<Map<?, ?>> list = y.getMapList("burrows");
        for (Map<?, ?> m : list) {
            try {
                World world = Bukkit.getWorld(String.valueOf(m.get("world")));
                if (world == null) world = player.getWorld();
                int x = num(m.get("x"));
                int yv = num(m.get("y"));
                int z = num(m.get("z"));
                BurrowType type = BurrowType.valueOf(String.valueOf(m.get("type")));
                int index = num(m.get("index"));
                int len = num(m.get("chain-length"));
                UUID cid = UUID.fromString(String.valueOf(m.get("chain")));
                Burrow b = new Burrow(player.getUniqueId(), cid, index, len, world.getBlockAt(x, yv, z), type);
                Object times = m.get("times");
                if (times instanceof Number n) b.timesDug = n.intValue();
                else if (times != null) b.timesDug = Integer.parseInt(String.valueOf(times));
                s.burrows.add(b);
            } catch (Exception ignored) {
            }
        }
        enforceCap(player, s, startCap());
        if (s.active) fillToCap(player, s);
    }

    private static int num(Object o) {
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        save(event.getPlayer());
        sessions.remove(event.getPlayer().getUniqueId());
        plugin.parties().onQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        Burrow burrow = burrowAt(player, block);
        if (burrow == null) {
            if (block.getType() == Material.GRASS_BLOCK) {
                ParticleEmitter.emitRemoved(player, block.getLocation());
            }
        }
        // Live burrow: don't smoke. Dig happens when the block actually breaks.
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getType() != Material.GRASS_BLOCK) return;
        if (!ItemFactory.isSpade(player.getInventory().getItemInMainHand())) return;
        Burrow burrow = burrowAt(player, block);
        if (burrow == null) return;
        event.setDropItems(false);
        event.setCancelled(true);
        dig(player, burrow);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.deathMessage(null);
        PlayerSession s = session(player);
        MythoKind killer = s.lastHitKind != null ? s.lastHitKind : s.lastKind;
        if (killer != null) {
            String who = "§2" + s.griffin.prefix + " " + killer.display + "§7.";
            // SBO BurrowDetector: ^§c ☠ §7You .+$  (space after §c, personal "You")
            player.sendMessage("§c ☠ §7You were killed by " + who);
            String others = "§c ☠ §7" + player.getName() + " was killed by " + who;
            var component = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(others);
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (!p.getUniqueId().equals(player.getUniqueId())) p.sendMessage(component);
            }
        }
        boolean fighting = s.pendingMobs > 0 || s.activeChain != null;
        if (fighting) {
            failActiveFight(player);
            player.sendMessage("§cYou died! Your burrow defenders despawned and the chain broke.");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerSession s = session(player);
        s.stats.health = s.stats.maxHealth;
        s.stats.mana = s.stats.maxMana;
        s.lastHitKind = null;
        plugin.getServer().getScheduler().runTask(plugin, () -> applyVanilla(player));
    }

    public static final class PlayerSession {
        public final UUID id;
        public boolean active;
        public GriffinRarity griffin = GriffinRarity.MYTHIC;
        public final PlayerStats stats = new PlayerStats();
        public final List<Burrow> burrows = new ArrayList<>();
        public UUID activeChain;
        public long lastEcho;
        public long echoBusyUntil;
        public long lastDig;
        public int regenTicks;
        public LivingEntity pendingMob;
        public int pendingMobs;
        public MythoKind lastKind;
        public MythoKind lastHitKind;
        public long lastMobHitPlayer;
        public double inqShred;
        public long inqShredUntil;
        public double harpyShred;
        public int minoBleed;
        public long minoBleedLastInc;
        public long minoBleedPulse;
        public long stingUntil;
        public long healDisabledUntil;
        public double kingRage;
        public double kingHealCut;
        public String sphinxAnswer;
        public boolean sphinxInfuriated;
        public boolean nymphWater;
        public final java.util.List<org.bukkit.Location> nymphCells = new java.util.ArrayList<>();

        PlayerSession(UUID id) {
            this.id = id;
        }
    }
}
