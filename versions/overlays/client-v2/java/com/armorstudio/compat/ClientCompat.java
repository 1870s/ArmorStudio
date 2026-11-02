package com.armorstudio.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Client API differences between Minecraft versions, "v2" flavor (26.2+): screens, overlays and toasts moved to
 * {@code Gui}, the HUD (including its hidden state and the action bar) to {@code Gui.hud}.
 */
public final class ClientCompat {
    private ClientCompat() {}

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static Screen screen() {
        return mc().gui.screen();
    }

    public static void setScreen(Screen screen) {
        mc().gui.setScreen(screen);
    }

    public static Overlay overlay() {
        return mc().gui.overlay();
    }

    /** True while the player has hidden the HUD (F1). */
    public static boolean isHudHidden() {
        return mc().gui.hud.isHidden();
    }

    public static void setHudHidden(boolean hidden) {
        if (mc().gui.hud.isHidden() != hidden) mc().gui.hud.toggle();
    }

    /** Shows a message above the hotbar (client-side only). */
    public static void actionBar(Component message) {
        mc().gui.hud.setOverlayMessage(message, false);
    }

    public static void clearToasts() {
        mc().gui.toastManager().clear();
    }

    public static RenderTarget mainRenderTarget() {
        return mc().gameRenderer.mainRenderTarget();
    }
}
