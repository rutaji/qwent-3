package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Counts cards in a pile and returns the number, without changing the game; use {@link #thenWith} for what to do with
 * it, e.g. "+3 power for every Potato in your graveyard". Cancelled if there is no such card. No target to pick.
 */
public final class CountEffect implements ResultEffect<Integer> {
    private final CardType source;
    private final PileCount count;
    private final String text;

    /** @param text the whole effect for the description, e.g. "+3 power for every Potato in your graveyard" */
    public CountEffect(CardType source, PileCount count, String text) {
        this.source = source;
        this.count = count;
        this.text = text;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": " + text);
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return count.count(state, owner) > 0;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public String logDetail(GameState state, int owner) {
        return String.valueOf(count.count(state, owner));
    }

    @Override
    public @Nullable Integer resolveWithResult(GameEngine engine, int owner, int target) {
        int n = count.count(engine.state(), owner);
        return n > 0 ? n : null;
    }
}
