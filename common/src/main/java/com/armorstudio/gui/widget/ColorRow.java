package com.armorstudio.gui.widget;

import com.armorstudio.gui.Theme;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Color picker: preset swatches, a hue bar and a shade bar (white over the pure hue to black). */
public class ColorRow extends SettingRow {
    private static final int[] SWATCHES = {
        0x8B5CF6, 0xEC4899, 0xEF4444, 0xF97316, 0xEAB308, 0x22C55E,
        0x14B8A6, 0x3B82F6, 0x1E3A8A, 0xFFFFFF, 0x9CA3AF, 0x1F1F1F
    };
    private final IntSupplier getter;
    private final IntConsumer setter;
    private int dragging = -1;

    public ColorRow(Component label, Component description, IntSupplier getter, IntConsumer setter, int defaultValue) {
        super(label, description);
        this.getter = getter;
        this.setter = setter;
        resettable(() -> getter.getAsInt() == defaultValue, () -> setter.accept(defaultValue));
    }

    @Override
    public int height() {
        return 52;
    }

    @Override
    protected int controlWidth() {
        return Math.max(110, Math.min(170, width / 2));
    }

    private int swatchWidth() {
        return Math.max(6, (controlWidth() - (SWATCHES.length - 1) * 2) / SWATCHES.length);
    }

    @Override
    protected void extractControl(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt, boolean enabled) {
        int rgb = getter.getAsInt();
        int cx = controlX(), cw = controlWidth(), sw = swatchWidth();
        for (int i = 0; i < SWATCHES.length; i++) {
            int sx = cx + i * (sw + 2), sy = y + 5;
            boolean selected = (rgb & 0xFFFFFF) == SWATCHES[i];
            int frame = selected ? 0xFFFFFFFF : inside(mouseX, mouseY, sx, sy, sw, 10) ? Theme.ACCENT_LIGHT : 0xFF000000;
            g.fill(sx - 1, sy - 1, sx + sw + 1, sy + 11, frame);
            g.fill(sx, sy, sx + sw, sy + 10, 0xFF000000 | SWATCHES[i]);
        }
        float[] hsv = Theme.toHsv(rgb);
        int hy = y + 21;
        for (int i = 0; i < cw; i++) g.fill(cx + i, hy, cx + i + 1, hy + 8, Theme.hsv(i / (float) cw, 1f, 1f));
        int hk = cx + Math.round(hsv[0] * (cw - 1));
        g.fill(hk - 1, hy - 2, hk + 2, hy + 10, 0xFFFFFFFF);
        int by = y + 35;
        for (int i = 0; i < cw; i++) g.fill(cx + i, by, cx + i + 1, by + 8, shadeColor(hsv[0], i / (float) (cw - 1)));
        int bk = cx + Math.round(shadeOf(hsv) * (cw - 1));
        g.fill(bk - 1, by - 2, bk + 2, by + 10, 0xFFFFFFFF);
        int px = x + 8, py = y + 32;
        g.fill(px - 1, py - 1, px + 23, py + 13, Theme.BORDER_LIGHT);
        g.fill(px, py, px + 22, py + 12, 0xFF000000 | rgb);
        g.text(font, String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF), px + 28, py + 2, enabled ? Theme.TEXT_DIM : Theme.TEXT_MUTED, false);
        if (!enabled) g.fill(cx - 2, y + 2, cx + cw + 2, y + 46, 0xB0101018);
    }

    /** Position 0..1 on the shade bar: 0 = white, 0.5 = full color, 1 = near black. */
    private static int shadeColor(float hue, float t) {
        return t < 0.5f ? Theme.hsv(hue, t * 2f, 1f) : Theme.hsv(hue, 1f, 1f - (t - 0.5f) * 1.8f);
    }

    private static float shadeOf(float[] hsv) {
        return hsv[2] < 0.999f ? 0.5f + (1f - hsv[2]) / 1.8f : hsv[1] * 0.5f;
    }

    @Override
    protected boolean clickControl(double mouseX, double mouseY, int button) {
        int cx = controlX(), cw = controlWidth(), sw = swatchWidth();
        for (int i = 0; i < SWATCHES.length; i++) {
            if (inside(mouseX, mouseY, cx + i * (sw + 2) - 1, y + 4, sw + 2, 12)) {
                setter.accept(SWATCHES[i]);
                changed();
                return true;
            }
        }
        if (inside(mouseX, mouseY, cx - 2, y + 18, cw + 4, 14)) dragging = 0;
        else if (inside(mouseX, mouseY, cx - 2, y + 32, cw + 4, 14)) dragging = 1;
        else return false;
        apply(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging < 0) return false;
        apply(mouseX);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = -1;
    }

    private void apply(double mouseX) {
        float t = Mth.clamp((float) (mouseX - controlX()) / (controlWidth() - 1), 0f, 1f);
        float[] hsv = Theme.toHsv(getter.getAsInt());
        int rgb;
        if (dragging == 0) {
            // Picking a hue from white/gray would show nothing, so jump to a saturated color in that case.
            float s = hsv[1] < 0.05f ? 1f : hsv[1];
            rgb = Theme.hsv(t, s, Math.max(hsv[2], 0.15f));
        } else {
            rgb = shadeColor(hsv[0], t);
        }
        setter.accept(rgb & 0xFFFFFF);
        changed();
    }
}
