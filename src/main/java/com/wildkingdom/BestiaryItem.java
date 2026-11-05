package com.wildkingdom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BestiaryItem extends Item {
    public BestiaryItem(Item.Properties p) { super(p); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) BestiaryClient.open();
        return InteractionResult.SUCCESS;
    }
}
