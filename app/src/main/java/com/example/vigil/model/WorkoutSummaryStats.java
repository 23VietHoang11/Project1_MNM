package com.example.vigil.model;

public class WorkoutSummaryStats {
    private final int totalWorkouts;
    private final int totalReps;
    private final int totalCalories;
    private final int totalXp;
    private final int totalGold;
    private final int itemsDroppedCount;

    public WorkoutSummaryStats(int totalWorkouts, int totalReps, int totalCalories,
                               int totalXp, int totalGold, int itemsDroppedCount) {
        this.totalWorkouts = totalWorkouts;
        this.totalReps = totalReps;
        this.totalCalories = totalCalories;
        this.totalXp = totalXp;
        this.totalGold = totalGold;
        this.itemsDroppedCount = itemsDroppedCount;
    }

    public WorkoutSummaryStats() {
        this(0, 0, 0, 0, 0, 0);
    }

    public int getTotalWorkouts() { return totalWorkouts; }
    public int getTotalReps() { return totalReps; }
    public int getTotalCalories() { return totalCalories; }
    public int getTotalXp() { return totalXp; }
    public int getTotalGold() { return totalGold; }
    public int getItemsDroppedCount() { return itemsDroppedCount; }
}
