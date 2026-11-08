package com.viltrum.network;

import com.viltrum.Viltrum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * client -> server: combat actions.
 * kind 0 = charge started, 1 = charge cancelled, 2 = strike released (value = charge 0..1),
 * 3 = barrage start, 4 = barrage stop, 5 = impact (value = speed, direction in dx/dy/dz).
 */
public record ActionPayload(int kind, float value, float dx, float dy, float dz) implements CustomPacketPayload {
    public static final Type<ActionPayload> TYPE = new Type<>(Viltrum.id("action"));
    public static final StreamCodec<FriendlyByteBuf, ActionPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeByte(p.kind); b.writeFloat(p.value); b.writeFloat(p.dx); b.writeFloat(p.dy); b.writeFloat(p.dz); },
            b -> new ActionPayload(b.readByte(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat()));

    public static ActionPayload simple(int kind) { return new ActionPayload(kind, 0, 0, 0, 0); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
