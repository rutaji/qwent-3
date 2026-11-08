package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.effect.combinator.ToEffect;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Something a card does: when played from hand, when it is damaged (and survives) or dies, or when a spell is cast.
 * Effects wait in the game's effect queue and resolve one by one; resolving one may queue more.
 *
 * Every queued effect has an owner (side 0 or 1): the player who played the card, or the player on whose side the
 * monster was damaged / died. "Your side", "your deck" etc. mean the owner's. A targeted effect waits for its owner
 * to pick a target - a board tile, or a card in their hand ({@link #targetKind()}) - and cannot be skipped; an
 * effect with no valid target when it is its turn is cancelled.
 */
public interface Effect {
    enum TargetKind {
        /** A board tile index, 0 .. {@link GameState#BOARD_SIZE} - 1. */
        TILE,
        /** An index in the owner's hand. */
        HAND
    }
    /** Shown to the player while they pick the target, e.g. "Mage: deal 2 damage to another monster". */
    Component description();

    /** False for effects that resolve on their own; such effects get target -1. */
    default boolean needsTarget() {
        return true;
    }

    /** What the target index means: a tile (default) or a card in the owner's hand. */
    default TargetKind targetKind() {
        return TargetKind.TILE;
    }

    /** @param target a tile or a hand index, see {@link #targetKind()} */
    boolean isValidTarget(GameState state, int owner, int target);

    default boolean hasValidTargets(GameState state, int owner) {
        if (!needsTarget()) {
            return true;
        }
        int count = targetKind() == TargetKind.TILE ? GameState.BOARD_SIZE : state.side(owner).handSize();
        for (int target = 0; target < count; target++) {
            if (isValidTarget(state, owner, target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extra text for the game history of an effect without a picked target, e.g. what it will destroy; logged as
     * " -> detail" just before it resolves. Null = nothing extra.
     */
    default @Nullable String logDetail(GameState state, int owner) {
        return null;
    }

    /**
     * Applies the effect, changing the game through the engine (read it with {@code engine.state()}).
     * {@code target} is the picked tile or hand index (see {@link #targetKind()}), or -1 if {@link #needsTarget()}
     * is false.
     */
    void resolve(GameEngine engine, int owner, int target);

    /** This, then the other effects (in order); see {@link ToEffect}. */
    default Effect then(Effect... next) {
        return new ToEffect(this, next);
    }
}
