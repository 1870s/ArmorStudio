package com.armorstudio.gui.widget;

import com.armorstudio.gui.Theme;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Wrapped informational text, e.g. technical limitations. The text may change at runtime. */
public class NoteRow extends Row {
    private final Supplier<Component> text;
    private final int color;
    private List<FormattedCharSequence> lines = List.of();
    private Component lastText;
    private int lastWidth = -1;
    private final Font font;

    public NoteRow(Font font, Component text, int color) {
        this(font, () -> text, color);
    }

    public NoteRow(Font font, Supplier<Component> text, int color) {
        this.font = font;
        this.text = text;
        this.color = color;
    }

    private void layout() {
        Component current = text.get();
        if (width != lastWidth || !current.equals(lastText)) {
            lastWidth = width;
            lastText = current;
            lines = font.split(current, Math.max(40, width - 16));
        }
    }

    @Override
    public int height() {
        layout();
        return lines.size() * 10 + 8;
    }

    @Override
    protected void extractRow(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt) {
        layout();
        int h = height() - 2;
        card(g, x, y, width, h, 0xFF13131A);
        g.fill(x, y + 1, x + 2, y + h - 1, color);
        for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), x + 9, y + 4 + i * 10, Theme.TEXT_DIM, false);
    }
}
