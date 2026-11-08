package com.viltrum.server;

import com.viltrum.data.PowerData;
import com.viltrum.network.FxPayload;
import com.viltrum.network.SyncPowerPayload;
import com.viltrum.registry.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PowerSync {
    private PowerSync() {}

    public static PowerData get(Player p) {
        return p.getData(ModAttachments.POWER.get());
    }

    /** pushes the full state of this player to himself and everyone who is tracking him. */
    public static void sync(ServerPlayer p) {
        PowerData d = get(p);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new SyncPowerPayload(p.getId(), d));
        d.dirty = false;
        d.lastSyncTick = p.level().getGameTime();
    }

    public static void fx(ServerLevel level, int type, Vec3 pos, Vec3 dir, float strength, int source) {
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, 112.0,
                new FxPayload(type, pos.x, pos.y, pos.z, (float) dir.x, (float) dir.y, (float) dir.z, strength, source));
    }
}
