package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * An effect that produces a value when it resolves (e.g. the card it discarded), so the effects after it can depend
 * on it, see {@link #thenWith}.
 */
public interface ResultEffect<T> extends Effect {
    /** Like {@link #resolve}, returning what it did; null = nothing happened. */
    @Nullable T resolveWithResult(GameEngine engine, int owner, int target);

    @Override
    default void resolve(GameEngine engine, int owner, int target) {
        resolveWithResult(engine, owner, target);
    }

    /** This, then the effects built from its result (none if the result is null). */
    default Effect thenWith(Function<T, List<Effect>> next) {
        return new SwitchEffect<>(this, next);
    }
}
