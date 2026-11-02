package com.armorstudio.gui;

import com.armorstudio.ArmorStudio;
import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.PlayerLook;
import com.armorstudio.config.StudioConfig;
import com.armorstudio.profile.ProfileManager;
import com.armorstudio.render.LookHolder;
import com.armorstudio.render.RenderContext;
import com.armorstudio.render.ResolvedLook;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Live 3D preview of your own player, rendered through the normal player renderer (so it shows exactly what the
 * world shows, mixins included). Drag to rotate, scroll to zoom. The strip below toggles pieces directly.
 */
final class PreviewPanel {
    private static final int STRIP_H = 22;
    private static final int BUTTONS_H = 16;

    // Remembered between openings.
    private static float yaw = 25f;
    private static float pitch = -5f;
    private static float zoom = 1f;

    private int x, y, w, h;
    private boolean dragging;
    private double lastDragX, lastDragY;
    private ItemStack[] samples;
    private Level samplesLevel;
    private ItemStack elytraIcon, headItemIcon;

    void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    private int viewH() {
        return h - STRIP_H - BUTTONS_H - 6;
    }

    private int stripY() {
        return y + viewH() + 2;
    }

    private int buttonsY() {
        return stripY() + STRIP_H + 2;
    }

    void extract(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float partial, float dt, boolean others) {
        StudioConfig cfg = ConfigManager.get();
        if (cfg.previewAutoRotate && !dragging) yaw += dt * 30f;
        int vh = viewH();
        g.fillGradient(x, y, x + w, y + vh, 0xFF241C3D, 0xFF0D0D14);
        g.outline(x, y, w, vh, Theme.BORDER);
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            List<FormattedCharSequence> lines = font.split(Component.translatable("armorstudio.preview.no_world"), w - 16);
            for (int i = 0; i < lines.size(); i++) g.centeredText(font, lines.get(i), x + w / 2, y + vh / 2 - lines.size() * 5 + i * 10, Theme.TEXT_DIM);
        } else {
            extractEntity(g, mc, player, cfg, others);
        }
        Component title = Component.translatable(others ? "armorstudio.preview.others" : "armorstudio.preview.self");
        g.text(font, title, x + 5, y + 5, Theme.TEXT_DIM, false);
        if (!others && cfg.quickHideSelf) {
            g.text(font, Component.translatable("armorstudio.preview.quick_hidden"), x + 5, y + 15, Theme.WARN, false);
        }
        if (!cfg.enabled) g.centeredText(font, Component.translatable("armorstudio.preview.disabled"), x + w / 2, y + vh - 14, Theme.WARN);

        extractStrip(g, font, mouseX, mouseY, others);
        extractButtons(g, font, mouseX, mouseY, cfg);
    }

    private void extractEntity(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer player, StudioConfig cfg, boolean others) {
        EntityRenderer<? super LocalPlayer, ?> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
        EntityRenderState state;
        RenderContext.setPreviewTarget(others ? RenderContext.PreviewTarget.OTHERS : RenderContext.PreviewTarget.SELF);
        try {
            state = renderer.createRenderState(player, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        } catch (RuntimeException e) {
            ArmorStudio.LOGGER.warn("Preview render state failed", e);
            return;
        } finally {
            RenderContext.setPreviewTarget(null);
        }
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        if (state instanceof AvatarRenderState avatar && cfg.previewSampleArmor) addSamples(mc, player, avatar);
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180f + yaw;
            living.yRot = 0f;
            living.xRot = 0f;
            living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
            living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
            living.scale = 1f;
        }
        float size = viewH() * 0.36f * zoom;
        Vector3f translation = new Vector3f(0f, state.boundingBoxHeight / 2f + 0.1f, 0f);
        Quaternionf camera = new Quaternionf().rotateX(pitch * Mth.DEG_TO_RAD);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
        g.entity(state, size, translation, rotation, camera, x + 1, y + 1, x + w - 1, y + viewH() - 1);
    }

    /** Fills empty slots with sample netherite armor (preview only) so styles can be judged without wearing armor. */
    private void addSamples(Minecraft mc, LocalPlayer player, AvatarRenderState state) {
        ItemStack[] sample = samples(mc);
        ResolvedLook look = ((LookHolder) state).armorstudio$getLook();
        for (ArmorSlot slot : ArmorSlot.all()) {
            if (!player.getItemBySlot(slot.equipmentSlot).isEmpty()) continue;
            if (look != null && look.isHidden(slot)) continue;
            ItemStack stack = sample[slot.ordinal()];
            switch (slot) {
                case HEAD -> state.headEquipment = stack;
                case CHEST -> state.chestEquipment = stack;
                case LEGS -> state.legsEquipment = stack;
                case FEET -> state.feetEquipment = stack;
            }
        }
    }

    private ItemStack[] samples(Minecraft mc) {
        if (samples != null && samplesLevel == mc.level) return samples;
        samplesLevel = mc.level;
        samples = new ItemStack[4];
        ArmorTrim trim = sampleTrim(mc);
        for (ArmorSlot slot : ArmorSlot.all()) {
            ItemStack stack = new ItemStack(slot.iconItem());
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            if (trim != null) stack.set(DataComponents.TRIM, trim);
            samples[slot.ordinal()] = stack;
        }
        return samples;
    }

    private static ArmorTrim sampleTrim(Minecraft mc) {
        if (mc.level == null) return null;
        var access = mc.level.registryAccess();
        Holder<TrimMaterial> material = access.lookup(Registries.TRIM_MATERIAL).flatMap(r -> r.get(TrimMaterials.AMETHYST)).orElse(null);
        Holder<TrimPattern> pattern = access.lookup(Registries.TRIM_PATTERN).flatMap(r -> r.get(TrimPatterns.SILENCE)).orElse(null);
        return material == null || pattern == null ? null : new ArmorTrim(material, pattern);
    }

    // ------------------------------------------------------------------ toggle strip

    private int stripCellW() {
        return (w - 5 * 2) / 6;
    }

    private int cellX(int i) {
        return x + i * (stripCellW() + 2);
    }

    private ItemStack icon(int i, LocalPlayer player) {
        if (elytraIcon == null) {
            elytraIcon = new ItemStack(Items.ELYTRA);
            headItemIcon = new ItemStack(Items.CARVED_PUMPKIN);
        }
        if (i == 4) return elytraIcon;
        if (i == 5) return headItemIcon;
        ArmorSlot slot = ArmorSlot.all()[i];
        ItemStack worn = player == null ? ItemStack.EMPTY : player.getItemBySlot(slot.equipmentSlot);
        if (!worn.isEmpty()) return worn;
        return samples != null ? samples[i] : new ItemStack(slot.iconItem());
    }

    private static boolean visible(PlayerLook look, int i) {
        return switch (i) {
            case 4 -> look.elytra.visible;
            case 5 -> look.showHeadItems;
            default -> look.piece(ArmorSlot.all()[i]).visible;
        };
    }

    private static void toggle(PlayerLook look, int i) {
        switch (i) {
            case 4 -> look.elytra.visible = !look.elytra.visible;
            case 5 -> look.showHeadItems = !look.showHeadItems;
            default -> look.piece(ArmorSlot.all()[i]).visible = !look.piece(ArmorSlot.all()[i]).visible;
        }
        ConfigManager.markDirty();
    }

    private static Component cellName(int i) {
        return switch (i) {
            case 4 -> Component.translatable("armorstudio.piece.elytra");
            case 5 -> Component.translatable("armorstudio.piece.head_items");
            default -> ArmorSlot.all()[i].displayName();
        };
    }

    private void extractStrip(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, boolean others) {
        PlayerLook look = ProfileManager.effective().look(!others);
        LocalPlayer player = Minecraft.getInstance().player;
        int cw = stripCellW(), sy = stripY();
        for (int i = 0; i < 6; i++) {
            int cx = cellX(i);
            boolean on = visible(look, i);
            boolean hot = inside(mouseX, mouseY, cx, sy, cw, STRIP_H);
            g.fill(cx, sy, cx + cw, sy + STRIP_H, hot ? Theme.CARD_HOVER : Theme.CARD);
            g.fill(cx, sy + STRIP_H - 2, cx + cw, sy + STRIP_H, on ? Theme.ACCENT : 0xFF5A2330);
            g.item(icon(i, player), cx + (cw - 16) / 2, sy + 2);
            if (!on) {
                g.fill(cx, sy, cx + cw, sy + STRIP_H - 2, 0xA0101018);
                g.centeredText(font, "✖", cx + cw / 2, sy + 7, Theme.BAD);
            }
        }
    }

    // ------------------------------------------------------------------ buttons

    private static final String[] BUTTON_KEYS = {"armorstudio.preview.reset", "armorstudio.preview.rotate", "armorstudio.preview.sample"};

    private int buttonW() {
        return (w - 4) / 3;
    }

    private void extractButtons(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, StudioConfig cfg) {
        int by = buttonsY(), bw = buttonW();
        for (int i = 0; i < 3; i++) {
            int bx = x + i * (bw + 2);
            boolean active = i == 1 ? cfg.previewAutoRotate : i == 2 && cfg.previewSampleArmor;
            boolean hot = inside(mouseX, mouseY, bx, by, bw, BUTTONS_H);
            g.fill(bx, by, bx + bw, by + BUTTONS_H, active ? Theme.ACCENT_DARK : hot ? Theme.CARD_HOVER : Theme.CARD);
            Component label = Component.translatable(BUTTON_KEYS[i]);
            g.centeredText(font, net.minecraft.locale.Language.getInstance().getVisualOrder(
                font.substrByWidth(label, bw - 4)), bx + bw / 2, by + 4, Theme.TEXT);
        }
    }

    // ------------------------------------------------------------------ input

    boolean contains(double mx, double my) {
        return inside(mx, my, x, y, w, h);
    }

    boolean mouseClicked(double mx, double my, int button, boolean others) {
        if (!contains(mx, my)) return false;
        if (inside(mx, my, x, y, w, viewH())) {
            dragging = true;
            lastDragX = mx;
            lastDragY = my;
            return true;
        }
        int sy = stripY(), cw = stripCellW();
        for (int i = 0; i < 6; i++) {
            if (inside(mx, my, cellX(i), sy, cw, STRIP_H)) {
                toggle(ProfileManager.effective().look(!others), i);
                click();
                return true;
            }
        }
        int by = buttonsY(), bw = buttonW();
        StudioConfig cfg = ConfigManager.get();
        for (int i = 0; i < 3; i++) {
            if (!inside(mx, my, x + i * (bw + 2), by, bw, BUTTONS_H)) continue;
            switch (i) {
                case 0 -> {
                    yaw = 25f;
                    pitch = -5f;
                    zoom = 1f;
                }
                case 1 -> cfg.previewAutoRotate = !cfg.previewAutoRotate;
                default -> cfg.previewSampleArmor = !cfg.previewSampleArmor;
            }
            ConfigManager.markDirty();
            click();
            return true;
        }
        return true;
    }

    boolean mouseDragged(double mx, double my) {
        if (!dragging) return false;
        yaw -= (float) (mx - lastDragX) * 1.6f;
        pitch = Mth.clamp(pitch + (float) (my - lastDragY) * 1.2f, -60f, 60f);
        lastDragX = mx;
        lastDragY = my;
        return true;
    }

    void mouseReleased() {
        dragging = false;
    }

    boolean mouseScrolled(double mx, double my, double amount) {
        if (!inside(mx, my, x, y, w, viewH())) return false;
        zoom = Mth.clamp(zoom * (amount > 0 ? 1.1f : 1f / 1.1f), 0.5f, 2.6f);
        return true;
    }

    List<FormattedCharSequence> tooltip(Font font, int mx, int my, boolean others) {
        int sy = stripY(), cw = stripCellW();
        for (int i = 0; i < 6; i++) {
            if (inside(mx, my, cellX(i), sy, cw, STRIP_H)) {
                boolean on = visible(ProfileManager.effective().look(!others), i);
                return font.split(Component.translatable(on ? "armorstudio.preview.click_hide" : "armorstudio.preview.click_show", cellName(i)), 200);
            }
        }
        if (inside(mx, my, x, y, w, viewH())) return font.split(Component.translatable("armorstudio.preview.help"), 200);
        return null;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
