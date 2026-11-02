package com.armorstudio.fabric.mixin;

import com.armorstudio.render.RenderContext;
import com.armorstudio.render.ResolvedPiece;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies tint, brightness and opacity to each armor texture layer (vanilla color lookup, used on Fabric). */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentColorMixin {
    @WrapOperation(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;"
            + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I"))
    private int armorstudio$color(EquipmentClientInfo.Layer layer, int dyeColor, Operation<Integer> original) {
        int color = original.call(layer, dyeColor);
        ResolvedPiece piece = RenderContext.active();
        return piece == null ? color : piece.color(color, layer.dyeable().isPresent());
    }
}
