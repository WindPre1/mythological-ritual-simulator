package dev.practice.ritual.ritual;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.UUID;

public final class Burrow {
    public final UUID owner;
    public final UUID chainId;
    public final int index;
    public final int chainLength;
    public final Block block;
    public BurrowType type;
    public boolean dug;
    /** START needs 1, MOB/TREASURE need 2 (break, [kill], break). */
    public int timesDug;

    public Burrow(UUID owner, UUID chainId, int index, int chainLength, Block block, BurrowType type) {
        this.owner = owner;
        this.chainId = chainId;
        this.index = index;
        this.chainLength = chainLength;
        this.block = block;
        this.type = type;
    }

    public int requiredDigs() {
        return type == BurrowType.START ? 1 : 2;
    }

    public boolean complete() {
        return timesDug >= requiredDigs();
    }

    public Location particleOrigin() {
        return block.getLocation().add(0.5, 1.0, 0.5);
    }

    public Location center() {
        return block.getLocation().add(0.5, 0.0, 0.5);
    }
}
