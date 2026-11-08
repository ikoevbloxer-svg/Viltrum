package com.viltrum.network;

import com.viltrum.Viltrum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** client -> server: flight toggled / boost state changed. */
public record FlightPayload(boolean flying, boolean boosting) implements CustomPacketPayload {
    public static final Type<FlightPayload> TYPE = new Type<>(Viltrum.id("flight"));
    public static final StreamCodec<FriendlyByteBuf, FlightPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeBoolean(p.flying); b.writeBoolean(p.boosting); },
            b -> new FlightPayload(b.readBoolean(), b.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
