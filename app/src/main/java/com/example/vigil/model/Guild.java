package com.example.vigil.model;

public class Guild {
    private final int id;
    private final String name;
    private final String badge;
    private final String slogan;
    private final int leaderId;
    private final String leaderName;
    private final int level;
    private final int memberCount;
    private final int totalReps;
    private final boolean isUserMember;
    private final boolean isUserLeader;

    public Guild(int id, String name, String badge, String slogan,
                 int leaderId, String leaderName, int level, int memberCount,
                 int totalReps, boolean isUserMember, boolean isUserLeader) {
        this.id = id;
        this.name = name;
        this.badge = badge != null ? badge : "🛡️";
        this.slogan = slogan != null ? slogan : "";
        this.leaderId = leaderId;
        this.leaderName = leaderName != null ? leaderName : "";
        this.level = level;
        this.memberCount = memberCount;
        this.totalReps = totalReps;
        this.isUserMember = isUserMember;
        this.isUserLeader = isUserLeader;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getBadge() { return badge; }
    public String getSlogan() { return slogan; }
    public int getLeaderId() { return leaderId; }
    public String getLeaderName() { return leaderName; }
    public int getLevel() { return level; }
    public int getMemberCount() { return memberCount; }
    public int getTotalReps() { return totalReps; }
    public boolean isUserMember() { return isUserMember; }
    public boolean isUserLeader() { return isUserLeader; }
}
