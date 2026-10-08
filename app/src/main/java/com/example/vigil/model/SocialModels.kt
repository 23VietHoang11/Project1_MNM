package com.example.vigil.model

data class UserProfile(
    val id: Int,
    val username: String,
    val gold: Int = 200,
    val gems: Int = 10,
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val totalReps: Int = 0,
    val stage: Int = 1,
    val avatar: String = "🧑‍🎤",
    val title: String = "Tân Binh"
)

data class FriendProfile(
    val id: Int,
    val username: String,
    val level: Int,
    val combatPower: Int,
    val totalReps: Int,
    val streak: Int,
    val avatar: String = "🧑‍🎤",
    val title: String = "Chiến Binh",
    val isOnline: Boolean = true
)

data class LeaderboardEntry(
    val rank: Int,
    val id: Int,
    val username: String,
    val level: Int,
    val combatPower: Int,
    val totalReps: Int,
    val streak: Int,
    val avatar: String = "🧑‍🎤",
    val title: String = "Chiến Binh",
    val isFriend: Boolean = false,
    val isCurrentUser: Boolean = false
)
