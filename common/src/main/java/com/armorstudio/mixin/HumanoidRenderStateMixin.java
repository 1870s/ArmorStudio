package com.armorstudio.mixin;

import com.armorstudio.render.LookHolder;
import com.armorstudio.render.ResolvedLook;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Stores the resolved look on the render state so render layers can read it. */
@Mixin(HumanoidRenderState.class)
public abstract class HumanoidRenderStateMixin implements LookHolder {
    @Unique
    private ResolvedLook armorstudio$look;

    @Override
    public ResolvedLook armorstudio$getLook() {
        return armorstudio$look;
    }

    @Override
    public void armorstudio$setLook(ResolvedLook look) {
        armorstudio$look = look;
    }
}
