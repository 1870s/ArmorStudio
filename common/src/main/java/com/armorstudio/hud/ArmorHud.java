package com.armorstudio.hud;

import com.armorstudio.compat.ClientCompat;
import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.HudSettings;
import com.armorstudio.config.Options.DurabilityText;
import com.armorstudio.config.Options.HudLayout;
import com.armorstudio.gui.HudEditorScreen;
import com.armorstudio.gui.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The armor durability HUD. Always shows the real equipment, independent of any visual hiding. Layout math is shared
 * with the drag-to-position editor through {@link #measure} and {@link #position}.
 */
public final class ArmorHud {
    private static final int ICON = 16;
    private static final int PAD = 3;
    private static final int ROW_GAP = 2;
    private static final int TEXT_GAP = 4;
    private static final int CELL_GAP = 4;

    /** Reused every frame to avoid allocations. */
    private static final ItemStack[] STACKS = new ItemStack[4];
    private static final String[] TEXTS = new String[4];
    private static final int[] SIZE = new int[2];
    private static final int[] POS = new int[2];
    private static final int[] BOUNDS = new int[4];

    private ArmorHud() {}

    /** HUD layer callback (registered through Fabric's HudElementRegistry / NeoForge's RegisterGuiLayersEvent). */
    public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Notifications.extract(g);
        HudSettings hud = ConfigManager.get().hud;
        Player player = mc.player;
        if (!hud.enabled || player == null || ClientCompat.isHudHidden() || mc.getDebugOverlay().showDebugScreen()) return;
        if (ClientCompat.screen() instanceof HudEditorScreen) return; // the editor draws its own copy
        boolean any = false;
        for (ArmorSlot slot : ArmorSlot.all()) {
            ItemStack stack = player.getItemBySlot(slot.equipmentSlot);
            STACKS[slot.ordinal()] = stack;
            any |= !stack.isEmpty();
        }
        if (!any && hud.hideWhenNoArmor) return;
        draw(g, hud, STACKS, g.guiWidth(), g.guiHeight(), false);
    }

    /**
     * Draws the HUD at its configured position. Returns {x, y, width, height} in GUI pixels (after scaling); the array
     * is reused on the next call (render thread only).
     */
    public static int[] draw(GuiGraphicsExtractor g, HudSettings hud, ItemStack[] stacks, int screenW, int screenH, boolean editor) {
        Font font = Minecraft.getInstance().font;
        int[] size = measure(hud, stacks, font);
        int contentW = size[0], contentH = size[1];
        float scale = hud.scale / 100f;
        int w = Math.round(contentW * scale), h = Math.round(contentH * scale);
        int[] pos = position(hud, w, h, screenW, screenH);
        g.pose().pushMatrix();
        g.pose().translate(pos[0], pos[1]);
        g.pose().scale(scale, scale);
        extractContent(g, hud, stacks, font, contentW, contentH, editor);
        g.pose().popMatrix();
        BOUNDS[0] = pos[0];
        BOUNDS[1] = pos[1];
        BOUNDS[2] = w;
        BOUNDS[3] = h;
        return BOUNDS;
    }

    /** Unscaled content size {width, height} (reused array). Fills {@link #TEXTS} as a side effect. */
    public static int[] measure(HudSettings hud, ItemStack[] stacks, Font font) {
        int count = 0, maxText = 0, totalCells = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = stacks[i];
            TEXTS[i] = stack.isEmpty() ? "" : durabilityText(stack, hud.text);
            if (stack.isEmpty() && !hud.showEmptySlots) continue;
            count++;
            int tw = font.width(TEXTS[i]);
            maxText = Math.max(maxText, tw);
            totalCells += Math.max(ICON, tw);
        }
        if (count == 0) count = 1;
        boolean hasText = hud.text != DurabilityText.NONE;
        if (hud.layout == HudLayout.VERTICAL) {
            int width = PAD * 2 + ICON + (hasText && maxText > 0 ? TEXT_GAP + maxText : 0);
            int height = PAD * 2 + count * ICON + (count - 1) * ROW_GAP;
            SIZE[0] = width;
            SIZE[1] = height;
            return SIZE;
        }
        int width = PAD * 2 + Math.max(totalCells, ICON) + (count - 1) * CELL_GAP;
        int height = PAD * 2 + ICON + (hasText && maxText > 0 ? 10 : 0);
        SIZE[0] = width;
        SIZE[1] = height;
        return SIZE;
    }

    /** Top-left corner for a box of the given (scaled) size, clamped to the screen (reused array). */
    public static int[] position(HudSettings hud, int w, int h, int screenW, int screenH) {
        int x, y;
        switch (hud.anchor) {
            case TOP_LEFT -> { x = hud.offsetX; y = hud.offsetY; }
            case TOP_RIGHT -> { x = screenW - w - hud.offsetX; y = hud.offsetY; }
            case MIDDLE_LEFT -> { x = hud.offsetX; y = (screenH - h) / 2 + hud.offsetY; }
            case MIDDLE_RIGHT -> { x = screenW - w - hud.offsetX; y = (screenH - h) / 2 + hud.offsetY; }
            case BOTTOM_LEFT -> { x = hud.offsetX; y = screenH - h - hud.offsetY; }
            case BOTTOM_RIGHT -> { x = screenW - w - hud.offsetX; y = screenH - h - hud.offsetY; }
            case HOTBAR_LEFT -> { x = screenW / 2 - 91 - w - hud.offsetX; y = screenH - h - hud.offsetY; }
            default -> { x = screenW / 2 + 91 + hud.offsetX; y = screenH - h - hud.offsetY; }
        }
        x = Math.max(0, Math.min(screenW - w, x));
        y = Math.max(0, Math.min(screenH - h, y));
        POS[0] = x;
        POS[1] = y;
        return POS;
    }

    private static void extractContent(GuiGraphicsExtractor g, HudSettings hud, ItemStack[] stacks, Font font, int width, int height,
                                       boolean editor) {
        if (hud.backgroundOpacity > 0) {
            g.fill(0, 0, width, height, Theme.withAlpha(0xFF0C0C12, hud.backgroundOpacity / 100f));
            g.fill(0, height - 1, width, height, Theme.withAlpha(Theme.ACCENT, hud.backgroundOpacity / 100f));
        }
        boolean blinkOn = (Util.getMillis() / 400) % 2 == 0;
        boolean vertical = hud.layout == HudLayout.VERTICAL;
        int x = PAD, y = PAD;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = stacks[i];
            if (stack.isEmpty() && !hud.showEmptySlots) continue;
            String text = TEXTS[i];
            int cellW = vertical ? ICON : Math.max(ICON, font.width(text));
            int iconX = vertical ? x : x + (cellW - ICON) / 2;
            boolean low = isLow(stack, hud.lowThreshold);
            if (low && hud.blinkWhenLow && (blinkOn || editor)) {
                g.fill(iconX - 1, y - 1, iconX + ICON + 1, y + ICON + 1, 0x90E53935);
            }
            if (stack.isEmpty()) {
                g.fill(iconX + 2, y + 2, iconX + ICON - 2, y + ICON - 2, 0x30FFFFFF);
            } else {
                g.item(stack, iconX, y);
                g.itemDecorations(font, stack, iconX, y);
                if (hud.showEnchantMarker && stack.isEnchanted()) g.text(font, "✦", iconX + ICON - 5, y - 2, Theme.ACCENT_LIGHT, true);
            }
            if (hud.text != DurabilityText.NONE && !text.isEmpty()) {
                int color = hud.colorText && stack.isDamageableItem() ? 0xFF000000 | stack.getBarColor() : Theme.TEXT;
                if (low) color = blinkOn || !hud.blinkWhenLow ? Theme.BAD : 0xFFFFB4B4;
                if (vertical) {
                    g.text(font, text, x + ICON + TEXT_GAP, y + 4, color, true);
                } else {
                    g.text(font, text, x + (cellW - font.width(text)) / 2, y + ICON + 1, color, true);
                }
            }
            if (vertical) y += ICON + ROW_GAP;
            else x += cellW + CELL_GAP;
        }
    }

    public static String durabilityText(ItemStack stack, DurabilityText mode) {
        if (!stack.isDamageableItem()) return mode == DurabilityText.NONE ? "" : "∞";
        int max = stack.getMaxDamage();
        int left = max - stack.getDamageValue();
        int percent = Math.round(left * 100f / max);
        return switch (mode) {
            case NONE -> "";
            case PERCENT -> percent + "%";
            case EXACT -> left + "/" + max;
            case BOTH -> percent + "% · " + left;
        };
    }

    /** Remaining durability in percent, or -1 for items without durability. */
    public static int percentLeft(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return -1;
        return Math.round((stack.getMaxDamage() - stack.getDamageValue()) * 100f / stack.getMaxDamage());
    }

    public static boolean isLow(ItemStack stack, int threshold) {
        int p = percentLeft(stack);
        return threshold > 0 && p >= 0 && p <= threshold;
    }
}
