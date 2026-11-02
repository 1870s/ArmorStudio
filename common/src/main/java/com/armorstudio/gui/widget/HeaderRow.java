package com.armorstudio.gui.widget;

import com.armorstudio.gui.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Section title with an accent underline. */
public class HeaderRow extends Row {
    private final Component title;

    public HeaderRow(Component title) {
        this.title = title;
    }

    @Override
    public int height() {
        return 20;
    }

    @Override
    protected void extractRow(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt) {
        g.text(font, title, x + 2, y + 7, Theme.ACCENT_LIGHT, false);
        int lineX = x + font.width(title) + 8;
        if (lineX < x + width) g.fill(lineX, y + 11, x + width, y + 12, Theme.BORDER);
    }
}
