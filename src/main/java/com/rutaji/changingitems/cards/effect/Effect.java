package com.rutaji.changingitems.cards.effect;

import com.rutaji.changingitems.cards.GameState;
import net.kyori.adventure.text.Component;

/**
 * Something a card does: when played from hand, when it dies, or when a spell is cast.
 * Effects wait in the game's effect queue and resolve one by one; resolving one may queue more.
 *
 * An effect with no valid target when it is its turn to resolve is cancelled.
 * A targeted effect waits for the player to pick a tile and cannot be skipped.
 */
public interface Effect {
    /** Shown to the player while they pick the target, e.g. "Mage: deal 2 damage to another monster". */
    Component description();

    /** False for effects that resolve on their own; such effects get target -1. */
    default boolean needsTarget() {
        return true;
    }

    boolean isValidTarget(GameState state, int tile);

    /** True to resolve without asking when exactly one tile is a valid target (e.g. "the lowest monster"). */
    default boolean autoPickSingleTarget() {
        return false;
    }

    default boolean hasValidTargets(GameState state) {
        if (!needsTarget()) {
            return true;
        }
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            if (isValidTarget(state, tile)) {
                return true;
            }
        }
        return false;
    }

    /** Applies the effect. {@code tile} is the picked target, or -1 if {@link #needsTarget()} is false. */
    void resolve(GameState state, int tile);
}
