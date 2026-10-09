package com.example.vigil.model;

import java.util.Objects;

public class InventoryItem {
    private final int invId;
    private final Item item;
    private final boolean isEquipped;
    private final String acquiredAt;

    public InventoryItem(int invId, Item item, boolean isEquipped, String acquiredAt) {
        this.invId = invId;
        this.item = item;
        this.isEquipped = isEquipped;
        this.acquiredAt = acquiredAt != null ? acquiredAt : "";
    }

    public InventoryItem(int invId, Item item, boolean isEquipped) {
        this(invId, item, isEquipped, "");
    }

    public int getInvId() {
        return invId;
    }

    public Item getItem() {
        return item;
    }

    public boolean isEquipped() {
        return isEquipped;
    }

    public String getAcquiredAt() {
        return acquiredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem that = (InventoryItem) o;
        return invId == that.invId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(invId);
    }
}
