package com.example.vigil.model

/**
 * Data model biểu diễn thông tin người chơi trong Bảng Quản Trị (Admin Panel).
 */
data class AdminUserEntry(
    val id: Int,
    val username: String,
    val avatar: String,
    val level: Int,
    val gold: Int,
    val streak: Int,
    val totalReps: Int,
    val role: String,
    var isLocked: Boolean,
    var lockReason: String? = null
)

/**
 * Data model biểu diễn cảnh báo gian lận / bất thường từ hệ thống Anti-cheat.
 */
data class CheatAlertItem(
    val id: Int,
    val username: String,
    val exercise: String,
    val reps: Int,
    val durationSeconds: Int,
    val reason: String,
    var isResolved: Boolean = false
)

/**
 * Data model cấu hình ngưỡng góc khớp bài tập AI MediaPipe và Calo tiêu hao.
 */
data class ExerciseConfigItem(
    val code: String,
    val name: String,
    var extendAngle: Float,
    var contractAngle: Float,
    var damageFactor: Float,
    var caloPerRep: Float
)
