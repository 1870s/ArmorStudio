package com.armorstudio.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/** Text helpers for the custom widgets. */
public final class Text {
    private static final FormattedText ELLIPSIS = FormattedText.of("…");

    private Text() {}

    /** The text, cut with an ellipsis if it is wider than {@code maxWidth}. */
    public static FormattedCharSequence trim(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text.getVisualOrderText();
        FormattedText cut = font.substrByWidth(text, Math.max(0, maxWidth - font.width(ELLIPSIS)));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, ELLIPSIS));
    }
}
