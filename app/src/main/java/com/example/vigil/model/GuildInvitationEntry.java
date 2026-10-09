package com.example.vigil.model;

public class GuildInvitationEntry {
    private final int id;
    private final int guildId;
    private final String guildName;
    private final String guildBadge;
    private final int guildLevel;
    private final int guildMemberCount;
    private final String guildSlogan;
    private final int inviterId;
    private final String inviterName;
    private final String inviterAvatar;
    private final String createdAt;

    public GuildInvitationEntry(int id, int guildId, String guildName, String guildBadge,
                                int guildLevel, int guildMemberCount, String guildSlogan,
                                int inviterId, String inviterName, String inviterAvatar,
                                String createdAt) {
        this.id = id;
        this.guildId = guildId;
        this.guildName = guildName;
        this.guildBadge = guildBadge != null ? guildBadge : "🛡️";
        this.guildLevel = guildLevel;
        this.guildMemberCount = guildMemberCount;
        this.guildSlogan = guildSlogan != null ? guildSlogan : "";
        this.inviterId = inviterId;
        this.inviterName = inviterName != null ? inviterName : "";
        this.inviterAvatar = inviterAvatar != null ? inviterAvatar : "🧑‍🎤";
        this.createdAt = createdAt != null ? createdAt : "";
    }

    public int getId() { return id; }
    public int getGuildId() { return guildId; }
    public String getGuildName() { return guildName; }
    public String getGuildBadge() { return guildBadge; }
    public int getGuildLevel() { return guildLevel; }
    public int getGuildMemberCount() { return guildMemberCount; }
    public String getGuildSlogan() { return guildSlogan; }
    public int getInviterId() { return inviterId; }
    public String getInviterName() { return inviterName; }
    public String getInviterAvatar() { return inviterAvatar; }
    public String getCreatedAt() { return createdAt; }
}
