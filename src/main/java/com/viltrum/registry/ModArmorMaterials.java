package com.viltrum.registry;

import com.viltrum.Viltrum;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Viltrum.MODID);

    public static final Holder<ArmorMaterial> INVINCIBLE = reg("invincible_suit", 3, 6, 8, 3, 3f, 0.1f);
    public static final Holder<ArmorMaterial> OMNIMAN = reg("omniman_suit", 3, 6, 8, 3, 3f, 0.1f);

    private static Holder<ArmorMaterial> reg(String name, int boots, int legs, int chest, int helmet, float toughness, float knockbackRes) {
        return MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                    m.put(ArmorItem.Type.BOOTS, boots);
                    m.put(ArmorItem.Type.LEGGINGS, legs);
                    m.put(ArmorItem.Type.CHESTPLATE, chest);
                    m.put(ArmorItem.Type.HELMET, helmet);
                    m.put(ArmorItem.Type.BODY, chest);
                }),
                25,
                ModSounds.SUIT_EQUIP,
                () -> Ingredient.of(Items.WHITE_WOOL),
                List.of(new ArmorMaterial.Layer(Viltrum.id(name))),
                toughness, knockbackRes));
    }
}
