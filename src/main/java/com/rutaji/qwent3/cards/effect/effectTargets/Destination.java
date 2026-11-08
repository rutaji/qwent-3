package com.rutaji.qwent3.cards.effect.effectTargets;

import com.rutaji.qwent3.cards.cardsRepository.Pile;
import org.jspecify.annotations.Nullable;

/** Where a card goes: which pile, and where in it. */
public enum Destination {
    /** Into the graveyard, on top (a normal discard). */
    GraveyardTop(Pile.GRAVEYARD),
    /** Into the graveyard at a random position. */
    GraveyardRandom(Pile.GRAVEYARD),
    /** Into the deck at a random position. */
    DeckRandom(Pile.DECK),
    /** On top of the deck: drawn next. */
    DeckTop(Pile.DECK),
    /** At the bottom of the deck: drawn last. */
    DeckBottom(Pile.DECK),
    /** Removed from the game for good. */
    Banished(null);

    private final @Nullable Pile pile;

    Destination(@Nullable Pile pile) {
        this.pile = pile;
    }

    /** The pile the card ends up in; null if it is banished. */
    public @Nullable Pile pile() {
        return pile;
    }
}
