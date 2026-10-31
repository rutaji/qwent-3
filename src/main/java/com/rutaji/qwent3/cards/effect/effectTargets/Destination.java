package com.rutaji.qwent3.cards.effect.effectTargets;

/** Where a card goes when it leaves the hand, seen from its owner. */
public enum Destination {
    /** Into the owner's graveyard, on top (a normal discard). */
    Graveyard,
    /** Into the owner's deck at a random position. */
    DeckRandom,
    /** On top of the owner's deck: drawn next. */
    DeckTop,
    /** At the bottom of the owner's deck: drawn last. */
    DeckBottom,
    /** Removed from the game for good. */
    Banished
}
