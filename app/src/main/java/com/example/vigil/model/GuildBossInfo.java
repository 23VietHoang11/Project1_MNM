package com.example.vigil.model;

import java.util.Collections;
import java.util.List;

/**
 * Model chứa toàn bộ thông tin chiến dịch Boss Thế Giới của Bang Hội.
 */
public class GuildBossInfo {
    private final GuildBoss boss;
    private final List<GuildBossContribution> contributors;
    private final int myDamage;
    private final int myReps;
    private final double myPercentage;
    private final int myRank;
    private final boolean hasClaimedDefeatReward;

    public GuildBossInfo(GuildBoss boss, List<GuildBossContribution> contributors,
                         int myDamage, int myReps, double myPercentage, int myRank,
                         boolean hasClaimedDefeatReward) {
        this.boss = boss;
        this.contributors = contributors != null ? contributors : Collections.emptyList();
        this.myDamage = myDamage;
        this.myReps = myReps;
        this.myPercentage = myPercentage;
        this.myRank = myRank;
        this.hasClaimedDefeatReward = hasClaimedDefeatReward;
    }

    public GuildBoss getBoss() { return boss; }
    public List<GuildBossContribution> getContributors() { return contributors; }
    public int getMyDamage() { return myDamage; }
    public int getMyReps() { return myReps; }
    public double getMyPercentage() { return myPercentage; }
    public int getMyRank() { return myRank; }
    public boolean isHasClaimedDefeatReward() { return hasClaimedDefeatReward; }
}
