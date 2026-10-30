package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.CardFilter;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * The owner draws cards from the top of their deck; with a filter, the top-most matching ones (fewer if the deck runs
 * out of them). No target to pick.
 */
public final class DrawEffect implements Effect {
    private final CardType source;
    private final int count;
    /** Null = any card. */
    private final @Nullable CardFilter filter;

    public DrawEffect(CardType source, int count,CardFilter filter) {
        this.source = source;
        this.count = count;
        this.filter = filter;
    }
    public DrawEffect(CardType source, int count) {
        this.source = source;
        this.count = count;
        this.filter = null;
    }


    @Override
    public Component description() {
        return Component.text(source.displayName() + ": draw " + count + (count == 1 ? " card" : " cards"));
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        engine.draw(owner, count, filter);
    }
}
