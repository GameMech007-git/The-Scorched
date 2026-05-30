package com.thescorched.entity.custom;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CinderlingEntity extends PathfinderMob implements GeoEntity, RangedAttackMob {

    private static class CinderlingRangedAttackGoal extends RangedAttackGoal {

        private final PathfinderMob mob;
        private final float minAttackDistanceSqr;

        public CinderlingRangedAttackGoal(
                PathfinderMob mob,
                double speedModifier,
                int attackInterval,
                float maxAttackDistance,
                float minAttackDistance
        ) {
            super((RangedAttackMob) mob, speedModifier, attackInterval, maxAttackDistance);
            this.mob = mob;
            this.minAttackDistanceSqr = minAttackDistance * minAttackDistance;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();

            if (target == null) {
                return false;
            }

            double distanceSqr = this.mob.distanceToSqr(target);

            if (distanceSqr < this.minAttackDistanceSqr) {
                return false;
            }

            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();

            if (target == null) {
                return false;
            }

            double distanceSqr = this.mob.distanceToSqr(target);

            if (distanceSqr < this.minAttackDistanceSqr) {
                return false;
            }

            return super.canContinueToUse();
        }
    }

    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(CinderlingEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cinderling.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cinderling.walk");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.cinderling.hurt");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.cinderling.bite");
    private static final RawAnimation FIRE_ATTACK = RawAnimation.begin().thenPlay("animation.cinderling.attack");

    private int invisibilityDelay = 0;
    private int fireballDelay = 0;
    private LivingEntity pendingFireballTarget = null;
    private int attackAnimationLock = 0;

    public CinderlingEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACKING, false);
    }

    private void setAttackingAnimationState(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    private boolean isAttackingAnimationState() {
        return this.entityData.get(ATTACKING);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Lava/fireball ranged attack
        // 1.0 = speed, 200 = cooldown ticks, 8.0f = max range, 4.0f = min range
        this.goalSelector.addGoal(1, new CinderlingRangedAttackGoal(this, 1.0, 200, 8.0f, 4.0f));

        // Bite attack when close
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));

        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                true
        ));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (this.level().isClientSide()) {
            return;
        }

        if (this.fireballDelay > 0) {
            return;
        }

        this.pendingFireballTarget = target;

        // Do not change this delay
        this.fireballDelay = 100;

        // Sync to client so idle/walk animation stops during ranged combat
        this.setAttackingAnimationState(true);
    }

    private void shootFireballAt(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        this.triggerAnim("fire_attack_controller", "fire_attack");

// After one fireball, block idle animation for 10 seconds
        this.attackAnimationLock = 20 * 10;
        this.setAttackingAnimationState(true);

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5) - this.getY(0.5);
        double dz = target.getZ() - this.getZ();

        Vec3 direction = new Vec3(dx, dy, dz);

        SmallFireball fireball = new SmallFireball(
                this.level(),
                this,
                direction
        );

        Vec3 mouthDirection = direction.normalize();

        fireball.setPos(
                this.getX() + mouthDirection.x * 0.8,
                this.getEyeY() - 0.35,
                this.getZ() + mouthDirection.z * 0.8
        );

        this.level().addFreshEntity(fireball);

        this.level().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE,
                1.0f,
                1.0f
        );
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.setAttackingAnimationState(true);
        this.attackAnimationLock = 12;
        this.triggerAnim("attack_controller", "bite");

        return super.doHurtTarget(level, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (damageSource.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }

        boolean wasHurt = super.hurtServer(level, damageSource, amount);

        if (wasHurt && this.isAlive()) {
            this.triggerAnim("hurt_controller", "hurt");
            this.invisibilityDelay = 12;
        }

        return wasHurt;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.attackAnimationLock > 0) {
            this.attackAnimationLock--;
        }

        if (!this.level().isClientSide() && this.invisibilityDelay > 0) {
            this.invisibilityDelay--;

            if (this.invisibilityDelay == 0 && this.isAlive()) {
                this.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY,
                        20 * 5,
                        0,
                        false,
                        false,
                        true
                ));
            }
        }

        if (!this.level().isClientSide() && this.fireballDelay > 0) {
            this.fireballDelay--;

            if (this.fireballDelay == 0) {
                this.shootFireballAt(this.pendingFireballTarget);
                this.pendingFireballTarget = null;
            }
        }

        if (!this.level().isClientSide()
                && this.fireballDelay <= 0
                && this.attackAnimationLock <= 0) {
            this.setAttackingAnimationState(false);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("fire_attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("fire_attack", FIRE_ATTACK));

        controllers.add(new AnimationController<>("movement_controller", 5, state -> {
            if (this.isAttackingAnimationState()) {
                return PlayState.STOP;
            }

            if (state.isMoving()) {
                state.setAnimation(WALK);
            } else {
                state.setAnimation(IDLE);
            }

            return PlayState.CONTINUE;
        }));

        controllers.add(new AnimationController<>("hurt_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", HURT));

        controllers.add(new AnimationController<>("attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("bite", BITE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}