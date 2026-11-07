package com.wildkingdom;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class WildMob extends Animal implements GeoEntity {
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("move.fly");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack.swing");

    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(WildMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(WildMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(WildMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(WildMob.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected final Species.Spec spec;
    private ServerBossEvent bossBar;
    private boolean enraged = false;
    private boolean babyApplied = false;

    protected WildMob(EntityType<? extends WildMob> type, Level level, Species.Spec spec) {
        super(type, level);
        this.spec = spec;
        this.xpReward = Math.max(1, (int) (spec.hp() / 8.0));
        if (spec.boss()) {
            this.bossBar = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        }
        if (!level.isClientSide()) initGoals();
    }

    public Species.Spec spec() { return spec; }

    public static AttributeSupplier.Builder attrs(Species.Spec s) {
        AttributeSupplier.Builder b = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, s.hp())
                .add(Attributes.MOVEMENT_SPEED, s.spd())
                .add(Attributes.ATTACK_DAMAGE, s.dmg())
                .add(Attributes.FOLLOW_RANGE, 24.0);
        if (s.arch().equals("F")) {
            b.add(Attributes.FLYING_SPEED, s.fspd() > 0 ? s.fspd() : 0.5);
        }
        if (s.ride()) {
            b.add(Attributes.STEP_HEIGHT, 1.1);
        }
        if (s.boss()) {
            b.add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
        }
        return b;
    }

    // ------------------------------------------------------------ data
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TAMED, false);
        builder.define(SITTING, false);
        builder.define(SADDLED, false);
        builder.define(OWNER, "");
    }

    public boolean isTamed() { return this.entityData.get(TAMED); }
    public boolean isSitting() { return this.entityData.get(SITTING); }
    public boolean isSaddled() { return this.entityData.get(SADDLED); }
    public String ownerId() { return this.entityData.get(OWNER); }

    public LivingEntity getOwner() {
        String id = ownerId();
        if (id.isEmpty()) return null;
        try {
            return this.level().getPlayerByUUID(java.util.UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Tamed", isTamed());
        output.putBoolean("Sitting", isSitting());
        output.putBoolean("Saddled", isSaddled());
        output.putString("Owner", ownerId());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(TAMED, input.getBooleanOr("Tamed", false));
        this.entityData.set(SITTING, input.getBooleanOr("Sitting", false));
        this.entityData.set(SADDLED, input.getBooleanOr("Saddled", false));
        this.entityData.set(OWNER, input.getStringOr("Owner", ""));
    }

    // ------------------------------------------------------------ goals
    private void initGoals() {
        this.goalSelector.addGoal(0, new RiddenGoal(this));
        this.goalSelector.addGoal(0, new SitGoal(this));
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (spec.ai() == 0) {
            this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        } else {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        }
        this.goalSelector.addGoal(1, new RestGoal(this));
        if (Abilities.ranged(this)) this.goalSelector.addGoal(2, new Abilities.AbilityGoal(this));
        if (!spec.boss()) {
            this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        }
        if (!spec.prey().isEmpty()) {
            this.goalSelector.addGoal(3, new HuntGoal(this, new HashSet<>(Arrays.asList(spec.prey().split(",")))));
        }
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2));
        if (spec.herd()) {
            this.goalSelector.addGoal(4, new FlockGoal(this));
        }
        addMoveGoals();
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        if (spec.ai() >= 1) {
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new GuardOwnerGoal(this));
        }
        if (spec.ai() == 2) {
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    protected void addMoveGoals() {
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
    }

    /** While a player rides the animal its own AI must not move it. */
    static final class RiddenGoal extends Goal {
        private final WildMob mob;

        RiddenGoal(WildMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() { return mob.getControllingPassenger() != null; }

        @Override
        public void start() {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }

        @Override
        public void tick() { mob.setTarget(null); }
    }

    /** Sit command for tamed animals. */
    static final class SitGoal extends Goal {
        private final WildMob mob;

        SitGoal(WildMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() { return mob.isTamed() && mob.isSitting(); }

        @Override
        public void start() { mob.getNavigation().stop(); }
    }

    /** Diurnal animals sleep at night, nocturnal by day, insects shelter from rain. */
    static final class RestGoal extends Goal {
        private final WildMob mob;

        RestGoal(WildMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || mob.isTamed() || mob.isVehicle() || mob.isBaby()) return false;
            Level l = mob.level();
            boolean dark = l.isDarkOutside();
            if (mob.spec.act() == 0 && dark && !mob.spec.arch().equals("S")) return true;
            if (mob.spec.act() == 1 && !dark) return true;
            return mob.spec.rain() && l.isRaining();
        }

        @Override
        public void start() { mob.getNavigation().stop(); }
    }

    /** Follows the owner when tamed. */
    static final class FollowOwnerGoal extends Goal {
        private final WildMob mob;
        private final double speed;
        private LivingEntity owner;

        FollowOwnerGoal(WildMob mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!mob.isTamed() || mob.isSitting() || mob.getControllingPassenger() != null) return false;
            LivingEntity o = mob.getOwner();
            if (o == null || o.isSpectator() || mob.distanceToSqr(o) < 49.0) return false;
            this.owner = o;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && mob.isTamed() && !mob.isSitting() && mob.distanceToSqr(owner) > 9.0;
        }

        @Override
        public void tick() {
            if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(owner, speed);
            if (mob.distanceToSqr(owner) > 900.0) {
                mob.getNavigation().stop();
                mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            }
        }
    }

    /** Herd animals stay near each other. */
    static final class FlockGoal extends Goal {
        private final WildMob mob;
        private WildMob mate;

        FlockGoal(WildMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.isTamed() || mob.getTarget() != null || (mob.tickCount + mob.getId()) % 40 != 0) return false;
            List<WildMob> list = mob.level().getEntitiesOfClass(WildMob.class, mob.getBoundingBox().inflate(24.0, 8.0, 24.0),
                    e -> e != mob && e.isAlive() && e.spec.id().equals(mob.spec.id()));
            WildMob best = null;
            double bd = 64.0;
            for (WildMob e : list) {
                double d = mob.distanceToSqr(e);
                if (d > bd) {
                    bd = d;
                    best = e;
                }
            }
            if (best == null) return false;
            mate = best;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return mate != null && mate.isAlive() && mob.distanceToSqr(mate) > 25.0 && mob.getTarget() == null;
        }

        @Override
        public void start() { mob.getNavigation().moveTo(mate, 1.0); }
    }

    /** Predators look for their prey species nearby and set it as the attack target. */
    static final class HuntGoal extends Goal {
        private final WildMob mob;
        private final Set<String> prey;

        HuntGoal(WildMob mob, Set<String> prey) {
            this.mob = mob;
            this.prey = prey;
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || mob.getControllingPassenger() != null || mob.isBaby()) return false;
            if (mob.isTamed() && mob.isSitting()) return false;
            if ((mob.tickCount + mob.getId()) % 20 != 0) return false;
            List<WildMob> list = mob.level().getEntitiesOfClass(WildMob.class,
                    mob.getBoundingBox().inflate(16.0, 6.0, 16.0),
                    e -> e != mob && e.isAlive() && prey.contains(e.spec.id()) && !e.isTamed());
            WildMob best = null;
            double bd = Double.MAX_VALUE;
            for (WildMob e : list) {
                double d = mob.distanceToSqr(e);
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            if (best != null) mob.setTarget(best);
            return false;
        }
    }

    /** Tamed animals fight whoever hurts their owner or whom the owner attacks. */
    static final class GuardOwnerGoal extends Goal {
        private final WildMob mob;

        GuardOwnerGoal(WildMob mob) { this.mob = mob; }

        @Override
        public boolean canUse() {
            if (!mob.isTamed() || mob.getTarget() != null || mob.isSitting()) return false;
            LivingEntity o = mob.getOwner();
            if (o == null) return false;
            LivingEntity t = o.getLastHurtByMob();
            if (t == null) t = o.getLastHurtMob();
            if (t != null && t.isAlive() && t != mob && t != o && mob.canAttack(t)) mob.setTarget(t);
            return false;
        }
    }

    // ------------------------------------------------------------ combat
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
    public boolean canAttack(LivingEntity target) {
        if (isTamed()) {
            if (target instanceof Player) return false;
            if (target instanceof WildMob w && w.isTamed()) return false;
        }
        return super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            Abilities.onHit(this, level, target);
            triggerAnim("Actions", "attack.swing");
            if (target instanceof WildMob victim && !victim.isAlive()) {
                this.heal(4.0f);
            }
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
        if (r) Abilities.onHurt(this, level, source);
        if (r && "spray".equals(spec.react()) && source.getEntity() instanceof LivingEntity attacker) {
            attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0));
        }
        return r;
    }

    @Override
    public void die(DamageSource cause) {
        if (isSaddled() && this.level() instanceof ServerLevel sl) {
            this.spawnAtLocation(sl, Items.SADDLE);
        }
        super.die(cause);
    }

    // ------------------------------------------------------------ breeding
    @Override
    public boolean isFood(ItemStack stack) {
        return isTameFood(stack);
    }

    private boolean isTameFood(ItemStack st) {
        if (spec.tame().isEmpty() || st.isEmpty()) return false;
        return BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals(spec.tame());
    }

    @Override
    public boolean canMate(Animal other) {
        return other instanceof WildMob w && w.spec.id().equals(spec.id()) && this.isInLove() && other.isInLove();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        EntityType<WildMob> t = WildKingdom.TYPES.get(spec.id()).get();
        WildMob child = t.create(level, EntitySpawnReason.BREEDING);
        if (child != null) child.setBaby(true);
        return child;
    }

    @Override
    public void setBaby(boolean baby) {
        super.setBaby(baby);
        applyBabyScale();
    }

    private void applyBabyScale() {
        AttributeInstance a = this.getAttribute(Attributes.SCALE);
        if (a != null) a.setBaseValue(isBaby() ? 0.5 : 1.0);
        babyApplied = isBaby();
    }

    // ------------------------------------------------------------ interaction
    private boolean isOwner(Player p) {
        return isTamed() && ownerId().equals(p.getUUID().toString());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!(this.level() instanceof ServerLevel sl)) {
            return super.mobInteract(player, hand);
        }
        if (!isTamed() && isTameFood(st) && !spec.boss()) {
            if (!player.getAbilities().instabuild) st.shrink(1);
            int chance = spec.ai() == 2 ? 6 : 3;
            if (this.random.nextInt(chance) == 0) {
                this.entityData.set(TAMED, true);
                this.entityData.set(OWNER, player.getUUID().toString());
                this.setTarget(null);
                sl.broadcastEntityEvent(this, (byte) 7);
                player.displayClientMessage(Component.translatable("msg.wildkingdom.tamed"), true);
            } else {
                sl.broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }
        if (isOwner(player)) {
            if (player.isShiftKeyDown() && st.isEmpty()) {
                boolean s = !isSitting();
                this.entityData.set(SITTING, s);
                player.displayClientMessage(Component.translatable(s ? "msg.wildkingdom.sit" : "msg.wildkingdom.stand"), true);
                return InteractionResult.SUCCESS;
            }
            if (spec.ride() && !isSaddled() && st.is(Items.SADDLE) && !isBaby()) {
                if (!player.getAbilities().instabuild) st.shrink(1);
                this.entityData.set(SADDLED, true);
                player.displayClientMessage(Component.translatable("msg.wildkingdom.saddled"), true);
                return InteractionResult.SUCCESS;
            }
            if (spec.ride() && isSaddled() && !this.isVehicle() && st.isEmpty() && !isBaby()) {
                player.startRiding(this);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    // ------------------------------------------------------------ riding
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dims, float scale) {
        Float top = SaddleTop.TOP.get(spec.id());
        return new Vec3(0.0, top != null ? top : dims.height() * 0.9, 0.0);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        if (spec.ride() && isSaddled() && this.getFirstPassenger() instanceof Player p && isOwner(p)) return p;
        return null;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isAlive() && this.getControllingPassenger() instanceof Player p) {
            this.setYRot(p.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(p.getXRot() * 0.5f);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            float side = p.xxa * 0.5f;
            float fwd = p.zza;
            if (fwd <= 0.0f) fwd *= 0.25f;
            float spd = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double up = travelVector.y;
            if (this instanceof FlyingMob) {
                spd *= 3.0f;
                up = -Math.sin(Math.toRadians(p.getXRot())) * Math.abs(fwd) * 1.2;
            }
            this.setSpeed(spd);
            super.travel(new Vec3(side, up, fwd));
        } else {
            super.travel(travelVector);
        }
    }

    // ------------------------------------------------------------ boss
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (bossBar != null) bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossBar != null) bossBar.removePlayer(player);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel sl0) Abilities.tick(this, sl0);
        if (bossBar != null) {
            bossBar.setProgress(this.getHealth() / this.getMaxHealth());
            if (!enraged && this.getHealth() < this.getMaxHealth() * 0.5f) {
                enraged = true;
                AttributeInstance sp = this.getAttribute(Attributes.MOVEMENT_SPEED);
                if (sp != null) sp.setBaseValue(spec.spd() * 1.35);
                AttributeInstance dm = this.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dm != null) dm.setBaseValue(spec.dmg() * 1.4);
            }
        }
        if (babyApplied != isBaby()) applyBabyScale();
    }

    // ------------------------------------------------------------ sounds
    @Override
    protected SoundEvent getAmbientSound() { return Sounds.get(spec.id())[0]; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return Sounds.get(spec.id())[1]; }

    @Override
    protected SoundEvent getDeathSound() { return Sounds.get(spec.id())[2]; }

    // ------------------------------------------------------------ animation
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
