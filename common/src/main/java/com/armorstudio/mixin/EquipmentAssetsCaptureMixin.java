package com.armorstudio.mixin;

import com.armorstudio.render.EquipmentAssets;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers the client's equipment asset manager so armor and wings can be told apart by their actual layers. */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentAssetsCaptureMixin {
    @Shadow
    @Final
    private EquipmentAssetManager equipmentAssets;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void armorstudio$capture(CallbackInfo ci) {
        EquipmentAssets.set(equipmentAssets);
    }
}
