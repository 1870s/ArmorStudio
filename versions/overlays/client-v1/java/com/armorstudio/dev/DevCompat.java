package com.armorstudio.dev;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Version differences used only by the development self test (26.1.x). */
final class DevCompat {
    private DevCompat() {}

    static EntityType<Mannequin> mannequinType() {
        return EntityType.MANNEQUIN;
    }

    static WorldDimensions flatWorld(HolderLookup.Provider registries) {
        return WorldPresets.createFlatWorldDimensions(registries);
    }
}
