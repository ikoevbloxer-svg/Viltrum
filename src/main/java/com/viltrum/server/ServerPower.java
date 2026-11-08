package com.viltrum.server;

import com.viltrum.Config;
import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import com.viltrum.network.FxPayload;
import com.viltrum.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Per-tick server logic of a Viltrumite player. */
public final class ServerPower {
    private ServerPower() {}

    public static void tick(ServerPlayer p, PowerData d) {
        boolean survival = !p.isCreative() && !p.isSpectator();

        if (!d.viltrumite) {
            if (d.flying) { d.flying = false; d.boosting = false; PowerSync.sync(p); }
            if (d.grantedFly && survival) {
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
                d.grantedFly = false;
            }
            return;
        }

        // anti-kick + no fall damage: vanilla treats the player as "allowed to fly"
        if (survival) {
            if (!p.getAbilities().mayfly) {
                p.getAbilities().mayfly = true;
                d.grantedFly = true;
                p.onUpdateAbilities();
            }
            if (p.getAbilities().flying) { // our own flight replaces the creative one
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
        }

        if (d.flying && (p.isSpectator() || p.isPassenger() || p.isSleeping() || !p.isAlive())) {
            d.flying = false; d.boosting = false; PowerSync.sync(p);
        }

        // regeneration
        if (p.tickCount % 20 == 0 && p.getHealth() < p.getMaxHealth()) {
            p.heal(0.5f + 0.35f * d.level);
        }

        if (d.barraging) Combat.tickBarrage(p, d);
        if (d.flying) flightTick(p, d);
        else if (d.machActive) d.machActive = false;

        long now = p.level().getGameTime();
        if (d.dirty && now - d.lastSyncTick >= 40) PowerSync.sync(p);
    }

    private static void flightTick(ServerPlayer p, PowerData d) {
        p.fallDistance = 0;
        p.getFoodData().setExhaustion(0f);
        Vec3 vel = p.position().subtract(p.xo, p.yo, p.zo);
        double speed = vel.length();
        ServerLevel level = p.serverLevel();

        if (speed > 0.15) addXp(p, d, speed * 0.02);

        // sound barrier
        if (speed >= PowerMath.MACH && !d.machActive) {
            d.machActive = true;
            Combat.play(level, p.position(), ModSounds.SONIC_BOOM.get(), 2.2f, 1.0f);
            if (d.sonicFx) PowerSync.fx(level, FxPayload.SONIC_BOOM, p.position().add(0, 0.9, 0), vel.normalize(), (float) (speed / PowerMath.MACH), p.getId());
        } else if (speed < PowerMath.MACH * 0.82) {
            d.machActive = false;
        }

        // supersonic tunnelling: the pressure wave shatters whatever is in the way
        if (speed > 1.4 && PowerMath.breakHardness(d) > 0) tunnel(p, d, vel, speed);
    }

    private static void tunnel(ServerPlayer p, PowerData d, Vec3 vel, double speed) {
        ServerLevel level = p.serverLevel();
        double hard = PowerMath.breakHardness(d);
        Vec3 dir = vel.normalize();
        double reach = Math.min(speed * 2.5, 12.0);
        double radius = 1.0 + Math.min(1.2, speed * 0.25);
        int r = (int) Math.ceil(radius);
        Vec3 base = p.position().add(0, p.getBbHeight() * 0.5, 0);
        int budget = Config.MAX_BLOCKS_PER_TICK.get();
        int smashed = 0;
        for (double t = 0; t <= reach && budget > 0; t += 1.0) {
            Vec3 c = base.add(dir.scale(t));
            BlockPos cp = BlockPos.containing(c);
            for (int dx = -r; dx <= r && budget > 0; dx++)
                for (int dy = -r; dy <= r && budget > 0; dy++)
                    for (int dz = -r; dz <= r && budget > 0; dz++) {
                        BlockPos bp = cp.offset(dx, dy, dz);
                        if (bp.getCenter().distanceToSqr(c) > radius * radius) continue;
                        if (BlockSmasher.smash(p, level, bp, hard, (smashed & 3) == 0)) { budget--; smashed++; }
                    }
        }
        if (smashed > 8 && p.tickCount % 6 == 0) {
            Combat.play(level, p.position().add(dir.scale(3)), ModSounds.LANDING.get(), 0.9f, 1.3f);
        }
    }

    // ------------------------------------------------------------------ xp
    public static void addXp(ServerPlayer p, PowerData d, double amount) {
        if (!d.viltrumite || amount <= 0) return;
        amount *= Config.XP_MULTIPLIER.get();
        d.xp += amount;
        d.dirty = true;
        boolean up = false;
        while (d.level < PowerMath.MAX_LEVEL && d.xp >= PowerMath.xpForNext(d.level)) {
            d.xp -= PowerMath.xpForNext(d.level);
            d.level++;
            up = true;
        }
        if (d.level >= PowerMath.MAX_LEVEL) d.xp = Math.min(d.xp, PowerMath.xpForNext(PowerMath.MAX_LEVEL - 1));
        if (up) levelUpFx(p, d);
    }

    public static void levelUpFx(ServerPlayer p, PowerData d) {
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("msg.viltrum.levelup.title", d.level)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("msg.viltrum.levelup.subtitle")));
        PowerSync.fx(level, FxPayload.LEVEL_UP, p.position().add(0, 0.2, 0), Vec3.ZERO, 1f, p.getId());
        PowerSync.sync(p);
    }
}
