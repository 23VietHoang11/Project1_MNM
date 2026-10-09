package com.example.vigil.model;

public class FriendProfile {
    private final int id;
    private final String username;
    private final int level;
    private final int combatPower;
    private final int totalReps;
    private final int streak;
    private final String avatar;
    private final String title;
    private final boolean isOnline;

    public FriendProfile(int id, String username, int level, int combatPower,
                         int totalReps, int streak, String avatar, String title, boolean isOnline) {
        this.id = id;
        this.username = username;
        this.level = level;
        this.combatPower = combatPower;
        this.totalReps = totalReps;
        this.streak = streak;
        this.avatar = avatar != null ? avatar : "🧑‍🎤";
        this.title = title != null ? title : "Chiến Binh";
        this.isOnline = isOnline;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public int getLevel() { return level; }
    public int getCombatPower() { return combatPower; }
    public int getTotalReps() { return totalReps; }
    public int getStreak() { return streak; }
    public String getAvatar() { return avatar; }
    public String getTitle() { return title; }
    public boolean isOnline() { return isOnline; }
}
