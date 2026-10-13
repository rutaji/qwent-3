package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;

/**
 * If the unit was played on the opponent's side (a treacherous monster), the effect's owner draws the top card of the
 * opponent's deck. Otherwise, or if that deck is empty, it is cancelled. No target to pick.
 */
public final class DrawFromOpponentEffect implements Effect {
    private final CardType source;
    private final Unit unit;

    public DrawFromOpponentEffect(CardType source, Unit unit) {
        this.source = source;
        this.unit = unit;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": draw a card from your opponent's deck");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return unit.owner() != owner && state.side(GameState.other(owner)).deckSize() > 0;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        state.drawFromDeckOf(GameState.other(owner), owner);
    }
}
