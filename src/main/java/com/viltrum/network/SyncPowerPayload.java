package com.viltrum.network;

import com.viltrum.Viltrum;
import com.viltrum.data.PowerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** server -> client: complete synced power state of one player. */
public record SyncPowerPayload(int entityId, PowerData data) implements CustomPacketPayload {
    public static final Type<SyncPowerPayload> TYPE = new Type<>(Viltrum.id("sync_power"));
    public static final StreamCodec<FriendlyByteBuf, SyncPowerPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.entityId); p.data.writeNet(buf); },
            buf -> { int id = buf.readVarInt(); PowerData d = new PowerData(); d.readNet(buf); return new SyncPowerPayload(id, d); });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
