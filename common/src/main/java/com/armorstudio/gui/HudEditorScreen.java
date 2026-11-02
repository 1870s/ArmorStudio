package com.armorstudio.gui;

import com.armorstudio.compat.ClientCompat;
import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.HudSettings;
import com.armorstudio.config.Options.HudAnchor;
import com.armorstudio.hud.ArmorHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Drag-and-drop positioning of the armor HUD. The anchor (screen corner, edge or hotbar side) is picked automatically
 * from where the HUD is dropped, so the HUD keeps its place when the window or GUI scale changes.
 */
public class HudEditorScreen extends Screen {
    private static final float[] SAMPLE_DURABILITY = {0.82f, 0.46f, 0.11f, 1f};

    private final Screen parent;
    private final ItemStack[] stacks = new ItemStack[4];
    private final int[] bounds = {0, 0, 0, 0};
    private boolean dragging;
    private double grabX, grabY;

    public HudEditorScreen(Screen parent) {
        super(Component.translatable("armorstudio.hud_editor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var player = minecraft.player;
        boolean any = false;
        for (ArmorSlot slot : ArmorSlot.all()) {
            ItemStack worn = player == null ? ItemStack.EMPTY : player.getItemBySlot(slot.equipmentSlot);
            stacks[slot.ordinal()] = worn;
            any |= !worn.isEmpty();
        }
        if (!any) {
            // Nothing equipped: show sample pieces with different durability so colors and warnings are visible.
            for (ArmorSlot slot : ArmorSlot.all()) {
                ItemStack sample = new ItemStack(slot.iconItem());
                sample.setDamageValue(Math.round(sample.getMaxDamage() * (1f - SAMPLE_DURABILITY[slot.ordinal()])));
                stacks[slot.ordinal()] = sample;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, 0x50000000);
        // Hotbar outline for orientation
        g.outline(width / 2 - 91, height - 22, 182, 22, 0x60FFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        HudSettings hud = ConfigManager.get().hud;
        System.arraycopy(ArmorHud.draw(g, hud, stacks, width, height, true), 0, bounds, 0, 4);
        boolean hot = dragging || inside(mouseX, mouseY);
        g.outline(bounds[0] - 1, bounds[1] - 1, bounds[2] + 2, bounds[3] + 2, hot ? Theme.ACCENT_LIGHT : Theme.ACCENT);

        int boxW = Math.min(width - 20, Math.max(300, font.width(Component.translatable("armorstudio.hud_editor.help")) + 16));
        int bx = (width - boxW) / 2, by = height / 2 - 30;
        g.fill(bx, by, bx + boxW, by + 50, Theme.PANEL);
        g.fill(bx, by, bx + boxW, by + 1, Theme.ACCENT);
        g.centeredText(font, title, width / 2, by + 6, Theme.TEXT);
        g.centeredText(font, Text.trim(font, Component.translatable("armorstudio.hud_editor.help"), boxW - 10), width / 2, by + 20, Theme.TEXT_DIM);
        Component info = Component.translatable("armorstudio.hud_editor.info", hud.anchor.label(), hud.offsetX, hud.offsetY, hud.scale);
        g.centeredText(font, Text.trim(font, info, boxW - 10), width / 2, by + 34, Theme.ACCENT_LIGHT);
        super.extractRenderState(g, mouseX, mouseY, partial);
    }

    private boolean inside(double mx, double my) {
        return mx >= bounds[0] && mx < bounds[0] + bounds[2] && my >= bounds[1] && my < bounds[1] + bounds[3];
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && inside(event.x(), event.y())) {
            dragging = true;
            grabX = event.x() - bounds[0];
            grabY = event.y() - bounds[1];
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        int w = bounds[2], h = bounds[3];
        int nx = Mth.clamp((int) Math.round(event.x() - grabX), 0, Math.max(0, width - w));
        int ny = Mth.clamp((int) Math.round(event.y() - grabY), 0, Math.max(0, height - h));
        place(ConfigManager.get().hud, nx, ny, w, h);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            ConfigManager.markDirty();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        HudSettings hud = ConfigManager.get().hud;
        hud.scale = Mth.clamp(hud.scale + (scrollY > 0 ? 5 : -5), 50, 200);
        ConfigManager.markDirty();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int dx = 0, dy = 0;
        switch (event.key()) {
            case InputConstants.KEY_LEFT -> dx = -1;
            case InputConstants.KEY_RIGHT -> dx = 1;
            case InputConstants.KEY_UP -> dy = -1;
            case InputConstants.KEY_DOWN -> dy = 1;
            default -> {
                return super.keyPressed(event);
            }
        }
        place(ConfigManager.get().hud, bounds[0] + dx, bounds[1] + dy, bounds[2], bounds[3]);
        ConfigManager.markDirty();
        return true;
    }

    /** Chooses the anchor nearest to the box and stores offsets that reproduce the given top-left corner. */
    private void place(HudSettings hud, int nx, int ny, int w, int h) {
        int cx = nx + w / 2, cy = ny + h / 2;
        boolean nearHotbar = ny + h > height - 34 && Math.abs(cx - width / 2) < 91 + w + 30;
        HudAnchor anchor;
        if (nearHotbar) {
            anchor = cx < width / 2 ? HudAnchor.HOTBAR_LEFT : HudAnchor.HOTBAR_RIGHT;
        } else {
            boolean left = cx < width / 2;
            if (cy < height / 3) anchor = left ? HudAnchor.TOP_LEFT : HudAnchor.TOP_RIGHT;
            else if (cy > height * 2 / 3) anchor = left ? HudAnchor.BOTTOM_LEFT : HudAnchor.BOTTOM_RIGHT;
            else anchor = left ? HudAnchor.MIDDLE_LEFT : HudAnchor.MIDDLE_RIGHT;
        }
        hud.anchor = anchor;
        switch (anchor) {
            case TOP_LEFT -> { hud.offsetX = nx; hud.offsetY = ny; }
            case TOP_RIGHT -> { hud.offsetX = width - w - nx; hud.offsetY = ny; }
            case MIDDLE_LEFT -> { hud.offsetX = nx; hud.offsetY = ny - (height - h) / 2; }
            case MIDDLE_RIGHT -> { hud.offsetX = width - w - nx; hud.offsetY = ny - (height - h) / 2; }
            case BOTTOM_LEFT -> { hud.offsetX = nx; hud.offsetY = height - h - ny; }
            case BOTTOM_RIGHT -> { hud.offsetX = width - w - nx; hud.offsetY = height - h - ny; }
            case HOTBAR_LEFT -> { hud.offsetX = width / 2 - 91 - w - nx; hud.offsetY = height - h - ny; }
            case HOTBAR_RIGHT -> { hud.offsetX = nx - (width / 2 + 91); hud.offsetY = height - h - ny; }
        }
        ConfigManager.markRenderChanged();
    }

    @Override
    public void onClose() {
        ConfigManager.saveNow();
        ClientCompat.setScreen(parent);
    }
}
