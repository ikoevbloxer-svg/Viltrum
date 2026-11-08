package com.viltrum.server;

import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import com.viltrum.network.FxPayload;
import com.viltrum.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Neck grab (level 10+): hold a creature at arm's length, choke it, pummel it, use it as a shield,
 * ram it into mountains while flying, or hurl it away.
 */
public final class GrabHandler {
    private static final class State {
        int targetId;
        boolean wasNoAi;
        boolean wasNoGravity;
    }

    private static final class Thrown {
        final ServerLevel level;
        final int id;
        final UUID owner;
        final double speed;
        int age;

        Thrown(ServerLevel level, int id, UUID owner, double speed) {
            this.level = level; this.id = id; this.owner = owner; this.speed = speed;
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Set<Integer> GRABBED = new HashSet<>();
    private static final Map<Integer, Thrown> THROWN = new HashMap<>();

    private GrabHandler() {}

    public static boolean isGrabbed(Entity e) { return GRABBED.contains(e.getId()); }

    public static LivingEntity target(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null) return null;
        Entity e = p.serverLevel().getEntity(s.targetId);
        return e instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    private static boolean canGrab(ServerPlayer p, LivingEntity t) {
        if (t == p || !t.isAlive() || t.isSpectator() || isGrabbed(t)) return false;
        if (t instanceof EnderDragon || t instanceof WitherBoss) return false;
        if (t.getBbHeight() > 4.5f) return false;
        if (t instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) return false;
        return true;
    }

    // ------------------------------------------------------------------ start / stop
    public static void toggle(ServerPlayer p, PowerData d) {
        if (d.grabbedId >= 0) { release(p, d, false, 0f); return; }
        if (d.level < PowerMath.UNLOCK_GRAB) {
            p.displayClientMessage(Component.translatable("msg.viltrum.locked", PowerMath.UNLOCK_GRAB), true);
            return;
        }
        LivingEntity t = Combat.findTarget(p, 4.6);
        if (t == null || !canGrab(p, t)) {
            p.displayClientMessage(Component.translatable("msg.viltrum.nothing_to_grab"), true);
            return;
        }
        State s = new State();
        s.targetId = t.getId();
        if (t instanceof Mob m) { s.wasNoAi = m.isNoAi(); m.setNoAi(true); }
        s.wasNoGravity = t.isNoGravity();
        t.setNoGravity(true);
        STATES.put(p.getUUID(), s);
        GRABBED.add(t.getId());
        d.grabbedId = t.getId();
        d.barraging = false;
        ServerLevel level = p.serverLevel();
        Combat.play(level, t.position(), ModSounds.GRAB.get(), 1.2f, 1.0f);
        PowerSync.fx(level, FxPayload.GRAB, t.position().add(0, t.getBbHeight() * 0.8, 0), p.getLookAngle(), 0.6f, p.getId());
        PowerSync.sync(p);
    }

    public static void release(ServerPlayer p, PowerData d, boolean hurl, float charge) {
        State s = STATES.remove(p.getUUID());
        d.grabbedId = -1;
        if (s != null) {
            GRABBED.remove(s.targetId);
            ServerLevel level = p.serverLevel();
            Entity e = level.getEntity(s.targetId);
            if (e instanceof LivingEntity t) {
                if (t instanceof Mob m) m.setNoAi(s.wasNoAi);
                t.setNoGravity(s.wasNoGravity);
                Vec3 look = p.getLookAngle();
                if (hurl) {
                    double speed = PowerMath.knockback(d) * (1.2 + 1.0 * charge);
                    t.setDeltaMovement(look.scale(speed).add(0, 0.15, 0));
                    t.hurtMarked = true;
                    THROWN.put(t.getId(), new Thrown(level, t.getId(), p.getUUID(), speed));
                    Combat.play(level, t.position(), ModSounds.PUNCH_SUPER.get(), 1.2f, 1.1f);
                    PowerSync.fx(level, FxPayload.SHOCKWAVE, t.position().add(0, 1.0, 0), look.scale(-1), 0.5f + 0.4f * charge, p.getId());
                } else {
                    t.setDeltaMovement(look.scale(0.15));
                    t.hurtMarked = true;
                }
            }
        }
        PowerSync.sync(p);
    }

    /** cleanup when the holder disappears (death, logout...). */
    public static void forget(ServerPlayer p) {
        PowerData d = PowerSync.get(p);
        if (d.grabbedId >= 0 || STATES.containsKey(p.getUUID())) release(p, d, false, 0f);
    }

    // ------------------------------------------------------------------ per tick
    public static void tick(ServerPlayer p, PowerData d) {
        State s = STATES.get(p.getUUID());
        LivingEntity t = target(p);
        if (s == null || t == null || p.isSpectator() || !d.viltrumite || d.level < PowerMath.UNLOCK_GRAB || p.distanceToSqr(t) > 144.0) {
            release(p, d, false, 0f);
            return;
        }
        Vec3 look = p.getLookAngle();
        Vec3 desired = p.getEyePosition().add(look.scale(1.15)).subtract(0, t.getBbHeight() * 0.82, 0);
        Vec3 delta = desired.subtract(t.position());
        double len = delta.length();
        if (len > 3.5) delta = delta.scale(3.5 / len);
        t.setDeltaMovement(delta.scale(0.85));
        t.hurtMarked = true;
        t.fallDistance = 0;
        float yaw = p.getYRot() + 180f;
        t.setYRot(yaw);
        t.setYHeadRot(yaw);
        t.yBodyRot = yaw;
    }

    /** execute the held creature. Only ever happens when the player explicitly asks for it. */
    public static void kill(ServerPlayer p, PowerData d) {
        LivingEntity t = target(p);
        if (t == null) return;
        ServerLevel level = p.serverLevel();
        Vec3 c = t.getBoundingBox().getCenter();
        Combat.noRevive = true;
        try { Combat.hurtOnly(p, t, 1.0e6f); } finally { Combat.noRevive = false; }
        Combat.play(level, c, ModSounds.BONE_CRACK.get(), 1.6f, 0.7f);
        PowerSync.fx(level, FxPayload.BARRAGE_HIT, c, p.getLookAngle().scale(-1), 1.0f, p.getId());
        release(p, d, false, 0f);
    }

    // ------------------------------------------------------------------ thrown victims
    public static void tickThrown(MinecraftServer server) {
        if (THROWN.isEmpty()) return;
        Iterator<Thrown> it = THROWN.values().iterator();
        while (it.hasNext()) {
            Thrown th = it.next();
            Entity e = th.level.getEntity(th.id);
            th.age++;
            if (!(e instanceof LivingEntity le) || !le.isAlive() || th.age > 120) { it.remove(); continue; }
            if (th.age > 3 && (le.horizontalCollision || le.verticalCollision || le.onGround())) {
                it.remove();
                ServerPlayer owner = server.getPlayerList().getPlayer(th.owner);
                if (owner == null) continue;
                PowerData od = PowerSync.get(owner);
                double energy = th.speed * (0.35 + 0.65 * PowerMath.eff(od));
                float dmg = (float) (PowerMath.fullPunch(od.level) * 0.5 * Math.min(2.0, th.speed / 3.0) * PowerMath.eff(od) * PowerMath.eff(od) + 2.0);
                Combat.hurtOnly(owner, le, dmg);
                Combat.play(th.level, le.position(), ModSounds.BONE_CRACK.get(), 1.2f, 0.9f);
                Combat.play(th.level, le.position(), ModSounds.LANDING.get(), 1.2f, 0.9f);
                PowerSync.fx(th.level, FxPayload.SHOCKWAVE, le.position().add(0, 0.5, 0), new Vec3(0, 1, 0), (float) Math.min(1.0, energy / 4.0), owner.getId());
                Combat.crater(owner, od, le.position().add(0, 0.4, 0), energy * 0.8);
            }
        }
    }
}
