package com.example.vigil.model;

public class LeaderboardEntry {
    public final int rank;
    public final int id;
    public final String username;
    public final int level;
    public final int combatPower;
    public final int totalReps;
    public final int streak;
    public final String avatar;
    public final String title;
    public final boolean isFriend;
    public final boolean isCurrentUser;
    public final boolean hasPendingFriendRequest;

    public LeaderboardEntry(int rank, int id, String username, int level,
                            int combatPower, int totalReps, int streak,
                            String avatar, String title, boolean isFriend,
                            boolean isCurrentUser, boolean hasPendingFriendRequest) {
        this.rank = rank;
        this.id = id;
        this.username = username;
        this.level = level;
        this.combatPower = combatPower;
        this.totalReps = totalReps;
        this.streak = streak;
        this.avatar = avatar != null ? avatar : "🧑‍🎤";
        this.title = title != null ? title : "Chiến Binh";
        this.isFriend = isFriend;
        this.isCurrentUser = isCurrentUser;
        this.hasPendingFriendRequest = hasPendingFriendRequest;
    }

    public LeaderboardEntry copy(int rank) {
        return new LeaderboardEntry(rank, id, username, level, combatPower, totalReps, streak, avatar, title, isFriend, isCurrentUser, hasPendingFriendRequest);
    }

    public int getRank() { return rank; }
    public int getId() { return id; }
    public String getUsername() { return username; }
    public int getLevel() { return level; }
    public int getCombatPower() { return combatPower; }
    public int getTotalReps() { return totalReps; }
    public int getStreak() { return streak; }
    public String getAvatar() { return avatar; }
    public String getTitle() { return title; }
    public boolean isFriend() { return isFriend; }
    public boolean isCurrentUser() { return isCurrentUser; }
    public boolean hasPendingFriendRequest() { return hasPendingFriendRequest; }
    public boolean getHasPendingFriendRequest() { return hasPendingFriendRequest; }
}
