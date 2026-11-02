package com.armorstudio.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Automatic profile switching. An empty string means "no automatic profile". Server rules are matched by the
 * lower-cased address the player connected with and take precedence over the multiplayer default.
 */
public final class AutoProfileSettings {
    public boolean enabled = true;
    public String singleplayer = "";
    public String multiplayer = "";
    public String realms = "";
    public Map<String, String> servers = new LinkedHashMap<>();

    public AutoProfileSettings validate() {
        if (singleplayer == null) singleplayer = "";
        if (multiplayer == null) multiplayer = "";
        if (realms == null) realms = "";
        Map<String, String> clean = new LinkedHashMap<>();
        if (servers != null) {
            servers.forEach((k, v) -> {
                if (k != null && !k.isBlank() && v != null && !v.isBlank()) clean.put(normalizeAddress(k), v);
            });
        }
        servers = clean;
        return this;
    }

    public static String normalizeAddress(String address) {
        String a = address.trim().toLowerCase(Locale.ROOT);
        if (a.endsWith(":25565")) a = a.substring(0, a.length() - 6);
        if (a.endsWith(".")) a = a.substring(0, a.length() - 1);
        return a;
    }

    /** Replaces references to a renamed profile; an empty {@code newName} removes them. */
    public void renameProfile(String oldName, String newName) {
        if (singleplayer.equals(oldName)) singleplayer = newName;
        if (multiplayer.equals(oldName)) multiplayer = newName;
        if (realms.equals(oldName)) realms = newName;
        if (newName.isEmpty()) {
            servers.values().removeIf(v -> v.equals(oldName));
        } else {
            servers.replaceAll((k, v) -> v.equals(oldName) ? newName : v);
        }
    }
}
