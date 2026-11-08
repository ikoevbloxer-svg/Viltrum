package com.viltrum.client;

import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import com.viltrum.entity.ViltrumiteEntity;
import com.viltrum.registry.ModAttachments;
import com.viltrum.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the motion of every flying entity on the client (lean, roll, speed) and spawns all the
 * speed particles: wind streaks, contrails, Mach cone rings, ground wake.
 */
public final class FlightVisuals {
    public static final Map<Integer, Track> TRACKS = new HashMap<>();

    public static final class Track {
        public Vec3 vel = Vec3.ZERO;
        public double speed;
        public float alpha, alphaO;   // body lean in degrees (0 upright .. 90 horizontal)
        public float roll, rollO;     // banking
        public float lastYaw;
        public boolean flying;
        public int age;
    }

    private FlightVisuals() {}

    public static boolean isFlying(Entity e) {
        if (e instanceof net.minecraft.world.entity.player.Player p) {
            PowerData d = p.getData(ModAttachments.POWER.get());
            return d.viltrumite && d.flying;
        }
        return e instanceof ViltrumiteEntity v && v.isFlyingPose();
    }

    public static void tick(Minecraft mc) {
        ClientLevel lvl = mc.level;
        if (lvl == null) { TRACKS.clear(); return; }
        Set<Integer> seen = new HashSet<>();
        for (AbstractClientPlayer p : lvl.players()) handle(mc, lvl, p, seen);
        for (Entity e : lvl.entitiesForRendering()) {
            if (e instanceof ViltrumiteEntity v) handle(mc, lvl, v, seen);
        }
        TRACKS.keySet().removeIf(id -> !seen.contains(id));
    }

    private static void handle(Minecraft mc, ClientLevel lvl, LivingEntity e, Set<Integer> seen) {
        boolean fly = isFlying(e);
        Track t = TRACKS.get(e.getId());
        if (!fly && (t == null || t.alpha < 1.0f)) return; // finished
        if (t == null) {
            t = new Track();
            t.lastYaw = e.getYRot();
            TRACKS.put(e.getId(), t);
        }
        seen.add(e.getId());
        t.flying = fly;
        t.age++;

        Vec3 raw = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
        t.vel = t.vel.lerp(raw, 0.55);
        double speed = t.vel.length();
        t.speed = speed;

        double horiz = Math.hypot(t.vel.x, t.vel.z);
        double elev = Math.toDegrees(Math.atan2(t.vel.y, horiz));
        float f = (float) Mth.clamp((speed - 0.08) / 0.35, 0.0, 1.0);
        float target = fly ? f * (float) Mth.clamp(90.0 - elev, 0.0, 150.0) : 0.0f;
        t.alphaO = t.alpha;
        t.alpha = Mth.lerp(0.2f, t.alpha, target);

        float dy = Mth.wrapDegrees(e.getYRot() - t.lastYaw);
        t.lastYaw = e.getYRot();
        float rollTarget = fly ? Mth.clamp(-dy * 1.8f, -40.0f, 40.0f) * f : 0.0f;
        t.rollO = t.roll;
        t.roll = Mth.lerp(0.18f, t.roll, rollTarget);

        if (fly && speed > 0.2) spawn(mc, lvl, e, t, speed);
    }

    private static void spawn(Minecraft mc, ClientLevel lvl, LivingEntity e, Track t, double speed) {
        Vec3 dir = t.vel.normalize();
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
        var rnd = lvl.random;
        boolean local = e == mc.player;
        boolean sonicFx = !(e instanceof net.minecraft.world.entity.player.Player p) || p.getData(ModAttachments.POWER.get()).sonicFx;

        // contrail / vapour behind the body
        if (speed > 0.45) {
            int n = 1 + (int) (speed * 1.2);
            for (int i = 0; i < n; i++) {
                Vec3 p = c.subtract(dir.scale(0.7 + rnd.nextDouble() * 0.6)).add(rnd.nextGaussian() * 0.18, rnd.nextGaussian() * 0.22, rnd.nextGaussian() * 0.18);
                lvl.addParticle(ModParticles.VAPOR.get(), p.x, p.y, p.z, -dir.x * 0.03, -dir.y * 0.03, -dir.z * 0.03);
            }
        }
        // glowing sparks from hands and boots when really fast
        if (speed > 1.5) {
            for (int i = 0; i < 2; i++) {
                Vec3 p = c.subtract(dir.scale(0.8)).add(rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25);
                lvl.addParticle(ModParticles.SPARK.get(), p.x, p.y, p.z, -dir.x * 0.25, -dir.y * 0.25, -dir.z * 0.25);
            }
        }
        // wind streaks rushing past; much denser for the local camera
        if (speed > 1.0) {
            int n = local ? 2 + (int) (speed * 2.5) : 1;
            Vec3 up = Math.abs(dir.y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 a = dir.cross(up).normalize();
            Vec3 b = dir.cross(a).normalize();
            for (int i = 0; i < n; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                double rad = 1.1 + rnd.nextDouble() * 3.2;
                Vec3 p = c.add(dir.scale(1.0 + rnd.nextDouble() * 9.0)).add(a.scale(Math.cos(ang) * rad)).add(b.scale(Math.sin(ang) * rad));
                Vec3 v = t.vel.scale(1.6 + rnd.nextDouble());
                lvl.addParticle(ModParticles.STREAK.get(), p.x, p.y, p.z, v.x, v.y, v.z);
            }
        }
        // Mach cone: a chain of rings behind the nose
        if (sonicFx && speed >= PowerMath.MACH * 0.95 && (t.age & 1) == 0) {
            double r = 1.8 + Math.min(4.0, (speed - PowerMath.MACH * 0.9) * 1.6);
            Vec3 p = c.add(dir.scale(0.4));
            lvl.addParticle(ModParticles.SHOCK_RING.get(), p.x, p.y, p.z, dir.x * r, dir.y * r, dir.z * r);
            if ((t.age & 3) == 0) lvl.addParticle(ModParticles.VAPOR.get(), p.x - dir.x, p.y, p.z - dir.z, 0, 0, 0);
        }
        // ground wake: dust sprayed from the surface we are skimming
        if (speed > 0.8) {
            Vec3 from = e.position();
            BlockHitResult hit = lvl.clip(new ClipContext(from, from.add(0, -6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
            if (hit.getType() == HitResult.Type.BLOCK) {
                double h = from.y - hit.getLocation().y;
                int n = (int) Mth.clamp((6.0 - h) * 0.8, 0, 4);
                var state = lvl.getBlockState(hit.getBlockPos());
                if (!state.isAir()) {
                    for (int i = 0; i < n; i++) {
                        lvl.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                                hit.getLocation().x + rnd.nextGaussian() * 0.8, hit.getLocation().y + 0.1, hit.getLocation().z + rnd.nextGaussian() * 0.8,
                                -t.vel.x * 0.25 + rnd.nextGaussian() * 0.1, 0.15 + rnd.nextDouble() * 0.15, -t.vel.z * 0.25 + rnd.nextGaussian() * 0.1);
                    }
                }
            }
        }
    }
}
