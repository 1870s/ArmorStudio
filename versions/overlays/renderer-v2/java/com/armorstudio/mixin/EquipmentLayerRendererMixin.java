package com.armorstudio.mixin;

import com.armorstudio.render.RenderContext;
import com.armorstudio.render.ResolvedPiece;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renderer "v2" (Minecraft 26.3+): trims are paletted textures, the glint is baked into the armor render type when the
 * piece has no trim, and the dedicated translucent armor render type is gone, so the generic translucent entity type
 * is used instead. The layer color hook lives in the loader modules (NeoForge replaces that call). Every hook is a no-op unless {@link RenderContext#active()} is set (player armor/elytra only).
 */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {
    private static final String RENDER_LAYERS =
        "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;"
            + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

    @Inject(method = RENDER_LAYERS, at = @At("HEAD"))
    private void armorstudio$enter(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> asset, Model<?> model,
                                   Object state, ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector, int light,
                                   Identifier playerTexture, int outlineColor, int order, CallbackInfo ci) {
        RenderContext.enterEquipment(stack);
    }

    @Inject(method = RENDER_LAYERS, at = @At("RETURN"))
    private void armorstudio$exit(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> asset, Model<?> model,
                                  Object state, ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector, int light,
                                  Identifier playerTexture, int outlineColor, int order, CallbackInfo ci) {
        RenderContext.exitEquipment();
    }

    @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"))
    private boolean armorstudio$glint(ItemStack stack, Operation<Boolean> original) {
        ResolvedPiece piece = RenderContext.active();
        boolean vanilla = original.call(stack);
        return piece == null ? vanilla : piece.glint(vanilla);
    }

    @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType armorstudio$layerType(Identifier texture, Operation<RenderType> original) {
        ResolvedPiece piece = RenderContext.active();
        return piece != null && piece.translucent ? RenderTypes.entityTranslucent(texture) : original.call(texture);
    }

    /** The glint-baked armor type has no translucent variant; translucent pieces are drawn without glint. */
    @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCullGlint(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType armorstudio$glintLayerType(Identifier texture, Operation<RenderType> original) {
        ResolvedPiece piece = RenderContext.active();
        return piece != null && piece.translucent ? RenderTypes.entityTranslucent(texture) : original.call(texture);
    }

    @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object armorstudio$trim(ItemStack stack, DataComponentType<?> type, Operation<Object> original) {
        ResolvedPiece piece = RenderContext.active();
        if (piece != null && !piece.showTrim && type == DataComponents.TRIM) return null;
        return original.call(stack, type);
    }

    @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorTrim(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType armorstudio$trimType(Identifier texture, boolean decal, Operation<RenderType> original) {
        ResolvedPiece piece = RenderContext.active();
        return piece != null && piece.translucent ? RenderTypes.entityTranslucent(texture) : original.call(texture, decal);
    }

    /** The trim overlay is submitted with a fixed white color; give it the piece's opacity and brightness. */
    @ModifyArg(method = RENDER_LAYERS, index = 6, at = @At(value = "INVOKE", ordinal = 1,
        target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"))
    private int armorstudio$trimColor(int color) {
        ResolvedPiece piece = RenderContext.active();
        return piece == null ? color : piece.trimColor();
    }
}
