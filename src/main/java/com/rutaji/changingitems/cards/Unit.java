package com.rutaji.changingitems.cards;

/** A monster on the board. Its power can change; identity matters (effects can exclude a specific unit). */
public final class Unit {
    private final CardType type;
    private int power;

    Unit(CardType type) {
        this.type = type;
        this.power = type.power();
    }

    public CardType type() {
        return type;
    }

    public int power() {
        return power;
    }

    void addPower(int amount) {
        power += amount;
    }
}
