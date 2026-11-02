package com.armorstudio.config;

/** A named, switchable cosmetic configuration. */
public final class Profile {
    public static final int MAX_NAME_LENGTH = 32;

    public String name = "Profile";
    /** How your own player looks. */
    public PlayerLook self = new PlayerLook();
    /** How every other player looks on your screen. */
    public PlayerLook others = new PlayerLook();

    public Profile() {}

    public Profile(String name) {
        this.name = name;
    }

    public Profile copy(String newName) {
        Profile p = new Profile(newName);
        p.self = self.copy();
        p.others = others.copy();
        return p;
    }

    public PlayerLook look(boolean forSelf) {
        return forSelf ? self : others;
    }

    public Profile validate() {
        name = sanitizeName(name);
        if (name.isEmpty()) name = "Profile";
        self = self == null ? new PlayerLook() : self.validate();
        others = others == null ? new PlayerLook() : others.validate();
        return this;
    }

    /** Strips control characters and limits the length. */
    public static String sanitizeName(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        raw.trim().codePoints().filter(cp -> !Character.isISOControl(cp)).forEach(sb::appendCodePoint);
        String s = sb.toString().trim();
        return s.length() > MAX_NAME_LENGTH ? s.substring(0, MAX_NAME_LENGTH).trim() : s;
    }
}
