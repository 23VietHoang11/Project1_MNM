package com.example.vigil.model;

public class DailyCheckInReward {
    private final int day;
    private final int gold;
    private final int gems;
    private final boolean isClaimed;
    private final boolean isAvailableToday;
    private final boolean isUpcoming;

    public DailyCheckInReward(int day, int gold, int gems,
                              boolean isClaimed, boolean isAvailableToday, boolean isUpcoming) {
        this.day = day;
        this.gold = gold;
        this.gems = gems;
        this.isClaimed = isClaimed;
        this.isAvailableToday = isAvailableToday;
        this.isUpcoming = isUpcoming;
    }

    public int getDay() { return day; }
    public int getGold() { return gold; }
    public int getGems() { return gems; }
    public boolean isClaimed() { return isClaimed; }
    public boolean isAvailableToday() { return isAvailableToday; }
    public boolean isUpcoming() { return isUpcoming; }
}
