package com.viltrum.server;

import com.viltrum.Viltrum;
import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Bare-handed mining speed: Viltrumites punch through stone. Scales with output and level. */
@EventBusSubscriber(modid = Viltrum.MODID)
public class MiningEvents {
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        PowerData d = e.getEntity().getData(com.viltrum.registry.ModAttachments.POWER.get());
        if (!d.viltrumite || !e.getEntity().getMainHandItem().isEmpty()) return;
        float mult = 1f + (float) (d.level * 2.5 * PowerMath.eff(d) * PowerMath.eff(d));
        e.setNewSpeed(e.getNewSpeed() * mult);
    }

    @SubscribeEvent
    public static void onHarvest(PlayerEvent.HarvestCheck e) {
        PowerData d = e.getEntity().getData(com.viltrum.registry.ModAttachments.POWER.get());
        if (d.viltrumite && PowerMath.eff(d) >= 0.3f && e.getEntity().getMainHandItem().isEmpty()) e.setCanHarvest(true);
    }
}
