package com.armorstudio.config;

/** Everything that defines how one group of players (you, or everybody else) looks. */
public final class PlayerLook {
    public PieceStyle head = new PieceStyle();
    public PieceStyle chest = new PieceStyle();
    public PieceStyle legs = new PieceStyle();
    public PieceStyle feet = new PieceStyle();
    public PieceStyle elytra = new PieceStyle();
    /** Carved pumpkins, mob heads, player heads, banners and other non-armor items worn on the head. */
    public boolean showHeadItems = true;

    public PieceStyle piece(ArmorSlot slot) {
        return switch (slot) {
            case HEAD -> head;
            case CHEST -> chest;
            case LEGS -> legs;
            case FEET -> feet;
        };
    }

    public void setAllVisible(boolean visible) {
        for (ArmorSlot slot : ArmorSlot.all()) piece(slot).visible = visible;
    }

    public PlayerLook copy() {
        PlayerLook c = new PlayerLook();
        c.head = head.copy();
        c.chest = chest.copy();
        c.legs = legs.copy();
        c.feet = feet.copy();
        c.elytra = elytra.copy();
        c.showHeadItems = showHeadItems;
        return c;
    }

    public PlayerLook validate() {
        head = head == null ? new PieceStyle() : head.validate();
        chest = chest == null ? new PieceStyle() : chest.validate();
        legs = legs == null ? new PieceStyle() : legs.validate();
        feet = feet == null ? new PieceStyle() : feet.validate();
        elytra = elytra == null ? new PieceStyle() : elytra.validate();
        return this;
    }
}
