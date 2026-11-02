package com.armorstudio.neoforge.mixin;

import com.armorstudio.render.RenderContext;
import com.armorstudio.render.ResolvedPiece;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Applies tint, brightness and opacity to each armor texture layer. NeoForge replaces the vanilla color lookup with
 * {@link IClientItemExtensions#getArmorLayerTintColor}, so this wraps that call; modded items keep their own tint and
 * Armor Studio's styling is applied on top.
 */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentColorMixin {
    @WrapOperation(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;"
            + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;getArmorLayerTintColor(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;II)I"))
    private int armorstudio$color(IClientItemExtensions extensions, ItemStack stack, EquipmentClientInfo.Layer layer, int index, int dyeColor,
                                  Operation<Integer> original) {
        int color = original.call(extensions, stack, layer, index, dyeColor);
        ResolvedPiece piece = RenderContext.active();
        return piece == null ? color : piece.color(color, layer.dyeable().isPresent());
    }
}
