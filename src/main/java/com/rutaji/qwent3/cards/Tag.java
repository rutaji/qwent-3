package com.rutaji.qwent3.cards;

/** Monster tags, e.g. for effects like "buff all zombies". A monster can have any number of them. */
public enum Tag {
    ZOMBIE("Zombie"),
    PLANT("Plant");

    private final String displayName;

    Tag(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
