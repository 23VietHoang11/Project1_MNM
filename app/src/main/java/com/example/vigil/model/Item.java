package com.example.vigil.model;

import java.util.Objects;

public class Item {
    public final String id;
    public final String name;
    public final ItemSlot slot;
    public final Rarity rarity;
    public final int priceGold;
    public final int priceGems;
    public final int bonusStr;
    public final int bonusEnd;
    public final int bonusPre;
    public final int bonusLuck;
    public final String description;
    public final String icon;

    public Item(String id, String name, ItemSlot slot, Rarity rarity,
                int priceGold, int priceGems, int bonusStr, int bonusEnd,
                int bonusPre, int bonusLuck, String description, String icon) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.rarity = rarity;
        this.priceGold = priceGold;
        this.priceGems = priceGems;
        this.bonusStr = bonusStr;
        this.bonusEnd = bonusEnd;
        this.bonusPre = bonusPre;
        this.bonusLuck = bonusLuck;
        this.description = description != null ? description : "";
        this.icon = icon != null ? icon : "⚔️";
    }

    public Item(String id, String name, ItemSlot slot, Rarity rarity,
                int priceGold, int priceGems, int bonusStr, int bonusEnd,
                int bonusPre, int bonusLuck, String description) {
        this(id, name, slot, rarity, priceGold, priceGems, bonusStr, bonusEnd, bonusPre, bonusLuck, description, "⚔️");
    }

    public Item(String id, String name, ItemSlot slot, Rarity rarity,
                int priceGold, int priceGems, int bonusStr, int bonusEnd,
                int bonusPre, int bonusLuck) {
        this(id, name, slot, rarity, priceGold, priceGems, bonusStr, bonusEnd, bonusPre, bonusLuck, "", "⚔️");
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public ItemSlot getSlot() { return slot; }
    public Rarity getRarity() { return rarity; }
    public int getPriceGold() { return priceGold; }
    public int getPriceGems() { return priceGems; }
    public int getBonusStr() { return bonusStr; }
    public int getBonusEnd() { return bonusEnd; }
    public int getBonusPre() { return bonusPre; }
    public int getBonusLuck() { return bonusLuck; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }

    public int getTotalStats() {
        return bonusStr + bonusEnd + bonusPre + bonusLuck;
    }

    public int getCombatPower() {
        return (bonusStr * 10) + (bonusEnd * 8) + (bonusPre * 9) + (bonusLuck * 6);
    }

    public int getSellPriceGold() {
        if (priceGold > 0) {
            return Math.max(10, (int) (priceGold * 0.5));
        } else {
            return Math.max(20, priceGems * 50);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
