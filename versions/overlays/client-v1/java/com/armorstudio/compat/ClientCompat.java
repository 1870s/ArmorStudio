package com.armorstudio.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Client API differences between Minecraft versions, "v1" flavor (26.1.x): screens and the hidden-HUD flag still live
 * on {@link Minecraft} and its options.
 */
public final class ClientCompat {
    private ClientCompat() {}

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static Screen screen() {
        return mc().screen;
    }

    public static void setScreen(Screen screen) {
        mc().setScreen(screen);
    }

    public static Overlay overlay() {
        return mc().getOverlay();
    }

    /** True while the player has hidden the HUD (F1). */
    public static boolean isHudHidden() {
        return mc().options.hideGui;
    }

    public static void setHudHidden(boolean hidden) {
        mc().options.hideGui = hidden;
    }

    /** Shows a message above the hotbar (client-side only). */
    public static void actionBar(Component message) {
        mc().gui.setOverlayMessage(message, false);
    }

    public static void clearToasts() {
        mc().getToastManager().clear();
    }

    public static RenderTarget mainRenderTarget() {
        return mc().getMainRenderTarget();
    }

}
