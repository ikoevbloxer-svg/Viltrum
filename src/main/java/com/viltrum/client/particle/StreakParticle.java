package com.viltrum.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A wind streak: a thin quad stretched along the velocity vector that is passed as the particle "speed".
 * It stays in place in the world (the player flies past it), so at high speed the air visibly rushes by.
 */
public class StreakParticle extends TextureSheetParticle {
    private final Vector3f axis;
    private final float length;
    private final float width;

    protected StreakParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = 0; this.yd = 0; this.zd = 0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = 4 + this.random.nextInt(3);
        Vector3f v = new Vector3f((float) dx, (float) dy, (float) dz);
        this.length = Math.max(0.5f, v.length());
        this.axis = v.lengthSquared() > 1.0e-6f ? v.normalize() : new Vector3f(0, 1, 0);
        this.width = 0.025f + this.random.nextFloat() * 0.035f;
        this.rCol = 0.86f; this.gCol = 0.93f; this.bCol = 1.0f;
        this.alpha = 0.0f;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float t = (float) this.age / (float) this.lifetime;
        this.alpha = Mth.sin(t * (float) Math.PI) * 0.85f;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float px = (float) (this.x - cam.x());
        float py = (float) (this.y - cam.y());
        float pz = (float) (this.z - cam.z());

        Vector3f toCam = new Vector3f(-px, -py, -pz);
        if (toCam.lengthSquared() < 1.0e-6f) return;
        toCam.normalize();
        Vector3f side = new Vector3f(axis).cross(toCam);
        if (side.lengthSquared() < 1.0e-6f) return;
        side.normalize().mul(width);
        Vector3f half = new Vector3f(axis).mul(length * 0.5f);

        float u0 = this.getU0(), u1 = this.getU1(), v0 = this.getV0(), v1 = this.getV1();
        int light = 0xF000F0;
        quad(buffer, px, py, pz, half, side, u0, u1, v0, v1, light);
    }

    private void quad(VertexConsumer b, float px, float py, float pz, Vector3f h, Vector3f s, float u0, float u1, float v0, float v1, int light) {
        b.addVertex(px - h.x - s.x, py - h.y - s.y, pz - h.z - s.z).setUv(u0, v1).setColor(rCol, gCol, bCol, alpha).setLight(light);
        b.addVertex(px - h.x + s.x, py - h.y + s.y, pz - h.z + s.z).setUv(u0, v0).setColor(rCol, gCol, bCol, alpha).setLight(light);
        b.addVertex(px + h.x + s.x, py + h.y + s.y, pz + h.z + s.z).setUv(u1, v0).setColor(rCol, gCol, bCol, alpha).setLight(light);
        b.addVertex(px + h.x - s.x, py + h.y - s.y, pz + h.z - s.z).setUv(u1, v1).setColor(rCol, gCol, bCol, alpha).setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xs, double ys, double zs) {
            return new StreakParticle(level, x, y, z, xs, ys, zs, sprites);
        }
    }
}
