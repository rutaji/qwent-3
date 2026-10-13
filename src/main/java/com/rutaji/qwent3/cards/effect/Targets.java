package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.Unit;

/** Which monsters an effect may target, seen from the effect's owner. */
public enum Targets {
    ANY("a monster"),
    OWN("one of your monsters"),
    ENEMY("an enemy monster");

    private final String text;

    Targets(String text) {
        this.text = text;
    }

    /** E.g. "an enemy monster", for effect descriptions. */
    public String text() {
        return text;
    }

    public boolean allows(Unit unit, int owner) {
        return switch (this) {
            case ANY -> true;
            case OWN -> unit.owner() == owner;
            case ENEMY -> unit.owner() != owner;
        };
    }
}
