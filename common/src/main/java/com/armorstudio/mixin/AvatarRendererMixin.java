package com.armorstudio.mixin;

import com.armorstudio.render.ArmorStateEditor;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the configured look after vanilla filled in a player's render state. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void armorstudio$applyLook(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        ArmorStateEditor.apply(entity, state);
    }
}
