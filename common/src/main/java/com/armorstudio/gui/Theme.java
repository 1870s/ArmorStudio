package com.armorstudio.gui;

/** Colors and small color helpers shared by the settings screen, the HUD editor and the HUD. */
public final class Theme {
    public static final int BACKDROP = 0xB0050508;
    public static final int PANEL = 0xF2111118;
    public static final int SIDEBAR = 0xFF0C0C12;
    public static final int HEADER = 0xFF15151F;
    public static final int CARD = 0xFF17171F;
    public static final int CARD_HOVER = 0xFF1E1E2A;
    public static final int FIELD = 0xFF0E0E14;
    public static final int BORDER = 0xFF262635;
    public static final int BORDER_LIGHT = 0xFF34344A;

    public static final int ACCENT = 0xFF8B5CF6;
    public static final int ACCENT_LIGHT = 0xFFA78BFA;
    public static final int ACCENT_DARK = 0xFF5B3BB5;
    public static final int ACCENT_BG = 0x338B5CF6;

    public static final int TEXT = 0xFFECEAF4;
    public static final int TEXT_DIM = 0xFFA09EB4;
    public static final int TEXT_MUTED = 0xFF6E6C82;
    public static final int GOOD = 0xFF4ADE80;
    public static final int WARN = 0xFFFBBF24;
    public static final int BAD = 0xFFF87171;

    private Theme() {}

    /** Multiplies the alpha channel of an ARGB color by {@code factor}. */
    public static int withAlpha(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Linear interpolation between two ARGB colors. */
    public static int lerp(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** HSV (all 0..1) to opaque RGB. */
    public static int hsv(float h, float s, float v) {
        h = (h % 1f + 1f) % 1f;
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1f - s), q = v * (1f - f * s), t = v * (1f - (1f - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    /** RGB to {hue, saturation, value}, all 0..1. */
    public static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) / 6f;
        else if (max == g) h = ((b - r) / d + 2f) / 6f;
        else h = ((r - g) / d + 4f) / 6f;
        if (h < 0) h += 1f;
        return new float[] {h, max == 0 ? 0 : d / max, max};
    }

    /** Smooth approach used for UI animations; frame-rate independent. */
    public static float approach(float current, float target, float deltaSeconds, float speed) {
        float k = 1f - (float) Math.exp(-speed * deltaSeconds);
        float next = current + (target - current) * k;
        return Math.abs(next - target) < 0.001f ? target : next;
    }
}
