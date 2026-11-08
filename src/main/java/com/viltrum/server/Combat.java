package com.viltrum.server;

import com.viltrum.Config;
import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import com.viltrum.network.FxPayload;
import com.viltrum.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Server-side combat: charged strikes, barrage, flight impacts, craters. */
public final class Combat {
    /** set while we deal damage ourselves so that the damage event does not amplify it a second time. */
    public static boolean internal = false;
    /** set while an execution / stomp kills things so that Reanimen cannot get back up. */
    public static boolean noRevive = false;

    private Combat() {}

    // ------------------------------------------------------------------ helpers
    public static LivingEntity findTarget(ServerPlayer p, double reach) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(reach));
        BlockHitResult bhr = p.serverLevel().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = bhr.getType() == HitResult.Type.MISS ? end : bhr.getLocation();
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(eye, stop).inflate(1.2),
                x -> x != p && x.isAlive() && !x.isSpectator())) {
            Optional<Vec3> h = e.getBoundingBox().inflate(0.3).clip(eye, stop);
            if (h.isPresent()) {
                double d = eye.distanceToSqr(h.get());
                if (d < bd) { bd = d; best = e; }
            }
        }
        return best;
    }

    /** damage without any launch (used for holds, squeezes, thrown victims). */
    public static boolean hurtOnly(ServerPlayer p, LivingEntity target, float dmg) {
        if (target instanceof Player && !Config.PVP_SUPER_DAMAGE.get()) dmg = Math.min(dmg, 8f);
        boolean prev = internal;
        internal = true;
        try {
            target.invulnerableTime = 0;
            boolean ok = target.hurt(p.damageSources().playerAttack(p), dmg);
            if (ok) ServerPower.addXp(p, PowerSync.get(p), dmg * 0.25);
            return ok;
        } finally { internal = prev; }
    }

    /** damage + launch. returns true when damage was actually applied. */
    public static boolean hit(ServerPlayer p, PowerData d, LivingEntity target, float dmg, Vec3 dir, double kb) {
        if (target instanceof Player && !Config.PVP_SUPER_DAMAGE.get()) dmg = Math.min(dmg, 8f);
        boolean ok;
        boolean prev = internal;
        internal = true;
        try { ok = target.hurt(p.damageSources().playerAttack(p), dmg); } finally { internal = prev; }
        if (ok) {
            Vec3 v = dir.normalize().scale(kb).add(0, 0.12 + 0.1 * kb, 0);
            target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 0.2, 0.2).add(v));
            target.hurtMarked = true;
            ServerPower.addXp(p, d, dmg * 0.35);
        }
        return ok;
    }

    public static void play(ServerLevel level, Vec3 pos, SoundEvent s, float vol, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    // ------------------------------------------------------------------ charged strike
    public static void strike(ServerPlayer p, PowerData d, float charge) {
        if (!d.viltrumite) return;
        if (d.grabbedId >= 0) { // while holding someone the strike key hurls them
            d.charging = false;
            GrabHandler.release(p, d, true, Mth.clamp(charge, 0f, 1f));
            return;
        }
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (now - d.lastStrike < 10) return;
        d.lastStrike = now;
        charge = Mth.clamp(charge, 0f, 1f);
        d.charging = false;
        PowerSync.sync(p);

        float suit = PowerMath.suitDamageMult(p);
        float dmg = PowerMath.punchDamage(d) * (1f + 2.2f * charge) * suit;
        double reach = 3.6 + 3.2 * charge + (d.flying ? 1.5 : 0.0);
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(reach));
        p.swing(InteractionHand.MAIN_HAND, true);

        LivingEntity target = findTarget(p, reach);
        BlockHitResult bhr = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        boolean blockHit = bhr.getType() == HitResult.Type.BLOCK;
        if (target == null && !blockHit) {
            play(level, p.position(), ModSounds.PUNCH_HEAVY.get(), 0.7f, 0.55f + 0.2f * (1 - charge));
            return;
        }
        Vec3 impact = target != null ? target.getBoundingBox().getCenter() : bhr.getLocation();
        double kb = PowerMath.knockback(d) * (1.0 + 0.9 * charge);

        if (target != null) {
            boolean ok = hit(p, d, target, dmg, look, kb);
            if (ok && dmg >= 22f) play(level, impact, ModSounds.BONE_CRACK.get(), 1.0f, 0.9f + level.random.nextFloat() * 0.2f);
        }

        double energy = (0.4 + 2.2 * charge) * (0.4 + 0.6 * PowerMath.eff(d));
        float strengthFx = (float) (0.3 + 0.7 * charge * PowerMath.eff(d));
        if (charge >= 0.5f && PowerMath.eff(d) > 0.3f) {
            play(level, impact, ModSounds.PUNCH_SUPER.get(), 1.4f, 1.0f);
            // shock wave: damages everything around the impact
            double radius = 2.5 + 5.0 * charge * (0.4 + 0.6 * PowerMath.eff(d));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(impact, impact).inflate(radius),
                    x -> x != p && x != target && x.isAlive())) {
                double dist = e.position().distanceTo(impact);
                if (dist > radius) continue;
                float falloff = (float) (1.0 - dist / radius);
                Vec3 away = e.position().subtract(impact).add(0, 0.4, 0);
                hit(p, d, e, dmg * 0.4f * falloff, away, kb * 0.6 * falloff);
            }
            PowerSync.fx(level, FxPayload.SHOCKWAVE, impact, bhr.getType() == HitResult.Type.BLOCK ? new Vec3(bhr.getDirection().getStepX(), bhr.getDirection().getStepY(), bhr.getDirection().getStepZ()) : look.scale(-1), strengthFx, p.getId());
        } else {
            play(level, impact, ModSounds.PUNCH_HEAVY.get(), 1.2f, 0.9f + 0.4f * (1 - charge));
            PowerSync.fx(level, FxPayload.HIT_SPARKS, impact, look.scale(-1), strengthFx, p.getId());
        }
        if (blockHit && PowerMath.breakHardness(d) > 0) {
            crater(p, d, bhr.getLocation().add(look.scale(0.4)), energy * 0.8);
        }
        ServerPower.addXp(p, d, 2.0 + 6.0 * charge);
    }

    // ------------------------------------------------------------------ barrage
    public static void startBarrage(ServerPlayer p, PowerData d) {
        long now = p.level().getGameTime();
        if (!d.viltrumite || d.barraging || now < d.barrageCooldownUntil) return;
        d.barraging = true;
        d.barrageStart = now;
        play(p.serverLevel(), p.position(), ModSounds.PUNCH_BARRAGE.get(), 1.0f, 1.0f);
        PowerSync.sync(p);
    }

    public static void stopBarrage(ServerPlayer p, PowerData d, boolean cutSound) {
        if (!d.barraging) return;
        d.barraging = false;
        d.barrageCooldownUntil = p.level().getGameTime() + 50;
        if (cutSound) PowerSync.fx(p.serverLevel(), FxPayload.STOP_BARRAGE, p.position(), Vec3.ZERO, 0, p.getId());
        PowerSync.sync(p);
    }

    public static void tickBarrage(ServerPlayer p, PowerData d) {
        long now = p.level().getGameTime();
        if (now - d.barrageStart > 88) { stopBarrage(p, d, false); return; }
        if (now % 2 != 0) return;
        p.swing((now / 2) % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
        LivingEntity held = GrabHandler.target(p);
        LivingEntity t = held != null ? held : findTarget(p, 4.2);
        if (t == null) return;
        float dmg = PowerMath.punchDamage(d) * (held != null ? 0.35f : 0.22f) * PowerMath.suitDamageMult(p);
        Vec3 look = p.getLookAngle();
        if (held != null) hurtOnly(p, t, Math.max(1f, dmg));
        else hit(p, d, t, Math.max(1f, dmg), look, 0.12); // tiny knock-back keeps the target pinned in front of the fists
        t.invulnerableTime = 0; // allow the next rapid hit
        ServerLevel level = p.serverLevel();
        Vec3 c = t.getBoundingBox().getCenter();
        PowerSync.fx(level, FxPayload.BARRAGE_HIT, c, look.scale(-1), 0.35f + 0.5f * PowerMath.eff(d), p.getId());
        if (now % 4 == 0) play(level, c, ModSounds.PUNCH_HEAVY.get(), 0.55f, 1.15f + level.random.nextFloat() * 0.25f);
        if (now % 12 == 0 && dmg > 12f) play(level, c, ModSounds.BONE_CRACK.get(), 0.6f, 0.9f + level.random.nextFloat() * 0.3f);
    }

    // ------------------------------------------------------------------ thunderclap (level 15+)
    public static void clap(ServerPlayer p, PowerData d) {
        if (d.level < PowerMath.UNLOCK_CLAP) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.viltrum.locked", PowerMath.UNLOCK_CLAP), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (now - d.lastClap < 100) return;
        d.lastClap = now;
        p.swing(InteractionHand.MAIN_HAND, true);
        Vec3 c = p.position().add(0, 0.9, 0);
        double radius = 5.0 + 0.3 * d.level * (0.3 + 0.7 * PowerMath.eff(d));
        float dmg = PowerMath.punchDamage(d) * 0.45f * PowerMath.suitDamageMult(p);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius), x -> x != p && x.isAlive())) {
            double dist = e.position().distanceTo(c);
            if (dist > radius) continue;
            float falloff = (float) (1.0 - dist / radius * 0.8);
            hit(p, d, e, dmg * falloff, e.position().subtract(c).add(0, 0.6, 0), PowerMath.knockback(d) * 0.9 * falloff);
        }
        play(level, c, ModSounds.SLAM.get(), 1.6f, 1.15f);
        play(level, c, ModSounds.SONIC_BOOM.get(), 1.2f, 1.2f);
        PowerSync.fx(level, FxPayload.SHOCKWAVE, p.position().add(0, 0.1, 0), new Vec3(0, 1, 0), (float) Mth.clamp(0.4 + PowerMath.eff(d) * 0.6, 0, 1), p.getId());
        PowerSync.fx(level, FxPayload.SONIC_BOOM, c, new Vec3(0, 1, 0), 1.0f, p.getId());
        ServerPower.addXp(p, d, 3.0);
    }

    // ------------------------------------------------------------------ stomp (level 18+)
    public static void stomp(ServerPlayer p, PowerData d) {
        if (d.level < PowerMath.UNLOCK_STOMP) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.viltrum.locked", PowerMath.UNLOCK_STOMP), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (now - d.lastStomp < 70 || d.stompAt > 0) return;
        d.lastStomp = now;
        d.stompAt = now + 7; // the foot comes down after the wind-up animation
        PowerSync.fx(level, FxPayload.STOMP, p.position(), Vec3.ZERO, 1f, p.getId());
        play(level, p.position(), ModSounds.CHARGE.get(), 0.7f, 1.6f);
    }

    /** the foot lands: every hostile creature around dies, everything else is only shoved away. */
    public static void stompImpact(ServerPlayer p, PowerData d) {
        ServerLevel level = p.serverLevel();
        double eff = PowerMath.eff(d);
        double radius = 8.0 + 0.4 * d.level * (0.4 + 0.6 * Math.min(1.0, eff));
        Vec3 c = p.position();
        int killed = 0;
        boolean prevRevive = noRevive;
        noRevive = true;
        try {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius, radius * 0.6, radius), x -> x != p && x.isAlive())) {
                double dist = e.position().distanceTo(c);
                if (dist > radius) continue;
                if (e instanceof net.minecraft.world.entity.monster.Enemy && !(e instanceof Player)) {
                    hurtOnly(p, e, 1.0e6f);
                    if (!e.isAlive()) killed++;
                } else {
                    Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
                    away = away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : away.normalize();
                    float f = (float) (1.0 - dist / radius * 0.7);
                    e.setDeltaMovement(e.getDeltaMovement().add(away.x * 0.9 * f, 0.5 * f, away.z * 0.9 * f));
                    e.hurtMarked = true;
                }
            }
        } finally { noRevive = prevRevive; }
        play(level, c, ModSounds.SLAM.get(), 2.0f, 0.85f);
        play(level, c, ModSounds.SONIC_BOOM.get(), 1.6f, 0.9f);
        play(level, c, ModSounds.LANDING.get(), 1.6f, 0.7f);
        PowerSync.fx(level, FxPayload.STOMP_IMPACT, c, new Vec3(0, 1, 0), (float) radius, p.getId());
        if (PowerMath.breakHardness(d) > 0) crater(p, d, c.add(0, -0.3, 0), 3.0 * Math.max(0.5, eff));
        ServerPower.addXp(p, d, 4.0 + killed * 0.5);
    }

    // ------------------------------------------------------------------ flight impact
    public static void impact(ServerPlayer p, PowerData d, double speed, Vec3 dir) {
        if (!d.viltrumite || !d.flying) return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (now - d.lastStrike < 6) return;
        d.lastStrike = now;
        speed = Mth.clamp(speed, 0, Config.MAX_FLIGHT_SPEED.get() * 1.3);
        if (speed < 1.0) return;
        Vec3 origin = p.position().add(0, p.getBbHeight() * 0.5, 0).add(dir.scale(0.9));
        double energy = speed * (0.35 + 0.65 * PowerMath.eff(d));
        play(level, origin, speed > 2.4 ? ModSounds.SLAM.get() : ModSounds.LANDING.get(), 1.3f, 0.9f + level.random.nextFloat() * 0.2f);
        PowerSync.fx(level, FxPayload.SHOCKWAVE, origin, dir.scale(-1), (float) Mth.clamp(energy / 4.0, 0.2, 1.0), p.getId());
        crater(p, d, origin, energy);

        LivingEntity held = GrabHandler.target(p);
        if (held != null) { // ramming someone into the mountain
            float ram = (float) (PowerMath.fullPunch(d.level) * (0.4 + speed / 3.0) * PowerMath.eff(d) * PowerMath.eff(d) + 4.0);
            hurtOnly(p, held, ram);
            play(level, held.position(), ModSounds.BONE_CRACK.get(), 1.4f, 0.8f);
            PowerSync.fx(level, FxPayload.BARRAGE_HIT, held.getBoundingBox().getCenter(), dir.scale(-1), 1.0f, p.getId());
        }

        double radius = 2.0 + energy * 0.9;
        float dmg = PowerMath.punchDamage(d) * 0.55f * (float) Mth.clamp(speed / 3.0, 0.3, 1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(radius), x -> x != p && x != held && x.isAlive())) {
            double dist = e.position().distanceTo(origin);
            if (dist > radius) continue;
            float falloff = (float) (1.0 - dist / radius);
            hit(p, d, e, dmg * falloff, e.position().subtract(origin).add(0, 0.5, 0), PowerMath.knockback(d) * 0.7 * falloff);
        }
        if (speed > 3.2 && !p.isCreative()) {
            float self = (float) ((speed - 2.6) * 2.0 * (1.0 - PowerMath.damageResist(d)) * (held != null ? 0.25 : 1.0));
            if (self > 0.5f) { internal = true; try { p.hurt(p.damageSources().flyIntoWall(), Math.min(self, 6f)); } finally { internal = false; } }
        }
    }

    // ------------------------------------------------------------------ crater
    /** digs a rough sphere; hardness-limited and capped by the config. */
    public static void crater(ServerPlayer p, PowerData d, Vec3 center, double energy) {
        int r = PowerMath.craterRadius(d, energy);
        if (r < 1) return;
        double hard = PowerMath.breakHardness(d);
        ServerLevel level = p.serverLevel();
        BlockPos c = BlockPos.containing(center);
        int budget = Config.MAX_CRATER_BLOCKS.get();
        int broken = 0;
        RandomLike rnd = new RandomLike(c.asLong());
        for (int dx = -r; dx <= r && budget > 0; dx++)
            for (int dy = -r; dy <= r && budget > 0; dy++)
                for (int dz = -r; dz <= r && budget > 0; dz++) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double lim = r + 0.35 * (rnd.next() - 0.5) * r; // ragged rim
                    if (dist > lim) continue;
                    BlockPos bp = c.offset(dx, dy, dz);
                    if (BlockSmasher.smash(p, level, bp, hard, broken % 30 == 0)) { budget--; broken++; }
                }
        if (broken > 0) PowerSync.fx(level, FxPayload.CRATER, center, Vec3.ZERO, r, p.getId());
    }

    private static final class RandomLike {
        private long s;
        RandomLike(long seed) { s = seed * 6364136223846793005L + 1442695040888963407L; }
        double next() { s = s * 6364136223846793005L + 1442695040888963407L; return ((s >>> 33) & 0xFFFFFF) / (double) 0x1000000; }
    }

}
