package com.armorstudio.config;

import com.armorstudio.ArmorStudio;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Loads, validates and saves the config. All config objects are owned by the client thread; saving serializes on the
 * client thread and only the file write happens on a background thread, so nothing is shared across threads.
 */
public final class ConfigManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int AUTOSAVE_DELAY_TICKS = 40;

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ArmorStudio config writer");
        t.setDaemon(true);
        return t;
    });

    private static StudioConfig config = new StudioConfig().validate();
    private static Path file;
    private static int revision;
    private static int dirtyTicks = -1;

    private ConfigManager() {}

    public static StudioConfig get() {
        return config;
    }

    /**
     * Increases whenever any setting changes. Render code caches derived data and compares against this number,
     * which keeps the per-frame cost to a single int comparison.
     */
    public static int revision() {
        return revision;
    }

    public static Path directory() {
        return ArmorStudio.platform().configDir().resolve(ArmorStudio.MOD_ID);
    }

    public static void load() {
        file = ArmorStudio.platform().configDir().resolve(ArmorStudio.MOD_ID + ".json");
        StudioConfig loaded = null;
        if (Files.isRegularFile(file)) {
            try {
                loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), StudioConfig.class);
            } catch (IOException | JsonParseException | IllegalStateException e) {
                ArmorStudio.LOGGER.error("Could not read {}, keeping a backup and starting with defaults", file, e);
                backupBrokenFile();
            }
        }
        boolean fresh = loaded == null;
        config = (fresh ? new StudioConfig() : loaded).validate();
        revision++;
        if (fresh) saveNow();
    }

    /** Marks the config as changed: render caches refresh immediately, the file is written shortly after. */
    public static void markDirty() {
        revision++;
        dirtyTicks = AUTOSAVE_DELAY_TICKS;
    }

    /** Invalidates render caches without saving (for runtime state such as automatic profile switching). */
    public static void markRenderChanged() {
        revision++;
    }

    /** Called every client tick; writes pending changes once they have settled. */
    public static void tick() {
        if (dirtyTicks > 0 && --dirtyTicks == 0) {
            dirtyTicks = -1;
            saveNow();
        }
    }

    /** Writes the config right away (on the writer thread). */
    public static void saveNow() {
        dirtyTicks = -1;
        if (file == null) return;
        String json = GSON.toJson(config);
        Path target = file;
        WRITER.execute(() -> writeAtomically(target, json));
    }

    /** Blocks until pending writes are done. Used on shutdown and by the self test. */
    public static void flush() {
        if (dirtyTicks > 0) saveNow();
        try {
            WRITER.submit(() -> {}).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            ArmorStudio.LOGGER.warn("Timed out waiting for the config writer", e);
        }
    }

    /** Re-reads the file from disk, discarding unsaved changes. */
    public static void reload() {
        dirtyTicks = -1;
        flush();
        load();
    }

    /** Replaces everything with defaults (built-in profiles included). */
    public static void resetAll() {
        config = new StudioConfig().validate();
        markDirty();
    }

    static void writeAtomically(Path target, String json) {
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            ArmorStudio.LOGGER.error("Could not save {}", target, e);
        }
    }

    private static void backupBrokenFile() {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis()),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            ArmorStudio.LOGGER.warn("Could not back up the broken config", e);
        }
    }
}
