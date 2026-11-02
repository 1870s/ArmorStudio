package com.armorstudio.render;

import net.minecraft.world.item.ItemStack;

/**
 * Small amount of state passed between mixins. Render state extraction and layer submission both run on the render
 * thread, which is the only thread that touches these fields.
 */
public final class RenderContext {
    /** Who the settings-screen preview pretends to be. */
    public enum PreviewTarget { SELF, OTHERS }

    private static int inventoryDepth;
    private static PreviewTarget previewTarget;

    // Piece currently submitted by the armor or wings layer, bound to the exact stack instance being rendered so a
    // stale value can never leak into other equipment renderers (horse armor, wolf armor, ...).
    private static ResolvedPiece piece;
    private static ItemStack pieceStack;
    private static ResolvedPiece active;

    private RenderContext() {}

    public static void enterInventoryPreview() {
        inventoryDepth++;
    }

    public static void exitInventoryPreview() {
        if (inventoryDepth > 0) inventoryDepth--;
    }

    public static boolean inInventoryPreview() {
        return inventoryDepth > 0;
    }

    public static PreviewTarget previewTarget() {
        return previewTarget;
    }

    /** Set by the settings screen around its own render state extraction; pass null to clear. */
    public static void setPreviewTarget(PreviewTarget target) {
        previewTarget = target;
    }

    /** Called by the armor/wings layer before it submits a piece. */
    public static void beginPiece(ResolvedPiece resolved, ItemStack stack) {
        piece = resolved;
        pieceStack = resolved == null ? null : stack;
    }

    public static void endPiece() {
        piece = null;
        pieceStack = null;
    }

    /** Called at the start of EquipmentLayerRenderer.renderLayers; activates the piece only for the matching stack. */
    public static void enterEquipment(ItemStack stack) {
        active = piece != null && stack == pieceStack ? piece : null;
    }

    public static void exitEquipment() {
        active = null;
    }

    /** The style for the equipment currently being submitted, or null for vanilla rendering. */
    public static ResolvedPiece active() {
        return active;
    }
}
