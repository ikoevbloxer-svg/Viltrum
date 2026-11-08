package com.viltrum.entity;

import com.viltrum.network.FxPayload;
import com.viltrum.registry.ModSounds;
import com.viltrum.server.PowerSync;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A Viltrumite warrior. Fast, nearly unkillable by normal weapons, and it fights dirty:
 * supersonic ram, flurry of fists, and the neck-grab-and-slam that carries you up and drives you into the ground.
 */
public class ViltrumiteEntity extends Monster {
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(ViltrumiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GRAB = SynchedEntityData.defineId(ViltrumiteEntity.class, EntityDataSerializers.INT);

    public ViltrumiteEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, 26.0)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.5)
                .add(Attributes.STEP_HEIGHT, 1.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
        builder.define(GRAB, -1);
    }

    public boolean isFlyingPose() { return this.entityData.get(FLYING); }

    public void setFlyingPose(boolean v) { this.entityData.set(FLYING, v); }

    public int getGrabTarget() { return this.entityData.get(GRAB); }

    public void setGrabTarget(int id) { this.entityData.set(GRAB, id); }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GrabSlamGoal(this));
        this.goalSelector.addGoal(2, new DashGoal(this));
        this.goalSelector.addGoal(3, new FlurryGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(4.0f);
    }

    /** a single blow can never take more than a fifth of the health: you have to out-fight it. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, Math.min(amount, Math.max(30.0f, getMaxHealth() * 0.2f)));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            launch(le, 2.6, 0.9);
            level().playSound(null, blockPosition(), ModSounds.PUNCH_HEAVY.get(), SoundSource.HOSTILE, 1.4f, 0.8f);
        }
        return ok;
    }

    private void launch(LivingEntity le, double horizontal, double up) {
        Vec3 d = le.position().subtract(position()).multiply(1, 0, 1);
        d = d.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1) : d.normalize();
        le.setDeltaMovement(le.getDeltaMovement().add(d.x * horizontal, up, d.z * horizontal));
        le.hurtMarked = true;
    }

    private void smashAhead(Vec3 dir) {
        if (!(level() instanceof ServerLevel sl) || !sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        BlockPos c = BlockPos.containing(position().add(0, getBbHeight() * 0.5, 0).add(dir.scale(1.3)));
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 2; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    BlockState st = sl.getBlockState(p);
                    if (!st.isAir() && st.getDestroySpeed(sl, p) >= 0 && st.getDestroySpeed(sl, p) <= 12) sl.destroyBlock(p, false, this);
                }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    @Override
    public boolean canBeLeashed() { return false; }

    // ================================================================== goals
    /** leaps into supersonic flight and rams the target. */
    static class DashGoal extends Goal {
        private final ViltrumiteEntity m;
        private int cooldown = 30;
        private int ticks;
        private boolean rammed;

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
            return d > 36.0 || (d > 16.0 && t.getY() > m.getY() + 3.0);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = m.getTarget();
            return t != null && t.isAlive() && ticks < 100 && !rammed;
        }

        @Override
        public void start() {
            ticks = 0;
            rammed = false;
            m.setFlyingPose(true);
            m.setNoGravity(true);
            m.level().playSound(null, m.blockPosition(), ModSounds.FLIGHT_START.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
            LivingEntity t = m.getTarget();
            if (t != null && m.level() instanceof ServerLevel sl) {
                Vec3 dir = t.position().subtract(m.position()).normalize();
                PowerSync.fx(sl, FxPayload.SONIC_BOOM, m.position().add(0, 1, 0), dir, 1.0f, m.getId());
                sl.playSound(null, m.blockPosition(), ModSounds.SONIC_BOOM.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
            }
        }

        @Override
        public void tick() {
            LivingEntity t = m.getTarget();
            if (t == null) return;
            Vec3 center = m.position().add(0, m.getBbHeight() * 0.5, 0);
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(center);
            Vec3 dir = to.normalize();
            m.setDeltaMovement(m.getDeltaMovement().lerp(dir.scale(2.1), 0.3));
            m.getLookControl().setLookAt(t, 40.0f, 40.0f);
            ticks++;
            if (m.horizontalCollision) m.smashAhead(dir);
            if (!rammed && to.length() < 2.6) {
                rammed = true;
                t.hurt(m.damageSources().mobAttack(m), (float) m.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9f);
                m.launch(t, 3.2, 0.8);
                m.level().playSound(null, t.blockPosition(), ModSounds.PUNCH_SUPER.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
                if (m.level() instanceof ServerLevel sl) PowerSync.fx(sl, FxPayload.SHOCKWAVE, t.position().add(0, 1, 0), dir.scale(-1), 0.7f, m.getId());
            }
        }

        @Override
        public void stop() {
            m.setFlyingPose(false);
            m.setNoGravity(false);
            m.setDeltaMovement(m.getDeltaMovement().scale(0.25));
            cooldown = 50 + m.getRandom().nextInt(40);
        }
    }

    /** rapid-fire punches at close range. */
    static class FlurryGoal extends Goal {
        private final ViltrumiteEntity m;
        private int cooldown = 40;
        private int ticks;

        FlurryGoal(ViltrumiteEntity m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = m.getTarget();
            if (t == null || !t.isAlive() || m.distanceToSqr(t) > 16.0) return false;
            if (cooldown > 0) { cooldown--; return false; }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = m.getTarget();
            return t != null && t.isAlive() && ticks < 36 && m.distanceToSqr(t) < 36.0;
        }

        @Override
        public void start() { ticks = 0; }

        @Override
        public void tick() {
            LivingEntity t = m.getTarget();
            if (t == null) return;
            m.getLookControl().setLookAt(t, 60.0f, 60.0f);
            if (m.distanceToSqr(t) > 5.0) m.getNavigation().moveTo(t, 1.5); else m.getNavigation().stop();
            ticks++;
            if (ticks % 3 == 0 && m.distanceToSqr(t) <= 9.0 && m.level() instanceof ServerLevel sl) {
                m.swing(InteractionHand.MAIN_HAND);
                t.invulnerableTime = 0;
                t.hurt(m.damageSources().mobAttack(m), (float) m.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.35f);
                Vec3 push = m.getLookAngle().multiply(1, 0, 1).scale(0.1);
                t.setDeltaMovement(t.getDeltaMovement().multiply(0.3, 0.3, 0.3).add(push));
                t.hurtMarked = true;
                PowerSync.fx(sl, FxPayload.BARRAGE_HIT, t.getBoundingBox().getCenter(), m.getLookAngle().scale(-1), 0.6f, m.getId());
                if (ticks % 6 == 0) sl.playSound(null, t.blockPosition(), ModSounds.PUNCH_HEAVY.get(), SoundSource.HOSTILE, 1.0f, 1.2f);
            }
        }

        @Override
        public void stop() { cooldown = 80 + m.getRandom().nextInt(60); }
    }

    /** grabs the target by the throat, flies straight up and drives it into the ground. */
    static class GrabSlamGoal extends Goal {
        private final ViltrumiteEntity m;
        private LivingEntity victim;
        private boolean wasNoAi, wasNoGravity;
        private int cooldown = 120;
        private int ticks;
        private int phase; // 0 squeeze, 1 ascend, 2 dive, 3 done

        GrabSlamGoal(ViltrumiteEntity m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = m.getTarget();
            if (t == null || !t.isAlive() || m.distanceToSqr(t) > 12.0) return false;
            if (t instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
            if (cooldown > 0) { cooldown--; return false; }
            return m.getRandom().nextInt(12) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return phase != 3 && victim != null && victim.isAlive() && ticks < 140;
        }

        @Override
        public void start() {
            victim = m.getTarget();
            ticks = 0;
            phase = 0;
            if (victim instanceof Mob mob) { wasNoAi = mob.isNoAi(); mob.setNoAi(true); }
            wasNoGravity = victim.isNoGravity();
            victim.setNoGravity(true);
            m.setGrabTarget(victim.getId());
            m.level().playSound(null, m.blockPosition(), ModSounds.GRAB.get(), SoundSource.HOSTILE, 1.4f, 0.9f);
        }

        @Override
        public void tick() {
            if (victim == null) return;
            ticks++;
            Vec3 desired = m.getEyePosition().add(m.getLookAngle().scale(1.1)).subtract(0, victim.getBbHeight() * 0.82, 0);
            Vec3 delta = desired.subtract(victim.position());
            double len = delta.length();
            if (len > 3.5) delta = delta.scale(3.5 / len);
            victim.setDeltaMovement(delta.scale(0.85));
            victim.hurtMarked = true;
            victim.fallDistance = 0;
            m.getLookControl().setLookAt(victim, 60.0f, 60.0f);

            switch (phase) {
                case 0 -> {
                    m.getNavigation().stop();
                    if (ticks % 5 == 0) victim.hurt(m.damageSources().mobAttack(m), 3.0f);
                    if (ticks >= 12) {
                        phase = 1;
                        m.setFlyingPose(true);
                        m.setNoGravity(true);
                        m.level().playSound(null, m.blockPosition(), ModSounds.FLIGHT_START.get(), SoundSource.HOSTILE, 2.0f, 0.9f);
                    }
                }
                case 1 -> {
                    m.setDeltaMovement(m.getDeltaMovement().lerp(new Vec3(0, 1.2, 0), 0.35));
                    if (ticks >= 12 + 26) phase = 2;
                }
                case 2 -> {
                    m.setDeltaMovement(0, -2.6, 0);
                    if ((m.onGround() || m.verticalCollision) && ticks > 42) slam();
                }
                default -> { }
            }
        }

        private void slam() {
            phase = 3;
            if (!(m.level() instanceof ServerLevel sl)) return;
            Vec3 at = victim.position();
            victim.hurt(m.damageSources().mobAttack(m), (float) m.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.6f);
            double r = 7.0;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r), x -> x != m && x != victim && x.isAlive())) {
                double dist = e.position().distanceTo(at);
                if (dist > r) continue;
                float f = (float) (1.0 - dist / r);
                e.hurt(m.damageSources().mobAttack(m), 16.0f * f);
                m.launch(e, 2.5 * f, 1.1 * f);
            }
            sl.playSound(null, BlockPos.containing(at), ModSounds.SLAM.get(), SoundSource.HOSTILE, 2.0f, 0.9f);
            sl.playSound(null, BlockPos.containing(at), ModSounds.BONE_CRACK.get(), SoundSource.HOSTILE, 1.5f, 0.8f);
            PowerSync.fx(sl, FxPayload.SHOCKWAVE, at.add(0, 0.2, 0), new Vec3(0, 1, 0), 1.0f, m.getId());
            if (sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                BlockPos c = BlockPos.containing(at);
                for (int dx = -3; dx <= 3; dx++)
                    for (int dy = -3; dy <= 1; dy++)
                        for (int dz = -3; dz <= 3; dz++) {
                            if (dx * dx + dy * dy + dz * dz > 10) continue;
                            BlockPos p = c.offset(dx, dy, dz);
                            BlockState st = sl.getBlockState(p);
                            if (!st.isAir() && st.getDestroySpeed(sl, p) >= 0 && st.getDestroySpeed(sl, p) <= 6) sl.destroyBlock(p, false, m);
                        }
            }
        }

        @Override
        public void stop() {
            if (victim != null) {
                if (victim instanceof Mob mob) mob.setNoAi(wasNoAi);
                victim.setNoGravity(wasNoGravity);
                victim.setDeltaMovement(0, -0.2, 0);
                victim.hurtMarked = true;
            }
            victim = null;
            m.setGrabTarget(-1);
            m.setFlyingPose(false);
            m.setNoGravity(false);
            cooldown = 220 + m.getRandom().nextInt(120);
        }
    }
}
