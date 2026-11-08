package com.rutaji.qwent3.cards.gameLogic;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** One player's cards in a game: deck and graveyard (top = index 0 of both), hand, and their mulligan / pass state. */
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
    /** Rounds won (a tied round gives both players a point). */
    int points;

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

    /** The deck, top first. Don't show this order to the player. */
    public List<CardType> deck() {
        return Collections.unmodifiableList(deck);
    }

    /** The graveyard, top (newest) first. */
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

    public int points() {
        return points;
    }

    public List<CardType> pile(Pile pile) {
        return pile == Pile.DECK ? deck : graveyard;
    }
}
