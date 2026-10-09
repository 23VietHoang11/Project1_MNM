package com.example.vigil.model;

public class OutgoingGuildInvitation {
    private final int id;
    private final int guildId;
    private final int inviteeId;
    private final String inviteeName;
    private final String inviteeAvatar;
    private final int inviteeLevel;
    private final String createdAt;

    public OutgoingGuildInvitation(int id, int guildId, int inviteeId,
                                   String inviteeName, String inviteeAvatar,
                                   int inviteeLevel, String createdAt) {
        this.id = id;
        this.guildId = guildId;
        this.inviteeId = inviteeId;
        this.inviteeName = inviteeName;
        this.inviteeAvatar = inviteeAvatar != null ? inviteeAvatar : "🧑‍🎤";
        this.inviteeLevel = inviteeLevel;
        this.createdAt = createdAt != null ? createdAt : "";
    }

    public int getId() { return id; }
    public int getGuildId() { return guildId; }
    public int getInviteeId() { return inviteeId; }
    public String getInviteeName() { return inviteeName; }
    public String getInviteeAvatar() { return inviteeAvatar; }
    public int getInviteeLevel() { return inviteeLevel; }
    public String getCreatedAt() { return createdAt; }
}
