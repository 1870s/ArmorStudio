package com.armorstudio.render;

import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.Options.Translucency;
import com.armorstudio.config.PlayerLook;
import net.minecraft.world.entity.EquipmentSlot;

/** Immutable, render-ready look of one player. A null piece means "render like vanilla". */
public final class ResolvedLook {
    private final ResolvedPiece[] pieces = new ResolvedPiece[4];
    public final ResolvedPiece elytra;
    public final boolean hideHeadItems;

    private ResolvedLook(PlayerLook look, Translucency mode, boolean hideArmor) {
        for (ArmorSlot slot : ArmorSlot.all()) pieces[slot.ordinal()] = ResolvedPiece.of(look.piece(slot), mode, hideArmor);
        elytra = ResolvedPiece.of(look.elytra, mode, false);
        hideHeadItems = !look.showHeadItems;
    }

    private ResolvedLook(boolean hideEverything) {
        for (int i = 0; i < 4; i++) pieces[i] = hideEverything ? ResolvedPiece.HIDDEN : null;
        elytra = hideEverything ? ResolvedPiece.HIDDEN : null;
        hideHeadItems = hideEverything;
    }

    /** Returns null if the look does not change anything, so vanilla rendering is left completely untouched. */
    public static ResolvedLook of(PlayerLook look, Translucency mode, boolean hideArmor) {
        ResolvedLook resolved = new ResolvedLook(look, mode, hideArmor);
        return resolved.isVanilla() ? null : resolved;
    }

    /** Everything hidden: armor, elytra and head items. */
    public static ResolvedLook allHidden() {
        return new ResolvedLook(true);
    }

    public ResolvedPiece piece(ArmorSlot slot) {
        return pieces[slot.ordinal()];
    }

    public ResolvedPiece piece(EquipmentSlot slot) {
        ArmorSlot armorSlot = ArmorSlot.fromEquipmentSlot(slot);
        return armorSlot == null ? null : pieces[armorSlot.ordinal()];
    }

    public boolean isHidden(ArmorSlot slot) {
        ResolvedPiece p = pieces[slot.ordinal()];
        return p != null && p.hidden;
    }

    public boolean isElytraHidden() {
        return elytra != null && elytra.hidden;
    }

    private boolean isVanilla() {
        for (ResolvedPiece p : pieces) {
            if (p != null) return false;
        }
        return elytra == null && !hideHeadItems;
    }
}
