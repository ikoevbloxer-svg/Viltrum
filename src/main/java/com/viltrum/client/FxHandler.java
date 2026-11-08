package com.viltrum.client;

import com.viltrum.client.hud.PowerHud;
import com.viltrum.data.PowerData;
import com.viltrum.network.FxPayload;
import com.viltrum.registry.ModAttachments;
import com.viltrum.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Visual side of server FxPayloads. */
public final class FxHandler {
    private FxHandler() {}

    public static void play(FxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lvl = mc.level;
        if (lvl == null) return;
        Vec3 pos = new Vec3(p.x(), p.y(), p.z());
        Vec3 dir = new Vec3(p.dx(), p.dy(), p.dz());
        float s = p.strength();
        boolean mine = mc.player != null && p.source() == mc.player.getId();
        var rnd = lvl.random;

        switch (p.type()) {
            case FxPayload.SHOCKWAVE -> {
                Vec3 n = dir.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : dir.normalize();
                double r = 2.0 + 5.0 * s;
                Vec3 at = pos.add(n.scale(0.1));
                lvl.addParticle(ModParticles.SHOCK_RING_WARM.get(), at.x, at.y, at.z, n.x * r, n.y * r, n.z * r);
                lvl.addParticle(ModParticles.SHOCK_RING.get(), at.x, at.y, at.z, n.x * r * 0.7, n.y * r * 0.7, n.z * r * 0.7);
                int sparks = (int) (24 + 40 * s);
                for (int i = 0; i < sparks; i++) {
                    Vec3 v = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize().scale(0.25 + rnd.nextDouble() * (0.5 + s));
                    if (v.dot(n) < 0) v = v.scale(-0.4);
                    lvl.addParticle(ModParticles.SPARK.get(), pos.x, pos.y, pos.z, v.x, v.y, v.z);
                }
                for (int i = 0; i < 6 + (int) (10 * s); i++) {
                    lvl.addParticle(ModParticles.VAPOR.get(), pos.x + rnd.nextGaussian() * 0.5, pos.y + rnd.nextGaussian() * 0.4, pos.z + rnd.nextGaussian() * 0.5,
                            rnd.nextGaussian() * 0.1, 0.05 + rnd.nextDouble() * 0.1, rnd.nextGaussian() * 0.1);
                }
                if (s > 0.7f) lvl.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 0, 0, 0);
                CameraShake.addAt(pos, mine ? 0.55f + 0.5f * s : 0.9f * s);
            }
            case FxPayload.SONIC_BOOM -> {
                Vec3 d = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
                for (int i = 0; i < 4; i++) {
                    double r = 3.0 + i * 1.6 + s * 1.5;
                    Vec3 at = pos.subtract(d.scale(i * 0.9));
                    lvl.addParticle(ModParticles.SHOCK_RING.get(), at.x, at.y, at.z, d.x * r, d.y * r, d.z * r);
                }
                for (int i = 0; i < 30; i++) {
                    Vec3 v = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize().scale(0.6 + rnd.nextDouble());
                    lvl.addParticle(ModParticles.VAPOR.get(), pos.x + v.x * 0.5, pos.y + v.y * 0.5, pos.z + v.z * 0.5, v.x * 0.2, v.y * 0.2, v.z * 0.2);
                }
                if (mine) { PowerHud.flash(1.0f); CameraShake.add(0.7f); } else CameraShake.addAt(pos, 0.6f);
            }
            case FxPayload.CRATER -> {
                int r = (int) s;
                for (int i = 0; i < 10 + r * 8; i++) {
                    Vec3 v = new Vec3(rnd.nextGaussian(), Math.abs(rnd.nextGaussian()) * 0.8, rnd.nextGaussian()).normalize().scale(0.15 + rnd.nextDouble() * 0.5);
                    lvl.addParticle(ParticleTypes.LARGE_SMOKE, pos.x + v.x * r, pos.y + v.y * r * 0.5, pos.z + v.z * r, v.x * 0.3, v.y * 0.3, v.z * 0.3);
                }
                for (int i = 0; i < 4; i++) lvl.addParticle(ParticleTypes.EXPLOSION, pos.x + rnd.nextGaussian() * r * 0.5, pos.y + rnd.nextGaussian() * r * 0.3, pos.z + rnd.nextGaussian() * r * 0.5, 0, 0, 0);
                CameraShake.addAt(pos, 0.2f + 0.08f * r);
            }
            case FxPayload.STOP_BARRAGE -> mc.getSoundManager().stop(com.viltrum.registry.ModSounds.PUNCH_BARRAGE.getId(), SoundSource.PLAYERS);
            case FxPayload.LEVEL_UP -> {
                lvl.addParticle(ModParticles.SHOCK_RING.get(), pos.x, pos.y + 0.1, pos.z, 0, 4.5, 0);
                lvl.addParticle(ModParticles.SHOCK_RING.get(), pos.x, pos.y + 0.9, pos.z, 0, 3.0, 0);
                for (int i = 0; i < 70; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2;
                    double r = 0.3 + rnd.nextDouble() * 0.9;
                    lvl.addParticle(ModParticles.SPARK.get(), pos.x + Math.cos(a) * r, pos.y + rnd.nextDouble() * 0.5, pos.z + Math.sin(a) * r,
                            0, 0.25 + rnd.nextDouble() * 0.35, 0);
                }
                if (mine) PowerHud.flash(0.6f);
            }
            case FxPayload.HIT_SPARKS -> {
                Vec3 n = dir.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : dir.normalize();
                int cnt = (int) (6 + 14 * s);
                for (int i = 0; i < cnt; i++) {
                    Vec3 v = n.add(rnd.nextGaussian() * 0.6, rnd.nextGaussian() * 0.6, rnd.nextGaussian() * 0.6).normalize().scale(0.2 + rnd.nextDouble() * 0.5);
                    lvl.addParticle(ModParticles.SPARK.get(), pos.x, pos.y, pos.z, v.x, v.y, v.z);
                }
                Entity src = lvl.getEntity(p.source());
                boolean blood = !(src instanceof Player sp) || sp.getData(ModAttachments.POWER.get()).blood;
                if (blood) {
                    DustParticleOptions red = new DustParticleOptions(new Vector3f(0.62f, 0.02f, 0.03f), 1.1f);
                    for (int i = 0; i < cnt / 2; i++) {
                        Vec3 v = n.add(rnd.nextGaussian() * 0.5, rnd.nextGaussian() * 0.5, rnd.nextGaussian() * 0.5).scale(0.18);
                        lvl.addParticle(red, pos.x, pos.y, pos.z, v.x, v.y, v.z);
                    }
                }
                CameraShake.addAt(pos, 0.12f + 0.2f * s);
            }
            case FxPayload.TAKEOFF -> {
                lvl.addParticle(ModParticles.SHOCK_RING.get(), pos.x, pos.y + 0.1, pos.z, 0, 3.2, 0);
                for (int i = 0; i < 24; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2;
                    lvl.addParticle(ParticleTypes.CLOUD, pos.x, pos.y + 0.1, pos.z, Math.cos(a) * 0.25, 0.05, Math.sin(a) * 0.25);
                }
                if (mine) CameraShake.add(0.25f);
            }
            default -> { }
        }
    }
}
