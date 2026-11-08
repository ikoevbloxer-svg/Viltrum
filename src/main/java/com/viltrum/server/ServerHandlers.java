package com.viltrum.server;

import com.viltrum.Config;
import com.viltrum.data.PowerData;
import com.viltrum.network.*;
import com.viltrum.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class ServerHandlers {
    private ServerHandlers() {}

    public static void onSettings(SettingsPayload s, Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        PowerData d = PowerSync.get(sp);
        if (!d.viltrumite) return;
        d.strength = Mth.clamp(s.strength(), 0f, 1f);
        d.flightLimit = Mth.clamp(s.flightLimit(), 0f, 1f);
        d.allowDestruction = s.destruction();
        d.sonicFx = s.sonicFx();
        d.blood = s.blood();
        PowerSync.sync(sp);
    }

    public static void onFlight(FlightPayload f, Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        PowerData d = PowerSync.get(sp);
        if (!d.viltrumite || sp.isSpectator()) return;
        boolean wasFlying = d.flying;
        boolean wasBoost = d.boosting;
        d.flying = f.flying();
        d.boosting = f.flying() && f.boosting();
        if (d.flying && !wasFlying) {
            Combat.play(sp.serverLevel(), sp.position(), ModSounds.FLIGHT_START.get(), 1.4f, 1.0f);
            PowerSync.fx(sp.serverLevel(), FxPayload.TAKEOFF, sp.position(), Vec3.ZERO, 1f, sp.getId());
            sp.fallDistance = 0;
        }
        if (d.boosting && !wasBoost) {
            Combat.play(sp.serverLevel(), sp.position(), ModSounds.FLIGHT_BOOST.get(), 1.2f, 1.0f);
        }
        if (!d.flying) d.machActive = false;
        if (d.flying != wasFlying || d.boosting != wasBoost) PowerSync.sync(sp);
    }

    public static void onAction(ActionPayload a, Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        PowerData d = PowerSync.get(sp);
        if (!d.viltrumite) return;
        switch (a.kind()) {
            case 0 -> { // charge started
                if (!d.charging) {
                    d.charging = true;
                    Combat.play(sp.serverLevel(), sp.position(), ModSounds.CHARGE.get(), 0.9f, 1.0f);
                    PowerSync.sync(sp);
                }
            }
            case 1 -> { if (d.charging) { d.charging = false; PowerSync.sync(sp); } }
            case 2 -> Combat.strike(sp, d, a.value());
            case 3 -> Combat.startBarrage(sp, d);
            case 4 -> Combat.stopBarrage(sp, d, true);
            case 5 -> {
                Vec3 dir = new Vec3(a.dx(), a.dy(), a.dz());
                if (dir.lengthSqr() < 1.0e-4) return;
                Combat.impact(sp, d, Math.min(a.value(), Config.MAX_FLIGHT_SPEED.get() * 1.3), dir.normalize());
            }
            case 6 -> GrabHandler.toggle(sp, d);
            case 7 -> toggleSenses(sp, d);
            case 8 -> Combat.clap(sp, d);
            default -> { }
        }
    }

    private static void toggleSenses(ServerPlayer sp, PowerData d) {
        if (d.level < com.viltrum.data.PowerMath.UNLOCK_SENSES) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.viltrum.locked", com.viltrum.data.PowerMath.UNLOCK_SENSES), true);
            return;
        }
        d.senses = !d.senses;
        if (!d.senses) sp.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        PowerSync.sync(sp);
    }
}
