package com.example.vigil.model

import androidx.compose.ui.graphics.Color

enum class ItemSlot(val label: String, val icon: String) {
    HELMET("Mũ giáp", "🪖"),
    ARMOR("Giáp ngực", "🥋"),
    GLOVES("Găng tay", "🥊"),
    BOOTS("Giày", "👟"),
    AMULET("Bùa chú", "🪬"),
    WEAPON("Vũ khí", "⚔️")
}

enum class Rarity(val label: String, val color: Color, val borderHex: Long) {
    COMMON("Thường", Color(0xFFB0B0B0), 0xFF6E6E6E),
    RARE("Hiếm", Color(0xFF4DA8E8), 0xFF2879B5),
    EPIC("Sử Thi", Color(0xFFB486FF), 0xFF7D4FE0),
    LEGENDARY("Huyền Thoại", Color(0xFFFFC94D), 0xFFFF9800)
}

data class Item(
    val id: String,
    val name: String,
    val slot: ItemSlot,
    val rarity: Rarity,
    val priceGold: Int = 0,
    val priceGems: Int = 0,
    val bonusStr: Int = 0,
    val bonusEnd: Int = 0,
    val bonusPre: Int = 0,
    val bonusLuck: Int = 0,
    val description: String = "",
    val icon: String = "⚔️"
) {
    val totalStats: Int get() = bonusStr + bonusEnd + bonusPre + bonusLuck
    val combatPower: Int get() = (bonusStr * 10) + (bonusEnd * 8) + (bonusPre * 9) + (bonusLuck * 6)
    val sellPriceGold: Int get() = if (priceGold > 0) (priceGold * 0.5).toInt().coerceAtLeast(10) else (priceGems * 50).coerceAtLeast(20)
}

data class InventoryItem(
    val invId: Int,
    val item: Item,
    val isEquipped: Boolean = false,
    val acquiredAt: String = ""
)

data class WorkoutReward(
    val score: Int,
    val xpEarned: Int,
    val goldEarned: Int,
    val newLevel: Int,
    val newGold: Int,
    val newXp: Int,
    val newStreak: Int,
    val droppedItem: Item? = null
)
