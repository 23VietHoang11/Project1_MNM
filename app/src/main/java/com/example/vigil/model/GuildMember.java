package com.example.vigil.model;

public class GuildMember {
    private final int userId;
    private final String username;
    private final String avatar;
    private final int level;
    private final String role;
    private final int totalReps;

    public GuildMember(int userId, String username, String avatar, int level, String role, int totalReps) {
        this.userId = userId;
        this.username = username;
        this.avatar = avatar != null ? avatar : "🧑‍🎤";
        this.level = level;
        this.role = role != null ? role : "MEMBER";
        this.totalReps = totalReps;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public int getLevel() { return level; }
    public String getRole() { return role; }
    public int getTotalReps() { return totalReps; }
}
