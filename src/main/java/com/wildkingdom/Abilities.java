package com.wildkingdom;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Special abilities of Wild Kingdom animals. Spec.abil is a comma separated list. */
public final class Abilities {
    private Abilities() {}

    public static boolean has(WildMob m, String a) {
        String s = m.spec().abil();
        return s != null && !s.isEmpty() && ("," + s + ",").contains("," + a + ",");
    }

    public static boolean ranged(WildMob m) {
        return has(m, "leap") || has(m, "charge") || has(m, "spit") || has(m, "breath") || has(m, "bolt") || has(m, "web_shot") || has(m, "roar");
    }

    private static void effect(LivingEntity e, Holder<MobEffect> ef, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(ef, ticks, amp));
    }

    // ---------------------------------------------------------------- on hit
    public static void onHit(WildMob m, ServerLevel lv, Entity target) {
        if (!(target instanceof LivingEntity le)) return;
        if (has(m, "fire")) le.setRemainingFireTicks(120);
        if (has(m, "freeze")) { effect(le, MobEffects.SLOWNESS, 100, 2); le.setTicksFrozen(le.getTicksRequiredToFreeze() + 60); }
        if (has(m, "wither")) effect(le, MobEffects.WITHER, 100, 1);
        if (has(m, "drain")) m.heal(3.0f);
        if (has(m, "blind")) effect(le, MobEffects.BLINDNESS, 80, 0);
        if (has(m, "weak")) effect(le, MobEffects.WEAKNESS, 120, 1);
        if (has(m, "hunger")) effect(le, MobEffects.HUNGER, 160, 1);
        if (has(m, "knock")) {
            Vec3 d = le.position().subtract(m.position()).normalize();
            le.push(d.x * 1.4, 0.45, d.z * 1.4);
        }
        if (has(m, "web")) {
            BlockPos p = le.blockPosition();
            if (lv.getBlockState(p).isAir()) lv.setBlockAndUpdate(p, Blocks.COBWEB.defaultBlockState());
        }
        if (has(m, "lightning")) strike(lv, le.position());
        if (has(m, "quake")) quake(m, lv);
    }

    private static void strike(ServerLevel lv, Vec3 pos) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(lv, EntitySpawnReason.TRIGGERED);
        if (b != null) {
            b.setPos(pos.x, pos.y, pos.z);
            lv.addFreshEntity(b);
        }
    }

    private static boolean friend(WildMob m, Entity e) {
        if (e == m || e instanceof WildMob w && w.spec().id().equals(m.spec().id())) return true;
        if (e instanceof WildMob w2 && w2.isTamed() && m.isTamed()) return true;
        return e instanceof Player p && m.isTamed() && m.ownerId().equals(p.getUUID().toString());
    }

    private static void quake(WildMob m, ServerLevel lv) {
        AABB box = m.getBoundingBox().inflate(4.0, 1.5, 4.0);
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, box)) {
            if (friend(m, e)) continue;
            e.hurtServer(lv, lv.damageSources().mobAttack(m), 4.0f);
            Vec3 d = e.position().subtract(m.position());
            Vec3 n = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : d.normalize();
            e.push(n.x * 0.9, 0.7, n.z * 0.9);
        }
        lv.sendParticles(ParticleTypes.EXPLOSION, m.getX(), m.getY() + 0.2, m.getZ(), 3, 1.5, 0.1, 1.5, 0.0);
    }

    // ---------------------------------------------------------------- when hurt
    public static void onHurt(WildMob m, ServerLevel lv, DamageSource src) {
        if (!(src.getEntity() instanceof LivingEntity a) || friend(m, a)) return;
        if (has(m, "thorns")) a.hurtServer(lv, lv.damageSources().thorns(m), 3.0f);
        if (has(m, "static")) { a.hurtServer(lv, lv.damageSources().lightningBolt(), 2.0f); effect(a, MobEffects.GLOWING, 200, 0); }
        if (has(m, "stink")) { effect(a, MobEffects.NAUSEA, 160, 0); effect(a, MobEffects.BLINDNESS, 60, 0); }
        if (has(m, "flee_invis")) effect(m, MobEffects.INVISIBILITY, 160, 0);
    }

    // ---------------------------------------------------------------- passive, every second
    public static void tick(WildMob m, ServerLevel lv) {
        if (m.tickCount % 20 != 0) return;
        if (has(m, "regen") && m.getHealth() < m.getMaxHealth()) m.heal(1.5f);
        if (has(m, "invis")) effect(m, MobEffects.INVISIBILITY, 60, 0);
        if (has(m, "glow")) effect(m, MobEffects.GLOWING, 60, 0);
        if (has(m, "fireproof")) { m.clearFire(); effect(m, MobEffects.FIRE_RESISTANCE, 60, 0); }
        if (has(m, "swimmer")) effect(m, MobEffects.DOLPHINS_GRACE, 60, 0);
        if (has(m, "swift")) effect(m, MobEffects.SPEED, 60, 1);
        if (has(m, "tough")) effect(m, MobEffects.RESISTANCE, 60, 0);
        if (has(m, "fireaura")) {
            for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(3.0))) {
                if (!friend(m, e)) e.setRemainingFireTicks(60);
            }
            lv.sendParticles(ParticleTypes.FLAME, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 6, 0.6, 0.4, 0.6, 0.01);
        }
        if (m.isTamed() && !m.ownerId().isEmpty()) {
            Player owner = null;
            try { owner = lv.getPlayerByUUID(UUID.fromString(m.ownerId())); } catch (IllegalArgumentException ignored) { }
            if (owner != null && owner.distanceToSqr(m) < 400.0) {
                if (has(m, "aura_speed")) effect(owner, MobEffects.SPEED, 100, 0);
                if (has(m, "aura_strength")) effect(owner, MobEffects.STRENGTH, 100, 0);
                if (has(m, "aura_regen")) effect(owner, MobEffects.REGENERATION, 100, 0);
                if (has(m, "aura_night")) effect(owner, MobEffects.NIGHT_VISION, 260, 0);
                if (has(m, "aura_resist")) effect(owner, MobEffects.RESISTANCE, 100, 0);
                if (has(m, "aura_jump")) effect(owner, MobEffects.JUMP_BOOST, 100, 1);
                if (has(m, "aura_haste")) effect(owner, MobEffects.HASTE, 100, 0);
                if (has(m, "aura_fire")) effect(owner, MobEffects.FIRE_RESISTANCE, 100, 0);
                if (has(m, "aura_water")) { effect(owner, MobEffects.WATER_BREATHING, 100, 0); effect(owner, MobEffects.DOLPHINS_GRACE, 100, 0); }
                if (has(m, "aura_luck")) effect(owner, MobEffects.LUCK, 200, 0);
            }
        }
    }

    // ---------------------------------------------------------------- ranged / special attacks
    public static final class AbilityGoal extends Goal {
        private final WildMob m;
        private long next;

        public AbilityGoal(WildMob m) {
            this.m = m;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = m.getTarget();
            return t != null && t.isAlive() && m.level().getGameTime() >= next && !m.isVehicle();
        }

        @Override
        public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            LivingEntity t = m.getTarget();
            if (t == null || !(m.level() instanceof ServerLevel lv)) return;
            double d = m.distanceTo(t);
            Vec3 dir = t.position().subtract(m.position());
            Vec3 n = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : dir.normalize();
            boolean used = false;
            if (has(m, "leap") && d > 3.0 && d < 9.0 && m.onGround()) {
                m.setDeltaMovement(n.x * 0.9, 0.55, n.z * 0.9);
                used = true;
            } else if (has(m, "charge") && d > 4.0 && d < 14.0 && m.onGround()) {
                m.setDeltaMovement(n.x * 1.25, 0.15, n.z * 1.25);
                used = true;
            } else if (has(m, "spit") && d > 2.5 && d < 12.0) {
                t.hurtServer(lv, lv.damageSources().mobAttack(m), 3.0f);
                effect(t, MobEffects.POISON, 100, 0);
                lv.sendParticles(ParticleTypes.SPIT, m.getX(), m.getEyeY(), m.getZ(), 8, n.x * 0.3, 0.1, n.z * 0.3, 0.3);
                used = true;
            } else if (has(m, "breath") && d < 9.0) {
                t.hurtServer(lv, lv.damageSources().mobAttack(m), 5.0f);
                t.setRemainingFireTicks(120);
                lv.sendParticles(ParticleTypes.FLAME, m.getX() + n.x, m.getEyeY(), m.getZ() + n.z, 20, n.x * 0.5, 0.1, n.z * 0.5, 0.1);
                used = true;
            } else if (has(m, "bolt") && d < 18.0) {
                strike(lv, t.position());
                used = true;
            } else if (has(m, "web_shot") && d > 2.5 && d < 12.0) {
                BlockPos p = t.blockPosition();
                if (lv.getBlockState(p).isAir()) lv.setBlockAndUpdate(p, Blocks.COBWEB.defaultBlockState());
                effect(t, MobEffects.SLOWNESS, 60, 1);
                used = true;
            } else if (has(m, "roar") && d < 10.0) {
                List<LivingEntity> list = lv.getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(8.0));
                for (LivingEntity e : list) {
                    if (friend(m, e)) continue;
                    effect(e, MobEffects.WEAKNESS, 120, 0);
                    effect(e, MobEffects.SLOWNESS, 80, 0);
                }
                lv.sendParticles(ParticleTypes.SONIC_BOOM, m.getX(), m.getEyeY(), m.getZ(), 1, 0, 0, 0, 0);
                used = true;
            }
            next = m.level().getGameTime() + (used ? 100 : 20);
        }
    }
}
