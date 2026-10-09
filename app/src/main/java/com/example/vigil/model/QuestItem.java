package com.example.vigil.model;

public class QuestItem {
    private final String id;
    private final String name;
    private final String quote;
    private final int current;
    private final int goal;
    private final int xp;
    private final int gold;
    private final int gems;
    private final boolean isClaimed;
    private final String category; // "DAILY", "WEEKLY", "MONTHLY"

    public QuestItem(String id, String name, String quote, int current, int goal,
                     int xp, int gold, int gems, boolean isClaimed, String category) {
        this.id = id;
        this.name = name;
        this.quote = quote;
        this.current = current;
        this.goal = goal;
        this.xp = xp;
        this.gold = gold;
        this.gems = gems;
        this.isClaimed = isClaimed;
        this.category = category != null ? category : "DAILY";
    }

    public QuestItem(String id, String name, String quote, int current, int goal,
                     int xp, int gold, String category) {
        this(id, name, quote, current, goal, xp, gold, 0, false, category);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getQuote() { return quote; }
    public int getCurrent() { return current; }
    public int getGoal() { return goal; }
    public int getXp() { return xp; }
    public int getGold() { return gold; }
    public int getGems() { return gems; }
    public boolean isClaimed() { return isClaimed; }
    public String getCategory() { return category; }
}
