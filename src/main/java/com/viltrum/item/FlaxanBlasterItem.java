package com.viltrum.item;

import com.viltrum.server.BeamUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FlaxanBlasterItem extends Item {
    public FlaxanBlasterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            var look = player.getLookAngle();
            var from = player.getEyePosition().add(look.scale(0.6)).add(0, -0.12, 0);
            BeamUtil.fire(sl, player, from, look, 48.0, 9.0f);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        player.getCooldowns().addCooldown(this, 12);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
