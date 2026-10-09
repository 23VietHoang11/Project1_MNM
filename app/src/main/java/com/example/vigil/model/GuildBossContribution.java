package com.example.vigil.model;

/**
 * Model đại diện cho một thành viên đã đóng góp sát thương đánh Boss Thế Giới Bang Hội.
 */
public class GuildBossContribution {
    private final int userId;
    private final String username;
    private final String avatar;
    private final int level;
    private final int damage;
    private final int reps;
    private final double percentage;
    private final int rank;
    private final boolean hasClaimed;

    public GuildBossContribution(int userId, String username, String avatar, int level,
                                 int damage, int reps, double percentage, int rank, boolean hasClaimed) {
        this.userId = userId;
        this.username = username != null ? username : "Chiến Binh";
        this.avatar = avatar != null ? avatar : "🧑‍🎤";
        this.level = level;
        this.damage = damage;
        this.reps = reps;
        this.percentage = percentage;
        this.rank = rank;
        this.hasClaimed = hasClaimed;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public int getLevel() { return level; }
    public int getDamage() { return damage; }
    public int getReps() { return reps; }
    public double getPercentage() { return percentage; }
    public int getRank() { return rank; }
    public boolean isHasClaimed() { return hasClaimed; }
}
