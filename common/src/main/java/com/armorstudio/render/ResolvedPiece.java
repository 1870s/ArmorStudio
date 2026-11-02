package com.armorstudio.render;

import com.armorstudio.config.Options.GlintMode;
import com.armorstudio.config.Options.Translucency;
import com.armorstudio.config.PieceStyle;

/**
 * Render-ready form of a {@link PieceStyle}: everything that can be precomputed is computed once when the settings
 * change, so the per-frame work is a handful of float multiplications and no allocations.
 */
public final class ResolvedPiece {
    public static final ResolvedPiece HIDDEN = new ResolvedPiece(true, false, 255, false, 1f, 1f, 1f, 0f, 1f, GlintMode.DEFAULT, true);

    public final boolean hidden;
    /** Use the translucent render type (alpha below 255 in SMOOTH mode). */
    public final boolean translucent;
    public final int alpha;
    private final boolean colorize;
    private final float tintR, tintG, tintB, strength, brightness;
    private final GlintMode glint;
    public final boolean showTrim;
    private final boolean changesColor;

    private ResolvedPiece(boolean hidden, boolean translucent, int alpha, boolean colorize, float tintR, float tintG, float tintB,
                          float strength, float brightness, GlintMode glint, boolean showTrim) {
        this.hidden = hidden;
        this.translucent = translucent;
        this.alpha = alpha;
        this.colorize = colorize;
        this.tintR = tintR;
        this.tintG = tintG;
        this.tintB = tintB;
        this.strength = strength;
        this.brightness = brightness;
        this.glint = glint;
        this.showTrim = showTrim;
        this.changesColor = colorize || brightness < 1f || alpha < 255;
    }

    /** Returns null when the style is identical to vanilla (so callers can skip all work). */
    public static ResolvedPiece of(PieceStyle style, Translucency mode, boolean forceHidden) {
        if (forceHidden || !style.visible || style.opacity <= 0) return HIDDEN;
        if (mode == Translucency.THRESHOLD && style.opacity < 50) return HIDDEN;
        if (style.isVanilla()) return null;
        boolean translucent = mode == Translucency.SMOOTH && style.opacity < 100;
        int alpha = translucent ? Math.round(style.opacity * 2.55f) : 255;
        return new ResolvedPiece(false, translucent, alpha, style.colorEnabled,
            ((style.color >> 16) & 0xFF) / 255f, ((style.color >> 8) & 0xFF) / 255f, (style.color & 0xFF) / 255f,
            style.colorStrength / 100f, style.brightness / 100f, style.glint, style.showTrim);
    }

    /** Whether the enchantment glint is drawn. Glint on see-through armor looks like a glitch, so DEFAULT hides it there. */
    public boolean glint(boolean vanillaGlint) {
        return switch (glint) {
            case HIDE -> false;
            case FORCE -> true;
            case DEFAULT -> vanillaGlint && !translucent;
        };
    }

    /**
     * Applies tint, brightness and opacity to the color vanilla computed for one texture layer.
     *
     * @param layerColor ARGB color from vanilla; 0 means "skip this layer" and is preserved
     * @param dyeable    true for dyeable layers (leather): the tint replaces the dye instead of multiplying the texture
     */
    public int color(int layerColor, boolean dyeable) {
        if (layerColor == 0 || !changesColor) return layerColor;
        float r = ((layerColor >> 16) & 0xFF) / 255f;
        float g = ((layerColor >> 8) & 0xFF) / 255f;
        float b = (layerColor & 0xFF) / 255f;
        if (colorize) {
            if (dyeable) {
                // Leather textures are grayscale and colored entirely by the dye, so blend the dye towards the tint.
                r += (tintR - r) * strength;
                g += (tintG - g) * strength;
                b += (tintB - b) * strength;
            } else {
                // Other materials have colored textures; the vertex color can only multiply them.
                r *= 1f + (tintR - 1f) * strength;
                g *= 1f + (tintG - 1f) * strength;
                b *= 1f + (tintB - 1f) * strength;
            }
        }
        r *= brightness;
        g *= brightness;
        b *= brightness;
        int a = ((layerColor >>> 24) * alpha) / 255;
        return (a << 24) | (to255(r) << 16) | (to255(g) << 8) | to255(b);
    }

    /** Color for the trim overlay (vanilla draws it white and opaque). */
    public int trimColor() {
        int c = to255(brightness);
        return (alpha << 24) | (c << 16) | (c << 8) | c;
    }

    private static int to255(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255f)));
    }
}
