package com.comfykit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Harder survival: stronger mobs, faster hunger, more damage. Works in singleplayer / own server. */
public class HardMode {
    public static int level = 2; // 0 = off, 1..3

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (level <= 0 || e.world.isRemote) return;
        Entity en = e.entity;
        if (!(en instanceof EntityMob)) return;
        if (en.getEntityData().getBoolean("ck_hard")) return;
        en.getEntityData().setBoolean("ck_hard", true);
        EntityMob m = (EntityMob) en;
        double hp = m.getEntityAttribute(SharedMonsterAttributes.maxHealth).getBaseValue();
        m.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(hp * (1.0 + 0.3 * level));
        m.setHealth(m.getMaxHealth());
        double sp = m.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getBaseValue();
        m.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(sp * (1.0 + 0.05 * level));
        if (m.getEntityAttribute(SharedMonsterAttributes.followRange) != null) {
            double fr = m.getEntityAttribute(SharedMonsterAttributes.followRange).getBaseValue();
            m.getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(fr + 8.0 * level);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent e) {
        if (level <= 0 || e.entityLiving.worldObj.isRemote) return;
        if (e.entityLiving instanceof EntityPlayer && e.source.getEntity() instanceof EntityMob) {
            e.ammount *= 1.0f + 0.2f * level;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (level <= 0 || e.phase != TickEvent.Phase.END || e.player.worldObj.isRemote) return;
        if (e.player.ticksExisted % 40 == 0 && !e.player.capabilities.isCreativeMode) {
            e.player.addExhaustion(0.12f * level);
        }
    }
}
