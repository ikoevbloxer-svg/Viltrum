package com.viltrum;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side balance configuration (config/viltrum-common.toml). */
public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ALLOW_BLOCK_DESTRUCTION;
    public static final ModConfigSpec.BooleanValue TUNNEL_DROPS;
    public static final ModConfigSpec.IntValue MAX_CRATER_RADIUS;
    public static final ModConfigSpec.IntValue MAX_BLOCKS_PER_TICK;
    public static final ModConfigSpec.IntValue MAX_CRATER_BLOCKS;
    public static final ModConfigSpec.DoubleValue FIRST_JOIN_CHANCE;
    public static final ModConfigSpec.DoubleValue MAX_FLIGHT_SPEED;
    public static final ModConfigSpec.DoubleValue XP_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue PVP_SUPER_DAMAGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("destruction");
        ALLOW_BLOCK_DESTRUCTION = b.comment("Master switch: may Viltrumite power destroy blocks (craters, tunnelling at supersonic speed)?")
                .define("allowBlockDestruction", true);
        TUNNEL_DROPS = b.comment("Do blocks smashed while flying drop items? (off = much less lag)")
                .define("tunnelDrops", false);
        MAX_CRATER_RADIUS = b.comment("Largest crater radius (blocks) a single impact can dig")
                .defineInRange("craterRadiusCap", 20, 1, 48);
        MAX_BLOCKS_PER_TICK = b.comment("Hard cap of blocks destroyed per player per tick while flying through terrain")
                .defineInRange("tunnelBlocksPerTick", 256, 1, 2048);
        MAX_CRATER_BLOCKS = b.comment("Hard cap of blocks a single crater may remove (protects the server from lag spikes)")
                .defineInRange("craterBlockBudget", 8000, 100, 60000);
        b.pop();
        b.push("balance");
        MAX_FLIGHT_SPEED = b.comment("Absolute flight speed cap in blocks per tick (vanilla anti-cheat starts rubber-banding near 10)")
                .defineInRange("maxFlightSpeed", 6.5, 1.0, 9.0);
        XP_MULTIPLIER = b.comment("Multiplier for all power experience gained")
                .defineInRange("xpMultiplier", 1.0, 0.0, 100.0);
        FIRST_JOIN_CHANCE = b.comment("Chance (0..1) that a player becomes a Viltrumite the first time he joins a world")
                .defineInRange("firstJoinViltrumiteChance", 0.30, 0.0, 1.0);
        PVP_SUPER_DAMAGE = b.comment("Allow super-strength damage against other players")
                .define("pvpSuperDamage", true);
        b.pop();
        SPEC = b.build();
    }
}
