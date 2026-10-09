package com.example.vigil.model;

public enum ItemSlot {
    HELMET("Mũ giáp", "🪖"),
    ARMOR("Giáp ngực", "🥋"),
    GLOVES("Găng tay", "🥊"),
    BOOTS("Giày", "👟"),
    AMULET("Bùa chú", "🪬"),
    WEAPON("Vũ khí", "⚔️");

    private final String label;
    private final String icon;

    ItemSlot(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }
}
