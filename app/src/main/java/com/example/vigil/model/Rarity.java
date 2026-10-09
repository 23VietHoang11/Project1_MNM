package com.example.vigil.model;

public enum Rarity {
    COMMON("Thường", 0xFFB0B0B0L, 0xFF6E6E6EL),
    UNCOMMON("Tinh Xảo", 0xFF4CC38AL, 0xFF2E8B57L),
    RARE("Hiếm", 0xFF4DA8E8L, 0xFF2879B5L),
    EPIC("Sử Thi", 0xFFB486FFL, 0xFF7D4FE0L),
    LEGENDARY("Huyền Thoại", 0xFFFFC94DL, 0xFFFF9800L),
    MYTHIC("Thần Thoại", 0xFFFF3366L, 0xFFDC2626L),
    ANCIENT("Thượng Cổ", 0xFFFFD700L, 0xFFB8860BL),
    DIVINE("Chí Tôn", 0xFFE056FDL, 0xFFA855F7L);

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
