package com.viltrum.server;

import com.viltrum.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Optional;

/** Hit-scan energy beam used by the Flaxan blaster and by Flaxan soldiers. */
public final class BeamUtil {
    private BeamUtil() {}

    public static void fire(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 dir, double range, float damage) {
        Vec3 end = from.add(dir.scale(range));
        BlockHitResult bhr = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 stop = bhr.getType() == HitResult.Type.MISS ? end : bhr.getLocation();

        LivingEntity victim = null;
        double best = Double.MAX_VALUE;
        AABB box = new AABB(from, stop).inflate(1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != shooter && x.isAlive() && !x.isSpectator())) {
            Optional<Vec3> h = e.getBoundingBox().inflate(0.25).clip(from, stop);
            if (h.isPresent()) {
                double d = from.distanceToSqr(h.get());
                if (d < best) { best = d; victim = e; }
            }
        }
        if (victim != null) {
            stop = from.add(dir.scale(Math.sqrt(best)));
            DamageSource src = shooter instanceof Player p ? level.damageSources().playerAttack(p) : level.damageSources().mobAttack(shooter);
            boolean wasInternal = Combat.internal;
            Combat.internal = true;
            try { victim.hurt(src, damage); } finally { Combat.internal = wasInternal; }
            victim.setRemainingFireTicks(40);
        }

        // visuals: dense red dust line + hot core
        double len = from.distanceTo(stop);
        int steps = (int) Math.min(160, len / 0.3);
        DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0f, 0.12f, 0.08f), 1.15f);
        DustParticleOptions core = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.7f), 0.6f);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(dir.scale(i * 0.3));
            level.sendParticles(red, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
            if (i % 2 == 0) level.sendParticles(core, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, stop.x, stop.y, stop.z, 10, 0.2, 0.2, 0.2, 0.4);
        level.sendParticles(ParticleTypes.FLASH, stop.x, stop.y, stop.z, 1, 0, 0, 0, 0);
        level.playSound(null, from.x, from.y, from.z, ModSounds.LASER.get(), SoundSource.PLAYERS, 0.9f, 0.92f + level.random.nextFloat() * 0.2f);
    }
}
