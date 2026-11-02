package com.armorstudio.render;

/** Implemented on HumanoidRenderState by a mixin: carries the resolved look from extraction to the render layers. */
public interface LookHolder {
    ResolvedLook armorstudio$getLook();

    void armorstudio$setLook(ResolvedLook look);
}
