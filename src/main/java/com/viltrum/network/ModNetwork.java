package com.viltrum.network;

import com.viltrum.Viltrum;
import com.viltrum.client.ClientPayloadHandlers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Viltrum.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(SyncPowerPayload.TYPE, SyncPowerPayload.CODEC, (p, ctx) -> ClientPayloadHandlers.onSync(p));
        r.playToClient(FxPayload.TYPE, FxPayload.CODEC, (p, ctx) -> ClientPayloadHandlers.onFx(p));
        r.playToServer(SettingsPayload.TYPE, SettingsPayload.CODEC, (p, ctx) -> com.viltrum.server.ServerHandlers.onSettings(p, ctx.player()));
        r.playToServer(FlightPayload.TYPE, FlightPayload.CODEC, (p, ctx) -> com.viltrum.server.ServerHandlers.onFlight(p, ctx.player()));
        r.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, (p, ctx) -> com.viltrum.server.ServerHandlers.onAction(p, ctx.player()));
    }
}
