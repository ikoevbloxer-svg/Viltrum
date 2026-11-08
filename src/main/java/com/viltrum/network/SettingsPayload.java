package com.viltrum.network;

import com.viltrum.Viltrum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** client -> server: values of the power menu. */
public record SettingsPayload(float strength, float flightLimit, boolean destruction, boolean sonicFx, boolean blood) implements CustomPacketPayload {
    public static final Type<SettingsPayload> TYPE = new Type<>(Viltrum.id("settings"));
    public static final StreamCodec<FriendlyByteBuf, SettingsPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeFloat(p.strength); b.writeFloat(p.flightLimit); b.writeBoolean(p.destruction); b.writeBoolean(p.sonicFx); b.writeBoolean(p.blood); },
            b -> new SettingsPayload(b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean(), b.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
