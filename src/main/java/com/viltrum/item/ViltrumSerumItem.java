package com.viltrum.item;

import com.viltrum.data.PowerData;
import com.viltrum.registry.ModSounds;
import com.viltrum.server.PowerSync;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click: awakens the Viltrumite blood. */
public class ViltrumSerumItem extends Item {
    public ViltrumSerumItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player instanceof ServerPlayer sp) {
            PowerData d = PowerSync.get(sp);
            if (d.viltrumite) {
                sp.displayClientMessage(Component.translatable("msg.viltrum.already"), true);
                return InteractionResultHolder.fail(stack);
            }
            d.viltrumite = true;
            d.level = 1;
            d.xp = 0;
            ServerLevel sl = sp.serverLevel();
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1, sp.getZ(), 60, 0.4, 0.8, 0.4, 0.15);
            sp.displayClientMessage(Component.translatable("msg.viltrum.become"), false);
            PowerSync.sync(sp);
            if (!sp.isCreative()) stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
