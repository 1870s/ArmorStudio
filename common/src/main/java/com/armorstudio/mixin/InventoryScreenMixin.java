package com.armorstudio.mixin;

import com.armorstudio.render.RenderContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks render states extracted for the inventory (and creative inventory) player preview. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
        at = @At("HEAD"))
    private static void armorstudio$enter(LivingEntity entity, CallbackInfoReturnable<EntityRenderState> cir) {
        RenderContext.enterInventoryPreview();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
        at = @At("RETURN"))
    private static void armorstudio$exit(LivingEntity entity, CallbackInfoReturnable<EntityRenderState> cir) {
        RenderContext.exitInventoryPreview();
    }
}
