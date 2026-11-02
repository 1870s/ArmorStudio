package com.armorstudio.compat;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;

/** Input and platform differences, "v2" flavor (26.3+, SDL based). */
public final class InputCompat {
    private InputCompat() {}

    /** Input type for keyboard key mappings. */
    public static InputConstants.Type keyboard() {
        return InputConstants.Type.KEYBOARD;
    }

    /** Opens a folder in the system file browser. */
    public static void openFolder(Path folder) {
        Blaze3D.openPath(folder);
    }
}
