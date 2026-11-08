package com.viltrum.entity;

import com.viltrum.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

import java.util.EnumSet;

/** A Viltrumite warrior: huge, fast, flies at its target and launches whatever it punches. */
public class ViltrumiteEntity extends Monster {
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(ViltrumiteEntity.class, EntityDataSerializers.BOOLEAN);

    public ViltrumiteEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 140.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    public boolean isFlyingPose() { return this.entityData.get(FLYING); }

    public void setFlyingPose(boolean v) { this.entityData.set(FLYING, v); }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DashGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14.0f));
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
            le.setDeltaMovement(le.getDeltaMovement().add(d.x * 2.4, 0.8, d.z * 2.4));
            le.hurtMarked = true;
            level().playSound(null, blockPosition(), ModSounds.PUNCH_HEAVY.get(), SoundSource.HOSTILE, 1.3f, 0.85f);
        }
        return ok;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    @Override
    public boolean canBeLeashed() { return false; }

    /** leaps into flight toward a distant target. */
    static class DashGoal extends Goal {
        private final ViltrumiteEntity m;
        private int cooldown = 40;
        private int ticks;

        DashGoal(ViltrumiteEntity m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = m.getTarget();
            if (t == null || !t.isAlive()) return false;
            if (cooldown > 0) { cooldown--; return false; }
            double d = m.distanceToSqr(t);
            return d > 49.0 || (d > 16.0 && t.getY() > m.getY() + 4.0);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = m.getTarget();
            return t != null && t.isAlive() && ticks < 90 && m.distanceToSqr(t) > 6.0;
        }

        @Override
        public void start() {
            ticks = 0;
            m.setFlyingPose(true);
            m.setNoGravity(true);
            m.level().playSound(null, m.blockPosition(), ModSounds.FLIGHT_START.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
        }

        @Override
        public void tick() {
            LivingEntity t = m.getTarget();
            if (t == null) return;
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(m.position().add(0, m.getBbHeight() * 0.5, 0));
            Vec3 want = to.normalize().scale(1.25);
            m.setDeltaMovement(m.getDeltaMovement().lerp(want, 0.2));
            m.getLookControl().setLookAt(t, 40.0f, 40.0f);
            ticks++;
        }

        @Override
        public void stop() {
            m.setFlyingPose(false);
            m.setNoGravity(false);
            m.setDeltaMovement(m.getDeltaMovement().scale(0.3));
            cooldown = 80 + m.getRandom().nextInt(60);
        }
    }
}
