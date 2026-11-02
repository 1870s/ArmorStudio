package com.armorstudio.platform;

import java.nio.file.Path;

/** The few services that differ between Fabric and NeoForge. Implemented by each loader entry point. */
public interface Platform {
    /** The game's config directory. */
    Path configDir();

    /** "fabric" or "neoforge". */
    String loaderName();

    /** Whether a mod with the given id is loaded. Used for compatibility hints only. */
    boolean isModLoaded(String modId);
}
