package com.viltrum.registry;

import com.viltrum.Viltrum;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Viltrum.MODID);

    /** glowing spark (full bright). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK = PARTICLES.register("spark", () -> new SimpleParticleType(true));
    /** large soft vapour puff, used for contrails and Prandtl-Glauert clouds. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VAPOR = PARTICLES.register("vapor", () -> new SimpleParticleType(true));
    /** wind streak stretched along the vector passed as particle speed. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STREAK = PARTICLES.register("streak", () -> new SimpleParticleType(true));
    /** expanding shock ring whose normal is the vector passed as particle speed. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCK_RING = PARTICLES.register("shock_ring", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCK_RING_WARM = PARTICLES.register("shock_ring_warm", () -> new SimpleParticleType(true));
}
