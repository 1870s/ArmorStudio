package com.armorstudio.profile;

import com.armorstudio.ArmorStudio;
import com.armorstudio.config.AutoProfileSettings;
import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.Presets;
import com.armorstudio.config.Profile;
import com.armorstudio.config.StudioConfig;
import com.armorstudio.hud.Notifications;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

/**
 * Profile selection, automatic switching and profile management. Automatic switching never touches the profile the
 * player selected: it only overrides it while connected to a matching world, so leaving a server always restores the
 * player's own choice.
 */
public final class ProfileManager {
    /** Where the player currently is; recomputed every tick, cheap string comparison. */
    private static String worldKey = "";
    /** Profile chosen by automatic switching for the current world, or null. */
    private static String autoProfile;

    private ProfileManager() {}

    private static StudioConfig cfg() {
        return ConfigManager.get();
    }

    /** The profile that is used for rendering right now. */
    public static Profile effective() {
        StudioConfig cfg = cfg();
        if (autoProfile != null) {
            Profile auto = cfg.find(autoProfile);
            if (auto != null) return auto;
        }
        Profile active = cfg.find(cfg.activeProfile);
        return active != null ? active : cfg.profiles.getFirst();
    }

    /** True while automatic switching overrides the selected profile. */
    public static boolean isAutoApplied() {
        return autoProfile != null && cfg().find(autoProfile) != null;
    }

    public static List<Profile> all() {
        return cfg().profiles;
    }

    // ---------------------------------------------------------------- selection

    /** Selects a profile manually. This also suspends automatic switching until the world changes. */
    public static void select(Profile profile, boolean notify) {
        cfg().activeProfile = profile.name;
        autoProfile = null;
        ConfigManager.markDirty();
        if (notify) Notifications.show(Component.translatable("armorstudio.notify.profile", profile.name));
    }

    public static void cycle(int direction) {
        List<Profile> list = all();
        int index = list.indexOf(effective());
        int next = Math.floorMod(index + direction, list.size());
        select(list.get(next), true);
    }

    // ---------------------------------------------------------------- automatic switching

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        String key = currentWorldKey(mc);
        if (key.equals(worldKey)) return;
        worldKey = key;
        String previous = autoProfile;
        autoProfile = resolveAuto(key);
        if (autoProfile != null && cfg().find(autoProfile) == null) autoProfile = null;
        if (autoProfile != null && !autoProfile.equals(previous)) {
            Notifications.show(Component.translatable("armorstudio.notify.profile_auto", autoProfile));
        } else if (autoProfile == null && previous != null && !key.isEmpty()) {
            Notifications.show(Component.translatable("armorstudio.notify.profile", effective().name));
        }
        ConfigManager.markRenderChanged();
    }

    /** "" = not in a world, "sp" = singleplayer, "realms", or "mp:<address>". */
    public static String currentWorldKey(Minecraft mc) {
        if (mc.level == null) return "";
        if (mc.hasSingleplayerServer()) return "sp";
        ServerData server = mc.getCurrentServer();
        if (server == null) return "mp:";
        if (server.isRealm()) return "realms";
        return "mp:" + AutoProfileSettings.normalizeAddress(server.ip);
    }

    /** The current multiplayer server address, or null when not on a regular server. */
    public static String currentServerAddress() {
        return worldKey.startsWith("mp:") && worldKey.length() > 3 ? worldKey.substring(3) : null;
    }

    private static String resolveAuto(String key) {
        AutoProfileSettings auto = cfg().autoProfiles;
        if (!auto.enabled || key.isEmpty()) return null;
        String name;
        if (key.equals("sp")) {
            name = auto.singleplayer;
        } else if (key.equals("realms")) {
            name = auto.realms;
        } else {
            name = auto.servers.getOrDefault(key.substring(3), auto.multiplayer);
        }
        return name == null || name.isEmpty() ? null : name;
    }

    /** Re-evaluates automatic switching (after rules changed). */
    public static void refreshAuto() {
        worldKey = "\u0000";
    }

    // ---------------------------------------------------------------- management

    public static String uniqueName(String wanted) {
        String base = Profile.sanitizeName(wanted);
        if (base.isEmpty()) base = "Profile";
        String name = base;
        int n = 2;
        while (cfg().find(name) != null) {
            String suffix = " " + n++;
            name = (base.length() + suffix.length() > Profile.MAX_NAME_LENGTH
                ? base.substring(0, Profile.MAX_NAME_LENGTH - suffix.length()) : base) + suffix;
        }
        return name;
    }

    public static Profile create(String name) {
        Profile p = new Profile(uniqueName(name));
        cfg().profiles.add(p);
        ConfigManager.markDirty();
        return p;
    }

    public static Profile duplicate(Profile source) {
        Profile p = source.copy(uniqueName(source.name + " Copy"));
        List<Profile> list = cfg().profiles;
        list.add(list.indexOf(source) + 1, p);
        ConfigManager.markDirty();
        return p;
    }

    /** Returns false if the name is empty or taken by another profile. */
    public static boolean rename(Profile profile, String newName) {
        String clean = Profile.sanitizeName(newName);
        if (clean.isEmpty()) return false;
        Profile existing = cfg().find(clean);
        if (existing != null && existing != profile) return false;
        String old = profile.name;
        profile.name = clean;
        if (cfg().activeProfile.equalsIgnoreCase(old)) cfg().activeProfile = clean;
        if (old.equals(autoProfile)) autoProfile = clean;
        cfg().autoProfiles.renameProfile(old, clean);
        ConfigManager.markDirty();
        return true;
    }

    /** Deletes a profile; the last remaining profile can not be deleted. */
    public static boolean delete(Profile profile) {
        List<Profile> list = cfg().profiles;
        if (list.size() <= 1) return false;
        int index = list.indexOf(profile);
        list.remove(profile);
        cfg().autoProfiles.renameProfile(profile.name, "");
        if (profile.name.equals(autoProfile)) autoProfile = null;
        if (cfg().activeProfile.equalsIgnoreCase(profile.name)) {
            cfg().activeProfile = list.get(Math.max(0, Math.min(index, list.size() - 1))).name;
        }
        ConfigManager.markDirty();
        return true;
    }

    public static void move(Profile profile, int direction) {
        List<Profile> list = cfg().profiles;
        int i = list.indexOf(profile);
        int j = i + direction;
        if (i < 0 || j < 0 || j >= list.size()) return;
        list.set(i, list.get(j));
        list.set(j, profile);
        ConfigManager.markDirty();
    }

    /** Re-adds built-in presets that were deleted (existing profiles are left alone). */
    public static int restorePresets() {
        int added = 0;
        for (Profile preset : Presets.builtIn()) {
            if (cfg().find(preset.name) == null) {
                cfg().profiles.add(preset);
                added++;
            }
        }
        if (added > 0) ConfigManager.markDirty();
        return added;
    }

    // ---------------------------------------------------------------- import / export

    public static String toJson(Profile profile) {
        return ConfigManager.GSON.toJson(profile);
    }

    /** Parses and adds a profile from JSON. Returns null if the text is not a valid profile. */
    public static Profile importJson(String json) {
        if (json == null || json.isBlank()) return null;
        Profile p;
        try {
            p = ConfigManager.GSON.fromJson(json.trim(), Profile.class);
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
        if (p == null || (p.self == null && p.others == null)) return null;
        p.validate();
        p.name = uniqueName(p.name);
        cfg().profiles.add(p);
        ConfigManager.markDirty();
        return p;
    }

    /** Writes the profile to config/armorstudio/exports/&lt;name&gt;.json and returns the file. */
    public static Path exportToFile(Profile profile) throws IOException {
        Path dir = ConfigManager.directory().resolve("exports");
        Files.createDirectories(dir);
        String fileName = profile.name.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (fileName.isEmpty()) fileName = "profile";
        Path file = dir.resolve(fileName.toLowerCase(Locale.ROOT).replace(' ', '_') + ".json");
        Files.writeString(file, toJson(profile), StandardCharsets.UTF_8);
        ArmorStudio.LOGGER.info("Exported profile '{}' to {}", profile.name, file);
        return file;
    }

    /** Imports every *.json file from config/armorstudio/imports. Returns the number of imported profiles. */
    public static int importFolder() throws IOException {
        Path dir = ConfigManager.directory().resolve("imports");
        Files.createDirectories(dir);
        int count = 0;
        try (var files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json")).sorted()::iterator) {
                if (importJson(Files.readString(f, StandardCharsets.UTF_8)) != null) count++;
            }
        }
        return count;
    }
}
