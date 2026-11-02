package com.armorstudio.gui.widget;

import com.armorstudio.config.ConfigManager;
import com.armorstudio.gui.Theme;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * One entry of a settings page. Rows are laid out vertically by the screen; they read and write settings through
 * suppliers/consumers, so they never hold copies of config values and never need rebuilding when a value changes.
 */
public abstract class Row {
    protected int x, y, width;
    /** Hover animation progress 0..1. */
    protected float hover;

    public abstract int height();

    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int y() {
        return y;
    }

    public int bottom() {
        return y + height();
    }

    /** Draws the row. {@code dt} is the frame time in seconds, used for animations. */
    public final void extract(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt) {
        hover = Theme.approach(hover, contains(mouseX, mouseY) ? 1f : 0f, dt, 18f);
        extractRow(g, font, mouseX, mouseY, dt);
    }

    protected abstract void extractRow(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {}

    /** Tooltip lines for the given mouse position, or null. */
    public List<FormattedCharSequence> tooltip(Font font, int mouseX, int mouseY) {
        return null;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height();
    }

    protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Every setting change goes through here: render caches update immediately, the file is saved shortly after. */
    protected static void changed() {
        ConfigManager.markDirty();
    }

    protected static List<FormattedCharSequence> wrap(Font font, Component text, int width) {
        return font.split(text, width);
    }

    /** Draws a rounded-looking card background (1 px inset corners). */
    protected static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
