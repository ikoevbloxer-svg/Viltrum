package com.viltrum;

import com.mojang.logging.LogUtils;
import com.viltrum.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Viltrum.MODID)
public class Viltrum {
    public static final String MODID = "viltrum";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public Viltrum(IEventBus modBus, ModContainer container) {
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModArmorMaterials.MATERIALS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
