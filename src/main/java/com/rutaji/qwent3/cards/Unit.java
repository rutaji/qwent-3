package com.rutaji.qwent3.cards;

/** A monster on the board. Its power can change; identity matters (effects can exclude a specific unit). */
public final class Unit {
    private final CardType type;
    private final int owner;
    private int power;

    Unit(CardType type, int owner) {
        this.type = type;
        this.owner = owner;
        this.power = type.power();
    }

    public CardType type() {
        return type;
    }

    /** The side (0 or 1) the unit is on; its on-hurt and on-death effects belong to that player. */
    public int owner() {
        return owner;
    }

    public int power() {
        return power;
    }

    void addPower(int amount) {
        power += amount;
    }
}
