package com.wildkingdom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(WildKingdom.MODID)
public final class WildKingdom {
    public static final String MODID = "wildkingdom";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);

    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<WildMob>>> TYPES = new LinkedHashMap<>();
    public static final List<DeferredItem<SpawnEggItem>> EGGS = new ArrayList<>();

    private static WildMob make(EntityType<? extends WildMob> type, Level level, Species.Spec s) {
        return switch (s.arch()) {
            case "F" -> new FlyingMob(type, level, s);
            case "S" -> new SwimMob(type, level, s);
            default -> new LandMob(type, level, s);
        };
    }

    static {
        for (Species.Spec s : Species.ALL) {
            MobCategory cat = s.arch().equals("S") ? MobCategory.WATER_CREATURE : MobCategory.CREATURE;
            DeferredHolder<EntityType<?>, EntityType<WildMob>> holder = ENTITIES.<WildMob>registerEntityType(
                    s.id(), (t, l) -> make(t, l, s), cat,
                    b -> s.ride()
                            ? b.sized(s.w(), s.h()).clientTrackingRange(8).passengerAttachments(s.h() * 0.72f)
                            : b.sized(s.w(), s.h()).clientTrackingRange(8));
            TYPES.put(s.id(), holder);
            EGGS.add(ITEMS.registerItem(s.id() + "_spawn_egg",
                    props -> new SpawnEggItem(props.spawnEgg(holder.get()))));
        }
    }

    public WildKingdom(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener(WildKingdom::attributes);
        modBus.addListener(WildKingdom::spawnPlacements);
        modBus.addListener(WildKingdom::tabs);
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        for (Species.Spec s : Species.ALL) {
            e.put(TYPES.get(s.id()).get(), WildMob.attrs(s).build());
        }
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent e) {
        for (Species.Spec s : Species.ALL) {
            EntityType<WildMob> t = TYPES.get(s.id()).get();
            switch (s.arch()) {
                case "S" -> e.register(t, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                        (ty, lv, r, p, rn) -> lv.getFluidState(p).is(FluidTags.WATER),
                        RegisterSpawnPlacementsEvent.Operation.OR);
                case "F" -> e.register(t, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                        (ty, lv, r, p, rn) -> lv.getBlockState(p).isAir(),
                        RegisterSpawnPlacementsEvent.Operation.OR);
                default -> e.register(t, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (ty, lv, r, p, rn) -> lv.getBlockState(p).isAir() && !lv.getBlockState(p.below()).isAir()
                                && lv.getFluidState(p.below()).isEmpty(),
                        RegisterSpawnPlacementsEvent.Operation.OR);
            }
        }
    }

    private static void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            for (DeferredItem<SpawnEggItem> egg : EGGS) e.accept(egg.get());
        }
    }
}
