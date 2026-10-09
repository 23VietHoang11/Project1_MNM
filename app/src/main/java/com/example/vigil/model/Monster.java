package com.example.vigil.model;

import com.example.vigil.pose.Exercise;

public class Monster {
    public final int stageId;
    public final String name;
    public final String title;
    public final String avatar;
    public final int maxHp;
    public final Exercise exercise;
    public final boolean isBoss;
    public final int expReward;
    public final int goldReward;
    public final String lore;
    public final Rarity dropRarity;
    public final long colorHex;

    public Monster(int stageId, String name, String title, String avatar,
                   int maxHp, Exercise exercise, boolean isBoss,
                   int expReward, int goldReward, String lore,
                   Rarity dropRarity, long colorHex) {
        this.stageId = stageId;
        this.name = name;
        this.title = title;
        this.avatar = avatar;
        this.maxHp = maxHp;
        this.exercise = exercise;
        this.isBoss = isBoss;
        this.expReward = expReward;
        this.goldReward = goldReward;
        this.lore = lore;
        this.dropRarity = dropRarity != null ? dropRarity : Rarity.RARE;
        this.colorHex = colorHex;
    }

    public Monster(int stageId, String name, String title, String avatar,
                   int maxHp, Exercise exercise, boolean isBoss,
                   int expReward, int goldReward, String lore) {
        this(stageId, name, title, avatar, maxHp, exercise, isBoss, expReward, goldReward, lore, Rarity.RARE, 0xFFFF5A7AL);
    }

    public int getStageId() { return stageId; }
    public String getName() { return name; }
    public String getTitle() { return title; }
    public String getAvatar() { return avatar; }
    public int getMaxHp() { return maxHp; }
    public Exercise getExercise() { return exercise; }
    public boolean isBoss() { return isBoss; }
    public int getExpReward() { return expReward; }
    public int getGoldReward() { return goldReward; }
    public String getLore() { return lore; }
    public Rarity getDropRarity() { return dropRarity; }
    public long getColorHex() { return colorHex; }
}
