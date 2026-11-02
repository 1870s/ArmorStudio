package com.armorstudio.gui.widget;

import com.armorstudio.gui.Text;
import com.armorstudio.gui.Theme;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Shows one key mapping, its current key and whether another mapping uses the same key. */
public class KeyRow extends Row {
    private final KeyMapping mapping;
    private final Component description;

    public KeyRow(KeyMapping mapping, Component description) {
        this.mapping = mapping;
        this.description = description;
    }

    @Override
    public int height() {
        return 30;
    }

    /** The first other key mapping bound to the same key, or null. */
    private KeyMapping conflict() {
        if (mapping.isUnbound()) return null;
        for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
            if (other != mapping && other.same(mapping)) return other;
        }
        return null;
    }

    @Override
    protected void extractRow(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt) {
        card(g, x, y, width, height() - 2, Theme.lerp(Theme.CARD, Theme.CARD_HOVER, hover));
        Component key = mapping.isUnbound() ? Component.translatable("armorstudio.keys.unbound") : mapping.getTranslatedKeyMessage();
        int kw = Math.max(40, font.width(key) + 12);
        int kx = x + width - 6 - kw;
        KeyMapping conflict = conflict();
        card(g, kx, y + 6, kw, 16, conflict != null ? 0xFF7F1D1D : mapping.isUnbound() ? Theme.FIELD : Theme.ACCENT_DARK);
        g.centeredText(font, key, kx + kw / 2, y + 10, mapping.isUnbound() ? Theme.TEXT_MUTED : Theme.TEXT);
        int textMax = kx - x - 14;
        g.text(font, Text.trim(font, Component.translatable(mapping.getName()), textMax), x + 8, y + 5, Theme.TEXT, false);
        Component second = conflict != null
            ? Component.translatable("armorstudio.keys.conflict", Component.translatable(conflict.getName())).withColor(Theme.WARN)
            : description;
        g.text(font, Text.trim(font, second, textMax), x + 8, y + 16, Theme.TEXT_DIM, false);
    }

    @Override
    public List<FormattedCharSequence> tooltip(Font font, int mouseX, int mouseY) {
        return contains(mouseX, mouseY) ? wrap(font, description, 240) : null;
    }
}
