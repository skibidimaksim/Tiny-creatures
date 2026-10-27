package com.cursedstudios.tinycreatures;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(TinyCreatures.MODID)
public class TinyCreatures {
    public static final String MODID = "tinycreatures";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Snail>> SNAIL = ENTITIES.register("snail", () ->
        EntityType.Builder.<Snail>of(Snail::new, MobCategory.CREATURE).sized(0.5f, 0.35f).clientTrackingRange(8).build("snail"));
    public TinyCreatures(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(this::attributes);
    }
    private void attributes(EntityAttributeCreationEvent event) {
        event.put(SNAIL.get(), Turtle.createAttributes().add(Attributes.MAX_HEALTH, 2.0).add(Attributes.MOVEMENT_SPEED, 0.08).build());
    }
}
