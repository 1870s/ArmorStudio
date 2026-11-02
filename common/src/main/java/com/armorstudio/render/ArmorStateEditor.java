package com.armorstudio.render;

import com.armorstudio.ArmorStudio;
import com.armorstudio.config.ArmorSlot;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Applies a look to a freshly extracted player render state.
 *
 * <p>Hidden pieces are removed from the <em>render state</em>, which is a per-frame snapshot that only the renderer
 * reads. The player's real equipment, armor points, enchantments and everything the server knows are never touched.
 * Removing the stack from the snapshot (instead of cancelling one specific layer) also hides armor drawn by other
 * mods' custom armor renderers, and lets the cape render as it would without a chestplate.
 */
public final class ArmorStateEditor {
    private static boolean loggedError;

    private ArmorStateEditor() {}

    public static void apply(Avatar entity, AvatarRenderState state) {
        try {
            ResolvedLook look = LookResolver.forAvatar(entity);
            ((LookHolder) state).armorstudio$setLook(look);
            if (look != null) applyLook(look, state);
        } catch (RuntimeException e) {
            if (!loggedError) {
                loggedError = true;
                ArmorStudio.LOGGER.error("Armor Studio could not apply a look; this player renders like vanilla", e);
            }
        }
    }

    /** Removes hidden pieces from the state. Public so the settings preview can re-apply after adding sample armor. */
    public static void applyLook(ResolvedLook look, AvatarRenderState state) {
        if (look.isHidden(ArmorSlot.HEAD) && isArmor(state.headEquipment, EquipmentSlot.HEAD)) {
            state.headEquipment = ItemStack.EMPTY;
        }
        ItemStack chest = state.chestEquipment;
        if (!chest.isEmpty()) {
            boolean wings = EquipmentAssets.isWings(chest);
            boolean armor = !wings || EquipmentAssets.hasLayers(chest, EquipmentClientInfo.LayerType.HUMANOID);
            boolean hideArmor = !armor || look.isHidden(ArmorSlot.CHEST);
            boolean hideWings = !wings || look.isElytraHidden();
            if (hideArmor && hideWings) state.chestEquipment = ItemStack.EMPTY;
        }
        if (look.isHidden(ArmorSlot.LEGS) && isArmor(state.legsEquipment, EquipmentSlot.LEGS)) {
            state.legsEquipment = ItemStack.EMPTY;
        }
        if (look.isHidden(ArmorSlot.FEET) && isArmor(state.feetEquipment, EquipmentSlot.FEET)) {
            state.feetEquipment = ItemStack.EMPTY;
        }
        if (look.hideHeadItems) {
            state.headItem.clear();
            state.wornHeadType = null;
            state.wornHeadProfile = null;
        }
    }

    /**
     * Armor (vanilla or modded) worn in an armor slot. Non-armor head items (pumpkins, skulls) are rendered from a
     * separate field and handled by the head-item option. Modded armor drawn by a custom renderer may have no vanilla
     * layers but still has an equipment asset, so it counts as armor too.
     */
    private static boolean isArmor(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return false;
        if (slot != EquipmentSlot.HEAD) return true;
        return HumanoidArmorLayer.shouldRender(stack, slot) || !isHeadItem(stack);
    }

    /** Equippable head items without an equipment asset (carved pumpkin) are drawn as head items, not armor. */
    private static boolean isHeadItem(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable == null || equippable.assetId().isEmpty();
    }
}
