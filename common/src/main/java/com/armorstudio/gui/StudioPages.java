package com.armorstudio.gui;

import com.armorstudio.compat.InputCompat;
import com.armorstudio.compat.ClientCompat;
import com.armorstudio.ArmorStudio;
import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.HudSettings;
import com.armorstudio.config.Options;
import com.armorstudio.config.PieceStyle;
import com.armorstudio.config.PlayerLook;
import com.armorstudio.config.Profile;
import com.armorstudio.config.StudioConfig;
import com.armorstudio.gui.widget.ArmorDetailRow;
import com.armorstudio.gui.widget.ButtonRow;
import com.armorstudio.gui.widget.ChoiceRow;
import com.armorstudio.gui.widget.ColorRow;
import com.armorstudio.gui.widget.DualToggleRow;
import com.armorstudio.gui.widget.HeaderRow;
import com.armorstudio.gui.widget.KeyRow;
import com.armorstudio.gui.widget.NoteRow;
import com.armorstudio.gui.widget.ProfileRow;
import com.armorstudio.gui.widget.Row;
import com.armorstudio.gui.widget.SliderRow;
import com.armorstudio.gui.widget.ToggleRow;
import com.armorstudio.hud.Notifications;
import com.armorstudio.profile.ProfileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** Builds the rows of each settings page. Rows read and write the live config through lambdas. */
final class StudioPages {
    private StudioPages() {}

    private static StudioConfig cfg() {
        return ConfigManager.get();
    }

    private static Component t(String key, Object... args) {
        return Component.translatable("armorstudio." + key, args);
    }

    /** Label + description pair from "armorstudio.opt.&lt;key&gt;" and "armorstudio.opt.&lt;key&gt;.desc". */
    private static Component label(String key) {
        return t("opt." + key);
    }

    private static Component desc(String key) {
        return t("opt." + key + ".desc");
    }

    static List<Row> build(StudioScreen screen, StudioScreen.Page page) {
        List<Row> rows = new ArrayList<>();
        Font font = screen.font();
        switch (page) {
            case GENERAL -> general(rows, font);
            case VISIBILITY -> visibility(rows, font, StudioScreen.editOthers);
            case PLAYERS -> players(rows, font);
            case APPEARANCE -> appearance(rows, font, StudioScreen.editOthers, StudioScreen.editPiece);
            case EQUIPMENT -> equipment(rows, font, StudioScreen.editOthers);
            case HUD -> hud(rows, font, screen);
            case PROFILES -> profiles(rows, font, screen);
            case KEYBINDS -> keybinds(rows, font, screen);
            case COMPAT -> compat(rows, font, screen);
        }
        return rows;
    }

    private static PlayerLook look(boolean others) {
        return ProfileManager.effective().look(!others);
    }

    private static <E extends Enum<E> & Options.Labeled> ChoiceRow<E> enumChoice(String key, E[] values, Supplier<E> get,
                                                                                java.util.function.Consumer<E> set, E def) {
        return new ChoiceRow<>(label(key), desc(key), () -> Arrays.asList(values), get, set, def, Options.Labeled::label);
    }

    // ------------------------------------------------------------------ General

    private static void general(List<Row> rows, Font font) {
        StudioConfig c = cfg();
        rows.add(new ToggleRow(label("enabled"), desc("enabled"), () -> c.enabled, v -> c.enabled = v, true));
        rows.add(new ToggleRow(label("quick_hide_self"), desc("quick_hide_self"), () -> c.quickHideSelf, v -> c.quickHideSelf = v, false));
        rows.add(new ToggleRow(label("notifications"), desc("notifications"), () -> c.notifications, v -> c.notifications = v, true));
        rows.add(enumChoice("inventory_preview", Options.InventoryPreview.values(), () -> c.inventoryPreview, v -> c.inventoryPreview = v,
            Options.InventoryPreview.SAME_AS_WORLD));

        rows.add(new HeaderRow(t("section.glint")));
        Minecraft mc = Minecraft.getInstance();
        rows.add(vanillaPercent("glint_strength", mc.options.glintStrength(), 75));
        rows.add(vanillaPercent("glint_speed", mc.options.glintSpeed(), 50));
        rows.add(new NoteRow(font, t("note.glint_global"), Theme.ACCENT));

        rows.add(new HeaderRow(t("section.about")));
        rows.add(new NoteRow(font, t("note.client_side"), Theme.GOOD));
    }

    /** A slider bound to one of Minecraft's own 0..1 options (saved to options.txt). */
    private static SliderRow vanillaPercent(String key, OptionInstance<Double> option, int def) {
        return new SliderRow(label(key), desc(key), () -> (int) Math.round(option.get() * 100), v -> {
            option.set(v / 100.0);
            Minecraft.getInstance().options.save();
        }, 0, 100, 5, def, SliderRow.percent());
    }

    // ------------------------------------------------------------------ Armor visibility

    private static void visibility(List<Row> rows, Font font, boolean others) {
        PlayerLook look = look(others);
        rows.add(new ButtonRow()
            .add(t("action.show_all"), t("action.show_all.desc"), () -> {
                look.setAllVisible(true);
                ConfigManager.markDirty();
            })
            .add(t("action.hide_all"), t("action.hide_all.desc"), () -> {
                look.setAllVisible(false);
                ConfigManager.markDirty();
            }));
        for (ArmorSlot slot : ArmorSlot.all()) {
            PieceStyle style = look.piece(slot);
            rows.add(new ToggleRow(t("opt.show_piece", slot.displayName()), t("opt.show_piece.desc." + slot.key),
                () -> style.visible, v -> style.visible = v, true));
        }
        rows.add(new ToggleRow(label("head_items"), desc("head_items"), () -> look.showHeadItems, v -> look.showHeadItems = v, true));
        if (!others) {
            rows.add(new NoteRow(font, () -> cfg().quickHideSelf ? t("note.quick_hide_active") : t("note.quick_hide_hint"),
                cfg().quickHideSelf ? Theme.WARN : Theme.ACCENT));
        }
        rows.add(new NoteRow(font, t("note.visual_only"), Theme.GOOD));
    }

    // ------------------------------------------------------------------ Player visibility

    private static void players(List<Row> rows, Font font) {
        Profile p = ProfileManager.effective();
        rows.add(new HeaderRow(t("section.quick_modes")));
        rows.add(new ButtonRow()
            .add(t("mode.show_everything"), t("mode.show_everything.desc"), () -> mode(p, true, true, false))
            .add(t("mode.hide_mine"), t("mode.hide_mine.desc"), () -> mode(p, false, true, false))
            .add(t("mode.hide_others"), t("mode.hide_others.desc"), () -> mode(p, true, false, false)));
        rows.add(new ButtonRow()
            .add(t("mode.only_mine"), t("mode.only_mine.desc"), () -> mode(p, true, false, false))
            .add(t("mode.only_others"), t("mode.only_others.desc"), () -> mode(p, false, true, false))
            .add(t("mode.others_no_helmet"), t("mode.others_no_helmet.desc"), () -> mode(p, true, true, true)));

        rows.add(new HeaderRow(t("section.custom_visibility")));
        List<DualToggleRow> duals = new ArrayList<>();
        for (ArmorSlot slot : ArmorSlot.all()) {
            duals.add(new DualToggleRow(slot.displayName(), null,
                () -> p.self.piece(slot).visible, v -> p.self.piece(slot).visible = v,
                () -> p.others.piece(slot).visible, v -> p.others.piece(slot).visible = v));
        }
        duals.add(new DualToggleRow(t("piece.elytra"), null, () -> p.self.elytra.visible, v -> p.self.elytra.visible = v,
            () -> p.others.elytra.visible, v -> p.others.elytra.visible = v));
        duals.add(new DualToggleRow(t("piece.head_items"), null, () -> p.self.showHeadItems, v -> p.self.showHeadItems = v,
            () -> p.others.showHeadItems, v -> p.others.showHeadItems = v));
        rows.add(new ColumnLabels());
        rows.addAll(duals);
        rows.add(new NoteRow(font, t("note.others_local"), Theme.ACCENT));
    }

    /** Applies a quick visibility mode to armor pieces (elytra and head items are left alone). */
    private static void mode(Profile p, boolean self, boolean others, boolean othersNoHelmet) {
        p.self.setAllVisible(self);
        p.others.setAllVisible(others);
        if (othersNoHelmet) p.others.head.visible = false;
        ConfigManager.markDirty();
    }

    /** "You" / "Others" captions aligned with the switches of the {@link DualToggleRow}s below. */
    private static final class ColumnLabels extends Row {
        @Override
        public int height() {
            return 12;
        }

        @Override
        protected void extractRow(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt) {
            g.centeredText(font, t("target.self_short"), DualToggleRow.selfColumn(x, width), y + 2, Theme.TEXT_DIM);
            g.centeredText(font, t("target.others_short"), DualToggleRow.othersColumn(x, width), y + 2, Theme.TEXT_DIM);
        }
    }

    // ------------------------------------------------------------------ Appearance

    private static void appearance(List<Row> rows, Font font, boolean others, ArmorSlot slot) {
        PlayerLook look = look(others);
        PieceStyle s = look.piece(slot);
        styleRows(rows, s, true);
        rows.add(new ButtonRow()
            .add(t("action.apply_all"), t("action.apply_all.desc"), () -> {
                for (ArmorSlot other : ArmorSlot.all()) if (other != slot) look.piece(other).copyAppearanceFrom(s);
                ConfigManager.markDirty();
                Notifications.show(t("notify.applied_all"));
            })
            .add(() -> t("action.reset_piece"), t("action.reset_piece.desc"), () -> {
                s.copyAppearanceFrom(new PieceStyle());
                ConfigManager.markDirty();
            }, () -> false, () -> true, true));
        rows.add(new NoteRow(font, t("note.materials"), Theme.ACCENT));
        rows.add(new NoteRow(font, t("note.translucency"), Theme.ACCENT));
    }

    /** Opacity, color, brightness and glint rows shared by armor pieces and the elytra. */
    private static void styleRows(List<Row> rows, PieceStyle s, boolean trims) {
        PieceStyle d = new PieceStyle();
        rows.add(new SliderRow(label("opacity"), desc("opacity"), () -> s.opacity, v -> s.opacity = v, 0, 100, 5, d.opacity, SliderRow.percent())
            .enabledWhen(() -> s.visible));
        rows.add(new ToggleRow(label("color_enabled"), desc("color_enabled"), () -> s.colorEnabled, v -> s.colorEnabled = v, false)
            .enabledWhen(() -> s.visible));
        rows.add(new ColorRow(label("color"), desc("color"), () -> s.color, v -> s.color = v, d.color)
            .enabledWhen(() -> s.visible && s.colorEnabled));
        rows.add(new SliderRow(label("color_strength"), desc("color_strength"), () -> s.colorStrength, v -> s.colorStrength = v, 0, 100, 5,
            d.colorStrength, SliderRow.percent()).enabledWhen(() -> s.visible && s.colorEnabled));
        rows.add(new SliderRow(label("brightness"), desc("brightness"), () -> s.brightness, v -> s.brightness = v, 20, 100, 5,
            d.brightness, SliderRow.percent()).enabledWhen(() -> s.visible));
        rows.add(enumChoice("glint", Options.GlintMode.values(), () -> s.glint, v -> s.glint = v, Options.GlintMode.DEFAULT)
            .enabledWhen(() -> s.visible));
        if (trims) {
            rows.add(new ToggleRow(label("trims"), desc("trims"), () -> s.showTrim, v -> s.showTrim = v, true).enabledWhen(() -> s.visible));
        }
    }

    // ------------------------------------------------------------------ Elytra & equipment

    private static void equipment(List<Row> rows, Font font, boolean others) {
        PlayerLook look = look(others);
        rows.add(new HeaderRow(t("section.elytra")));
        rows.add(new ToggleRow(label("elytra"), desc("elytra"), () -> look.elytra.visible, v -> look.elytra.visible = v, true));
        styleRows(rows, look.elytra, false);
        rows.add(new HeaderRow(t("section.head_items")));
        rows.add(new ToggleRow(label("head_items"), desc("head_items"), () -> look.showHeadItems, v -> look.showHeadItems = v, true));
        rows.add(new NoteRow(font, t("note.accessories"), Theme.ACCENT));
    }

    // ------------------------------------------------------------------ HUD

    private static void hud(List<Row> rows, Font font, StudioScreen screen) {
        HudSettings h = cfg().hud;
        HudSettings d = new HudSettings();
        rows.add(new ToggleRow(label("hud_enabled"), desc("hud_enabled"), () -> h.enabled, v -> h.enabled = v, true));
        rows.add(new ButtonRow()
            .add(t("action.hud_editor"), t("action.hud_editor.desc"), () -> ClientCompat.setScreen(new HudEditorScreen(screen)))
            .add(t("action.hud_reset_position"), t("action.hud_reset_position.desc"), () -> {
                h.anchor = d.anchor;
                h.offsetX = d.offsetX;
                h.offsetY = d.offsetY;
                ConfigManager.markDirty();
            }));
        rows.add(enumChoice("hud_anchor", Options.HudAnchor.values(), () -> h.anchor, v -> h.anchor = v, d.anchor));
        rows.add(new SliderRow(label("hud_offset_x"), desc("hud_offset_x"), () -> h.offsetX, v -> h.offsetX = v, -200, 400, 1, d.offsetX, v -> v + " px"));
        rows.add(new SliderRow(label("hud_offset_y"), desc("hud_offset_y"), () -> h.offsetY, v -> h.offsetY = v, -200, 400, 1, d.offsetY, v -> v + " px"));
        rows.add(new SliderRow(label("hud_scale"), desc("hud_scale"), () -> h.scale, v -> h.scale = v, 50, 200, 5, d.scale, SliderRow.percent()));
        rows.add(new SliderRow(label("hud_background"), desc("hud_background"), () -> h.backgroundOpacity, v -> h.backgroundOpacity = v, 0, 100, 5,
            d.backgroundOpacity, SliderRow.percent()));
        rows.add(enumChoice("hud_layout", Options.HudLayout.values(), () -> h.layout, v -> h.layout = v, d.layout));
        rows.add(enumChoice("hud_text", Options.DurabilityText.values(), () -> h.text, v -> h.text = v, d.text));
        rows.add(new ToggleRow(label("hud_color_text"), desc("hud_color_text"), () -> h.colorText, v -> h.colorText = v, d.colorText));
        rows.add(new ToggleRow(label("hud_show_empty"), desc("hud_show_empty"), () -> h.showEmptySlots, v -> h.showEmptySlots = v, d.showEmptySlots));
        rows.add(new ToggleRow(label("hud_hide_no_armor"), desc("hud_hide_no_armor"), () -> h.hideWhenNoArmor, v -> h.hideWhenNoArmor = v, d.hideWhenNoArmor));
        rows.add(new ToggleRow(label("hud_enchant_marker"), desc("hud_enchant_marker"), () -> h.showEnchantMarker, v -> h.showEnchantMarker = v,
            d.showEnchantMarker));

        rows.add(new HeaderRow(t("section.low_durability")));
        rows.add(new SliderRow(label("low_threshold"), desc("low_threshold"), () -> h.lowThreshold, v -> h.lowThreshold = v, 0, 50, 1, d.lowThreshold,
            v -> v == 0 ? t("ui.off").getString() : v + "%"));
        rows.add(new ToggleRow(label("low_blink"), desc("low_blink"), () -> h.blinkWhenLow, v -> h.blinkWhenLow = v, d.blinkWhenLow)
            .enabledWhen(() -> h.lowThreshold > 0));
        rows.add(new ToggleRow(label("low_sound"), desc("low_sound"), () -> h.warnSound, v -> h.warnSound = v, d.warnSound)
            .enabledWhen(() -> h.lowThreshold > 0));
        rows.add(new ToggleRow(label("low_message"), desc("low_message"), () -> h.warnMessage, v -> h.warnMessage = v, d.warnMessage)
            .enabledWhen(() -> h.lowThreshold > 0));

        rows.add(new HeaderRow(t("section.equipped")));
        for (ArmorSlot slot : ArmorSlot.all()) rows.add(new ArmorDetailRow(slot));
        rows.add(new NoteRow(font, t("note.hud_real"), Theme.GOOD));
    }

    // ------------------------------------------------------------------ Profiles

    private static void profiles(List<Row> rows, Font font, StudioScreen screen) {
        rows.add(new HeaderRow(t("section.profiles")));
        for (Profile profile : ProfileManager.all()) {
            rows.add(new ProfileRow(profile, () -> screen.selectedProfile, screen::selectProfile, (p, action) -> profileAction(screen, p, action)));
        }
        rows.add(new ButtonRow()
            .add(t("action.import_clipboard"), t("action.import_clipboard.desc"), () -> {
                Profile imported = ProfileManager.importJson(Minecraft.getInstance().keyboardHandler.getClipboard());
                Notifications.show(imported == null ? t("notify.import_failed") : t("notify.imported", imported.name));
                if (imported != null) screen.selectProfile(imported);
            })
            .add(t("action.import_folder"), t("action.import_folder.desc"), () -> {
                try {
                    int count = ProfileManager.importFolder();
                    Notifications.show(t("notify.imported_folder", count));
                    screen.requestRebuild();
                } catch (IOException e) {
                    ArmorStudio.LOGGER.warn("Profile import failed", e);
                    Notifications.show(t("notify.import_failed"));
                }
            })
            .add(() -> t("action.restore_presets"), t("action.restore_presets.desc"), () -> {
                Notifications.show(t("notify.presets_restored", ProfileManager.restorePresets()));
                screen.requestRebuild();
            }, () -> false, () -> true, false));
        rows.add(new NoteRow(font, t("note.profiles"), Theme.ACCENT));

        var auto = cfg().autoProfiles;
        rows.add(new HeaderRow(t("section.auto_profiles")));
        rows.add(new ToggleRow(label("auto_enabled"), desc("auto_enabled"), () -> auto.enabled, v -> {
            auto.enabled = v;
            ProfileManager.refreshAuto();
        }, true));
        rows.add(profileChoice("auto_singleplayer", () -> auto.singleplayer, v -> auto.singleplayer = v).enabledWhen(() -> auto.enabled));
        rows.add(profileChoice("auto_multiplayer", () -> auto.multiplayer, v -> auto.multiplayer = v).enabledWhen(() -> auto.enabled));
        rows.add(profileChoice("auto_realms", () -> auto.realms, v -> auto.realms = v).enabledWhen(() -> auto.enabled));

        rows.add(new HeaderRow(t("section.server_rules")));
        String server = ProfileManager.currentServerAddress();
        rows.add(new ButtonRow().add(
            () -> server == null ? t("action.bind_server_none") : t("action.bind_server", server),
            t("action.bind_server.desc"),
            () -> {
                auto.servers.put(server, ProfileManager.effective().name);
                ConfigManager.markDirty();
                ProfileManager.refreshAuto();
                Notifications.show(t("notify.server_bound", server, ProfileManager.effective().name));
                screen.requestRebuild();
            }, () -> false, () -> server != null && auto.enabled, false));
        for (Map.Entry<String, String> rule : auto.servers.entrySet()) {
            String address = rule.getKey();
            rows.add(new ChoiceRow<>(Component.literal(address), t("opt.server_rule.desc"), StudioPages::profileNamesWithNone,
                () -> auto.servers.getOrDefault(address, ""), v -> {
                    if (v.isEmpty()) {
                        auto.servers.remove(address);
                        screen.requestRebuild();
                    } else {
                        auto.servers.put(address, v);
                    }
                    ProfileManager.refreshAuto();
                }, null, StudioPages::profileName).enabledWhen(() -> auto.enabled));
        }
        if (auto.servers.isEmpty()) rows.add(new NoteRow(font, t("note.no_server_rules"), Theme.TEXT_MUTED));
    }

    private static ChoiceRow<String> profileChoice(String key, Supplier<String> get, java.util.function.Consumer<String> set) {
        return new ChoiceRow<>(label(key), desc(key), StudioPages::profileNamesWithNone, get, v -> {
            set.accept(v);
            ProfileManager.refreshAuto();
        }, "", StudioPages::profileName);
    }

    private static List<String> profileNamesWithNone() {
        List<String> names = new ArrayList<>();
        names.add("");
        for (Profile p : ProfileManager.all()) names.add(p.name);
        return names;
    }

    private static Component profileName(String name) {
        return name == null || name.isEmpty() ? t("profiles.none") : Component.literal(name);
    }

    private static void profileAction(StudioScreen screen, Profile profile, ProfileRow.Action action) {
        switch (action) {
            case USE -> ProfileManager.select(profile, true);
            case DUPLICATE -> screen.selectProfile(ProfileManager.duplicate(profile));
            case EXPORT -> {
                Minecraft.getInstance().keyboardHandler.setClipboard(ProfileManager.toJson(profile));
                try {
                    Path file = ProfileManager.exportToFile(profile);
                    Notifications.show(t("notify.exported", file.getFileName().toString()));
                } catch (IOException e) {
                    ArmorStudio.LOGGER.warn("Profile export failed", e);
                    Notifications.show(t("notify.exported_clipboard"));
                }
            }
            case UP -> ProfileManager.move(profile, -1);
            case DOWN -> ProfileManager.move(profile, 1);
            case DELETE -> {
                if (!ProfileManager.delete(profile)) {
                    Notifications.show(t("notify.delete_last"));
                    return;
                }
                if (screen.selectedProfile == profile) screen.selectProfile(null);
                Notifications.show(t("notify.deleted", profile.name));
            }
        }
        screen.requestRebuild();
    }

    // ------------------------------------------------------------------ Keybinds

    private static void keybinds(List<Row> rows, Font font, StudioScreen screen) {
        String[] keys = {"open_menu", "toggle_self", "toggle_others", "next_profile", "previous_profile", "toggle_hud", "toggle_mod"};
        List<KeyMapping> mappings = ArmorStudio.keyMappings();
        for (int i = 0; i < mappings.size(); i++) rows.add(new KeyRow(mappings.get(i), t("keys." + keys[i] + ".desc")));
        Minecraft mc = Minecraft.getInstance();
        rows.add(new ButtonRow().add(t("action.edit_keys"), t("action.edit_keys.desc"), () -> ClientCompat.setScreen(new KeyBindsScreen(screen, mc.options))));
        rows.add(new NoteRow(font, t("note.keys"), Theme.ACCENT));
    }

    // ------------------------------------------------------------------ Compatibility

    private static void compat(List<Row> rows, Font font, StudioScreen screen) {
        StudioConfig c = cfg();
        rows.add(enumChoice("translucency", Options.Translucency.values(), () -> c.translucency, v -> c.translucency = v, Options.Translucency.SMOOTH));
        rows.add(new ToggleRow(label("mannequins"), desc("mannequins"), () -> c.affectMannequins, v -> c.affectMannequins = v, false));

        rows.add(new HeaderRow(t("section.detected")));
        boolean any = false;
        for (String[] mod : KNOWN_MODS) {
            boolean loaded = false;
            for (String id : mod[0].split(",")) loaded |= ArmorStudio.platform().isModLoaded(id);
            if (!loaded) continue;
            any = true;
            rows.add(new NoteRow(font, Component.literal(mod[1] + ": ").withColor(Theme.TEXT).append(t("compat." + mod[2])), Theme.WARN));
        }
        if (!any) rows.add(new NoteRow(font, t("compat.none"), Theme.GOOD));

        rows.add(new HeaderRow(t("section.maintenance")));
        rows.add(new ButtonRow()
            .add(t("action.open_folder"), t("action.open_folder.desc"), () -> {
                Path dir = ArmorStudio.platform().configDir();
                try {
                    Files.createDirectories(ConfigManager.directory());
                } catch (IOException ignored) {
                    // The config dir itself always exists; the subfolder is a convenience.
                }
                InputCompat.openFolder(dir);
            })
            .add(t("action.reload"), t("action.reload.desc"), () -> {
                ConfigManager.reload();
                ProfileManager.refreshAuto();
                Notifications.show(t("notify.reloaded"));
                screen.requestRebuild();
            })
            .add(() -> t("action.reset_all"), t("action.reset_all.desc"), () -> {
                ConfigManager.resetAll();
                ProfileManager.refreshAuto();
                Notifications.show(t("notify.reset_all"));
                screen.requestRebuild();
            }, () -> false, () -> true, true));
        rows.add(new NoteRow(font, t("note.version", ArmorStudio.platform().loaderName(), SharedConstants.getCurrentVersion().name()), Theme.TEXT_MUTED));
    }

    /** Mod ids (comma separated), display name, translation key of the note. */
    private static final String[][] KNOWN_MODS = {
        {"iris,oculus", "Iris / Oculus", "shaders"},
        {"sodium,embeddium", "Sodium / Embeddium", "sodium"},
        {"skinlayers3d", "3D Skin Layers", "skinlayers"},
        {"cosmeticarmorreworked,cosmetic-armor", "Cosmetic Armor", "cosmetic_armor"},
        {"geckolib", "GeckoLib", "geckolib"},
        {"curios,trinkets,accessories", "Curios / Trinkets / Accessories", "accessories"},
        {"figura", "Figura", "figura"},
        {"entity_model_features,entity_texture_features", "Entity Model/Texture Features", "emf"},
    };
}
