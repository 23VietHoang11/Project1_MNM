package com.example.vigil.model;

import java.util.Collections;
import java.util.List;

public class Chapter {
    private final int id;
    private final String name;
    private final String icon;
    private final String description;
    private final List<Monster> monsters;

    public Chapter(int id, String name, String icon, String description, List<Monster> monsters) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.monsters = monsters != null ? monsters : Collections.emptyList();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public List<Monster> getMonsters() {
        return monsters;
    }
}
