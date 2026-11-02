package com.armorstudio.gui.widget;

import com.armorstudio.gui.Theme;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** An integer slider; drag the knob or click anywhere on the track. */
public class SliderRow extends SettingRow {
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int min, max, step;
    private final IntFunction<String> format;
    private boolean dragging;
    private float shown = Float.NaN;

    public SliderRow(Component label, Component description, IntSupplier getter, IntConsumer setter, int min, int max, int step,
                     int defaultValue, IntFunction<String> format) {
        super(label, description);
        this.getter = getter;
        this.setter = setter;
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.format = format;
        resettable(() -> getter.getAsInt() == defaultValue, () -> setter.accept(defaultValue));
    }

    public static IntFunction<String> percent() {
        return v -> v + "%";
    }

    private int trackX() {
        return controlX() + 34;
    }

    private int trackW() {
        return controlWidth() - 34;
    }

    @Override
    protected void extractControl(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt, boolean enabled) {
        int value = getter.getAsInt();
        float target = (value - min) / (float) (max - min);
        shown = Float.isNaN(shown) || dragging ? target : Theme.approach(shown, target, dt, 20f);
        String text = format.apply(value);
        g.text(font, text, controlX() + 30 - font.width(text), y + (height() - 9) / 2, enabled ? Theme.TEXT : Theme.TEXT_MUTED, false);
        int tx = trackX(), tw = trackW(), ty = y + height() / 2 - 2;
        card(g, tx, ty, tw, 4, 0xFF2A2A38);
        int fill = Math.round(shown * (tw - 2));
        if (fill > 0) card(g, tx, ty, fill + 2, 4, enabled ? Theme.ACCENT : Theme.ACCENT_DARK);
        int kx = tx + fill - 3;
        boolean hot = dragging || inside(mouseX, mouseY, tx - 4, y, tw + 8, height());
        card(g, kx, ty - 3, 8, 10, enabled ? (hot ? 0xFFFFFFFF : 0xFFD8D6E4) : 0xFF8A889A);
    }

    @Override
    protected boolean clickControl(double mouseX, double mouseY, int button) {
        if (mouseX < trackX() - 4) return false;
        dragging = true;
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging) return false;
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
    }

    private void setFromMouse(double mouseX) {
        float t = Mth.clamp((float) (mouseX - trackX()) / trackW(), 0f, 1f);
        int value = min + Math.round(t * (max - min) / step) * step;
        value = Mth.clamp(value, min, max);
        if (value != getter.getAsInt()) {
            setter.accept(value);
            changed();
        }
    }
}
