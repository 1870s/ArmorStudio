package com.armorstudio.dev;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Version differences used only by the development self test (26.2+). */
final class DevCompat {
    private DevCompat() {}

    static EntityType<Mannequin> mannequinType() {
        return EntityTypes.MANNEQUIN;
    }

    static WorldDimensions flatWorld(HolderLookup.Provider registries) {
        return WorldPresets.createTestWorldDimensions(registries);
    }
}
