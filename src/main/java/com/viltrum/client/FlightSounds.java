package com.viltrum.client;

import com.viltrum.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/** Speed-reactive wind loops. Two layers: a cruising whoosh and a roaring layer that fades in near the sound barrier. */
public final class FlightSounds {
    private static final Map<Integer, Loop[]> LOOPS = new HashMap<>();

    private FlightSounds() {}

    public static void tick(Minecraft mc) {
        if (mc.level == null) { LOOPS.clear(); return; }
        LOOPS.values().removeIf(arr -> arr[0].isStopped() && arr[1].isStopped());
        for (Map.Entry<Integer, FlightVisuals.Track> en : FlightVisuals.TRACKS.entrySet()) {
            if (!en.getValue().flying) continue;
            Entity e = mc.level.getEntity(en.getKey());
            if (e == null) continue;
            Loop[] arr = LOOPS.get(en.getKey());
            if (arr == null || arr[0].isStopped()) {
                arr = new Loop[]{new Loop(e, false), new Loop(e, true)};
                LOOPS.put(en.getKey(), arr);
                mc.getSoundManager().play(arr[0]);
                mc.getSoundManager().play(arr[1]);
            }
        }
    }

    static class Loop extends AbstractTickableSoundInstance {
        private final Entity entity;
        private final boolean fast;
        private float vol = 0f;

        Loop(Entity e, boolean fast) {
            super(fast ? ModSounds.FLIGHT_FAST.get() : ModSounds.FLIGHT_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.entity = e;
            this.fast = fast;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.001f;
            boolean local = e == Minecraft.getInstance().player;
            this.relative = local;
            this.attenuation = local ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
            this.x = e.getX(); this.y = e.getY(); this.z = e.getZ();
        }

        @Override
        public void tick() {
            FlightVisuals.Track t = FlightVisuals.TRACKS.get(entity.getId());
            if (entity.isRemoved() || t == null || !t.flying) {
                this.stop();
                return;
            }
            this.x = entity.getX(); this.y = entity.getY(); this.z = entity.getZ();
            float sf = (float) Mth.clamp(t.speed / 3.2, 0.0, 1.0);
            float target;
            if (!fast) {
                target = t.speed < 0.04 ? 0.0f : 0.12f + 0.75f * Mth.clamp(sf * 1.6f, 0f, 1f);
                this.pitch = 0.75f + 0.5f * sf;
            } else {
                target = Mth.clamp((sf - 0.3f) / 0.5f, 0f, 1f) * 1.0f;
                this.pitch = 0.8f + 0.4f * sf;
            }
            vol = Mth.lerp(0.15f, vol, target);
            this.volume = Math.max(0.001f, vol);
        }
    }
}
