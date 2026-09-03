package dev.practice.ritual.item;

import dev.practice.ritual.RitualPlugin;
import dev.practice.ritual.stats.PlayerStats;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class ItemListener implements Listener {
    private final RitualPlugin plugin;

    public ItemListener(RitualPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (ItemFactory.isMenu(item) || (event.getAction().isLeftClick() || event.getAction().isRightClick())
                && ItemFactory.isMenu(player.getInventory().getItem(8))
                && event.getHand() == EquipmentSlot.HAND
                && player.getInventory().getHeldItemSlot() == 8) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK
                    || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.trades().open(player);
                return;
            }
        }

        if (ItemFactory.isMelon(item) && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            eatMelon(player);
            return;
        }

        if (ItemFactory.isMana(item) && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            eatMana(player);
            return;
        }

        if (ItemFactory.isSpade(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.rituals().echo(player);
            }
            return;
        }

        if (ItemFactory.isStaff(item)
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            castFireFreeze(player);
            return;
        }

        if (ItemFactory.isAote(item)) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!ItemFactory.isAote(item)) return;
        event.setCancelled(true);
        PlayerStats stats = plugin.rituals().session(player).stats;
        if (player.isSneaking()) {
            Location eye = player.getEyeLocation();
            Vector dir = eye.getDirection();
            if (dir.lengthSquared() < 1e-6) {
                player.sendMessage("§cNo block in range.");
                return;
            }
            // ignorePassableBlocks: ferns, grass, flowers, etc. don't eat the ray.
            // Slabs/stairs still hit so you can ether onto them.
            RayTraceResult hit = player.getWorld().rayTraceBlocks(eye, dir, 61, FluidCollisionMode.NEVER, true);
            if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
                player.sendMessage("§cNo block in range.");
                return;
            }
            Location dest = hit.getHitBlock().getRelative(hit.getHitBlockFace()).getLocation().add(0.5, 0, 0.5);
            if (!canStand(dest)) {
                Location up = dest.clone().add(0, 1, 0);
                if (canStand(up)) dest = up;
                else {
                    player.sendMessage("§cThat location is blocked.");
                    return;
                }
            }
            if (!stats.trySpendMana(25)) {
                player.sendMessage("§cNot enough mana!");
                return;
            }
            teleportExact(player, dest);
            return;
        }
        Location dest = aimOrWalk(player, 12.0);
        if (dest.distanceSquared(player.getLocation()) < 0.09) {
            player.sendMessage("§cThere's a wall in the way.");
            return;
        }
        if (!stats.trySpendMana(25)) {
            player.sendMessage("§cNot enough mana!");
            return;
        }
        teleportExact(player, dest);
    }

    private void eatMelon(Player player) {
        PlayerStats s = plugin.rituals().session(player).stats;
        long now = System.currentTimeMillis();
        if (now - s.lastMelon < 5000) {
            player.sendMessage("§cThis ability is on cooldown.");
            return;
        }
        if (plugin.rituals().session(player).healDisabledUntil > now) {
            player.sendMessage("§cThe Fatal Sting of the Manticore disabled all healing!");
            return;
        }
        if (!s.trySpendMana(200)) {
            player.sendMessage("§cNot enough mana!");
            return;
        }
        s.lastMelon = now;
        double heal = s.maxHealth * 0.5;
        s.health = Math.min(s.maxHealth, s.health + heal);
        plugin.rituals().applyVanilla(player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0f, 1.0f);
        player.sendMessage("§a+" + (int) heal + "❤");
    }

    private void eatMana(Player player) {
        PlayerStats s = plugin.rituals().session(player).stats;
        long now = System.currentTimeMillis();
        if (now - s.lastManaFruit < 5000) {
            player.sendMessage("§cThis ability is on cooldown.");
            return;
        }
        if (s.health <= 1) {
            player.sendMessage("§cYou don't have enough health.");
            return;
        }
        s.lastManaFruit = now;
        double cost = s.health * 0.5;
        s.health = Math.max(1, s.health - cost);
        double restore = s.maxMana * 0.5;
        s.mana = Math.min(s.maxMana, s.mana + restore);
        plugin.rituals().applyVanilla(player);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.4f);
        player.sendMessage("§b+" + (int) restore + "✎  §c-" + (int) cost + "❤");
    }

    private void castFireFreeze(Player player) {
        PlayerStats stats = plugin.rituals().session(player).stats;
        long now = System.currentTimeMillis();
        if (now - stats.lastFreeze < 10_000) {
            player.sendMessage("§cThis ability is on cooldown.");
            return;
        }
        if (!stats.trySpendMana(500)) {
            player.sendMessage("§cNot enough mana!");
            return;
        }
        stats.lastFreeze = now;
        player.setCooldown(org.bukkit.Material.BLAZE_ROD, 200);
        Location center = player.getLocation().clone();
        player.playSound(center, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.6f);
        new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;
            final int duration = 100; // 5s
            final double radius = 5.0;

            @Override
            public void run() {
                if (!player.isOnline() || tick > duration) {
                    if (tick > duration) freezeMobs(player, center, radius);
                    cancel();
                    return;
                }
                double inner = radius * (1.0 - tick / (double) duration);
                ring(player, center, radius, Particle.FLAME, 28);
                ring(player, center, Math.max(0.15, inner), Particle.SOUL_FIRE_FLAME, 24);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void ring(Player player, Location center, double r, Particle particle, int points) {
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0 * i) / points;
            Location at = center.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
            player.spawnParticle(particle, at, 1, 0, 0, 0, 0);
        }
    }

    private void freezeMobs(Player player, Location center, double radius) {
        double r2 = radius * radius;
        long until = System.currentTimeMillis() + 10_000;
        int n = 0;
        for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, radius + 1, 4, radius + 1)) {
            if (!(e instanceof LivingEntity living) || living.isDead()) continue;
            if (!living.getPersistentDataContainer().has(plugin.getKey("mytho"), PersistentDataType.STRING)) continue;
            String owner = living.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
            if (owner != null && !owner.equals(player.getUniqueId().toString())) continue;
            if (living.getLocation().distanceSquared(center) > r2) continue;
            living.getPersistentDataContainer().set(plugin.getKey("frozen-until"), PersistentDataType.LONG, until);
            living.setVelocity(new Vector(0, 0, 0));
            n++;
        }
        player.playSound(center, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
        player.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.0f, 0.8f);
        if (n > 0) player.sendMessage("§bFrozen " + n + " mob" + (n == 1 ? "" : "s") + " for 10s.");
        else player.sendMessage("§7Nothing was inside the ring.");
    }

    private void teleportExact(Player player, Location dest) {
        Location loc = dest.clone();
        loc.setX(Math.floor(loc.getX()) + 0.5);
        loc.setY(Math.floor(loc.getY()));
        loc.setZ(Math.floor(loc.getZ()) + 0.5);

        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        loc.setYaw(yaw);
        loc.setPitch(pitch);

        player.teleport(
                loc,
                org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN
        );

        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.setFallDistance(0);
        player.playSound(
                player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );
    }

    /**
     * Instant Transmission: 12 blocks along look.
     * If you are aiming at a block (ground, wall) inside that range, land on the
     * aimed face instead of skipping the full 12 blocks.
     */
    static Location aimOrWalk(Player player, double max) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        if (dir.lengthSquared() < 1e-6) return player.getLocation().clone();
        dir.normalize();
        org.bukkit.World w = eye.getWorld();
        if (w == null) return player.getLocation().clone();
        RayTraceResult hit = w.rayTraceBlocks(eye, dir, max, FluidCollisionMode.NEVER, true);
        if (hit != null && hit.getHitBlock() != null && hit.getHitBlockFace() != null) {
            Location dest = hit.getHitBlock().getRelative(hit.getHitBlockFace()).getLocation().add(0.5, 0, 0.5);
            if (canStand(dest)) return dest;
            Location up = dest.clone().add(0, 1, 0);
            if (canStand(up)) return up;
        }
        return walkRay(player, max);
    }

    /**
     * Instant Transmission: 12 blocks along look.
     * Passable plants and non-full cubes (slabs, stairs, ferns, flowers, grass) are ignored.
     * Only full cubes stop the ray. Samples the 0.6-wide player AABB so we never land inside a wall.
     */
    static Location walkRay(Player player, double max) {
        Location start = player.getLocation();
        Vector dir = start.getDirection();
        if (dir.lengthSquared() < 1e-6) return start.clone();
        dir.normalize();
        Location last = start.clone();
        Location eye = player.getEyeLocation();
        org.bukkit.World w = start.getWorld();
        if (w == null) return last;
        for (double d = 0.25; d <= max; d += 0.25) {
            Location feet = start.clone().add(dir.clone().multiply(d));
            Location eyes = eye.clone().add(dir.clone().multiply(d));
            if (hitsWall(w, feet, eyes)) break;
            if (canStand(feet)) last = feet;
        }
        return last;
    }

    /** Player is 0.6 wide. A center-only sample can land 0.3 inside a full cube. */
    static boolean hitsWall(org.bukkit.World w, Location feet, Location eyes) {
        double r = 0.31;
        for (double dx = -r; dx <= r + 1e-6; dx += r) {
            for (double dz = -r; dz <= r + 1e-6; dz += r) {
                int fx = (int) Math.floor(feet.getX() + dx);
                int fy = feet.getBlockY();
                int fz = (int) Math.floor(feet.getZ() + dz);
                if (isFullCube(w.getBlockAt(fx, fy, fz)) || isFullCube(w.getBlockAt(fx, fy + 1, fz))) return true;
                int ex = (int) Math.floor(eyes.getX() + dx);
                int ey = eyes.getBlockY();
                int ez = (int) Math.floor(eyes.getZ() + dz);
                if (isFullCube(w.getBlockAt(ex, ey, ez))) return true;
            }
        }
        return false;
    }

    /** True for a 1×1×1 collision cube. Ferns/grass/flowers/slabs/stairs are not. */
    static boolean isFullCube(Block b) {
        if (b == null || b.getType().isAir() || b.isEmpty()) return false;
        if (b.isPassable()) return false;
        try {
            var boxes = b.getCollisionShape().getBoundingBoxes();
            if (boxes.isEmpty()) return false;
            for (var box : boxes) {
                if (box.getWidthX() >= 0.99 && box.getHeight() >= 0.99 && box.getWidthZ() >= 0.99) return true;
            }
            return false;
        } catch (Throwable t) {
            return !b.isPassable();
        }
    }

    static boolean canStand(Location dest) {
        org.bukkit.World w = dest.getWorld();
        if (w == null) return false;
        int x = dest.getBlockX();
        int y = dest.getBlockY();
        int z = dest.getBlockZ();
        Block feet = w.getBlockAt(x, y, z);
        Block head = w.getBlockAt(x, y + 1, z);
        return !isFullCube(feet) && !isFullCube(head) && feet.isPassable() && head.isPassable();
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.rituals().session(player).stats.autoCompactor) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> ItemFactory.compact(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageBlock(BlockDamageEvent event) {
        ItemStack hand = event.getItemInHand();
        if (ItemFactory.isAote(hand) || ItemFactory.isDaedalus(hand) || ItemFactory.isStaff(hand)) {
            event.setCancelled(true);
            return;
        }
        if (ItemFactory.isSpade(hand) && event.getBlock().getType() == org.bukkit.Material.GRASS_BLOCK) {
            // Efficiency 5 is hidden; vanilla mine speed applies. 1s dig cooldown is in RitualManager.
            return;
        }
    }

    @EventHandler
    public void onShurikenLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Snowball snow)) return;
        if (!(snow.getShooter() instanceof Player player)) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (!ItemFactory.isShuriken(hand) && !ItemFactory.isShuriken(off)) return;
        if (player.getCooldown(org.bukkit.Material.SNOWBALL) > 0) {
            event.setCancelled(true);
            return;
        }
        player.setCooldown(org.bukkit.Material.SNOWBALL, 20);
        snow.getPersistentDataContainer().set(plugin.getKey("shuriken"), PersistentDataType.BOOLEAN, true);
        snow.getPersistentDataContainer().set(plugin.getKey("owner"), PersistentDataType.STRING, player.getUniqueId().toString());
        dev.practice.ritual.mob.MobFactory.revealToOwner(plugin, snow, player);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.6f);
    }

    @EventHandler
    public void onShurikenHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (!Boolean.TRUE.equals(proj.getPersistentDataContainer().get(plugin.getKey("shuriken"), PersistentDataType.BOOLEAN))) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity living
                && living.getPersistentDataContainer().has(plugin.getKey("mytho"), PersistentDataType.STRING)) {
            String owner = living.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
            String thrower = proj.getPersistentDataContainer().get(plugin.getKey("owner"), PersistentDataType.STRING);
            if (owner != null && thrower != null && !owner.equals(thrower)) {
                proj.remove();
                return;
            }
            living.getPersistentDataContainer().set(plugin.getKey("shuriken"), PersistentDataType.BOOLEAN, true);
            refreshTagHologram(living);
            if (proj.getShooter() instanceof Player player) {
                player.playSound(living.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.6f);
                player.sendMessage("§aTagged! §6+5% ✯ Magic Find §aon this mob.");
            }
        }
        proj.remove();
    }

    private void refreshTagHologram(LivingEntity victim) {
        String id = victim.getPersistentDataContainer().get(plugin.getKey("hologram"), PersistentDataType.STRING);
        if (id == null) return;
        org.bukkit.entity.Entity e;
        try {
            e = plugin.getServer().getEntity(java.util.UUID.fromString(id));
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (!(e instanceof org.bukkit.entity.ArmorStand stand) || stand.isDead()) return;
        String kn = victim.getPersistentDataContainer().get(plugin.getKey("mytho"), PersistentDataType.STRING);
        if (kn == null) return;
        try {
            var kind = dev.practice.ritual.ritual.MythoKind.valueOf(kn);
            var griffin = dev.practice.ritual.ritual.GriffinRarity.MYTHIC;
            String g = victim.getPersistentDataContainer().get(plugin.getKey("griffin"), PersistentDataType.STRING);
            if (g != null) griffin = dev.practice.ritual.ritual.GriffinRarity.valueOf(g);
            Double hp = victim.getPersistentDataContainer().get(plugin.getKey("sb-hp"), PersistentDataType.DOUBLE);
            Double max = victim.getPersistentDataContainer().get(plugin.getKey("sb-max"), PersistentDataType.DOUBLE);
            Integer hits = victim.getPersistentDataContainer().get(plugin.getKey("king-shield"), PersistentDataType.INTEGER);
            stand.customName(dev.practice.ritual.mob.MobFactory.hologramName(
                    kind, griffin, hp == null ? 0 : hp, max == null ? 1 : max, hits == null ? -1 : hits, true));
        } catch (IllegalArgumentException ignored) {
        }
    }
}
