package com.viltrum.registry;

import com.viltrum.Viltrum;
import com.viltrum.entity.FlaxanEntity;
import com.viltrum.entity.ReanimenEntity;
import com.viltrum.entity.ViltrumiteEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Viltrum.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ViltrumiteEntity>> VILTRUMITE = ENTITIES.register("viltrumite",
            () -> EntityType.Builder.of(ViltrumiteEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(12)
                    .fireImmune().build("viltrumite"));
    public static final DeferredHolder<EntityType<?>, EntityType<ReanimenEntity>> REANIMEN = ENTITIES.register("reanimen",
            () -> EntityType.Builder.of(ReanimenEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build("reanimen"));
    public static final DeferredHolder<EntityType<?>, EntityType<FlaxanEntity>> FLAXAN = ENTITIES.register("flaxan",
            () -> EntityType.Builder.of(FlaxanEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build("flaxan"));
}
