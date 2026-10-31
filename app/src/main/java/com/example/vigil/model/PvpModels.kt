package com.example.vigil.model

/**
 * Thực thể dữ liệu cho tính năng Đấu Trường Solo PvP (1 vs 1) ghép trận người chơi thật & Video Call.
 */
enum class PvpBattleMode(val label: String, val desc: String) {
    LIVE_SOLO("📹 Video Call 1v1", "2 người chơi thật solo trực tiếp qua khung hình Camera"),
    PASS_AND_PLAY("🤝 Luân Phiên 2 Người", "Người 1 thi đấu trước, Người 2 thi đấu sau")
}

data class PvpOpponent(
    val id: Int,
    val username: String,
    val avatar: String,
    val level: Int,
    val attackDamage: Int,
    val isRealUser: Boolean = true
)

data class PvpRoomSyncState @JvmOverloads constructor(
    val matched: Boolean,
    val roomId: String = "",
    val opponentName: String = "",
    val opponentAvatar: String = "🧑‍🎤",
    val opponentLevel: Int = 1,
    val opponentDamage: Int = 50,
    val opponentReps: Int = 0,
    val opponentScore: Int = 0,
    val opponentAngle: Int = 165,
    val opponentHint: String = "Đang giữ nhịp tập",
    val opponentFrameBase64: String? = null,
    val roomExercise: String = "",
    val roomDurationSec: Int = 0,
    val serverOnline: Boolean = false,
    val forfeitedBy: String = "",
    val waitingHint: String = ""
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
