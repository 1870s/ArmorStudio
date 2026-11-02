package com.armorstudio.config;

import com.armorstudio.config.Options.InventoryPreview;
import com.armorstudio.config.Options.Translucency;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Root of the config file (config/armorstudio.json). */
public final class StudioConfig {
    public static final int CURRENT_VERSION = 1;

    public int configVersion = CURRENT_VERSION;
    /** Master switch: when off, every player renders exactly like vanilla. */
    public boolean enabled = true;
    /** Quick toggle (keybind): hides all of your own armor regardless of the active profile. */
    public boolean quickHideSelf = false;
    /** Short on-screen notification when the profile changes or a quick toggle is used. */
    public boolean notifications = true;
    public InventoryPreview inventoryPreview = InventoryPreview.SAME_AS_WORLD;
    public Translucency translucency = Translucency.SMOOTH;
    /** Mannequins use the "other players" look (they are rendered by the player renderer). */
    public boolean affectMannequins = false;

    // Settings screen preview
    public boolean previewSampleArmor = true;
    public boolean previewAutoRotate = true;

    /** The profile chosen by the player. Automatic switching may temporarily override it, but never changes it. */
    public String activeProfile = "Default";
    public List<Profile> profiles = new ArrayList<>();
    public HudSettings hud = new HudSettings();
    public AutoProfileSettings autoProfiles = new AutoProfileSettings();

    public StudioConfig validate() {
        configVersion = CURRENT_VERSION;
        if (inventoryPreview == null) inventoryPreview = InventoryPreview.SAME_AS_WORLD;
        if (translucency == null) translucency = Translucency.SMOOTH;
        hud = hud == null ? new HudSettings() : hud.validate();
        autoProfiles = autoProfiles == null ? new AutoProfileSettings() : autoProfiles.validate();

        List<Profile> clean = new ArrayList<>();
        Set<String> names = new HashSet<>();
        if (profiles != null) {
            for (Profile p : profiles) {
                if (p == null) continue;
                p.validate();
                String base = p.name;
                int n = 2;
                while (!names.add(p.name.toLowerCase(Locale.ROOT))) p.name = base + " " + n++;
                clean.add(p);
            }
        }
        if (clean.isEmpty()) clean.addAll(Presets.builtIn());
        profiles = clean;
        if (activeProfile == null || find(activeProfile) == null) activeProfile = profiles.getFirst().name;
        return this;
    }

    public Profile find(String name) {
        if (name == null || profiles == null) return null;
        for (Profile p : profiles) {
            if (p.name.equalsIgnoreCase(name)) return p;
        }
        return null;
    }
}
