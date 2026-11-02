package com.armorstudio.gui.widget;

import com.armorstudio.gui.Theme;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Picks one value from a list: "◀ value ▶". The left half (or a right click) goes back, the right half forward. */
public class ChoiceRow<T> extends SettingRow {
    private final Supplier<List<T>> values;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Function<T, Component> names;

    public ChoiceRow(Component label, Component description, Supplier<List<T>> values, Supplier<T> getter, Consumer<T> setter,
                     T defaultValue, Function<T, Component> names) {
        super(label, description);
        this.values = values;
        this.getter = getter;
        this.setter = setter;
        this.names = names;
        if (defaultValue != null) resettable(() -> Objects.equals(getter.get(), defaultValue), () -> setter.accept(defaultValue));
    }

    @Override
    protected void extractControl(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt, boolean enabled) {
        int cx = controlX(), cw = controlWidth(), cy = y + (height() - 16) / 2;
        boolean hot = enabled && inside(mouseX, mouseY, cx, cy, cw, 16);
        card(g, cx, cy, cw, 16, hot ? Theme.BORDER_LIGHT : Theme.FIELD);
        boolean leftHot = hot && mouseX < cx + cw / 2;
        g.text(font, "◀", cx + 4, cy + 4, hot && leftHot ? Theme.ACCENT_LIGHT : Theme.TEXT_MUTED, false);
        g.text(font, "▶", cx + cw - 4 - font.width("▶"), cy + 4, hot && !leftHot ? Theme.ACCENT_LIGHT : Theme.TEXT_MUTED, false);
        Component name = names.apply(getter.get());
        g.centeredText(font, trim(font, name, cw - 32), cx + cw / 2, cy + 4, enabled ? Theme.TEXT : Theme.TEXT_MUTED);
    }

    @Override
    protected boolean clickControl(double mouseX, double mouseY, int button) {
        List<T> list = values.get();
        if (list.isEmpty()) return false;
        boolean back = button == 1 || (mouseX >= controlX() && mouseX < controlX() + controlWidth() / 2);
        int index = list.indexOf(getter.get());
        int next = Math.floorMod((index < 0 ? 0 : index) + (back ? -1 : 1), list.size());
        setter.accept(list.get(next));
        changed();
        return true;
    }
}
