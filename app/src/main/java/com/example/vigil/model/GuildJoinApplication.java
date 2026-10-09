package com.example.vigil.model;

/**
 * Model đại diện cho đơn xin gia nhập bang hội từ người chơi gửi tới Chủ bang.
 */
public class GuildJoinApplication {
    private final int id;
    private final int guildId;
    private final String guildName;
    private final int userId;
    private final String username;
    private final String avatar;
    private final int level;
    private final int totalReps;
    private final String status; // "PENDING", "ACCEPTED", "REJECTED"
    private final String createdAt;

    public GuildJoinApplication(int id, int guildId, String guildName, int userId,
                                String username, String avatar, int level, int totalReps,
                                String status, String createdAt) {
        this.id = id;
        this.guildId = guildId;
        this.guildName = guildName != null ? guildName : "";
        this.userId = userId;
        this.username = username != null ? username : "Chiến Binh";
        this.avatar = avatar != null ? avatar : "🧑‍🎤";
        this.level = level;
        this.totalReps = totalReps;
        this.status = status != null ? status : "PENDING";
        this.createdAt = createdAt != null ? createdAt : "";
    }

    public int getId() { return id; }
    public int getGuildId() { return guildId; }
    public String getGuildName() { return guildName; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public int getLevel() { return level; }
    public int getTotalReps() { return totalReps; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
