package com.armorstudio.gui.widget;

import com.armorstudio.gui.Theme;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Two switches side by side: one for your own player, one for other players. */
public class DualToggleRow extends SettingRow {
    private final BooleanSupplier selfGet, othersGet;
    private final Consumer<Boolean> selfSet, othersSet;
    private float selfKnob = -1f, othersKnob = -1f;

    public DualToggleRow(Component label, Component description, BooleanSupplier selfGet, Consumer<Boolean> selfSet,
                         BooleanSupplier othersGet, Consumer<Boolean> othersSet) {
        super(label, description);
        this.selfGet = selfGet;
        this.selfSet = selfSet;
        this.othersGet = othersGet;
        this.othersSet = othersSet;
        resettable(() -> selfGet.getAsBoolean() && othersGet.getAsBoolean(), () -> {
            selfSet.accept(true);
            othersSet.accept(true);
        });
    }

    @Override
    protected int controlWidth() {
        return 100;
    }

    /** Column centers for a row at {@code x} with {@code width}; shared with the column captions. */
    public static int selfColumn(int x, int width) {
        return x + width - 6 - 100 + 22;
    }

    public static int othersColumn(int x, int width) {
        return x + width - 6 - 100 + 78;
    }

    private int selfColumn() {
        return selfColumn(x, width);
    }

    private int othersColumn() {
        return othersColumn(x, width);
    }

    @Override
    protected void extractControl(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt, boolean enabled) {
        boolean s = selfGet.getAsBoolean(), o = othersGet.getAsBoolean();
        if (selfKnob < 0) selfKnob = s ? 1f : 0f;
        if (othersKnob < 0) othersKnob = o ? 1f : 0f;
        selfKnob = Theme.approach(selfKnob, s ? 1f : 0f, dt, 22f);
        othersKnob = Theme.approach(othersKnob, o ? 1f : 0f, dt, 22f);
        int sy = y + (height() - 14) / 2 - 1;
        ToggleRow.drawSwitch(g, selfColumn() - 13, sy, selfKnob, enabled, inside(mouseX, mouseY, selfColumn() - 22, y, 44, height()) ? 1f : 0f);
        ToggleRow.drawSwitch(g, othersColumn() - 13, sy, othersKnob, enabled, inside(mouseX, mouseY, othersColumn() - 22, y, 44, height()) ? 1f : 0f);
    }

    @Override
    protected boolean clickControl(double mouseX, double mouseY, int button) {
        if (mouseX >= selfColumn() - 24 && mouseX < selfColumn() + 24) {
            selfSet.accept(!selfGet.getAsBoolean());
        } else if (mouseX >= othersColumn() - 24 && mouseX < othersColumn() + 24) {
            othersSet.accept(!othersGet.getAsBoolean());
        } else {
            return false;
        }
        changed();
        return true;
    }
}
