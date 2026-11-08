package com.viltrum.data;

import com.viltrum.Config;
import com.viltrum.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/** All balance formulas in one place so the GUI preview always matches what the server really does. */
public final class PowerMath {
    public static final int MAX_LEVEL = 30;
    /** "sound barrier" in blocks per tick (stylised, 48 blocks/s). */
    public static final double MACH = 2.4;

    public static final int UNLOCK_SENSES = 5;
    public static final int UNLOCK_GRAB = 10;
    public static final int UNLOCK_CLAP = 15;

    private PowerMath() {}

    /** levels beyond 10 keep scaling, only more slowly. */
    private static double soft(int level, double perLevelLow, double perLevelHigh) {
        int low = Math.min(level, 10);
        int high = Math.max(0, level - 10);
        return perLevelLow * low + perLevelHigh * high;
    }

    public static double xpForNext(int level) {
        return 80.0 * level * level + 40.0 * level;
    }

    /** damage of a bare-handed hit at 100% output. */
    public static double fullPunch(int level) {
        return 6.0 + 8.5 * (level - 1);
    }

    public static float punchDamage(PowerData d) {
        double t = d.strength;
        return (float) (1.0 + (fullPunch(d.level) - 1.0) * t * t);
    }

    /** launch velocity in blocks per tick. */
    public static double knockback(PowerData d) {
        return Math.min(9.0, 0.35 + (0.35 + soft(d.level, 0.5, 0.12)) * d.strength);
    }

    public static double cruiseSpeed(PowerData d) {
        double max = 0.55 + soft(d.level, 0.27, 0.05);
        return 0.25 + (max - 0.25) * d.flightLimit;
    }

    public static double boostSpeed(PowerData d) {
        double mult = 1.35 + soft(d.level, 0.05, 0.01);
        return Math.min(Config.MAX_FLIGHT_SPEED.get(), cruiseSpeed(d) * mult);
    }

    /** hardest block (destroy speed) that the current output can shatter by tunnelling / craters. 0 = nothing. */
    public static double breakHardness(PowerData d) {
        if (!d.allowDestruction || !Config.ALLOW_BLOCK_DESTRUCTION.get()) return 0;
        if (d.strength < 0.25f) return 0;
        return 2.0 + 62.0 * Math.min(3.0, (d.level - 1) / 9.0) * d.strength;
    }

    public static int craterRadius(PowerData d, double energy) {
        if (breakHardness(d) <= 0) return 0;
        double r = 1.0 + energy * 1.2 * (0.35 + 0.65 * d.strength) * (0.5 + Math.min(d.level, 20) / 10.0);
        return Mth.clamp((int) Math.round(r), 0, Config.MAX_CRATER_RADIUS.get());
    }

    public static double damageResist(PowerData d) {
        return Math.min(0.94, 0.12 + 0.08 * Math.min(d.level, 11) + 0.002 * Math.max(0, d.level - 11));
    }

    public static double xpRequiredFraction(PowerData d) {
        if (d.level >= MAX_LEVEL) return 1.0;
        return Mth.clamp(d.xp / xpForNext(d.level), 0.0, 1.0);
    }

    // ---------------------------------------------------------- suit bonuses
    private static boolean wearing(Player p, Item head, Item chest, Item legs, Item boots) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(head) && p.getItemBySlot(EquipmentSlot.CHEST).is(chest)
                && p.getItemBySlot(EquipmentSlot.LEGS).is(legs) && p.getItemBySlot(EquipmentSlot.FEET).is(boots);
    }

    public static boolean fullInvincibleSuit(Player p) {
        return wearing(p, ModItems.INVINCIBLE_MASK.get(), ModItems.INVINCIBLE_CHEST.get(), ModItems.INVINCIBLE_LEGS.get(), ModItems.INVINCIBLE_BOOTS.get());
    }

    public static boolean fullOmniSuit(Player p) {
        return wearing(p, ModItems.OMNIMAN_HAIR.get(), ModItems.OMNIMAN_CHEST.get(), ModItems.OMNIMAN_LEGS.get(), ModItems.OMNIMAN_BOOTS.get());
    }

    /** Omni-Man set: +15% damage. */
    public static float suitDamageMult(Player p) {
        return fullOmniSuit(p) ? 1.15f : 1.0f;
    }

    /** Invincible set: +12% flight speed. */
    public static double suitFlightMult(Player p) {
        return fullInvincibleSuit(p) ? 1.12 : 1.0;
    }
}
