package com.example.vigil.model;

public class WorkoutHistoryEntry {
    private final int id;
    private final String exercise;
    private final int reps;
    private final int holdSeconds;
    private final int score;
    private final int xpEarned;
    private final int goldEarned;
    private final Item droppedItem;
    private final String createdAt;
    private final int caloriesBurned;

    public WorkoutHistoryEntry(int id, String exercise, int reps, int holdSeconds,
                               int score, int xpEarned, int goldEarned,
                               Item droppedItem, String createdAt, int caloriesBurned) {
        this.id = id;
        this.exercise = exercise;
        this.reps = reps;
        this.holdSeconds = holdSeconds;
        this.score = score;
        this.xpEarned = xpEarned;
        this.goldEarned = goldEarned;
        this.droppedItem = droppedItem;
        this.createdAt = createdAt != null ? createdAt : "";
        this.caloriesBurned = caloriesBurned;
    }

    public WorkoutHistoryEntry(int id, String exercise, int reps, int holdSeconds,
                               int score, int xpEarned, int goldEarned,
                               Item droppedItem, String createdAt) {
        this(id, exercise, reps, holdSeconds, score, xpEarned, goldEarned, droppedItem, createdAt, 0);
    }

    public int getId() { return id; }
    public String getExercise() { return exercise; }
    public int getReps() { return reps; }
    public int getHoldSeconds() { return holdSeconds; }
    public int getScore() { return score; }
    public int getXpEarned() { return xpEarned; }
    public int getGoldEarned() { return goldEarned; }
    public Item getDroppedItem() { return droppedItem; }
    public String getCreatedAt() { return createdAt; }
    public int getCaloriesBurned() { return caloriesBurned; }
}
