package com.cursedstudios.tinycreatures;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;

/** Prototype snail: turtle-based placeholder until custom 3D model is implemented. */
public class Snail extends Turtle {
    private float shellShield = 4.0f;
    private int safeTicks = 0;
    public Snail(EntityType<? extends Turtle> type, Level level) { super(type, level); }
    @Override public boolean hurt(DamageSource source, float amount) {
        safeTicks = 0;
        if (shellShield > 0) {
            float blocked = Math.min(shellShield, amount);
            shellShield -= blocked;
            amount -= blocked;
        }
        return amount <= 0 || super.hurt(source, amount);
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && ++safeTicks >= 600 && shellShield < 4.0f && tickCount % 20 == 0) {
            shellShield = Math.min(4.0f, shellShield + 0.25f);
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putFloat("ShellShield", shellShield);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); if (tag.contains("ShellShield")) shellShield = tag.getFloat("ShellShield");
    }
}
