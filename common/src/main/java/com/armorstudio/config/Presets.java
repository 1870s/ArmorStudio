package com.armorstudio.config;

import com.armorstudio.config.Options.GlintMode;
import java.util.List;

/** The built-in profiles created on first start (and restorable from the Profiles page). */
public final class Presets {
    private Presets() {}

    public static List<Profile> builtIn() {
        return List.of(defaultProfile(), invisible(), helmetless(), minimal(), pvp(), custom());
    }

    /** Vanilla look for everybody. */
    public static Profile defaultProfile() {
        return new Profile("Default");
    }

    /** Your armor is completely hidden; other players are unchanged. */
    public static Profile invisible() {
        Profile p = new Profile("Invisible Armor");
        p.self.setAllVisible(false);
        p.self.elytra.visible = false;
        return p;
    }

    /** Only your helmet is hidden, so your face stays visible. */
    public static Profile helmetless() {
        Profile p = new Profile("Helmetless");
        p.self.head.visible = false;
        return p;
    }

    /** Only your chestplate and elytra stay visible; trims are hidden for a cleaner silhouette. */
    public static Profile minimal() {
        Profile p = new Profile("Minimal");
        p.self.head.visible = false;
        p.self.legs.visible = false;
        p.self.feet.visible = false;
        p.self.chest.showTrim = false;
        return p;
    }

    /**
     * Combat oriented: your own armor is semi-transparent and your helmet hidden for a clear third-person view,
     * while other players' armor stays fully visible with glint and trims so you can judge their gear.
     */
    public static Profile pvp() {
        Profile p = new Profile("PvP");
        p.self.head.visible = false;
        for (ArmorSlot slot : ArmorSlot.all()) {
            p.self.piece(slot).opacity = 45;
            p.self.piece(slot).glint = GlintMode.HIDE;
        }
        p.self.elytra.opacity = 60;
        return p;
    }

    /** A starting point for your own configuration. */
    public static Profile custom() {
        return new Profile("Custom");
    }
}
