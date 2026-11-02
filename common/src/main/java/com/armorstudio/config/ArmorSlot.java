package com.armorstudio.config;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** The four humanoid armor slots, in the order they are shown everywhere in the UI. */
public enum ArmorSlot {
    HEAD(EquipmentSlot.HEAD, "helmet"),
    CHEST(EquipmentSlot.CHEST, "chestplate"),
    LEGS(EquipmentSlot.LEGS, "leggings"),
    FEET(EquipmentSlot.FEET, "boots");

    private static final ArmorSlot[] VALUES = values();

    public final EquipmentSlot equipmentSlot;
    public final String key;

    ArmorSlot(EquipmentSlot equipmentSlot, String key) {
        this.equipmentSlot = equipmentSlot;
        this.key = key;
    }

    public Component displayName() {
        return Component.translatable("armorstudio.piece." + key);
    }

    /** A representative item for icons when the slot is empty. Resolved lazily, never during class init. */
    public Item iconItem() {
        return switch (this) {
            case HEAD -> Items.NETHERITE_HELMET;
            case CHEST -> Items.NETHERITE_CHESTPLATE;
            case LEGS -> Items.NETHERITE_LEGGINGS;
            case FEET -> Items.NETHERITE_BOOTS;
        };
    }

    public static ArmorSlot[] all() {
        return VALUES;
    }

    public static ArmorSlot fromEquipmentSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> HEAD;
            case CHEST -> CHEST;
            case LEGS -> LEGS;
            case FEET -> FEET;
            default -> null;
        };
    }
}
