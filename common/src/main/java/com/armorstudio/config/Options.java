package com.armorstudio.config;

import net.minecraft.network.chat.Component;

/** All enum-valued settings. Each value has a translatable label used by the settings screen. */
public final class Options {
    private Options() {}

    /** Something that can be shown as a choice in the UI. */
    public interface Labeled {
        String labelKey();

        default Component label() {
            return Component.translatable(labelKey());
        }
    }

    /** How the enchantment glint of a piece is drawn. */
    public enum GlintMode implements Labeled {
        DEFAULT, HIDE, FORCE;

        @Override
        public String labelKey() {
            return "armorstudio.glint." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Which look the player model in inventory screens uses. */
    public enum InventoryPreview implements Labeled {
        SAME_AS_WORLD, SHOW_REAL, HIDE_ALL;

        @Override
        public String labelKey() {
            return "armorstudio.inventory_preview." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * How partial opacity is rendered. SMOOTH uses the translucent armor render type; THRESHOLD keeps the opaque
     * render type and simply hides pieces below 50 % (a fallback for shader packs that mishandle translucent entities).
     */
    public enum Translucency implements Labeled {
        SMOOTH, THRESHOLD;

        @Override
        public String labelKey() {
            return "armorstudio.translucency." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public enum HudAnchor implements Labeled {
        TOP_LEFT, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, HOTBAR_LEFT, HOTBAR_RIGHT;

        @Override
        public String labelKey() {
            return "armorstudio.anchor." + name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean right() {
            return this == TOP_RIGHT || this == MIDDLE_RIGHT || this == BOTTOM_RIGHT || this == HOTBAR_RIGHT;
        }
    }

    public enum HudLayout implements Labeled {
        VERTICAL, HORIZONTAL;

        @Override
        public String labelKey() {
            return "armorstudio.layout." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public enum DurabilityText implements Labeled {
        NONE, PERCENT, EXACT, BOTH;

        @Override
        public String labelKey() {
            return "armorstudio.durability_text." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
