package com.example.vigil.model;

public class FriendRequestEntry {
    private final int id;
    private final int senderId;
    private final String senderUsername;
    private final int senderLevel;
    private final String senderAvatar;
    private final String senderTitle;
    private final int senderCombatPower;
    private final int senderTotalReps;
    private final String createdAt;

    public FriendRequestEntry(int id, int senderId, String senderUsername, int senderLevel,
                              String senderAvatar, String senderTitle, int senderCombatPower,
                              int senderTotalReps, String createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderLevel = senderLevel;
        this.senderAvatar = senderAvatar != null ? senderAvatar : "🧑‍🎤";
        this.senderTitle = senderTitle != null ? senderTitle : "Chiến Binh";
        this.senderCombatPower = senderCombatPower;
        this.senderTotalReps = senderTotalReps;
        this.createdAt = createdAt != null ? createdAt : "";
    }

    public int getId() { return id; }
    public int getSenderId() { return senderId; }
    public String getSenderUsername() { return senderUsername; }
    public int getSenderLevel() { return senderLevel; }
    public String getSenderAvatar() { return senderAvatar; }
    public String getSenderTitle() { return senderTitle; }
    public int getSenderCombatPower() { return senderCombatPower; }
    public int getSenderTotalReps() { return senderTotalReps; }
    public String getCreatedAt() { return createdAt; }
}
