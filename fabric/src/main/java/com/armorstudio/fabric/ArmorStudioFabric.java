package com.armorstudio.fabric;

import com.armorstudio.ArmorStudio;
import com.armorstudio.hud.ArmorHud;
import com.armorstudio.platform.Platform;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/** Fabric entry point: wires Fabric API callbacks into {@link ArmorStudio}. */
public class ArmorStudioFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(ArmorStudio.KEY_CATEGORY_ID);
        ArmorStudio.init(new Platform() {
            @Override
            public Path configDir() {
                return FabricLoader.getInstance().getConfigDir();
            }

            @Override
            public String loaderName() {
                return "fabric";
            }

            @Override
            public boolean isModLoaded(String modId) {
                return FabricLoader.getInstance().isModLoaded(modId);
            }
        }, category);
        for (KeyMapping key : ArmorStudio.keyMappings()) KeyMappingHelper.registerKeyMapping(key);

        HudElementRegistry.addLast(ArmorStudio.HUD_ID, ArmorHud::extract);
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> ArmorStudio.onClientTick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> ArmorStudio.onShutdown());
    }
}
