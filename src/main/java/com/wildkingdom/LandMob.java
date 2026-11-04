package com.wildkingdom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class LandMob extends WildMob {
    public LandMob(EntityType<? extends WildMob> type, Level level, Species.Spec spec) {
        super(type, level, spec);
    }
}
