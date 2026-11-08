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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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
        tickRage(p, d);
        if (d.stompAt > 0 && p.level().getGameTime() >= d.stompAt) { d.stompAt = 0; Combat.stompImpact(p, d); }
        if (d.grabbedId >= 0) GrabHandler.tick(p, d);
        if (d.senses) sensesTick(p, d);
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
        if (speed > 1.6 && PowerMath.breakHardness(d) > 0) tunnel(p, d, vel, speed);
    }

    /**
     * Supersonic pressure wave: shatters only what is right in front of the nose (1 to 2.4 blocks ahead of the body),
     * never the floor under the feet and never anything behind or beside the flight path.
     */
    private static void tunnel(ServerPlayer p, PowerData d, Vec3 vel, double speed) {
        ServerLevel level = p.serverLevel();
        double hard = PowerMath.breakHardness(d);
        Vec3 dir = vel.normalize();
        Vec3 base = p.position().add(0, p.getBbHeight() * 0.5, 0);
        double footY = p.getY();
        boolean diving = dir.y < -0.45;
        double reach = speed > 3.0 ? 2.4 : 2.0;
        double width = 0.9 + 0.8 * Math.min(1.0, PowerMath.eff(d) * d.level / 30.0);
        int r = (int) Math.ceil(width);
        int budget = Config.MAX_BLOCKS_PER_TICK.get();
        int smashed = 0;
        for (double t = 1.0; t <= reach && budget > 0; t += 0.5) {
            Vec3 c = base.add(dir.scale(t));
            BlockPos cp = BlockPos.containing(c);
            for (int dx = -r; dx <= r && budget > 0; dx++)
                for (int dy = -r; dy <= r && budget > 0; dy++)
                    for (int dz = -r; dz <= r && budget > 0; dz++) {
                        BlockPos bp = cp.offset(dx, dy, dz);
                        Vec3 bc = bp.getCenter();
                        if (bc.distanceToSqr(c) > width * width) continue;
                        if (bc.subtract(base).dot(dir) < 0.6) continue;               // must be ahead of us
                        if (!diving && bp.getY() + 1 <= footY + 0.01) continue;       // never the ground we skim over
                        if (BlockSmasher.smash(p, level, bp, hard, (smashed & 1) == 0)) { budget--; smashed++; }
                    }
        }
        if (smashed > 2 && p.tickCount % 5 == 0) {
            Combat.play(level, p.position().add(dir.scale(2)), ModSounds.LANDING.get(), 0.8f, 1.3f);
        }
    }

    // ------------------------------------------------------------------ rage
    public static void toggleRage(ServerPlayer p, PowerData d) {
        long now = p.level().getGameTime();
        if (d.rage) { endRage(p, d, 120); return; }
        if (d.level < PowerMath.UNLOCK_RAGE) {
            p.displayClientMessage(Component.translatable("msg.viltrum.locked", PowerMath.UNLOCK_RAGE), true);
            return;
        }
        if (now < d.rageCooldownUntil) {
            p.displayClientMessage(Component.translatable("msg.viltrum.rage_cooldown", (d.rageCooldownUntil - now) / 20 + 1), true);
            return;
        }
        d.rage = true;
        d.rageTicks = 400 + 8 * d.level;
        Combat.play(p.serverLevel(), p.position(), ModSounds.RAGE.get(), 1.6f, 1.0f);
        PowerSync.fx(p.serverLevel(), FxPayload.RAGE, p.position().add(0, 0.9, 0), Vec3.ZERO, 1f, p.getId());
        PowerSync.sync(p);
    }

    private static void endRage(ServerPlayer p, PowerData d, int cooldownTicks) {
        d.rage = false;
        d.rageCooldownUntil = p.level().getGameTime() + cooldownTicks;
        PowerSync.sync(p);
    }

    private static void tickRage(ServerPlayer p, PowerData d) {
        if (d.rage) {
            if (d.level < PowerMath.UNLOCK_RAGE) { endRage(p, d, 0); return; }
            d.rageAmt = Math.min(1f, d.rageAmt + 1f / 30f);
            if (--d.rageTicks <= 0) endRage(p, d, 260);
        } else if (d.rageAmt > 0f) {
            d.rageAmt = Math.max(0f, d.rageAmt - 1f / 40f);
        }
        if (Math.abs(d.rageAmt - d.rageSynced) > 0.05f || (d.rageAmt == 0f && d.rageSynced != 0f)) {
            d.rageSynced = d.rageAmt;
            PowerSync.sync(p);
        }
    }

    /** title shown when the Viltrumite blood awakens by itself (first-join lottery). */
    public static void awakenFx(ServerPlayer p) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 25));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("msg.viltrum.awaken.title")));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("msg.viltrum.awaken.subtitle")));
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        PowerSync.fx(level, FxPayload.LEVEL_UP, p.position().add(0, 0.2, 0), Vec3.ZERO, 1f, p.getId());
    }

    private static void sensesTick(ServerPlayer p, PowerData d) {
        if (d.level < PowerMath.UNLOCK_SENSES) { d.senses = false; PowerSync.sync(p); return; }
        if (p.tickCount % 10 != 0) return;
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false));
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(40.0), x -> x != p && x.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
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
