package com.armorstudio.gui.widget;

import com.armorstudio.gui.Text;
import com.armorstudio.gui.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

/** A row of evenly sized action buttons. Destructive buttons can require a confirming second click. */
public class ButtonRow extends Row {
    private static final int BUTTON_H = 18;
    private static final long CONFIRM_MS = 3000;

    private final List<Button> buttons = new ArrayList<>();

    public ButtonRow add(Component label, Component tooltip, Runnable action) {
        return add(() -> label, tooltip, action, () -> false, () -> true, false);
    }

    public ButtonRow add(Supplier<Component> label, Component tooltip, Runnable action, BooleanSupplier highlighted,
                         BooleanSupplier enabled, boolean confirm) {
        buttons.add(new Button(label, tooltip, action, highlighted, enabled, confirm));
        return this;
    }

    @Override
    public int height() {
        return BUTTON_H + 4;
    }

    private int buttonWidth() {
        return (width - (buttons.size() - 1) * 4) / Math.max(1, buttons.size());
    }

    private int buttonX(int index) {
        return x + index * (buttonWidth() + 4);
    }

    @Override
    protected void extractRow(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt) {
        int bw = buttonWidth();
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            int bx = buttonX(i);
            boolean enabled = b.enabled.getAsBoolean();
            boolean hot = enabled && inside(mouseX, mouseY, bx, y, bw, BUTTON_H);
            b.hover = Theme.approach(b.hover, hot ? 1f : 0f, dt, 18f);
            boolean armed = b.isArmed();
            int base = armed ? 0xFF7F1D1D : b.highlighted.getAsBoolean() ? Theme.ACCENT_DARK : Theme.CARD;
            int top = armed ? 0xFFB91C1C : b.highlighted.getAsBoolean() ? Theme.ACCENT : Theme.CARD_HOVER;
            card(g, bx, y, bw, BUTTON_H, enabled ? Theme.lerp(base, top, b.hover) : 0xFF141419);
            if (b.highlighted.getAsBoolean() && !armed) g.fill(bx + 2, y + BUTTON_H - 1, bx + bw - 2, y + BUTTON_H, Theme.ACCENT_LIGHT);
            Component label = armed ? Component.translatable("armorstudio.ui.confirm") : b.label.get();
            g.centeredText(font, Text.trim(font, label, bw - 6), bx + bw / 2, y + 5, enabled ? Theme.TEXT : Theme.TEXT_MUTED);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bw = buttonWidth();
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            if (!inside(mouseX, mouseY, buttonX(i), y, bw, BUTTON_H) || !b.enabled.getAsBoolean()) continue;
            if (b.confirm && !b.isArmed()) {
                b.armedAt = Util.getMillis();
            } else {
                b.armedAt = 0;
                b.action.run();
            }
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            return true;
        }
        return false;
    }

    @Override
    public List<FormattedCharSequence> tooltip(Font font, int mouseX, int mouseY) {
        int bw = buttonWidth();
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            if (b.tooltip != null && inside(mouseX, mouseY, buttonX(i), y, bw, BUTTON_H)) return wrap(font, b.tooltip, 220);
        }
        return null;
    }

    private static final class Button {
        final Supplier<Component> label;
        final Component tooltip;
        final Runnable action;
        final BooleanSupplier highlighted;
        final BooleanSupplier enabled;
        final boolean confirm;
        float hover;
        long armedAt;

        Button(Supplier<Component> label, Component tooltip, Runnable action, BooleanSupplier highlighted, BooleanSupplier enabled,
               boolean confirm) {
            this.label = label;
            this.tooltip = tooltip;
            this.action = action;
            this.highlighted = highlighted;
            this.enabled = enabled;
            this.confirm = confirm;
        }

        boolean isArmed() {
            return armedAt != 0 && Util.getMillis() - armedAt < CONFIRM_MS;
        }
    }
}
