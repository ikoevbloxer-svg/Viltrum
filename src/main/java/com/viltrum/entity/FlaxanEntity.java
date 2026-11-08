package com.viltrum.entity;

import com.viltrum.registry.ModItems;
import com.viltrum.server.BeamUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Dimension-hopping soldier with an energy rifle. */
public class FlaxanEntity extends Monster {
    public FlaxanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 3.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.FLAXAN_BLASTER.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.08f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LaserGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PILLAGER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PILLAGER_DEATH; }

    static class LaserGoal extends Goal {
        private final FlaxanEntity f;
        private int cooldown = 30;
        private int aim;

        LaserGoal(FlaxanEntity f) {
            this.f = f;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = f.getTarget();
            if (t == null || !t.isAlive()) return false;
            double d = f.distanceToSqr(t);
            return d > 16.0 && d < 38.0 * 38.0 && f.hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public void start() { aim = 0; }

        @Override
        public void tick() {
            LivingEntity t = f.getTarget();
            if (t == null) return;
            f.getLookControl().setLookAt(t, 30.0f, 30.0f);
            f.getNavigation().stop();
            aim++;
            if (cooldown > 0) cooldown--;
            if (aim >= 16 && cooldown <= 0 && f.level() instanceof ServerLevel sl) {
                Vec3 from = f.getEyePosition().add(f.getLookAngle().scale(0.5));
                Vec3 dir = t.getEyePosition().subtract(from).normalize();
                BeamUtil.fire(sl, f, from, dir, 40.0, 5.0f);
                cooldown = 35 + f.getRandom().nextInt(25);
                aim = 0;
            }
        }

        @Override
        public void stop() { cooldown = 20; }
    }
}
