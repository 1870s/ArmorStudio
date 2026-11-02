package com.armorstudio;

import com.armorstudio.compat.InputCompat;
import com.armorstudio.compat.ClientCompat;
import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.StudioConfig;
import com.armorstudio.gui.StudioScreen;
import com.armorstudio.hud.DurabilityWarnings;
import com.armorstudio.hud.Notifications;
import com.armorstudio.platform.Platform;
import com.armorstudio.profile.ProfileManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/** Loader independent core. The Fabric and NeoForge entry points only forward loader events to this class. */
public final class ArmorStudio {
    public static final String MOD_ID = "armorstudio";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier KEY_CATEGORY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "main");
    public static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "armor_hud");

    public static KeyMapping OPEN_MENU;
    public static KeyMapping TOGGLE_SELF;
    public static KeyMapping TOGGLE_OTHERS;
    public static KeyMapping NEXT_PROFILE;
    public static KeyMapping PREVIOUS_PROFILE;
    public static KeyMapping TOGGLE_HUD;
    public static KeyMapping TOGGLE_MOD;

    private static final List<KeyMapping> KEYS = new ArrayList<>();
    private static final List<Runnable> TICK_LISTENERS = new ArrayList<>();
    private static Platform platform;

    private ArmorStudio() {}

    /** Called from the loader entry point. Creates the key mappings; the loader registers them. */
    public static void init(Platform loaderPlatform, KeyMapping.Category category) {
        platform = loaderPlatform;
        OPEN_MENU = key("open_menu", InputConstants.KEY_O, category);
        TOGGLE_SELF = key("toggle_self", InputConstants.KEY_H, category);
        TOGGLE_OTHERS = key("toggle_others", InputConstants.UNKNOWN.getValue(), category);
        NEXT_PROFILE = key("next_profile", InputConstants.UNKNOWN.getValue(), category);
        PREVIOUS_PROFILE = key("previous_profile", InputConstants.UNKNOWN.getValue(), category);
        TOGGLE_HUD = key("toggle_hud", InputConstants.UNKNOWN.getValue(), category);
        TOGGLE_MOD = key("toggle_mod", InputConstants.UNKNOWN.getValue(), category);
        ConfigManager.load();
        String selfTest = System.getProperty("armorstudio.selftest");
        if (selfTest != null && !selfTest.isBlank() && !selfTest.equals("false")) installSelfTest();
    }

    private static KeyMapping key(String name, int defaultKey, KeyMapping.Category category) {
        KeyMapping mapping = new KeyMapping("key.armorstudio." + name, InputCompat.keyboard(), defaultKey, category);
        KEYS.add(mapping);
        return mapping;
    }

    public static List<KeyMapping> keyMappings() {
        return KEYS;
    }

    public static Platform platform() {
        return platform;
    }

    public static void addTickListener(Runnable listener) {
        TICK_LISTENERS.add(listener);
    }

    /** Called at the end of every client tick. */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        StudioConfig cfg = ConfigManager.get();
        while (OPEN_MENU.consumeClick()) {
            if (ClientCompat.screen() == null) ClientCompat.setScreen(new StudioScreen(null));
        }
        while (TOGGLE_SELF.consumeClick()) {
            cfg.quickHideSelf = !cfg.quickHideSelf;
            ConfigManager.markDirty();
            Notifications.show(Component.translatable(cfg.quickHideSelf ? "armorstudio.notify.self_hidden" : "armorstudio.notify.self_shown"));
        }
        while (TOGGLE_OTHERS.consumeClick()) {
            var others = ProfileManager.effective().others;
            boolean anyVisible = others.head.visible || others.chest.visible || others.legs.visible || others.feet.visible;
            others.setAllVisible(!anyVisible);
            ConfigManager.markDirty();
            Notifications.show(Component.translatable(anyVisible ? "armorstudio.notify.others_hidden" : "armorstudio.notify.others_shown"));
        }
        while (NEXT_PROFILE.consumeClick()) ProfileManager.cycle(1);
        while (PREVIOUS_PROFILE.consumeClick()) ProfileManager.cycle(-1);
        while (TOGGLE_HUD.consumeClick()) {
            cfg.hud.enabled = !cfg.hud.enabled;
            ConfigManager.markDirty();
            Notifications.show(Component.translatable(cfg.hud.enabled ? "armorstudio.notify.hud_on" : "armorstudio.notify.hud_off"));
        }
        while (TOGGLE_MOD.consumeClick()) {
            cfg.enabled = !cfg.enabled;
            ConfigManager.markDirty();
            Notifications.show(Component.translatable(cfg.enabled ? "armorstudio.notify.mod_on" : "armorstudio.notify.mod_off"));
        }

        ProfileManager.tick(mc);
        DurabilityWarnings.tick(mc);
        ConfigManager.tick();
        for (Runnable listener : TICK_LISTENERS) listener.run();
    }

    /** Called when the game shuts down: makes sure pending changes reach the disk. */
    public static void onShutdown() {
        ConfigManager.flush();
    }

    /** Development-only in-game test; the class is excluded from release jars, so it is loaded reflectively. */
    private static void installSelfTest() {
        try {
            Class.forName("com.armorstudio.dev.SelfTest").getMethod("install").invoke(null);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Self test requested but not available", e);
        }
    }
}
