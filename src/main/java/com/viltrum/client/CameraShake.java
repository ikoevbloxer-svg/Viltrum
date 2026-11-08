package com.viltrum.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Trauma-based camera shake (decays quickly) plus a constant speed-driven rumble floor. */
public final class CameraShake {
    private static float trauma = 0f;
    private static float floor = 0f;
    private static float time = 0f;

    private CameraShake() {}

    public static void add(float amount) {
        trauma = Math.min(1.0f, trauma + amount);
    }

    /** distance-attenuated shake from a world position (explosions, impacts). */
    public static void addAt(Vec3 pos, float amount) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double d = mc.player.position().distanceTo(pos);
        add((float) (amount * Mth.clamp(1.0 - d / 48.0, 0.0, 1.0)));
    }

    public static void setFloor(float f) { floor = f; }

    public static void tick() {
        trauma = Math.max(0f, trauma - 0.035f);
        time += 1f;
    }

    public static void apply(ViewportEvent.ComputeCameraAngles e) {
        float amt = Math.max(trauma, floor);
        if (amt <= 0.001f) return;
        float s = amt * amt;
        float t = time + (float) e.getPartialTick();
        float nx = Mth.sin(t * 1.9f) + 0.6f * Mth.sin(t * 3.7f + 1.3f);
        float ny = Mth.sin(t * 2.3f + 0.7f) + 0.6f * Mth.sin(t * 4.1f + 2.1f);
        float nz = Mth.sin(t * 1.4f + 2.4f) + 0.5f * Mth.sin(t * 3.1f);
        e.setYaw(e.getYaw() + nx * 2.4f * s);
        e.setPitch(e.getPitch() + ny * 2.4f * s);
        e.setRoll(e.getRoll() + nz * 3.2f * s);
    }
}
