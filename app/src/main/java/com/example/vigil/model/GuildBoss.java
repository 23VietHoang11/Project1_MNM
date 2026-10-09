package com.example.vigil.model;

/**
 * Model biểu diễn Siêu Trùm Thế Giới của Bang Hội (Guild World Boss).
 */
public class GuildBoss {
    private final int id;
    private final int guildId;
    private final String bossId;
    private final String name;
    private final String title;
    private final String avatar;
    private final int maxHp;
    private final int currentHp;
    private final String status;
    private final int rewardGold;
    private final int rewardGems;
    private final String rewardItemId;
    private final String rewardItemName;

    public GuildBoss(int id, int guildId, String bossId, String name, String title,
                     String avatar, int maxHp, int currentHp, String status,
                     int rewardGold, int rewardGems, String rewardItemId, String rewardItemName) {
        this.id = id;
        this.guildId = guildId;
        this.bossId = bossId != null ? bossId : "boss_nether_dragon";
        this.name = name != null ? name : "Hắc Long Viễn Cổ - Nidhogg";
        this.title = title != null ? title : "SIÊU TRÙM THẾ GIỚI BANG HỘI";
        this.avatar = avatar != null ? avatar : "🐉";
        this.maxHp = maxHp > 0 ? maxHp : 500000;
        this.currentHp = Math.max(0, currentHp);
        this.status = status != null ? status : "ACTIVE";
        this.rewardGold = rewardGold;
        this.rewardGems = rewardGems;
        this.rewardItemId = rewardItemId != null ? rewardItemId : "weapon_dragon_slayer";
        this.rewardItemName = rewardItemName != null ? rewardItemName : "Đại Đao Trảm Long";
    }

    public int getId() { return id; }
    public int getGuildId() { return guildId; }
    public String getBossId() { return bossId; }
    public String getName() { return name; }
    public String getTitle() { return title; }
    public String getAvatar() { return avatar; }
    public int getMaxHp() { return maxHp; }
    public int getCurrentHp() { return currentHp; }
    public String getStatus() { return status; }
    public int getRewardGold() { return rewardGold; }
    public int getRewardGems() { return rewardGems; }
    public String getRewardItemId() { return rewardItemId; }
    public String getRewardItemName() { return rewardItemName; }

    public boolean isDefeated() {
        return "DEFEATED".equalsIgnoreCase(status) || currentHp <= 0;
    }

    public float getHpRatio() {
        if (maxHp <= 0) return 0f;
        return Math.max(0f, Math.min(1f, (float) currentHp / (float) maxHp));
    }
}
