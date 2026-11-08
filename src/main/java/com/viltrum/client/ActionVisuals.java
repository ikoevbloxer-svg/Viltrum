package com.viltrum.client;

import com.viltrum.data.PowerData;
import com.viltrum.entity.ViltrumiteEntity;
import com.viltrum.registry.ModAttachments;
import com.viltrum.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Client state about who holds whom, plus the effects of barrage (motion-blurred fists) and charge (energy aura). */
public final class ActionVisuals {
    /** victim entity id -> holder entity id */
    public static final Map<Integer, Integer> HELD = new HashMap<>();
    public static final Set<Integer> HOLDERS = new HashSet<>();

    private ActionVisuals() {}

    public static void tick(Minecraft mc) {
        HELD.clear();
        HOLDERS.clear();
        ClientLevel lvl = mc.level;
        if (lvl == null) return;
        for (AbstractClientPlayer p : lvl.players()) {
            PowerData d = p.getData(ModAttachments.POWER.get());
            if (!d.viltrumite) continue;
            if (d.grabbedId >= 0) { HELD.put(d.grabbedId, p.getId()); HOLDERS.add(p.getId()); }
            if (d.barraging) barrage(lvl, p);
            if (d.charging) aura(lvl, p, p == mc.player ? FlightController.chargeFrac : 0.6f);
        }
        for (Entity e : lvl.entitiesForRendering()) {
            if (e instanceof ViltrumiteEntity v && v.getGrabTarget() >= 0) { HELD.put(v.getGrabTarget(), v.getId()); HOLDERS.add(v.getId()); }
        }
    }

    private static void barrage(ClientLevel lvl, AbstractClientPlayer p) {
        var rnd = lvl.random;
        Vec3 look = p.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        right = right.lengthSqr() > 1e-6 ? right.normalize() : new Vec3(1, 0, 0);
        Vec3 base = p.getEyePosition().subtract(0, 0.42, 0).add(look.scale(1.0));
        for (int i = 0; i < 3; i++) {
            double side = ((p.tickCount + i) % 2 == 0 ? 1 : -1) * (0.22 + rnd.nextDouble() * 0.16);
            Vec3 pos = base.add(right.scale(side)).add(look.scale(rnd.nextDouble() * 0.9))
                    .add(rnd.nextGaussian() * 0.05, rnd.nextGaussian() * 0.12, rnd.nextGaussian() * 0.05);
            // motion-blur streaks along the punch line + hot sparks
            lvl.addParticle(ModParticles.STREAK.get(), pos.x, pos.y, pos.z, look.x * 1.6, look.y * 1.6, look.z * 1.6);
            lvl.addParticle(ModParticles.SPARK.get(), pos.x, pos.y, pos.z, look.x * 0.22 + rnd.nextGaussian() * 0.04, look.y * 0.22 + rnd.nextGaussian() * 0.04, look.z * 0.22 + rnd.nextGaussian() * 0.04);
        }
        if (p.tickCount % 3 == 0) {
            Vec3 at = base.add(look.scale(0.5));
            lvl.addParticle(ModParticles.SHOCK_RING.get(), at.x, at.y, at.z, look.x * 1.1, look.y * 1.1, look.z * 1.1);
        }
    }

    private static void aura(ClientLevel lvl, AbstractClientPlayer p, float frac) {
        var rnd = lvl.random;
        int n = 1 + (int) (frac * 4);
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double r = 0.9 + rnd.nextDouble() * 0.6;
            double y = p.getY() + rnd.nextDouble() * 1.8;
            // spiral inward and up
            lvl.addParticle(ModParticles.SPARK.get(), p.getX() + Math.cos(a) * r, y, p.getZ() + Math.sin(a) * r,
                    -Math.cos(a) * 0.09 - Math.sin(a) * 0.05, 0.05 + frac * 0.06, -Math.sin(a) * 0.09 + Math.cos(a) * 0.05);
        }
        if (frac > 0.5f && p.tickCount % 4 == 0) {
            lvl.addParticle(ModParticles.VAPOR.get(), p.getX() + rnd.nextGaussian() * 0.3, p.getY() + 0.1, p.getZ() + rnd.nextGaussian() * 0.3, 0, 0.04, 0);
        }
    }
}
