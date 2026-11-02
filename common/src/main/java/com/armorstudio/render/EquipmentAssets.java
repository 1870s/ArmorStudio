package com.armorstudio.render;

import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Classifies worn items by the layers of their equipment asset. Vanilla's own check only looks at the slot, which
 * would treat an elytra like a chestplate.
 */
public final class EquipmentAssets {
    private static volatile EquipmentAssetManager manager;

    private EquipmentAssets() {}

    public static void set(EquipmentAssetManager assets) {
        manager = assets;
    }

    /** True if the item has an equipment asset with the given layer type. */
    public static boolean hasLayers(ItemStack stack, EquipmentClientInfo.LayerType type) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) return false;
        EquipmentAssetManager assets = manager;
        if (assets == null) return type != EquipmentClientInfo.LayerType.WINGS;
        return !assets.get(equippable.assetId().get()).getLayers(type).isEmpty();
    }

    public static boolean isWings(ItemStack stack) {
        return hasLayers(stack, EquipmentClientInfo.LayerType.WINGS);
    }
}
