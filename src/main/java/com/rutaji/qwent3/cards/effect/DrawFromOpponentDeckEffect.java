package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

/**
 * Takes the top card of the opponent's deck and gives it to the effect's owner (into their hand; with a full hand
 * to their graveyard). Cancelled if the opponent's deck is empty. No target to pick.
 */
public final class DrawFromOpponentDeckEffect implements Effect {
    private final CardType source;

    public DrawFromOpponentDeckEffect(CardType source) {
        this.source = source;
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
        return state.side(GameState.other(owner)).deckSize() > 0;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        engine.drawFromDeckOf(GameState.other(owner), owner);
    }
}
