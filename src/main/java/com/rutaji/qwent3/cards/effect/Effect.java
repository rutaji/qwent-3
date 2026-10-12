package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.GameState;
import net.kyori.adventure.text.Component;

/**
 * Something a card does: when played from hand, when it is hurt or dies, or when a spell is cast.
 * Effects wait in the game's effect queue and resolve one by one; resolving one may queue more.
 *
 * Every queued effect has an owner (side 0 or 1): the player who played the card, or the player on whose side the
 * monster was hurt / died. "Your side", "your deck" etc. mean the owner's. A targeted effect waits for its owner
 * to pick a tile and cannot be skipped; an effect with no valid target when it is its turn is cancelled.
 */
public interface Effect {
    /** Shown to the player while they pick the target, e.g. "Mage: deal 2 damage to another monster". */
    Component description();

    /** False for effects that resolve on their own; such effects get target -1. */
    default boolean needsTarget() {
        return true;
    }

    boolean isValidTarget(GameState state, int owner, int tile);

    default boolean hasValidTargets(GameState state, int owner) {
        if (!needsTarget()) {
            return true;
        }
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            if (isValidTarget(state, owner, tile)) {
                return true;
            }
        }
        return false;
    }

    /** Applies the effect. {@code tile} is the picked target, or -1 if {@link #needsTarget()} is false. */
    void resolve(GameState state, int owner, int tile);
}
