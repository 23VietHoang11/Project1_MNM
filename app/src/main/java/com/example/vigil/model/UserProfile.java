package com.example.vigil.model;

public class UserProfile {
    private final int id;
    private final String username;
    private final int gold;
    private final int gems;
    private final int xp;
    private final int level;
    private final int streak;
    private final int totalReps;
    private final int stage;
    private final String avatar;
    private final String title;

    public UserProfile(int id, String username, int gold, int gems, int xp,
                       int level, int streak, int totalReps, int stage,
                       String avatar, String title) {
        this.id = id;
        this.username = username;
        this.gold = gold;
        this.gems = gems;
        this.xp = xp;
        this.level = level;
        this.streak = streak;
        this.totalReps = totalReps;
        this.stage = stage;
        this.avatar = avatar != null ? avatar : "🧑‍🎤";
        this.title = title != null ? title : "Tân Binh";
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public int getGold() { return gold; }
    public int getGems() { return gems; }
    public int getXp() { return xp; }
    public int getLevel() { return level; }
    public int getStreak() { return streak; }
    public int getTotalReps() { return totalReps; }
    public int getStage() { return stage; }
    public String getAvatar() { return avatar; }
    public String getTitle() { return title; }
}
