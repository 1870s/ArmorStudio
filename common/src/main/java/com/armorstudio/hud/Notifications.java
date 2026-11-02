package com.armorstudio.hud;

import com.armorstudio.config.ConfigManager;
import com.armorstudio.gui.Theme;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** A small fading message at the top of the screen (profile switched, armor toggled, ...). Can be disabled. */
public final class Notifications {
    private static final long DURATION_MS = 1800;
    private static final long FADE_MS = 300;

    private static Component message;
    private static long shownAt;

    private Notifications() {}

    public static void show(Component text) {
        if (!ConfigManager.get().notifications) return;
        message = text;
        shownAt = Util.getMillis();
    }

    public static void extract(GuiGraphicsExtractor g) {
        if (message == null) return;
        long age = Util.getMillis() - shownAt;
        if (age > DURATION_MS) {
            message = null;
            return;
        }
        float alpha = Math.min(1f, Math.min(age, DURATION_MS - age) / (float) FADE_MS);
        if (alpha <= 0.02f) return;
        Font font = Minecraft.getInstance().font;
        int w = font.width(message) + 16;
        int x = (g.guiWidth() - w) / 2;
        int y = 8 - Math.round((1f - alpha) * 6f);
        g.fill(x, y, x + w, y + 18, Theme.withAlpha(Theme.PANEL, alpha * 0.92f));
        g.fill(x, y + 17, x + w, y + 18, Theme.withAlpha(Theme.ACCENT, alpha));
        g.text(font, message, x + 8, y + 5, Theme.withAlpha(Theme.TEXT, alpha), false);
    }
}
