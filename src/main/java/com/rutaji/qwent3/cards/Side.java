package com.rutaji.qwent3.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** One player's cards in a game: deck (top = index 0), hand, graveyard, and their mulligan / pass state. */
public final class Side {
    final List<CardType> deck;
    final List<CardType> hand = new ArrayList<>();
    final List<CardType> graveyard = new ArrayList<>();
    int selectedHandIndex = -1;
    int redrawsLeft;
    boolean mulliganDone = true;
    /** Card types redrawn during the mulligan; they are not drawn again while it lasts. */
    final Set<CardType> redrawn = EnumSet.noneOf(CardType.class);
    boolean passed;

    Side(List<CardType> deck) {
        this.deck = new ArrayList<>(deck);
    }

    public int handSize() {
        return hand.size();
    }

    public CardType handCard(int index) {
        return hand.get(index);
    }

    public int deckSize() {
        return deck.size();
    }

    /** The deck in draw order (top first). Don't show this order to the player. */
    public List<CardType> deck() {
        return Collections.unmodifiableList(deck);
    }

    public List<CardType> graveyard() {
        return Collections.unmodifiableList(graveyard);
    }

    public int selectedHandIndex() {
        return selectedHandIndex;
    }

    public int redrawsLeft() {
        return redrawsLeft;
    }

    public boolean mulliganDone() {
        return mulliganDone;
    }

    public boolean passed() {
        return passed;
    }

    List<CardType> pile(Pile pile) {
        return pile == Pile.DECK ? deck : graveyard;
    }
}
