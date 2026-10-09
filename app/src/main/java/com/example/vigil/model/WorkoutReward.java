package com.example.vigil.model;

public class WorkoutReward {
    private final int score;
    private final int xpEarned;
    private final int goldEarned;
    private final int newLevel;
    private final int newGold;
    private final int newXp;
    private final int newStreak;
    private final Item droppedItem;
    private final int gemsEarned;
    private final int newGems;
    private final boolean isBossFirstClear;

    public WorkoutReward(int score, int xpEarned, int goldEarned, int newLevel,
                         int newGold, int newXp, int newStreak, Item droppedItem,
                         int gemsEarned, int newGems, boolean isBossFirstClear) {
        this.score = score;
        this.xpEarned = xpEarned;
        this.goldEarned = goldEarned;
        this.newLevel = newLevel;
        this.newGold = newGold;
        this.newXp = newXp;
        this.newStreak = newStreak;
        this.droppedItem = droppedItem;
        this.gemsEarned = gemsEarned;
        this.newGems = newGems;
        this.isBossFirstClear = isBossFirstClear;
    }

    public WorkoutReward(int score, int xpEarned, int goldEarned, int newLevel,
                         int newGold, int newXp, int newStreak, Item droppedItem) {
        this(score, xpEarned, goldEarned, newLevel, newGold, newXp, newStreak, droppedItem, 0, 0, false);
    }

    public int getScore() {
        return score;
    }

    public int getXpEarned() {
        return xpEarned;
    }

    public int getGoldEarned() {
        return goldEarned;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public int getNewGold() {
        return newGold;
    }

    public int getNewXp() {
        return newXp;
    }

    public int getNewStreak() {
        return newStreak;
    }

    public Item getDroppedItem() {
        return droppedItem;
    }

    public int getGemsEarned() {
        return gemsEarned;
    }

    public int getNewGems() {
        return newGems;
    }

    public boolean isBossFirstClear() {
        return isBossFirstClear;
    }
}
