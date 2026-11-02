package com.armorstudio.dev;

import com.armorstudio.compat.ClientCompat;
import com.armorstudio.ArmorStudio;
import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.Options;
import com.armorstudio.config.PieceStyle;
import com.armorstudio.config.Profile;
import com.armorstudio.config.StudioConfig;
import com.armorstudio.gui.HudEditorScreen;
import com.armorstudio.gui.StudioScreen;
import com.armorstudio.profile.ProfileManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FileUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;

/**
 * Development-only in-game test (run with -Darmorstudio.selftest=true; excluded from release jars).
 *
 * <p>Creates a flat creative world, equips enchanted and trimmed netherite armor on the player and on a mannequin,
 * walks through the visibility and appearance features while taking screenshots, opens every screen, and checks a few
 * hard facts: hidden pieces are missing from the render state while the real equipment and armor points are unchanged,
 * and settings survive a save/reload round trip. Results are logged with the prefix "[SelfTest]".
 */
public final class SelfTest {
    private static final List<Step> STEPS = new ArrayList<>();
    private static int index;
    private static int wait;
    private static boolean worldRequested;
    private static int failures;
    private static Mannequin mannequin;

    private record Step(String name, int waitTicks, Runnable action) {}

    private SelfTest() {}

    public static void install() {
        ArmorStudio.LOGGER.info("[SelfTest] enabled");
        buildSteps();
        ArmorStudio.addTickListener(SelfTest::tick);
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!worldRequested) {
                // Wait until loading has finished; a fresh game directory first shows the accessibility onboarding screen.
                if (ClientCompat.overlay() != null || ClientCompat.screen() == null) return;
                if (!(ClientCompat.screen() instanceof TitleScreen)) {
                    mc.options.onboardAccessibility = false;
                    mc.options.save();
                    ClientCompat.setScreen(new TitleScreen());
                    return;
                }
                worldRequested = true;
                createWorld(mc);
                return;
            }
            if (mc.player == null || mc.level == null) return;
            if (index == 0 && ClientCompat.screen() != null) return;
            if (wait > 0) {
                wait--;
                return;
            }
            if (index >= STEPS.size()) return;
            Step step = STEPS.get(index++);
            ArmorStudio.LOGGER.info("[SelfTest] step {}: {}", index, step.name);
            step.action.run();
            wait = step.waitTicks;
        } catch (Exception e) {
            failures++;
            ArmorStudio.LOGGER.error("[SelfTest] FAIL exception in step {}", index, e);
            index = STEPS.size() - 1;
        }
    }

    private static void createWorld(Minecraft mc) throws Exception {
        String folder = FileUtil.findAvailableName(mc.getLevelSource().getBaseDir(), "ArmorStudioTest", "");
        LevelSettings settings = new LevelSettings("ArmorStudioTest", GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true,
            WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(folder, settings, new WorldOptions(42L, false, false),
            DevCompat::flatWorld, ClientCompat.screen());
    }

    // ------------------------------------------------------------------ steps

    private static void buildSteps() {
        add("prepare world", 60, () -> {
            Minecraft mc = Minecraft.getInstance();
            StudioConfig cfg = ConfigManager.get();
            cfg.affectMannequins = true;
            cfg.hud.lowThreshold = 15;
            ProfileManager.select(cfg.find("Default"), false);
            onServer(server -> {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                equipArmor(server, player, true);
                ServerLevel level = player.level();
                Mannequin m = DevCompat.mannequinType().create(level, EntitySpawnReason.COMMAND);
                if (m != null) {
                    m.snapTo(player.getX() + 1.6, player.getY(), player.getZ() + 0.6, 200f, 0f);
                    equipArmor(server, m, false);
                    level.addFreshEntity(m);
                }
            });
            ClientCompat.setHudHidden(true);
            mc.options.fov().set(55);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        add("face camera", 40, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.player.setYRot(0f);
            mc.player.setXRot(10f);
            mc.player.yBodyRot = 0f;
            mc.player.yHeadRot = 0f;
            for (var entity : mc.level.entitiesForRendering()) {
                if (entity instanceof Mannequin m) mannequin = m;
            }
            check("mannequin present", mannequin != null);
        });
        shot("01_default");
        add("check vanilla state", 2, () -> checkState("default", false, false));

        profileStep("Invisible Armor");
        shot("02_invisible_armor");
        add("check hidden state", 2, () -> checkState("invisible", true, false));

        profileStep("Helmetless");
        shot("03_helmetless");

        add("translucent + tinted", 20, () -> {
            Profile p = ProfileManager.effective();
            p.self.setAllVisible(true);
            for (ArmorSlot slot : ArmorSlot.all()) {
                PieceStyle s = p.self.piece(slot);
                s.opacity = 45;
                s.colorEnabled = true;
                s.color = 0xEC4899;
                s.colorStrength = 80;
            }
            ConfigManager.markDirty();
        });
        shot("04_translucent_tinted");

        add("tint opaque, no glint, no trims", 20, () -> {
            Profile p = ProfileManager.effective();
            for (ArmorSlot slot : ArmorSlot.all()) {
                PieceStyle s = p.self.piece(slot);
                s.opacity = 100;
                s.color = 0x22C55E;
                s.colorStrength = 70;
                s.brightness = 80;
                s.glint = Options.GlintMode.HIDE;
                s.showTrim = false;
            }
            ConfigManager.markDirty();
        });
        shot("05_tinted_no_glint_no_trim");

        add("others hidden, mine visible", 20, () -> {
            Profile p = ProfileManager.effective();
            for (ArmorSlot slot : ArmorSlot.all()) p.self.piece(slot).copyFrom(new PieceStyle());
            p.others.setAllVisible(false);
            ConfigManager.markDirty();
        });
        shot("06_others_hidden");
        add("check others state", 2, () -> checkState("others hidden", false, true));

        add("quick toggle self", 20, () -> {
            Profile p = ProfileManager.effective();
            p.others.setAllVisible(true);
            ConfigManager.get().quickHideSelf = true;
            ConfigManager.markDirty();
        });
        shot("07_quick_hide_self");
        add("check quick hide", 2, () -> {
            checkState("quick hide", true, false);
            ConfigManager.get().quickHideSelf = false;
            ConfigManager.markDirty();
        });

        add("equip elytra", 20, () -> onServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        }));
        add("camera back", 20, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        shot("08_elytra_visible");
        add("elytra translucent", 20, () -> {
            ProfileManager.effective().self.elytra.opacity = 40;
            ConfigManager.markDirty();
        });
        shot("09_elytra_translucent");
        add("elytra hidden", 20, () -> {
            ProfileManager.effective().self.elytra.visible = false;
            ConfigManager.markDirty();
        });
        shot("10_elytra_hidden");
        add("check elytra hidden", 2, () -> {
            AvatarRenderState state = extract();
            check("elytra removed from render state", state.chestEquipment.isEmpty());
            check("elytra still equipped", Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA));
        });
        add("restore chestplate", 20, () -> {
            Profile p = ProfileManager.effective();
            p.self.elytra.copyFrom(new PieceStyle());
            p.self.head.visible = false;
            ConfigManager.markDirty();
            onServer(server -> equipArmor(server, server.getPlayerList().getPlayers().getFirst(), true));
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });

        add("survival mode for the inventory", 20, () -> onServer(server -> server.getCommands()
            .performPrefixedCommand(server.createCommandSourceStack(), "gamemode survival @a")));
        add("inventory (same as world)", 30, () -> {
            Minecraft mc = Minecraft.getInstance();
            ClientCompat.setHudHidden(false);
            ClientCompat.clearToasts();
            ClientCompat.setScreen(new InventoryScreen(mc.player));
        });
        shot("11_inventory_same_as_world");
        add("inventory (real armor)", 20, () -> {
            ConfigManager.get().inventoryPreview = Options.InventoryPreview.SHOW_REAL;
            ConfigManager.markDirty();
        });
        shot("12_inventory_real_armor");
        add("hud in game", 30, () -> {
            ConfigManager.get().inventoryPreview = Options.InventoryPreview.SAME_AS_WORLD;
            ConfigManager.markDirty();
            ClientCompat.setScreen(null);
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        shot("13_first_person_hud");

        screenStep("visibility", StudioScreen.Page.VISIBILITY);
        screenStep("players", StudioScreen.Page.PLAYERS);
        screenStep("appearance", StudioScreen.Page.APPEARANCE);
        screenStep("equipment", StudioScreen.Page.EQUIPMENT);
        screenStep("hud", StudioScreen.Page.HUD);
        screenStep("profiles", StudioScreen.Page.PROFILES);
        screenStep("keybinds", StudioScreen.Page.KEYBINDS);
        screenStep("compat", StudioScreen.Page.COMPAT);
        screenStep("general", StudioScreen.Page.GENERAL);
        add("open hud editor", 30, () -> ClientCompat.setScreen(new HudEditorScreen(null)));
        shot("22_hud_editor");

        add("persistence round trip", 5, () -> {
            ClientCompat.setScreen(null);
            Profile created = ProfileManager.create("Persistence Test");
            created.self.chest.opacity = 37;
            created.self.legs.color = 0x123456;
            ConfigManager.get().hud.scale = 135;
            ConfigManager.markDirty();
            ConfigManager.flush();
            ConfigManager.reload();
            Profile reloaded = ConfigManager.get().find("Persistence Test");
            check("profile survives reload", reloaded != null);
            check("piece settings survive reload", reloaded != null && reloaded.self.chest.opacity == 37 && reloaded.self.legs.color == 0x123456);
            check("hud settings survive reload", ConfigManager.get().hud.scale == 135);
            if (reloaded != null) ProfileManager.delete(reloaded);
            ConfigManager.get().hud.scale = 100;
            ProfileManager.select(ConfigManager.get().find("Default"), false);
            ConfigManager.markDirty();
            ConfigManager.flush();
        });
        add("finish", 5, () -> {
            ArmorStudio.LOGGER.info("[SelfTest] finished with {} failure(s)", failures);
            Minecraft.getInstance().stop();
        });
    }

    private static void add(String name, int waitTicks, Runnable action) {
        STEPS.add(new Step(name, waitTicks, action));
    }

    private static void shot(String name) {
        add("screenshot " + name, 10, () -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, name + ".png", ClientCompat.mainRenderTarget(), 1,
                msg -> ArmorStudio.LOGGER.info("[SelfTest] screenshot {}", name));
        });
    }

    private static void profileStep(String name) {
        add("profile " + name, 25, () -> {
            Profile p = ConfigManager.get().find(name);
            check("preset exists: " + name, p != null);
            if (p != null) ProfileManager.select(p, true);
        });
    }

    private static void screenStep(String name, StudioScreen.Page page) {
        add("studio " + name, 25, () -> StudioScreen.openOn(page));
        shot("2" + page.ordinal() + "_studio_" + name);
    }

    // ------------------------------------------------------------------ checks

    private static void check(String what, boolean ok) {
        if (ok) {
            ArmorStudio.LOGGER.info("[SelfTest] PASS {}", what);
        } else {
            failures++;
            ArmorStudio.LOGGER.error("[SelfTest] FAIL {}", what);
        }
    }

    /** Extracts a fresh render state for the local player through the real renderer (mixins included). */
    private static AvatarRenderState extract() {
        Minecraft mc = Minecraft.getInstance();
        return (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, 1f);
    }

    private static AvatarRenderState extractMannequin() {
        Minecraft mc = Minecraft.getInstance();
        return (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mannequin).createRenderState(mannequin, 1f);
    }

    private static void checkState(String label, boolean selfHidden, boolean othersHidden) {
        Minecraft mc = Minecraft.getInstance();
        AvatarRenderState self = extract();
        int armor = mc.player.getArmorValue();
        check(label + ": real armor still equipped", !mc.player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
            && !mc.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && !mc.player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
            && !mc.player.getItemBySlot(EquipmentSlot.FEET).isEmpty());
        check(label + ": armor points unchanged (" + armor + ")", armor == 20);
        check(label + ": self render state " + (selfHidden ? "hidden" : "visible"),
            selfHidden == (self.headEquipment.isEmpty() && self.chestEquipment.isEmpty() && self.legsEquipment.isEmpty() && self.feetEquipment.isEmpty()));
        if (mannequin != null) {
            AvatarRenderState other = extractMannequin();
            check(label + ": mannequin render state " + (othersHidden ? "hidden" : "visible"),
                othersHidden == (other.headEquipment.isEmpty() && other.chestEquipment.isEmpty() && other.legsEquipment.isEmpty()
                    && other.feetEquipment.isEmpty()));
        }
    }

    // ------------------------------------------------------------------ server-side setup (integrated server, dev only)

    private static void onServer(java.util.function.Consumer<MinecraftServer> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) server.execute(() -> action.accept(server));
    }

    private static void equipArmor(MinecraftServer server, LivingEntity entity, boolean damagedLeggings) {
        var registries = server.registryAccess();
        Holder<net.minecraft.world.item.enchantment.Enchantment> protection =
            registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        ArmorTrim trim = new ArmorTrim(registries.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.GOLD),
            registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY));
        Item[] items = {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS};
        for (ArmorSlot slot : ArmorSlot.all()) {
            ItemStack stack = new ItemStack(items[slot.ordinal()]);
            stack.enchant(protection, 4);
            stack.set(DataComponents.TRIM, trim);
            if (damagedLeggings && slot == ArmorSlot.LEGS) stack.setDamageValue(stack.getMaxDamage() * 9 / 10);
            entity.setItemSlot(slot.equipmentSlot, stack);
        }
    }
}
