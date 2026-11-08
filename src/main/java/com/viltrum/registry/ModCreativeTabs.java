package com.viltrum.registry;

import com.viltrum.Viltrum;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Viltrum.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.viltrum"))
            .icon(() -> new ItemStack(ModItems.SERUM.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.SERUM.get());
                out.accept(ModItems.INVINCIBLE_MASK.get());
                out.accept(ModItems.INVINCIBLE_CHEST.get());
                out.accept(ModItems.INVINCIBLE_LEGS.get());
                out.accept(ModItems.INVINCIBLE_BOOTS.get());
                out.accept(ModItems.OMNIMAN_HAIR.get());
                out.accept(ModItems.OMNIMAN_CHEST.get());
                out.accept(ModItems.OMNIMAN_LEGS.get());
                out.accept(ModItems.OMNIMAN_BOOTS.get());
                out.accept(ModItems.FLAXAN_BLASTER.get());
                out.accept(ModItems.VILTRUMITE_SPAWN_EGG.get());
                out.accept(ModItems.REANIMEN_SPAWN_EGG.get());
                out.accept(ModItems.FLAXAN_SPAWN_EGG.get());
            }).build());
}
