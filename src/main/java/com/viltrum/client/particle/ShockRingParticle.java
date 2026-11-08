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
 * Expanding ring lying in the plane perpendicular to the vector passed as particle speed;
 * the length of that vector is the final radius of the ring.
 */
public class ShockRingParticle extends TextureSheetParticle {
    private final Vector3f a = new Vector3f();
    private final Vector3f b = new Vector3f();
    private final float maxRadius;

    protected ShockRingParticle(ClientLevel level, double x, double y, double z, double nx, double ny, double nz, SpriteSet sprites, boolean warm) {
        super(level, x, y, z);
        this.xd = 0; this.yd = 0; this.zd = 0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        Vector3f n = new Vector3f((float) nx, (float) ny, (float) nz);
        this.maxRadius = Math.max(0.8f, n.length());
        if (n.lengthSquared() < 1.0e-6f) n.set(0, 1, 0);
        n.normalize();
        Vector3f ref = Math.abs(n.y) < 0.95f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        ref.cross(n, a).normalize();
        n.cross(a, b).normalize();
        this.lifetime = 12 + (int) Math.min(10, maxRadius);
        if (warm) { rCol = 1.0f; gCol = 0.78f; bCol = 0.45f; } else { rCol = 0.82f; gCol = 0.92f; bCol = 1.0f; }
        this.alpha = 1.0f;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        if (this.age++ >= this.lifetime) this.remove();
    }

    @Override
    public void render(VertexConsumer buf, Camera camera, float partialTicks) {
        float t = ((float) this.age + partialTicks) / (float) this.lifetime;
        float ease = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
        float r = maxRadius * (0.12f + 0.88f * ease);
        float alpha = Mth.clamp((1.0f - t) * 1.25f, 0.0f, 1.0f);
        Vec3 cam = camera.getPosition();
        float px = (float) (this.x - cam.x());
        float py = (float) (this.y - cam.y());
        float pz = (float) (this.z - cam.z());
        Vector3f ra = new Vector3f(a).mul(r);
        Vector3f rb = new Vector3f(b).mul(r);
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        int light = 0xF000F0;
        buf.addVertex(px - ra.x - rb.x, py - ra.y - rb.y, pz - ra.z - rb.z).setUv(u0, v1).setColor(rCol, gCol, bCol, alpha).setLight(light);
        buf.addVertex(px - ra.x + rb.x, py - ra.y + rb.y, pz - ra.z + rb.z).setUv(u0, v0).setColor(rCol, gCol, bCol, alpha).setLight(light);
        buf.addVertex(px + ra.x + rb.x, py + ra.y + rb.y, pz + ra.z + rb.z).setUv(u1, v0).setColor(rCol, gCol, bCol, alpha).setLight(light);
        buf.addVertex(px + ra.x - rb.x, py + ra.y - rb.y, pz + ra.z - rb.z).setUv(u1, v1).setColor(rCol, gCol, bCol, alpha).setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean warm;

        public Provider(SpriteSet sprites, boolean warm) { this.sprites = sprites; this.warm = warm; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xs, double ys, double zs) {
            return new ShockRingParticle(level, x, y, z, xs, ys, zs, sprites, warm);
        }
    }
}
