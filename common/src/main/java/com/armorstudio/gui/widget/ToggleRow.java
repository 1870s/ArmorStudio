package com.armorstudio.gui.widget;

import com.armorstudio.gui.Theme;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** An on/off switch. Clicking anywhere on the row toggles it. */
public class ToggleRow extends SettingRow {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private float knob = -1f;

    public ToggleRow(Component label, Component description, BooleanSupplier getter, Consumer<Boolean> setter, boolean defaultValue) {
        super(label, description);
        this.getter = getter;
        this.setter = setter;
        resettable(() -> getter.getAsBoolean() == defaultValue, () -> setter.accept(defaultValue));
    }

    @Override
    protected void extractControl(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt, boolean enabled) {
        boolean value = getter.getAsBoolean();
        if (knob < 0) knob = value ? 1f : 0f;
        knob = Theme.approach(knob, value ? 1f : 0f, dt, 22f);
        drawSwitch(g, x + width - 6 - 26, y + (height() - 14) / 2 - 1, knob, enabled, hover);
        String state = Component.translatable(value ? "armorstudio.ui.on" : "armorstudio.ui.off").getString();
        g.text(font, state, x + width - 6 - 26 - 6 - font.width(state), y + (height() - 9) / 2,
            value && enabled ? Theme.ACCENT_LIGHT : Theme.TEXT_MUTED, false);
    }

    /** Draws a 26x14 switch with the knob at position {@code t} (0 = off, 1 = on). */
    public static void drawSwitch(GuiGraphicsExtractor g, int sx, int sy, float t, boolean enabled, float hover) {
        int track = Theme.lerp(0xFF2A2A38, enabled ? Theme.ACCENT : Theme.ACCENT_DARK, t);
        card(g, sx, sy, 26, 14, track);
        int kx = sx + 2 + Math.round(t * 12);
        card(g, kx, sy + 2, 10, 10, enabled ? Theme.lerp(0xFFD8D6E4, 0xFFFFFFFF, hover) : 0xFF8A889A);
    }

    @Override
    protected boolean clickControl(double mouseX, double mouseY, int button) {
        setter.accept(!getter.getAsBoolean());
        changed();
        return true;
    }
}
