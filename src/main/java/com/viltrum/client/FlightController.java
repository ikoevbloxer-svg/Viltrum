package com.viltrum.client;

import com.viltrum.Config;
import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import com.viltrum.network.ActionPayload;
import com.viltrum.network.FlightPayload;
import com.viltrum.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client side Viltrumite flight. The local player owns the movement: every tick the velocity is steered towards
 * the look direction with inertia, boost ramps up smoothly, and collisions are reported to the server as impacts.
 */
public final class FlightController {
    public static Vec3 vel = Vec3.ZERO;
    public static double speed = 0;
    public static float boostAmt = 0f;
    public static float chargeFrac = 0f;
    public static boolean charging = false;
    public static float strafeSmooth = 0f;
    public static long lastToggle = -1000;

    private static boolean boostSent = false;
    private static Vec3 startPos = Vec3.ZERO;
    private static int impactCooldown = 0;
    private static int landTicks = 0;
    private static int jumpTap = 0;
    private static boolean wasJump = false;
    private static boolean barrageHeld = false;

    private FlightController() {}

    public static PowerData data(LocalPlayer p) { return p.getData(ModAttachments.POWER.get()); }

    // ------------------------------------------------------------------ toggling
    public static void toggle(LocalPlayer p) {
        PowerData d = data(p);
        if (!d.viltrumite) return;
        boolean on = !d.flying;
        d.flying = on;
        d.boosting = false;
        boostSent = false;
        boostAmt = 0f;
        lastToggle = p.level().getGameTime();
        if (on) {
            vel = p.getDeltaMovement();
            if (p.onGround()) vel = vel.add(0, 0.6, 0);
            landTicks = 0;
        } else {
            vel = Vec3.ZERO;
        }
        PacketDistributor.sendToServer(new FlightPayload(on, false));
    }

    // ------------------------------------------------------------------ per client tick (keys etc.)
    public static void clientTick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        PowerData d = data(p);
        Options o = mc.options;

        while (ModKeys.MENU.consumeClick()) {
            if (d.viltrumite) mc.setScreen(new com.viltrum.client.gui.PowerScreen());
            else p.displayClientMessage(Component.translatable("msg.viltrum.not_viltrumite"), true);
        }
        if (!d.viltrumite) {
            while (ModKeys.FLIGHT.consumeClick()) { }
            while (ModKeys.GRAB.consumeClick()) { }
            while (ModKeys.SENSES.consumeClick()) { }
            while (ModKeys.CLAP.consumeClick()) { }
            while (ModKeys.RAGE.consumeClick()) { }
            while (ModKeys.STOMP.consumeClick()) { }
            while (ModKeys.KILL.consumeClick()) { }
            charging = false; chargeFrac = 0f;
            return;
        }
        while (ModKeys.FLIGHT.consumeClick()) toggle(p);
        while (ModKeys.GRAB.consumeClick()) PacketDistributor.sendToServer(ActionPayload.simple(6));
        while (ModKeys.SENSES.consumeClick()) PacketDistributor.sendToServer(ActionPayload.simple(7));
        while (ModKeys.CLAP.consumeClick()) PacketDistributor.sendToServer(ActionPayload.simple(8));
        while (ModKeys.RAGE.consumeClick()) PacketDistributor.sendToServer(ActionPayload.simple(9));
        while (ModKeys.STOMP.consumeClick()) PacketDistributor.sendToServer(ActionPayload.simple(10));
        while (ModKeys.KILL.consumeClick()) PacketDistributor.sendToServer(ActionPayload.simple(11));

        // double tap jump starts flight (like creative)
        boolean jump = o.keyJump.isDown() && mc.screen == null;
        if (jump && !wasJump) {
            if (jumpTap > 0 && !d.flying) { toggle(p); jumpTap = 0; }
            else jumpTap = 7;
        }
        wasJump = jump;
        if (jumpTap > 0) jumpTap--;

        // charged strike
        if (mc.screen == null) {
            if (ModKeys.STRIKE.isDown() && !charging && !d.barraging) {
                charging = true; chargeFrac = 0f; d.charging = true;
                PacketDistributor.sendToServer(ActionPayload.simple(0));
            }
            if (charging) {
                if (ModKeys.STRIKE.isDown()) {
                    chargeFrac = Math.min(1f, chargeFrac + 1f / 36f);
                    CameraShake.add(0.012f + 0.03f * chargeFrac);
                } else {
                    release(p, d);
                }
            }
            boolean bar = ModKeys.BARRAGE.isDown();
            if (bar && !barrageHeld) {
                barrageHeld = true; d.barraging = true;
                PacketDistributor.sendToServer(ActionPayload.simple(3));
            } else if (!bar && barrageHeld) {
                barrageHeld = false; d.barraging = false;
                PacketDistributor.sendToServer(ActionPayload.simple(4));
            }
        } else if (charging) {
            charging = false; chargeFrac = 0f; d.charging = false;
            PacketDistributor.sendToServer(ActionPayload.simple(1));
        }
        if (!d.barraging && barrageHeld && !ModKeys.BARRAGE.isDown()) barrageHeld = false;

        if (d.barraging) CameraShake.add(0.018f);
        if (d.rageAmt > 0.05f) CameraShake.add(0.01f * d.rageAmt);

        // speed rumble + fov inputs
        float sf = (float) Mth.clamp((speed - 1.2) / 3.0, 0.0, 1.0);
        CameraShake.setFloor(d.flying ? (sf > 0.4f ? (sf - 0.4f) * 0.45f : 0f) : 0f);
    }

    private static void release(LocalPlayer p, PowerData d) {
        float c = chargeFrac;
        charging = false; chargeFrac = 0f; d.charging = false;
        PacketDistributor.sendToServer(new ActionPayload(2, c, 0, 0, 0));
        Vec3 look = p.getLookAngle();
        if (d.flying) {
            vel = vel.add(look.scale(0.8 + 1.8 * c));
        } else {
            p.setDeltaMovement(p.getDeltaMovement().add(look.x * (0.5 + 1.2 * c), 0.05 + 0.1 * c, look.z * (0.5 + 1.2 * c)));
            p.hasImpulse = true;
        }
        CameraShake.add(0.25f + 0.5f * c);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    // ------------------------------------------------------------------ physics, before vanilla ticks
    public static void pre(LocalPlayer p) {
        PowerData d = data(p);
        if (!d.viltrumite || !d.flying) { speed = 0; return; }
        if (p.isSpectator() || p.isPassenger() || p.isSleeping() || p.isDeadOrDying()) {
            d.flying = false;
            PacketDistributor.sendToServer(new FlightPayload(false, false));
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Options o = mc.options;
        boolean input = mc.screen == null;
        float f = input ? (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0) : 0;
        float s = input ? (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0) : 0;
        float v = input ? (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0) : 0;
        strafeSmooth = Mth.lerp(0.2f, strafeSmooth, s);

        boolean wantBoost = input && o.keySprint.isDown() && (f > 0 || vel.lengthSqr() > 1.0);
        boostAmt = Mth.clamp(boostAmt + (wantBoost ? 0.035f : -0.06f), 0f, 1f);
        if (wantBoost != boostSent) {
            boostSent = wantBoost;
            d.boosting = wantBoost;
            PacketDistributor.sendToServer(new FlightPayload(true, wantBoost));
        }

        double mult = PowerMath.suitFlightMult(p);
        double cap = Config.MAX_FLIGHT_SPEED.get();
        double cruise = Math.min(cap, PowerMath.cruiseSpeed(d) * mult);
        double top = Math.min(cap, PowerMath.boostSpeed(d) * mult);

        Vec3 look = p.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        right = right.lengthSqr() > 1.0e-6 ? right.normalize() : Vec3.ZERO;
        Vec3 wish = look.scale(f).add(right.scale(-s)).add(0, v * 0.9, 0);
        if (wish.lengthSqr() > 1.0) wish = wish.normalize();

        double targetSpeed = Mth.lerp(boostAmt, cruise, top);
        if (f < 0) targetSpeed *= 0.45;
        Vec3 target = wish.scale(targetSpeed);
        double accel = wish.lengthSqr() < 1.0e-4 ? 0.10 : 0.06 + 0.07 * boostAmt;
        // heavy inertia at high speed makes wide, sweeping turns
        accel *= 1.0 - 0.45 * Mth.clamp(vel.length() / Math.max(0.5, top), 0.0, 1.0);
        vel = vel.lerp(target, accel);

        if (wish.lengthSqr() < 1.0e-4 && vel.length() < 0.25) {
            vel = vel.add(0, Mth.sin(p.tickCount * 0.12f) * 0.004, 0);
        }
        double sp = vel.length();
        if (sp > cap) vel = vel.scale(cap / sp);

        startPos = p.position();
        p.setDeltaMovement(vel);
        p.fallDistance = 0f;

        // auto-land
        if (p.onGround() && f <= 0 && v <= 0) landTicks++; else landTicks = 0;
        if (landTicks > 8) {
            d.flying = false;
            vel = Vec3.ZERO;
            lastToggle = p.level().getGameTime();
            PacketDistributor.sendToServer(new FlightPayload(false, false));
        }
    }

    // ------------------------------------------------------------------ after vanilla moved us
    public static void post(LocalPlayer p) {
        PowerData d = data(p);
        if (!d.viltrumite || !d.flying) return;
        Vec3 moved = p.position().subtract(startPos);
        speed = moved.length();
        double vs = vel.length();
        if (impactCooldown > 0) impactCooldown--;
        if (vs > 0.9 && speed < vs * 0.55 && impactCooldown <= 0) {
            Vec3 dir = vel.normalize();
            PacketDistributor.sendToServer(new ActionPayload(5, (float) vs, (float) dir.x, (float) dir.y, (float) dir.z));
            vel = vel.scale(0.28);
            impactCooldown = 8;
            CameraShake.add((float) Mth.clamp(vs * 0.22, 0.2, 1.0));
        }
        // the body always faces where we look while flying
        p.yBodyRot = p.getYRot();
    }
}
