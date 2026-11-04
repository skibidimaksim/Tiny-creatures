package com.wildkingdom;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
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
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class WildMob extends PathfinderMob implements GeoEntity {
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("move.fly");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack.swing");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected final Species.Spec spec;

    protected WildMob(EntityType<? extends WildMob> type, Level level, Species.Spec spec) {
        super(type, level);
        this.spec = spec;
        this.xpReward = Math.max(1, (int) (spec.hp() / 8.0));
        initGoals();
    }

    public static AttributeSupplier.Builder attrs(Species.Spec s) {
        AttributeSupplier.Builder b = PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, s.hp())
                .add(Attributes.MOVEMENT_SPEED, s.spd())
                .add(Attributes.ATTACK_DAMAGE, s.dmg())
                .add(Attributes.FOLLOW_RANGE, 24.0);
        if (s.arch().equals("F")) {
            b.add(Attributes.FLYING_SPEED, s.fspd() > 0 ? s.fspd() : 0.5);
        }
        return b;
    }

    /** Called from the constructor (registerGoals runs too early, before spec is set). */
    private void initGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (spec.ai() == 0) {
            this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        } else {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        }
        addMoveGoals();
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        if (spec.ai() >= 1) {
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        }
        if (spec.ai() == 2) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    protected void addMoveGoals() {
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
    }

    private static Holder<MobEffect> effect(String n) {
        return switch (n) {
            case "slowness" -> MobEffects.SLOWNESS;
            case "weakness" -> MobEffects.WEAKNESS;
            case "blindness" -> MobEffects.BLINDNESS;
            case "nausea" -> MobEffects.NAUSEA;
            default -> MobEffects.POISON;
        };
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            triggerAnim("Actions", "attack.swing");
            if (spec.eff() != null && target instanceof LivingEntity le) {
                String[] p = spec.eff().split(":");
                le.addEffect(new MobEffectInstance(effect(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
            }
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean r = super.hurtServer(level, source, amount);
        if (r && "spray".equals(spec.react()) && source.getEntity() instanceof LivingEntity attacker) {
            attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0));
        }
        return r;
    }

    @Override
    protected SoundEvent getAmbientSound() { return Sounds.get(spec.snd())[0]; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return Sounds.get(spec.snd())[1]; }

    @Override
    protected SoundEvent getDeathSound() { return Sounds.get(spec.snd())[2]; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<WildMob>("Move", 3, t -> {
                    if (spec.arch().equals("F")) return t.setAndContinue(FLY);
                    return t.isMoving() ? t.setAndContinue(WALK) : t.setAndContinue(IDLE);
                }),
                new AnimationController<WildMob>("Actions", t -> PlayState.STOP)
                        .triggerableAnim("attack.swing", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
