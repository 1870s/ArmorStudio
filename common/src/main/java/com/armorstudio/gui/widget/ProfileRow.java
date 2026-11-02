package com.armorstudio.gui.widget;

import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.Profile;
import com.armorstudio.gui.Text;
import com.armorstudio.gui.Theme;
import com.armorstudio.profile.ProfileManager;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

/** One profile in the profile list: name, status badges, a visibility summary and small action buttons. */
public class ProfileRow extends Row {
    public enum Action { USE, DUPLICATE, EXPORT, UP, DOWN, DELETE }

    private static final String[] ICONS = {"▶", "⧉", "⇪", "▲", "▼", "✖"};
    private static final String[] TOOLTIPS = {"use", "duplicate", "export", "up", "down", "delete"};
    private static final int BTN = 14;

    private final Profile profile;
    private final Supplier<Profile> selected;
    private final Consumer<Profile> onSelect;
    private final ActionHandler handler;
    private long deleteArmedAt;

    @FunctionalInterface
    public interface ActionHandler {
        void run(Profile profile, Action action);
    }

    public ProfileRow(Profile profile, Supplier<Profile> selected, Consumer<Profile> onSelect, ActionHandler handler) {
        this.profile = profile;
        this.selected = selected;
        this.onSelect = onSelect;
        this.handler = handler;
    }

    @Override
    public int height() {
        return 30;
    }

    private int buttonX(int i) {
        return x + width - 6 - (ICONS.length - i) * (BTN + 2);
    }

    @Override
    protected void extractRow(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt) {
        boolean isSelected = selected.get() == profile;
        boolean isEffective = ProfileManager.effective() == profile;
        card(g, x, y, width, height() - 2, Theme.lerp(isSelected ? 0xFF231C38 : Theme.CARD, Theme.CARD_HOVER, hover * 0.6f));
        if (isEffective) g.fill(x, y + 2, x + 2, y + height() - 4, Theme.ACCENT);
        int textMax = buttonX(0) - x - 14;
        g.text(font, Text.trim(font, Component.literal(profile.name), textMax), x + 8, y + 5, isEffective ? Theme.TEXT : Theme.TEXT_DIM, false);
        int bx = x + 8 + Math.min(textMax, font.width(profile.name)) + 6;
        if (isEffective) {
            Component badge = Component.translatable(ProfileManager.isAutoApplied() ? "armorstudio.profiles.badge_auto" : "armorstudio.profiles.badge_active");
            int bw = font.width(badge) + 6;
            if (bx + bw < buttonX(0) - 4) {
                card(g, bx, y + 3, bw, 11, Theme.ACCENT_BG);
                g.text(font, badge, bx + 3, y + 5, Theme.ACCENT_LIGHT, false);
            }
        }
        g.text(font, Text.trim(font, summary(), textMax), x + 8, y + 17, Theme.TEXT_MUTED, false);

        boolean armed = Util.getMillis() - deleteArmedAt < 3000;
        for (int i = 0; i < ICONS.length; i++) {
            int bxi = buttonX(i), byi = y + (height() - 2 - BTN) / 2;
            boolean hot = inside(mouseX, mouseY, bxi, byi, BTN, BTN);
            boolean danger = i == Action.DELETE.ordinal();
            int bg = danger && armed ? 0xFFB91C1C : hot ? (danger ? 0xFF7F1D1D : Theme.ACCENT_DARK) : Theme.FIELD;
            card(g, bxi, byi, BTN, BTN, bg);
            g.centeredText(font, ICONS[i], bxi + BTN / 2, byi + 3, hot || (danger && armed) ? Theme.TEXT : Theme.TEXT_DIM);
        }
    }

    /** e.g. "You: 2 of 4 visible · Others: all visible". */
    private Component summary() {
        return Component.translatable("armorstudio.profiles.summary", countVisible(true), countVisible(false));
    }

    private int countVisible(boolean self) {
        int n = 0;
        for (ArmorSlot slot : ArmorSlot.all()) if (profile.look(self).piece(slot).visible) n++;
        return n;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) return false;
        for (int i = 0; i < ICONS.length; i++) {
            if (inside(mouseX, mouseY, buttonX(i), y + (height() - 2 - BTN) / 2, BTN, BTN)) {
                Action action = Action.values()[i];
                if (action == Action.DELETE && Util.getMillis() - deleteArmedAt >= 3000) {
                    deleteArmedAt = Util.getMillis();
                    return true;
                }
                deleteArmedAt = 0;
                handler.run(profile, action);
                return true;
            }
        }
        onSelect.accept(profile);
        return true;
    }

    @Override
    public List<FormattedCharSequence> tooltip(Font font, int mouseX, int mouseY) {
        for (int i = 0; i < ICONS.length; i++) {
            if (inside(mouseX, mouseY, buttonX(i), y + (height() - 2 - BTN) / 2, BTN, BTN)) {
                boolean armed = i == Action.DELETE.ordinal() && Util.getMillis() - deleteArmedAt < 3000;
                return wrap(font, Component.translatable(armed ? "armorstudio.ui.confirm" : "armorstudio.profiles.action." + TOOLTIPS[i]), 200);
            }
        }
        if (contains(mouseX, mouseY)) return wrap(font, Component.translatable("armorstudio.profiles.row_hint"), 220);
        return null;
    }
}
