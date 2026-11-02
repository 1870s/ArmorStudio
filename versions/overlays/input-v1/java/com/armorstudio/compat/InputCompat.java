package com.armorstudio.compat;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import net.minecraft.util.Util;

/** Input and platform differences, "v1" flavor (26.1.x and 26.2, GLFW based). */
public final class InputCompat {
    private InputCompat() {}

    /** Input type for keyboard key mappings. */
    public static InputConstants.Type keyboard() {
        return InputConstants.Type.KEYSYM;
    }

    /** Opens a folder in the system file browser. */
    public static void openFolder(Path folder) {
        Util.getPlatform().openPath(folder);
    }
}
