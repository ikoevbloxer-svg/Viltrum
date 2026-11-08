package com.viltrum.entity;

import com.viltrum.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Reanimated cyborg: tough, relentless, regenerates and gets back up once after being killed. */
public class ReanimenEntity extends Monster {
    private boolean revived = false;

    public ReanimenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 45;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(2.0f);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            Vec3 d = le.position().subtract(position()).multiply(1, 0, 1);
            d = d.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1) : d.normalize();
            le.setDeltaMovement(le.getDeltaMovement().add(d.x * 1.3, 0.45, d.z * 1.3));
            le.hurtMarked = true;
        }
        return ok;
    }

    /** called from the death event; returns true when the death must be cancelled. */
    public boolean tryRevive() {
        if (revived || level().isClientSide) return false;
        revived = true;
        removeAllEffects();
        setHealth(getMaxHealth() * 0.45f);
        invulnerableTime = 30;
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.0, getZ(), 40, 0.4, 0.8, 0.4, 0.3);
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.0, getZ(), 1, 0, 0, 0, 0);
            sl.playSound(null, blockPosition(), ModSounds.CHARGE.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
        }
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Revived", revived);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        revived = tag.getBoolean("Revived");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
}
