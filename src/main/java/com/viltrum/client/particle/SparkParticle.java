package com.viltrum.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Soft glowing billboard: used as spark (bright, shrinking) and as vapour puff (large, expanding, translucent). */
public class SparkParticle extends TextureSheetParticle {
    private final boolean vapor;
    private final float startSize;

    protected SparkParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, boolean vapor) {
        super(level, x, y, z);
        this.vapor = vapor;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = vapor ? 0.90f : 0.86f;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.lifetime = vapor ? 16 + this.random.nextInt(10) : 10 + this.random.nextInt(8);
        this.startSize = vapor ? 0.45f + this.random.nextFloat() * 0.4f : 0.16f + this.random.nextFloat() * 0.18f;
        this.quadSize = startSize;
        if (vapor) {
            this.rCol = 0.94f; this.gCol = 0.96f; this.bCol = 1.0f;
            this.alpha = 0.5f;
        } else {
            this.rCol = 0.72f + this.random.nextFloat() * 0.2f; this.gCol = 0.88f; this.bCol = 1.0f;
            this.alpha = 1.0f;
        }
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) this.age / (float) this.lifetime;
        this.alpha = vapor ? 0.5f * (1.0f - t) * (1.0f - t) : 1.0f - t * t;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = ((float) this.age + partialTick) / (float) this.lifetime;
        return startSize * (vapor ? 1.0f + 1.8f * t : 1.0f - 0.6f * t);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return vapor ? super.getLightColor(partialTick) : 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean vapor;

        public Provider(SpriteSet sprites, boolean vapor) {
            this.sprites = sprites;
            this.vapor = vapor;
        }

        @Override
        public net.minecraft.client.particle.Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                                    double xs, double ys, double zs) {
            return new SparkParticle(level, x, y, z, xs, ys, zs, sprites, vapor);
        }
    }
}
