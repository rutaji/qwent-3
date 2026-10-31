package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.effect.combinator.IfEffect;
import com.rutaji.qwent3.cards.effect.combinator.CountEffect;
import com.rutaji.qwent3.cards.gameLogic.GameState;

import java.util.function.BiPredicate;

/**
 * Counts something in a game, seen from an effect's owner, e.g. "every Potato in your graveyard" or "every monster
 * with 1 power". Use it with {@link CountEffect} or, via {@link #atLeast}, {@link IfEffect}.
 */
public interface Count {
    int count(GameState state, int owner);

    /** A condition for {@link IfEffect}: the count is at least {@code n}. */
    default BiPredicate<GameState, Integer> atLeast(int n) {
        return (state, owner) -> count(state, owner) >= n;
    }
}
