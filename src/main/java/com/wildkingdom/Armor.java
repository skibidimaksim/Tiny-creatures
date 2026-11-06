package com.wildkingdom;

import java.util.LinkedHashMap;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Armor {
    private Armor() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WildKingdom.MODID);
    public static final Map<String, DeferredItem<Item>> PIECES = new LinkedHashMap<>();

    private static ArmorMaterial mat(String n, int dur, int boots, int legs, int chest, int helm, int ench, float tough, float kb) {
        Identifier id = Identifier.fromNamespaceAndPath(WildKingdom.MODID, n);
        return new ArmorMaterial(dur,
                Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, legs, ArmorType.CHESTPLATE, chest, ArmorType.HELMET, helm, ArmorType.BODY, chest),
                ench, SoundEvents.ARMOR_EQUIP_LEATHER, tough, kb,
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(WildKingdom.MODID, n + "_repair")),
                ResourceKey.create(EquipmentAssets.ROOT_ID, id));
    }

    static {
        ArmorMaterial hide = mat("hide", 20, 2, 5, 6, 2, 12, 0f, 0f);
        ArmorMaterial chitin = mat("chitin", 28, 3, 6, 7, 3, 14, 1f, 0f);
        ArmorMaterial titan = mat("titan", 40, 3, 6, 8, 3, 15, 3f, 0.1f);
        add("hide", hide);
        add("chitin", chitin);
        add("titan", titan);
    }

    private static void add(String n, ArmorMaterial m) {
        PIECES.put(n + "_helmet", ITEMS.registerItem(n + "_helmet", p -> new ArmorGeoItem(p, n), () -> new Item.Properties().humanoidArmor(m, ArmorType.HELMET)));
        PIECES.put(n + "_chestplate", ITEMS.registerItem(n + "_chestplate", p -> new ArmorGeoItem(p, n), () -> new Item.Properties().humanoidArmor(m, ArmorType.CHESTPLATE)));
        PIECES.put(n + "_leggings", ITEMS.registerItem(n + "_leggings", p -> new ArmorGeoItem(p, n), () -> new Item.Properties().humanoidArmor(m, ArmorType.LEGGINGS)));
        PIECES.put(n + "_boots", ITEMS.registerItem(n + "_boots", p -> new ArmorGeoItem(p, n), () -> new Item.Properties().humanoidArmor(m, ArmorType.BOOTS)));
    }

    public static void init(IEventBus bus) { ITEMS.register(bus); }
}
