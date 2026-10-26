package com.rutaji.qwent3.cards.cardsRepository;

/** Card tags, e.g. for effects like "buff all zombies". A card (monster or spell) can have any number of them. */
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
