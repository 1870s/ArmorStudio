package com.armorstudio.gui.widget;

import com.armorstudio.gui.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A labelled setting: label and short description on the left, a control on the right and a reset button that
 * appears when the value differs from its default.
 */
public abstract class SettingRow extends Row {
    protected static final int RESET_SIZE = 12;

    protected final Component label;
    protected final Component description;
    private BooleanSupplier isDefault = () -> true;
    private Runnable reset;
    private BooleanSupplier enabled = () -> true;

    protected SettingRow(Component label, Component description) {
        this.label = label;
        this.description = description;
    }

    /** Adds a per-setting reset button. */
    public SettingRow resettable(BooleanSupplier isDefault, Runnable reset) {
        this.isDefault = isDefault;
        this.reset = reset;
        return this;
    }

    /** Greys the row out and ignores input while the condition is false. */
    public SettingRow enabledWhen(BooleanSupplier enabled) {
        this.enabled = enabled;
        return this;
    }

    protected boolean isEnabled() {
        return enabled.getAsBoolean();
    }

    @Override
    public int height() {
        return description == null ? 22 : 30;
    }

    protected int controlWidth() {
        return Math.max(70, Math.min(150, width * 2 / 5));
    }

    protected int controlX() {
        return x + width - 6 - controlWidth();
    }

    private int resetX() {
        return controlX() - RESET_SIZE - 5;
    }

    private int resetY() {
        return y + (height() - RESET_SIZE) / 2;
    }

    private boolean showReset() {
        return reset != null && isEnabled() && !isDefault.getAsBoolean();
    }

    @Override
    protected void extractRow(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt) {
        boolean on = isEnabled();
        card(g, x, y, width, height() - 2, Theme.lerp(Theme.CARD, Theme.CARD_HOVER, on ? hover : 0f));
        int textMax = (showReset() ? resetX() : controlX()) - x - 14;
        int labelY = description == null ? y + 6 : y + 5;
        g.text(font, trim(font, label, textMax), x + 8, labelY, on ? Theme.TEXT : Theme.TEXT_MUTED, false);
        if (description != null) {
            g.text(font, trim(font, description, textMax), x + 8, y + 16, on ? Theme.TEXT_DIM : Theme.TEXT_MUTED, false);
        }
        if (showReset()) {
            int rx = resetX(), ry = resetY();
            boolean hot = inside(mouseX, mouseY, rx, ry, RESET_SIZE, RESET_SIZE);
            card(g, rx, ry, RESET_SIZE, RESET_SIZE, hot ? Theme.ACCENT_DARK : Theme.FIELD);
            g.centeredText(font, "↺", rx + RESET_SIZE / 2 + 1, ry + 2, hot ? Theme.TEXT : Theme.ACCENT_LIGHT);
        }
        extractControl(g, font, mouseX, mouseY, dt, on);
    }

    protected abstract void extractControl(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt, boolean enabled);

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled() || !contains(mouseX, mouseY)) return false;
        if (showReset() && inside(mouseX, mouseY, resetX(), resetY(), RESET_SIZE, RESET_SIZE)) {
            reset.run();
            changed();
            return true;
        }
        return clickControl(mouseX, mouseY, button);
    }

    protected abstract boolean clickControl(double mouseX, double mouseY, int button);

    @Override
    public List<FormattedCharSequence> tooltip(Font font, int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) return null;
        if (showReset() && inside(mouseX, mouseY, resetX(), resetY(), RESET_SIZE, RESET_SIZE)) {
            return wrap(font, Component.translatable("armorstudio.ui.reset_setting"), 200);
        }
        if (mouseX >= controlX() || description == null) return null;
        List<FormattedCharSequence> lines = new ArrayList<>(wrap(font, label.copy().withColor(Theme.ACCENT_LIGHT), 240));
        lines.addAll(wrap(font, description, 240));
        return lines;
    }

    protected static FormattedCharSequence trim(Font font, Component text, int maxWidth) {
        return com.armorstudio.gui.Text.trim(font, text, maxWidth);
    }
}
