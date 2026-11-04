package com.wildkingdom;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class Sounds {
    private Sounds() {}

    /** returns {ambient, hurt, death} */
    public static SoundEvent[] get(String k) {
        return switch (k) {
            case "horse" -> new SoundEvent[]{SoundEvents.HORSE_AMBIENT, SoundEvents.HORSE_HURT, SoundEvents.HORSE_DEATH};
            case "cow" -> new SoundEvent[]{SoundEvents.COW_AMBIENT, SoundEvents.COW_HURT, SoundEvents.COW_DEATH};
            case "polar" -> new SoundEvent[]{SoundEvents.POLAR_BEAR_AMBIENT, SoundEvents.POLAR_BEAR_HURT, SoundEvents.POLAR_BEAR_DEATH};
            case "cat" -> new SoundEvent[]{SoundEvents.CAT_AMBIENT, SoundEvents.CAT_HURT, SoundEvents.CAT_DEATH};
            case "fox" -> new SoundEvent[]{SoundEvents.FOX_AMBIENT, SoundEvents.FOX_HURT, SoundEvents.FOX_DEATH};
            case "rabbit" -> new SoundEvent[]{SoundEvents.RABBIT_AMBIENT, SoundEvents.RABBIT_HURT, SoundEvents.RABBIT_DEATH};
            case "spider" -> new SoundEvent[]{SoundEvents.SPIDER_AMBIENT, SoundEvents.SPIDER_HURT, SoundEvents.SPIDER_DEATH};
            case "parrot" -> new SoundEvent[]{SoundEvents.PARROT_AMBIENT, SoundEvents.PARROT_HURT, SoundEvents.PARROT_DEATH};
            case "dolphin" -> new SoundEvent[]{SoundEvents.DOLPHIN_AMBIENT, SoundEvents.DOLPHIN_HURT, SoundEvents.DOLPHIN_DEATH};
            case "cod" -> new SoundEvent[]{null, SoundEvents.COD_HURT, SoundEvents.COD_DEATH};
            case "bee" -> new SoundEvent[]{null, SoundEvents.BEE_HURT, SoundEvents.BEE_DEATH};
            default -> new SoundEvent[]{SoundEvents.SILVERFISH_AMBIENT, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH};
        };
    }
}
