package com.example.vigil.model;

public enum Rarity {
    COMMON("Thường", 0xFFB0B0B0L, 0xFF6E6E6EL),
    RARE("Hiếm", 0xFF4DA8E8L, 0xFF2879B5L),
    EPIC("Sử Thi", 0xFFB486FFL, 0xFF7D4FE0L),
    LEGENDARY("Huyền Thoại", 0xFFFFC94DL, 0xFFFF9800L);

    public final String label;
    public final long colorHex;
    public final long borderHex;

    Rarity(String label, long colorHex, long borderHex) {
        this.label = label;
        this.colorHex = colorHex;
        this.borderHex = borderHex;
    }

    public String getLabel() {
        return label;
    }

    public long getColorHex() {
        return colorHex;
    }

    public long getBorderHex() {
        return borderHex;
    }
}
