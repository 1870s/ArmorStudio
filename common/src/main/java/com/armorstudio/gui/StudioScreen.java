package com.armorstudio.gui;

import com.armorstudio.compat.ClientCompat;
import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.Profile;
import com.armorstudio.config.StudioConfig;
import com.armorstudio.gui.widget.Row;
import com.armorstudio.gui.widget.ToggleRow;
import com.armorstudio.hud.Notifications;
import com.armorstudio.profile.ProfileManager;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * The Armor Studio settings screen: category sidebar, a scrollable page of settings and a live preview. Every change
 * applies immediately (and is visible in the world behind the screen); the file is saved automatically.
 */
public class StudioScreen extends Screen {
    public enum Page {
        GENERAL("general", () -> Items.COMPARATOR),
        VISIBILITY("visibility", () -> Items.NETHERITE_CHESTPLATE),
        PLAYERS("players", () -> Items.PLAYER_HEAD),
        APPEARANCE("appearance", () -> Items.AMETHYST_SHARD),
        EQUIPMENT("equipment", () -> Items.ELYTRA),
        HUD("hud", () -> Items.CLOCK),
        PROFILES("profiles", () -> Items.NAME_TAG),
        KEYBINDS("keybinds", () -> Items.TRIPWIRE_HOOK),
        COMPAT("compat", () -> Items.REDSTONE);

        final String key;
        final Supplier<Item> icon;

        Page(String key, Supplier<Item> icon) {
            this.key = key;
            this.icon = icon;
        }

        Component title() {
            return Component.translatable("armorstudio.page." + key);
        }

        Component subtitle() {
            return Component.translatable("armorstudio.page." + key + ".subtitle");
        }

        boolean hasTargetSelector() {
            return this == VISIBILITY || this == APPEARANCE || this == EQUIPMENT;
        }
    }

    private static final int HEADER_H = 26;
    private static final int FOOTER_H = 24;
    private static final int SEGMENT_H = 16;

    // Remembered between openings.
    static Page page = Page.VISIBILITY;
    static boolean editOthers;
    static ArmorSlot editPiece = ArmorSlot.HEAD;
    private static final Map<Page, Double> SCROLLS = new EnumMap<>(Page.class);

    private final Screen parent;
    private final PreviewPanel preview = new PreviewPanel();
    private final Map<Page, ItemStack> icons = new EnumMap<>(Page.class);
    private List<Row> rows = new ArrayList<>();
    private Row activeRow;
    private boolean rebuildRequested;

    private double scroll, scrollShown;
    private float sidebarSelY = -1f;
    private long lastFrame;
    private boolean draggingScrollbar;

    // Layout
    private int px, py, pw, ph;
    private int sideX, sideW;
    private int contentX, contentY, contentW, contentH, toolbarY, toolbarH;
    private boolean compact, showPreview;
    private int previewX, previewW;

    // Profiles page
    EditBox nameBox;
    Profile selectedProfile;

    public StudioScreen(Screen parent) {
        super(Component.translatable("armorstudio.title"));
        this.parent = parent;
    }

    /** Opens the screen directly on a page. */
    public static void openOn(Page target) {
        page = target;
        ClientCompat.setScreen(new StudioScreen(null));
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        if (icons.isEmpty()) for (Page p : Page.values()) icons.put(p, new ItemStack(p.icon.get()));
        int margin = width < 480 || height < 300 ? 0 : 10;
        pw = Math.min(width - margin * 2, 760);
        ph = Math.min(height - margin * 2, 440);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        compact = pw < 560;
        sideX = px;
        sideW = compact ? 30 : 124;
        showPreview = pw >= 380;
        previewW = showPreview ? Mth.clamp((int) (pw * 0.27f), 108, 190) : 0;
        previewX = px + pw - previewW - 6;
        contentX = sideX + sideW + 8;
        int contentRight = showPreview ? previewX - 8 : px + pw - 6;
        contentW = contentRight - contentX;
        int top = py + HEADER_H + 6;
        toolbarY = top + 22;
        toolbarH = toolbarHeight();
        contentY = toolbarY + toolbarH;
        contentH = py + ph - FOOTER_H - 4 - contentY;
        preview.setBounds(previewX, top, previewW, py + ph - FOOTER_H - 4 - top);

        nameBox = null;
        if (page == Page.PROFILES) {
            int bw = Math.min(70, (contentW - 8) / 4);
            nameBox = new EditBox(font, contentX + 1, toolbarY + 1, contentW - 2 * (bw + 4) - 2, 16, Component.translatable("armorstudio.profiles.name"));
            nameBox.setMaxLength(Profile.MAX_NAME_LENGTH);
            nameBox.setHint(Component.translatable("armorstudio.profiles.name_hint").withColor(Theme.TEXT_MUTED));
            if (selectedProfile != null && ProfileManager.all().contains(selectedProfile)) nameBox.setValue(selectedProfile.name);
            addRenderableWidget(nameBox);
        }
        rebuildRows();
        scroll = SCROLLS.getOrDefault(page, 0.0);
        clampScroll();
        scrollShown = scroll;
    }

    private int toolbarHeight() {
        if (page == Page.APPEARANCE) return SEGMENT_H * 2 + 8;
        if (page.hasTargetSelector()) return SEGMENT_H + 6;
        if (page == Page.PROFILES) return 22;
        return 0;
    }

    void requestRebuild() {
        rebuildRequested = true;
    }

    private void rebuildRows() {
        rows = StudioPages.build(this, page);
        layoutRows();
    }

    private void layoutRows() {
        int y = contentY;
        for (Row row : rows) {
            row.setBounds(contentX, y, contentW - 6);
            y += row.height() + 2;
        }
    }

    private int contentHeight() {
        int total = 0;
        for (Row row : rows) total += row.height() + 2;
        return total;
    }

    private void clampScroll() {
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight() - contentH));
    }

    private void switchPage(Page target) {
        if (target == page) return;
        SCROLLS.put(page, scroll);
        page = target;
        click();
        rebuildWidgets();
    }

    net.minecraft.client.gui.Font font() {
        return font;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        // Light dimming only, so the effect of every change stays visible on the world behind the menu.
        g.fill(0, 0, width, height, Theme.BACKDROP);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (rebuildRequested) {
            rebuildRequested = false;
            rebuildRows();
        }

        g.fill(px, py, px + pw, py + ph, Theme.PANEL);
        g.outline(px, py, pw, ph, Theme.BORDER);
        extractHeader(g, mouseX, mouseY);
        extractSidebar(g, mouseX, mouseY, dt);

        int top = py + HEADER_H + 6;
        g.text(font, page.title(), contentX + 1, top, Theme.TEXT, false);
        g.text(font, Text.trim(font, page.subtitle(), contentW - 4), contentX + 1, top + 11, Theme.TEXT_MUTED, false);
        extractToolbar(g, mouseX, mouseY);

        // Smooth scrolling
        clampScroll();
        scrollShown = Math.abs(scrollShown - scroll) < 0.5 ? scroll : scrollShown + (scroll - scrollShown) * Math.min(1f, dt * 16f);
        int offset = (int) Math.round(scrollShown);
        int y = contentY - offset;
        for (Row row : rows) {
            row.setBounds(contentX, y, contentW - 6);
            y += row.height() + 2;
        }
        boolean mouseInContent = inside(mouseX, mouseY, contentX, contentY, contentW, contentH);
        int mx = mouseInContent ? mouseX : -10000, my = mouseInContent ? mouseY : -10000;
        g.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        for (Row row : rows) {
            if (row.bottom() < contentY || row.y() > contentY + contentH) continue;
            row.extract(g, font, mx, my, dt);
        }
        g.disableScissor();
        extractScrollbar(g);

        if (showPreview) preview.extract(g, font, mouseX, mouseY, partial, dt, editOthers && page.hasTargetSelector());
        extractFooter(g, mouseX, mouseY);

        super.extractRenderState(g, mouseX, mouseY, partial);
        extractTooltip(g, mouseX, mouseY, mouseInContent);
    }

    private void extractHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(px, py, px + pw, py + HEADER_H, Theme.HEADER);
        g.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, Theme.BORDER);
        g.fill(px, py, px + pw, py + 1, Theme.ACCENT);
        g.text(font, Component.literal("◆ ").withColor(Theme.ACCENT_LIGHT).append(Component.translatable("armorstudio.title").withColor(Theme.TEXT)),
            px + 9, py + 9, Theme.TEXT, false);

        StudioConfig cfg = ConfigManager.get();
        // Master switch
        int sx = px + pw - 8 - 26, sy = py + 6;
        ToggleRow.drawSwitch(g, sx, sy, cfg.enabled ? 1f : 0f, true, inside(mouseX, mouseY, sx, sy, 26, 14) ? 1f : 0f);
        Component enabledLabel = Component.translatable(cfg.enabled ? "armorstudio.ui.enabled" : "armorstudio.ui.disabled");
        g.text(font, enabledLabel, sx - 6 - font.width(enabledLabel), py + 9, cfg.enabled ? Theme.ACCENT_LIGHT : Theme.WARN, false);

        // Profile chip
        Component chip = profileChip();
        int cw = font.width(chip) + 14, cx = sx - 18 - font.width(enabledLabel) - cw, cy = py + 5;
        if (cx > px + 9 + font.width(Component.translatable("armorstudio.title")) + 20) {
            boolean hot = inside(mouseX, mouseY, cx, cy, cw, 16);
            g.fill(cx, cy, cx + cw, cy + 16, hot ? Theme.ACCENT_DARK : Theme.ACCENT_BG);
            g.text(font, chip, cx + 7, cy + 4, Theme.TEXT, false);
        }
    }

    private Component profileChip() {
        Component name = Component.literal(ProfileManager.effective().name);
        return Component.translatable(ProfileManager.isAutoApplied() ? "armorstudio.header.profile_auto" : "armorstudio.header.profile", name);
    }

    private int[] chipBounds() {
        StudioConfig cfg = ConfigManager.get();
        Component enabledLabel = Component.translatable(cfg.enabled ? "armorstudio.ui.enabled" : "armorstudio.ui.disabled");
        int sx = px + pw - 8 - 26;
        int cw = font.width(profileChip()) + 14;
        return new int[] {sx - 18 - font.width(enabledLabel) - cw, py + 5, cw, 16};
    }

    private void extractSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY, float dt) {
        int top = py + HEADER_H;
        int bottom = py + ph - FOOTER_H;
        g.fill(sideX, top, sideX + sideW, bottom, Theme.SIDEBAR);
        g.fill(sideX + sideW, top, sideX + sideW + 1, bottom, Theme.BORDER);
        Page[] pages = Page.values();
        int itemH = Math.min(24, (bottom - top - 8) / pages.length);
        int targetY = top + 4 + page.ordinal() * itemH;
        sidebarSelY = sidebarSelY < 0 ? targetY : Theme.approach(sidebarSelY, targetY, dt, 20f);
        int sel = Math.round(sidebarSelY);
        g.fill(sideX + 2, sel, sideX + sideW - 2, sel + itemH - 2, Theme.ACCENT_BG);
        g.fill(sideX + 2, sel, sideX + 4, sel + itemH - 2, Theme.ACCENT);
        for (int i = 0; i < pages.length; i++) {
            Page p = pages[i];
            int iy = top + 4 + i * itemH;
            boolean hot = inside(mouseX, mouseY, sideX, iy, sideW, itemH - 2);
            if (hot && p != page) g.fill(sideX + 2, iy, sideX + sideW - 2, iy + itemH - 2, 0x18FFFFFF);
            g.item(icons.get(p), sideX + 7, iy + (itemH - 2 - 16) / 2);
            if (!compact) {
                g.text(font, Text.trim(font, p.title(), sideW - 34), sideX + 28, iy + (itemH - 2 - 8) / 2,
                    p == page ? Theme.TEXT : hot ? Theme.TEXT : Theme.TEXT_DIM, false);
            }
        }
    }

    private int sidebarIndexAt(double mouseX, double mouseY) {
        int top = py + HEADER_H;
        int bottom = py + ph - FOOTER_H;
        int itemH = Math.min(24, (bottom - top - 8) / Page.values().length);
        if (mouseX < sideX || mouseX >= sideX + sideW || mouseY < top + 4) return -1;
        int i = (int) ((mouseY - top - 4) / itemH);
        return i >= 0 && i < Page.values().length ? i : -1;
    }

    // ------------------------------------------------------------------ toolbar (target / piece selectors, profile name)

    private void extractToolbar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (page.hasTargetSelector()) {
            segmented(g, mouseX, mouseY, toolbarY, new Component[] {
                Component.translatable("armorstudio.target.self"), Component.translatable("armorstudio.target.others")}, editOthers ? 1 : 0);
        }
        if (page == Page.APPEARANCE) {
            Component[] names = new Component[4];
            for (ArmorSlot s : ArmorSlot.all()) names[s.ordinal()] = s.displayName();
            segmented(g, mouseX, mouseY, toolbarY + SEGMENT_H + 4, names, editPiece.ordinal());
        }
        if (page == Page.PROFILES) {
            int bw = Math.min(70, (contentW - 8) / 4);
            int bx = contentX + contentW - 2 * (bw + 4) + 4;
            toolbarButton(g, mouseX, mouseY, bx, Component.translatable("armorstudio.profiles.new"), bw, true);
            toolbarButton(g, mouseX, mouseY, bx + bw + 4, Component.translatable("armorstudio.profiles.rename"), bw, selectedProfile != null);
        }
    }

    private void toolbarButton(GuiGraphicsExtractor g, int mouseX, int mouseY, int bx, Component label, int bw, boolean enabled) {
        boolean hot = enabled && inside(mouseX, mouseY, bx, toolbarY, bw, 18);
        g.fill(bx, toolbarY, bx + bw, toolbarY + 18, !enabled ? 0xFF141419 : hot ? Theme.ACCENT : Theme.ACCENT_DARK);
        g.centeredText(font, Text.trim(font, label, bw - 6), bx + bw / 2, toolbarY + 5, enabled ? Theme.TEXT : Theme.TEXT_MUTED);
    }

    private void segmented(GuiGraphicsExtractor g, int mouseX, int mouseY, int y, Component[] labels, int selected) {
        int w = contentW - 6;
        int segW = w / labels.length;
        g.fill(contentX, y, contentX + w, y + SEGMENT_H, Theme.FIELD);
        for (int i = 0; i < labels.length; i++) {
            int sx = contentX + i * segW;
            boolean hot = inside(mouseX, mouseY, sx, y, segW, SEGMENT_H);
            if (i == selected) g.fill(sx + 1, y + 1, sx + segW - 1, y + SEGMENT_H - 1, Theme.ACCENT_DARK);
            else if (hot) g.fill(sx + 1, y + 1, sx + segW - 1, y + SEGMENT_H - 1, Theme.CARD_HOVER);
            g.centeredText(font, Text.trim(font, labels[i], segW - 6), sx + segW / 2, y + 4, i == selected ? Theme.TEXT : Theme.TEXT_DIM);
        }
    }

    private int segmentAt(double mouseX, double mouseY, int y, int count) {
        int w = contentW - 6;
        if (mouseY < y || mouseY >= y + SEGMENT_H || mouseX < contentX || mouseX >= contentX + w) return -1;
        return Math.min(count - 1, (int) ((mouseX - contentX) / (w / count)));
    }

    // ------------------------------------------------------------------ footer, scrollbar, tooltips

    private void extractFooter(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int fy = py + ph - FOOTER_H;
        g.fill(px, fy, px + pw, fy + 1, Theme.BORDER);
        int bw = 70, bx = px + pw - 8 - bw, by = fy + 4;
        boolean hot = inside(mouseX, mouseY, bx, by, bw, 16);
        g.fill(bx, by, bx + bw, by + 16, hot ? Theme.ACCENT : Theme.ACCENT_DARK);
        g.centeredText(font, Component.translatable("gui.done"), bx + bw / 2, by + 4, Theme.TEXT);
        Component hint = Component.translatable("armorstudio.footer.hint");
        g.text(font, Text.trim(font, hint, bx - px - 20), px + 9, fy + 8, Theme.TEXT_MUTED, false);
    }

    private int[] scrollbar() {
        int total = contentHeight();
        if (total <= contentH) return null;
        int barH = Math.max(20, contentH * contentH / total);
        int barY = contentY + (int) ((contentH - barH) * (scrollShown / (total - contentH)));
        return new int[] {contentX + contentW - 3, barY, 3, barH};
    }

    private void extractScrollbar(GuiGraphicsExtractor g) {
        int[] bar = scrollbar();
        if (bar == null) return;
        g.fill(bar[0], contentY, bar[0] + bar[2], contentY + contentH, 0x30FFFFFF);
        g.fill(bar[0], bar[1], bar[0] + bar[2], bar[1] + bar[3], draggingScrollbar ? Theme.ACCENT_LIGHT : Theme.ACCENT);
    }

    private void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean mouseInContent) {
        List<FormattedCharSequence> lines = null;
        if (mouseInContent) {
            for (Row row : rows) {
                lines = row.tooltip(font, mouseX, mouseY);
                if (lines != null) break;
            }
        } else if (showPreview && preview.contains(mouseX, mouseY)) {
            lines = preview.tooltip(font, mouseX, mouseY, editOthers && page.hasTargetSelector());
        } else if (compact) {
            int i = sidebarIndexAt(mouseX, mouseY);
            if (i >= 0) lines = font.split(Page.values()[i].title(), 200);
        }
        if (lines == null) {
            int[] chip = chipBounds();
            if (inside(mouseX, mouseY, chip[0], chip[1], chip[2], chip[3])) lines = font.split(Component.translatable("armorstudio.header.profile_tooltip"), 220);
        }
        if (lines != null && !lines.isEmpty()) g.setTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (super.mouseClicked(event, doubleClick)) return true;

        int sx = px + pw - 8 - 26;
        if (inside(mx, my, sx, py + 6, 26, 14)) {
            StudioConfig cfg = ConfigManager.get();
            cfg.enabled = !cfg.enabled;
            ConfigManager.markDirty();
            click();
            return true;
        }
        int[] chip = chipBounds();
        if (inside(mx, my, chip[0], chip[1], chip[2], chip[3])) {
            ProfileManager.cycle(button == 1 ? -1 : 1);
            click();
            return true;
        }
        int sidebar = sidebarIndexAt(mx, my);
        if (sidebar >= 0) {
            switchPage(Page.values()[sidebar]);
            return true;
        }
        int bw = 70, bx = px + pw - 8 - bw, by = py + ph - FOOTER_H + 4;
        if (inside(mx, my, bx, by, bw, 16)) {
            click();
            onClose();
            return true;
        }
        if (clickToolbar(mx, my)) return true;
        int[] bar = scrollbar();
        if (bar != null && inside(mx, my, bar[0] - 2, contentY, bar[2] + 4, contentH)) {
            draggingScrollbar = true;
            scrollToMouse(my);
            return true;
        }
        if (inside(mx, my, contentX, contentY, contentW, contentH)) {
            for (Row row : rows) {
                if (row.mouseClicked(mx, my, button)) {
                    activeRow = row;
                    return true;
                }
            }
        }
        if (showPreview && preview.mouseClicked(mx, my, button, editOthers && page.hasTargetSelector())) return true;
        return false;
    }

    private boolean clickToolbar(double mx, double my) {
        if (page.hasTargetSelector()) {
            int i = segmentAt(mx, my, toolbarY, 2);
            if (i >= 0) {
                editOthers = i == 1;
                click();
                requestRebuild();
                return true;
            }
        }
        if (page == Page.APPEARANCE) {
            int i = segmentAt(mx, my, toolbarY + SEGMENT_H + 4, 4);
            if (i >= 0) {
                editPiece = ArmorSlot.all()[i];
                click();
                requestRebuild();
                return true;
            }
        }
        if (page == Page.PROFILES) {
            int bw = Math.min(70, (contentW - 8) / 4);
            int bx = contentX + contentW - 2 * (bw + 4) + 4;
            if (inside(mx, my, bx, toolbarY, bw, 18)) {
                Profile created = ProfileManager.create(nameBox.getValue().isBlank() ? "New Profile" : nameBox.getValue());
                selectProfile(created);
                Notifications.show(Component.translatable("armorstudio.notify.created", created.name));
                click();
                return true;
            }
            if (selectedProfile != null && inside(mx, my, bx + bw + 4, toolbarY, bw, 18)) {
                if (ProfileManager.rename(selectedProfile, nameBox.getValue())) {
                    Notifications.show(Component.translatable("armorstudio.notify.renamed", selectedProfile.name));
                    requestRebuild();
                } else {
                    Notifications.show(Component.translatable("armorstudio.notify.rename_failed"));
                }
                click();
                return true;
            }
        }
        return false;
    }

    void selectProfile(Profile profile) {
        selectedProfile = profile;
        if (nameBox != null) nameBox.setValue(profile == null ? "" : profile.name);
        requestRebuild();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar) {
            scrollToMouse(event.y());
            return true;
        }
        if (activeRow != null && activeRow.mouseDragged(event.x(), event.y(), event.button())) return true;
        if (preview.mouseDragged(event.x(), event.y())) return true;
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        if (activeRow != null) {
            activeRow.mouseReleased(event.x(), event.y(), event.button());
            activeRow = null;
        }
        preview.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (showPreview && preview.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        if (inside(mouseX, mouseY, contentX, contentY, contentW, contentH)) {
            scroll -= scrollY * 24;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void scrollToMouse(double mouseY) {
        int total = contentHeight();
        float t = (float) ((mouseY - contentY) / contentH);
        scroll = t * total - contentH / 2.0;
        clampScroll();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameBox != null && nameBox.isFocused() && (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
            if (selectedProfile != null && ProfileManager.rename(selectedProfile, nameBox.getValue())) {
                Notifications.show(Component.translatable("armorstudio.notify.renamed", selectedProfile.name));
                requestRebuild();
            }
            return true;
        }
        if (nameBox == null || !nameBox.isFocused()) {
            if (event.key() == InputConstants.KEY_PAGEDOWN || event.key() == InputConstants.KEY_PAGEUP) {
                scroll += (event.key() == InputConstants.KEY_PAGEDOWN ? 1 : -1) * contentH * 0.8;
                clampScroll();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ClientCompat.setScreen(parent);
    }

    @Override
    public void removed() {
        SCROLLS.put(page, scroll);
        ConfigManager.saveNow();
    }

    // ------------------------------------------------------------------ helpers

    static void click() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
