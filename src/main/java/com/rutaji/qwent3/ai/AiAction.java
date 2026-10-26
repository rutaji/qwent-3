package com.rutaji.qwent3.ai;

/**
 * Something the AI does. The AI must return one of {@link AiView#legalActions()}; records compare by value, so a newly
 * built equal action (e.g. {@code new AiAction.Pass()}) counts as that legal action.
 */
public sealed interface AiAction {
    /** Mulligan: shuffle the hand card at this index back and draw a replacement. */
    record Redraw(int handIndex) implements AiAction {
    }

    /** Mulligan: keep the current hand. */
    record KeepHand() implements AiAction {
    }

    /** Play the hand card at this index: a monster on {@code tile}; for a spell the tile is -1. */
    record Play(int handIndex, int tile) implements AiAction {
    }

    /** End your turns for the rest of the round. */
    record Pass() implements AiAction {
    }

    /** Pick the target tile of your pending effect. */
    record PickTile(int tile) implements AiAction {
    }

    /** Pick the hand card (index) your pending effect asks for, e.g. what to discard. */
    record PickHand(int handIndex) implements AiAction {
    }
}
