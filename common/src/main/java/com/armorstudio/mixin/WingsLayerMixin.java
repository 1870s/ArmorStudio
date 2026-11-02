package com.armorstudio.mixin;

import com.armorstudio.render.LookHolder;
import com.armorstudio.render.RenderContext;
import com.armorstudio.render.ResolvedLook;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Elytra styling (opacity, color, glint) independent from the chestplate. Hiding is done on the render state. */
@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin {
    private static final String SUBMIT =
        "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V";

    @Inject(method = SUBMIT, at = @At("HEAD"))
    private void armorstudio$beginWings(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState state,
                                        float yRot, float xRot, CallbackInfo ci) {
        ResolvedLook look = ((LookHolder) state).armorstudio$getLook();
        RenderContext.beginPiece(look == null ? null : look.elytra, state.chestEquipment);
    }

    @Inject(method = SUBMIT, at = @At("RETURN"))
    private void armorstudio$endWings(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState state,
                                      float yRot, float xRot, CallbackInfo ci) {
        RenderContext.endPiece();
    }
}
