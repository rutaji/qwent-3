package com.rutaji.qwent3.cards;

/** A monster on the board. Its power can change; identity matters (effects can exclude a specific unit). */
public final class Unit {
    private final CardType type;
    private int owner;
    private int tile;
    private int power;

    Unit(CardType type, int owner, int tile) {
        this.type = type;
        this.owner = owner;
        this.tile = tile;
        this.power = type.power();
    }

    public CardType type() {
        return type;
    }

    /** The side (0 or 1) the unit is on; its on-damaged and on-death effects belong to that player. */
    public int owner() {
        return owner;
    }

    /** The tile the unit is on (or was on when it died). Units only move when pushed / pulled. */
    public int tile() {
        return tile;
    }

    public int power() {
        return power;
    }

    void addPower(int amount) {
        power += amount;
    }

    /** Moves the unit; it now belongs to the side of the new tile. */
    void moveTo(int tile) {
        this.tile = tile;
        this.owner = GameState.sideOf(tile);
    }
}
