package com.viltrum.client.render;

import com.viltrum.Viltrum;
import com.viltrum.entity.FlaxanEntity;
import com.viltrum.entity.ReanimenEntity;
import com.viltrum.entity.ViltrumiteEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The three mobs use the vanilla player model (so they get the full flight / punch animation set) with their own skins. */
public final class MobRenderers {
    private MobRenderers() {}

    public static class Viltrumite extends HumanoidMobRenderer<ViltrumiteEntity, PlayerModel<ViltrumiteEntity>> {
        private static final ResourceLocation TEX = Viltrum.id("textures/entity/viltrumite.png");

        public Viltrumite(EntityRendererProvider.Context ctx) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(ViltrumiteEntity e) { return TEX; }
    }

    public static class Reanimen extends HumanoidMobRenderer<ReanimenEntity, PlayerModel<ReanimenEntity>> {
        private static final ResourceLocation TEX = Viltrum.id("textures/entity/reanimen.png");

        public Reanimen(EntityRendererProvider.Context ctx) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(ReanimenEntity e) { return TEX; }
    }

    public static class Flaxan extends HumanoidMobRenderer<FlaxanEntity, PlayerModel<FlaxanEntity>> {
        private static final ResourceLocation TEX = Viltrum.id("textures/entity/flaxan.png");

        public Flaxan(EntityRendererProvider.Context ctx) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(FlaxanEntity e) { return TEX; }
    }
}
