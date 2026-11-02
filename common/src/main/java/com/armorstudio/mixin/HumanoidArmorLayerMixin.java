package com.armorstudio.mixin;

import com.armorstudio.render.LookHolder;
import com.armorstudio.render.RenderContext;
import com.armorstudio.render.ResolvedLook;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the equipment renderer which piece style applies to the armor piece that is about to be submitted. */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void armorstudio$beginPiece(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot,
                                        int light, HumanoidRenderState state, CallbackInfo ci) {
        ResolvedLook look = ((LookHolder) state).armorstudio$getLook();
        RenderContext.beginPiece(look == null ? null : look.piece(slot), stack);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void armorstudio$endPiece(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot,
                                      int light, HumanoidRenderState state, CallbackInfo ci) {
        RenderContext.endPiece();
    }
}
