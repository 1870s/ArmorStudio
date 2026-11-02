package com.armorstudio.render;

import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.Options.InventoryPreview;
import com.armorstudio.config.Profile;
import com.armorstudio.config.StudioConfig;
import com.armorstudio.profile.ProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;

/** Decides which look applies to an entity. Results are cached per config revision. */
public final class LookResolver {
    private static int cachedRevision = Integer.MIN_VALUE;
    private static ResolvedLook self;
    private static ResolvedLook selfIgnoringQuickHide;
    private static ResolvedLook others;
    private static ResolvedLook hiddenEverything;

    private LookResolver() {}

    /** The look for a player-like entity being extracted for rendering, or null for vanilla rendering. */
    public static ResolvedLook forAvatar(Avatar entity) {
        StudioConfig cfg = ConfigManager.get();
        if (!cfg.enabled) return null;
        refresh(cfg);

        RenderContext.PreviewTarget preview = RenderContext.previewTarget();
        if (preview != null) return preview == RenderContext.PreviewTarget.SELF ? self : others;

        boolean isPlayer = entity instanceof Player;
        if (!isPlayer) return cfg.affectMannequins ? others : null;

        Minecraft mc = Minecraft.getInstance();
        boolean isSelf = mc.player != null && (entity == mc.player || entity.getUUID().equals(mc.player.getUUID()));
        if (!isSelf) return others;
        if (RenderContext.inInventoryPreview()) {
            return switch (cfg.inventoryPreview) {
                case SAME_AS_WORLD -> self;
                case SHOW_REAL -> null;
                case HIDE_ALL -> hiddenEverything;
            };
        }
        return self;
    }

    /** Your own look as configured in the profile, ignoring the quick-hide toggle (used by UI previews of edits). */
    public static ResolvedLook selfIgnoringQuickHide() {
        refresh(ConfigManager.get());
        return selfIgnoringQuickHide;
    }

    private static void refresh(StudioConfig cfg) {
        int revision = ConfigManager.revision();
        if (revision == cachedRevision) return;
        cachedRevision = revision;
        Profile profile = ProfileManager.effective();
        self = ResolvedLook.of(profile.self, cfg.translucency, cfg.quickHideSelf);
        selfIgnoringQuickHide = ResolvedLook.of(profile.self, cfg.translucency, false);
        others = ResolvedLook.of(profile.others, cfg.translucency, false);
        hiddenEverything = ResolvedLook.allHidden();
    }

    /** Inventory preview mode used for documentation in the UI. */
    public static InventoryPreview inventoryMode() {
        return ConfigManager.get().inventoryPreview;
    }
}
