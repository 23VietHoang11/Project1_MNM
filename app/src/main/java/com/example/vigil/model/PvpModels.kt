package com.example.vigil.model

/**
 * Thực thể dữ liệu cho tính năng Đấu Trường Solo PvP (1 vs 1) tính điểm theo thời gian.
 */
enum class PvpBattleMode(val label: String, val desc: String) {
    LIVE_SOLO("⚡ Đua Điểm Trực Tiếp", "2 người chơi cùng đua điểm thời gian thực"),
    PASS_AND_PLAY("🤝 Đấu Luân Phiên (2 Người)", "Người 1 thi đấu trước, Người 2 thi đấu sau")
}

data class PvpOpponent(
    val id: Int,
    val username: String,
    val avatar: String,
    val level: Int,
    val attackDamage: Int,
    val isRealUser: Boolean = true
)

data class PvpMatchRecord(
    val id: Int,
    val player1Name: String,
    val player2Name: String,
    val exercise: String,
    val durationSec: Int,
    val p1Reps: Int,
    val p1Score: Int,
    val p2Reps: Int,
    val p2Score: Int,
    val winnerName: String,
    val goldReward: Int,
    val xpReward: Int,
    val createdAt: String
)
