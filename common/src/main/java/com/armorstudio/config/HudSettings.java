package com.armorstudio.config;

import com.armorstudio.config.Options.DurabilityText;
import com.armorstudio.config.Options.HudAnchor;
import com.armorstudio.config.Options.HudLayout;

/** Settings of the armor durability HUD. */
public final class HudSettings {
    public boolean enabled = true;
    public HudAnchor anchor = HudAnchor.HOTBAR_RIGHT;
    /** Offset from the anchor in GUI pixels; positive values move towards the screen center. */
    public int offsetX = 4;
    public int offsetY = 0;
    /** 50..200 percent. */
    public int scale = 100;
    /** 0..100 percent background opacity. */
    public int backgroundOpacity = 35;
    public HudLayout layout = HudLayout.VERTICAL;
    public DurabilityText text = DurabilityText.PERCENT;
    public boolean colorText = true;
    public boolean showEmptySlots = false;
    public boolean hideWhenNoArmor = true;
    public boolean showEnchantMarker = true;
    /** Durability percent at or below which a piece counts as "low". 0 disables warnings. */
    public int lowThreshold = 15;
    public boolean blinkWhenLow = true;
    public boolean warnSound = true;
    public boolean warnMessage = true;

    public HudSettings validate() {
        if (anchor == null) anchor = HudAnchor.HOTBAR_RIGHT;
        if (layout == null) layout = HudLayout.VERTICAL;
        if (text == null) text = DurabilityText.PERCENT;
        offsetX = PieceStyle.clamp(offsetX, -4000, 4000);
        offsetY = PieceStyle.clamp(offsetY, -4000, 4000);
        scale = PieceStyle.clamp(scale, 50, 200);
        backgroundOpacity = PieceStyle.clamp(backgroundOpacity, 0, 100);
        lowThreshold = PieceStyle.clamp(lowThreshold, 0, 50);
        return this;
    }
}
