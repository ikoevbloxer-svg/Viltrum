package com.viltrum.registry;

import com.viltrum.Viltrum;
import com.viltrum.item.FlaxanBlasterItem;
import com.viltrum.item.ViltrumSerumItem;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Viltrum.MODID);

    public static final DeferredItem<Item> SERUM = ITEMS.register("viltrumite_serum",
            () -> new ViltrumSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> FLAXAN_BLASTER = ITEMS.register("flaxan_blaster",
            () -> new FlaxanBlasterItem(new Item.Properties().stacksTo(1).durability(600).rarity(Rarity.RARE)));

    public static final DeferredItem<ArmorItem> INVINCIBLE_MASK = armor("invincible_mask", ModArmorMaterials.INVINCIBLE, ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> INVINCIBLE_CHEST = armor("invincible_chest", ModArmorMaterials.INVINCIBLE, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> INVINCIBLE_LEGS = armor("invincible_legs", ModArmorMaterials.INVINCIBLE, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> INVINCIBLE_BOOTS = armor("invincible_boots", ModArmorMaterials.INVINCIBLE, ArmorItem.Type.BOOTS);

    public static final DeferredItem<ArmorItem> OMNIMAN_HAIR = armor("omniman_hair", ModArmorMaterials.OMNIMAN, ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> OMNIMAN_CHEST = armor("omniman_chest", ModArmorMaterials.OMNIMAN, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> OMNIMAN_LEGS = armor("omniman_legs", ModArmorMaterials.OMNIMAN, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> OMNIMAN_BOOTS = armor("omniman_boots", ModArmorMaterials.OMNIMAN, ArmorItem.Type.BOOTS);

    public static final DeferredItem<DeferredSpawnEggItem> VILTRUMITE_SPAWN_EGG = ITEMS.register("viltrumite_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VILTRUMITE, 0xF0F0F4, 0xC41E28, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> REANIMEN_SPAWN_EGG = ITEMS.register("reanimen_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.REANIMEN, 0x3A3C46, 0x3CE6FF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FLAXAN_SPAWN_EGG = ITEMS.register("flaxan_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FLAXAN, 0x70A854, 0xDE701C, new Item.Properties()));

    private static DeferredItem<ArmorItem> armor(String name, Holder<ArmorMaterial> mat, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(mat, type,
                new Item.Properties().durability(type.getDurability(45)).rarity(Rarity.RARE)));
    }
}
