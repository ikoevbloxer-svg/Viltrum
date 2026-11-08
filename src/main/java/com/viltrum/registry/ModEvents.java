package com.viltrum.registry;

import com.viltrum.Viltrum;
import com.viltrum.entity.FlaxanEntity;
import com.viltrum.entity.ReanimenEntity;
import com.viltrum.entity.ViltrumiteEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = Viltrum.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.VILTRUMITE.get(), ViltrumiteEntity.createAttributes().build());
        e.put(ModEntities.REANIMEN.get(), ReanimenEntity.createAttributes().build());
        e.put(ModEntities.FLAXAN.get(), FlaxanEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawns(RegisterSpawnPlacementsEvent e) {
        e.register(ModEntities.VILTRUMITE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        e.register(ModEntities.REANIMEN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        e.register(ModEntities.FLAXAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
    }
}
