package com.rutaji.changingitems.cards;

import org.jspecify.annotations.Nullable;

/** State of one card game. Board index = row * COLUMNS + column. */
public final class GameState {
    public static final int HAND_SIZE = 9;
    public static final int ROWS = 2;
    public static final int COLUMNS = 5;
    public static final int BOARD_SIZE = ROWS * COLUMNS;

    private final @Nullable CardType[] hand;
    private final @Nullable CardType[] board = new CardType[BOARD_SIZE];
    private int selectedHandIndex = -1;

    public GameState(CardType[] hand) {
        if (hand.length != HAND_SIZE) {
            throw new IllegalArgumentException("Hand must have " + HAND_SIZE + " cards");
        }
        this.hand = hand.clone();
    }

    public @Nullable CardType handCard(int index) {
        return hand[index];
    }

    public @Nullable CardType boardCard(int index) {
        return board[index];
    }

    public int selectedHandIndex() {
        return selectedHandIndex;
    }

    public @Nullable CardType selectedCard() {
        return selectedHandIndex < 0 ? null : hand[selectedHandIndex];
    }

    /** Selects a card in hand; clicking the selected card again deselects it. */
    public void selectHand(int index) {
        if (index < 0 || index >= HAND_SIZE || hand[index] == null) {
            return;
        }
        selectedHandIndex = selectedHandIndex == index ? -1 : index;
    }

    /** Places the selected card on an empty tile. Returns true if a card was placed. */
    public boolean placeOn(int tile) {
        CardType card = selectedCard();
        if (card == null || tile < 0 || tile >= BOARD_SIZE || board[tile] != null) {
            return false;
        }
        board[tile] = card;
        hand[selectedHandIndex] = null;
        selectedHandIndex = -1;
        return true;
    }

    public int totalPower() {
        int total = 0;
        for (CardType card : board) {
            if (card != null) {
                total += card.power();
            }
        }
        return total;
    }
}
