package com.wildkingdom;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class Armors {
    private Armors() {}

    private static Identifier id(String n) { return Identifier.fromNamespaceAndPath(WildKingdom.MODID, n); }

    public static final ArmorMaterial CHITIN = new ArmorMaterial(24,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2, ArmorType.BODY, 5),
            12, SoundEvents.ARMOR_EQUIP_IRON, 0.5f, 0.0f,
            TagKey.create(Registries.ITEM, id("repairs_chitin")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("chitin")));

    public static final ArmorMaterial TITAN = new ArmorMaterial(40,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 11),
            15, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.5f, 0.1f,
            TagKey.create(Registries.ITEM, id("repairs_titan")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("titan")));

    public static Item.Properties props(String set, ArmorType t) {
        return new Item.Properties().humanoidArmor(set.equals("chitin") ? CHITIN : TITAN, t);
    }
}
