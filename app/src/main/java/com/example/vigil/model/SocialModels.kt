package com.example.vigil.model

data class UserProfile(
    val id: Int,
    val username: String,
    val gold: Int = 0,
    val gems: Int = 0,
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

data class Guild(
    val id: Int,
    val name: String,
    val badge: String = "🛡️",
    val slogan: String = "",
    val leaderId: Int,
    val leaderName: String = "",
    val level: Int = 1,
    val memberCount: Int = 1,
    val totalReps: Int = 0,
    val isUserMember: Boolean = false,
    val isUserLeader: Boolean = false
)

data class GuildMember(
    val userId: Int,
    val username: String,
    val avatar: String,
    val level: Int,
    val role: String, // "LEADER" or "MEMBER"
    val totalReps: Int
)

data class DailyCheckInReward(
    val day: Int, // 1 -> 7
    val gold: Int,
    val gems: Int,
    val isClaimed: Boolean,
    val isAvailableToday: Boolean,
    val isUpcoming: Boolean
)

data class QuestItem(
    val id: String,
    val name: String,
    val quote: String,
    val current: Int,
    val goal: Int,
    val xp: Int,
    val gold: Int,
    val gems: Int = 0,
    val isClaimed: Boolean = false,
    val category: String // "DAILY", "WEEKLY", "MONTHLY"
)

