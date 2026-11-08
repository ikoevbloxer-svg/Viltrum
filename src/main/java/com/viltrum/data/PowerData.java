package com.viltrum.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/** Per-player Viltrumite state. Persistent part is saved to NBT, the rest is runtime-only. */
public class PowerData {
    // ---- persistent
    public boolean viltrumite = false;
    public int level = 1;
    public double xp = 0;
    /** power output / limiter, 0..1 (what the player chose in the power menu). */
    public float strength = 1.0f;
    /** flight speed limiter 0..1. */
    public float flightLimit = 1.0f;
    public boolean allowDestruction = true;
    public boolean sonicFx = true;
    public boolean blood = true;
    public boolean grantedFly = false;
    /** the first-join lottery has already been rolled for this player. */
    public boolean rolled = false;

    // ---- runtime (synced, not saved)
    public boolean flying = false;
    public boolean boosting = false;
    public boolean charging = false;
    public boolean barraging = false;
    public boolean senses = false;
    public boolean rage = false;
    /** 0..1 ramp of the rage state; output is multiplied by 1 + 0.65 * rageAmt (up to 165%). */
    public float rageAmt = 0f;
    /** entity id of the creature we are holding by the neck, -1 when none. */
    public int grabbedId = -1;

    // ---- server only runtime
    public long lastStrike = 0;
    public long barrageStart = 0;
    public long barrageCooldownUntil = 0;
    public boolean machActive = false;
    public long lastClap = 0;
    public int rageTicks = 0;
    public long rageCooldownUntil = 0;
    public long stompAt = 0;
    public long lastStomp = 0;
    public float rageSynced = 0f;
    public boolean dirty = false;
    public long lastSyncTick = 0;

    public void write(CompoundTag t) {
        t.putBoolean("viltrumite", viltrumite);
        t.putInt("level", level);
        t.putDouble("xp", xp);
        t.putFloat("strength", strength);
        t.putFloat("flightLimit", flightLimit);
        t.putBoolean("allowDestruction", allowDestruction);
        t.putBoolean("sonicFx", sonicFx);
        t.putBoolean("blood", blood);
        t.putBoolean("grantedFly", grantedFly);
        t.putBoolean("rolled", rolled);
    }

    public void read(CompoundTag t) {
        viltrumite = t.getBoolean("viltrumite");
        level = Mth.clamp(t.contains("level") ? t.getInt("level") : 1, 1, PowerMath.MAX_LEVEL);
        xp = t.getDouble("xp");
        strength = t.contains("strength") ? Mth.clamp(t.getFloat("strength"), 0f, 1f) : 1f;
        flightLimit = t.contains("flightLimit") ? Mth.clamp(t.getFloat("flightLimit"), 0f, 1f) : 1f;
        allowDestruction = !t.contains("allowDestruction") || t.getBoolean("allowDestruction");
        sonicFx = !t.contains("sonicFx") || t.getBoolean("sonicFx");
        blood = !t.contains("blood") || t.getBoolean("blood");
        grantedFly = t.getBoolean("grantedFly");
        rolled = t.getBoolean("rolled");
    }

    public void writeNet(FriendlyByteBuf b) {
        b.writeBoolean(viltrumite);
        b.writeVarInt(level);
        b.writeDouble(xp);
        b.writeFloat(strength);
        b.writeFloat(flightLimit);
        int flags = (allowDestruction ? 1 : 0) | (sonicFx ? 2 : 0) | (blood ? 4 : 0)
                | (flying ? 8 : 0) | (boosting ? 16 : 0) | (charging ? 32 : 0) | (barraging ? 64 : 0) | (senses ? 128 : 0) | (rage ? 256 : 0);
        b.writeVarInt(flags);
        b.writeVarInt(grabbedId + 1);
        b.writeFloat(rageAmt);
    }

    public void readNet(FriendlyByteBuf b) {
        viltrumite = b.readBoolean();
        level = b.readVarInt();
        xp = b.readDouble();
        strength = b.readFloat();
        flightLimit = b.readFloat();
        int f = b.readVarInt();
        allowDestruction = (f & 1) != 0;
        sonicFx = (f & 2) != 0;
        blood = (f & 4) != 0;
        flying = (f & 8) != 0;
        boosting = (f & 16) != 0;
        charging = (f & 32) != 0;
        barraging = (f & 64) != 0;
        senses = (f & 128) != 0;
        grabbedId = b.readVarInt() - 1;
        rage = (f & 256) != 0;
        rageAmt = b.readFloat();
    }

    /** copies everything that the network sync carries. */
    public void copyNetFrom(PowerData o) {
        viltrumite = o.viltrumite; level = o.level; xp = o.xp; strength = o.strength; flightLimit = o.flightLimit;
        allowDestruction = o.allowDestruction; sonicFx = o.sonicFx; blood = o.blood;
        flying = o.flying; boosting = o.boosting; charging = o.charging; barraging = o.barraging;
        senses = o.senses; grabbedId = o.grabbedId; rage = o.rage; rageAmt = o.rageAmt;
    }
}
