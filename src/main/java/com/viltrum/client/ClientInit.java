package com.viltrum.client;

import com.viltrum.Viltrum;
import com.viltrum.client.hud.PowerHud;
import com.viltrum.client.particle.ShockRingParticle;
import com.viltrum.client.particle.SparkParticle;
import com.viltrum.client.particle.StreakParticle;
import com.viltrum.client.render.MobRenderers;
import com.viltrum.registry.ModEntities;
import com.viltrum.registry.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Viltrum.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInit {
    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(ModKeys.MENU);
        e.register(ModKeys.FLIGHT);
        e.register(ModKeys.STRIKE);
        e.register(ModKeys.BARRAGE);
        e.register(ModKeys.GRAB);
        e.register(ModKeys.SENSES);
        e.register(ModKeys.CLAP);
        e.register(ModKeys.RAGE);
        e.register(ModKeys.STOMP);
        e.register(ModKeys.KILL);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.SPARK.get(), s -> new SparkParticle.Provider(s, false));
        e.registerSpriteSet(ModParticles.VAPOR.get(), s -> new SparkParticle.Provider(s, true));
        e.registerSpriteSet(ModParticles.STREAK.get(), StreakParticle.Provider::new);
        e.registerSpriteSet(ModParticles.SHOCK_RING.get(), s -> new ShockRingParticle.Provider(s, false));
        e.registerSpriteSet(ModParticles.SHOCK_RING_WARM.get(), s -> new ShockRingParticle.Provider(s, true));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.VILTRUMITE.get(), MobRenderers.Viltrumite::new);
        e.registerEntityRenderer(ModEntities.REANIMEN.get(), MobRenderers.Reanimen::new);
        e.registerEntityRenderer(ModEntities.FLAXAN.get(), MobRenderers.Flaxan::new);
    }

    @SubscribeEvent
    public static void guiLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(Viltrum.id("power_hud"), new PowerHud());
    }
}
