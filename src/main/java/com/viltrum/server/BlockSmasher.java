package com.viltrum.server;

import com.viltrum.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Breaks blocks on behalf of a Viltrumite while respecting protection mods (BreakEvent) and the hardness limit. */
public final class BlockSmasher {
    private BlockSmasher() {}

    public static boolean smash(ServerPlayer p, ServerLevel level, BlockPos pos, double maxHardness, boolean sparkle) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir()) return false;
        if (st.getBlock() instanceof LiquidBlock) return false;
        float h = st.getDestroySpeed(level, pos);
        if (h < 0 || h > maxHardness) return false;
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, st, p)).isCanceled()) return false;

        BlockEntity be = level.getBlockEntity(pos);
        if (Config.TUNNEL_DROPS.get()) {
            net.minecraft.world.level.block.Block.dropResources(st, level, pos, be, p, ItemStack.EMPTY);
        }
        level.setBlock(pos, st.getFluidState().createLegacyBlock(), 3);
        if (sparkle) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    4, 0.35, 0.35, 0.35, 0.08);
        }
        return true;
    }
}
