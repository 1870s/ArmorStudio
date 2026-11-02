package com.armorstudio.neoforge;

import com.armorstudio.ArmorStudio;
import com.armorstudio.gui.StudioScreen;
import com.armorstudio.hud.ArmorHud;
import com.armorstudio.platform.Platform;
import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

/** NeoForge entry point: wires NeoForge events into {@link ArmorStudio}. */
@Mod(value = ArmorStudio.MOD_ID, dist = Dist.CLIENT)
public class ArmorStudioNeoForge {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(ArmorStudio.KEY_CATEGORY_ID);

    public ArmorStudioNeoForge(IEventBus modBus, ModContainer container) {
        ArmorStudio.init(new Platform() {
            @Override
            public Path configDir() {
                return FMLPaths.CONFIGDIR.get();
            }

            @Override
            public String loaderName() {
                return "neoforge";
            }

            @Override
            public boolean isModLoaded(String modId) {
                return ModList.get().isLoaded(modId);
            }
        }, CATEGORY);

        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            event.registerCategory(CATEGORY);
            for (KeyMapping key : ArmorStudio.keyMappings()) event.register(key);
        });
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAboveAll(ArmorStudio.HUD_ID, ArmorHud::extract));
        // "Config" button in the NeoForge mod list opens the settings screen.
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new StudioScreen(parent));

        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> ArmorStudio.onClientTick());
        NeoForge.EVENT_BUS.addListener(GameShuttingDownEvent.class, event -> ArmorStudio.onShutdown());
    }
}
