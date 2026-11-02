package com.armorstudio.config;

import com.armorstudio.config.Options.GlintMode;

/** Visual settings for one armor piece (or the elytra). Plain Gson POJO; always call {@link #validate()} after loading. */
public final class PieceStyle {
    public static final int DEFAULT_COLOR = 0x8B5CF6;

    public boolean visible = true;
    /** 0..100, 100 = fully opaque. */
    public int opacity = 100;
    public boolean colorEnabled = false;
    /** RGB without alpha. */
    public int color = DEFAULT_COLOR;
    /** 0..100, how strongly {@link #color} is applied. */
    public int colorStrength = 60;
    /** 20..100, multiplies the final color. */
    public int brightness = 100;
    public GlintMode glint = GlintMode.DEFAULT;
    public boolean showTrim = true;

    public PieceStyle copy() {
        PieceStyle c = new PieceStyle();
        c.copyFrom(this);
        return c;
    }

    public void copyFrom(PieceStyle o) {
        visible = o.visible;
        opacity = o.opacity;
        colorEnabled = o.colorEnabled;
        color = o.color;
        colorStrength = o.colorStrength;
        brightness = o.brightness;
        glint = o.glint;
        showTrim = o.showTrim;
    }

    /** Copies everything except visibility (used by "apply appearance to all pieces"). */
    public void copyAppearanceFrom(PieceStyle o) {
        boolean keepVisible = visible;
        copyFrom(o);
        visible = keepVisible;
    }

    public PieceStyle validate() {
        opacity = clamp(opacity, 0, 100);
        color &= 0xFFFFFF;
        colorStrength = clamp(colorStrength, 0, 100);
        brightness = clamp(brightness, 20, 100);
        if (glint == null) glint = GlintMode.DEFAULT;
        return this;
    }

    /** True if the piece renders exactly like vanilla. */
    public boolean isVanilla() {
        return visible && opacity >= 100 && !colorEnabled && brightness >= 100 && glint == GlintMode.DEFAULT && showTrim;
    }

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
