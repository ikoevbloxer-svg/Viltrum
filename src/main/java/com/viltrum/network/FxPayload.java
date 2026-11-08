package com.viltrum.network;

import com.viltrum.Viltrum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** server -> clients nearby: visual effect request. */
public record FxPayload(int type, double x, double y, double z, float dx, float dy, float dz, float strength, int source) implements CustomPacketPayload {
    public static final int SHOCKWAVE = 0, SONIC_BOOM = 1, CRATER = 2, STOP_BARRAGE = 3, LEVEL_UP = 4, HIT_SPARKS = 5, TAKEOFF = 6;

    public static final Type<FxPayload> TYPE = new Type<>(Viltrum.id("fx"));
    public static final StreamCodec<FriendlyByteBuf, FxPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeByte(p.type); b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
                b.writeFloat(p.dx); b.writeFloat(p.dy); b.writeFloat(p.dz); b.writeFloat(p.strength); b.writeVarInt(p.source); },
            b -> new FxPayload(b.readByte(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
