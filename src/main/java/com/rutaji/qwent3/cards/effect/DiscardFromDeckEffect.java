package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Pile;
import net.kyori.adventure.text.Component;

/** Moves every copy of a card from the owner's deck to their graveyard. Cancelled if there is none. */
public final class DiscardFromDeckEffect implements Effect {
    private final CardType source;
    private final CardType card;

    public DiscardFromDeckEffect(CardType source, CardType card) {
        this.source = source;
        this.card = card;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": discard every " + card.displayName() + " from your deck");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return state.pileContains(owner, Pile.DECK, card);
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        state.discardFromDeck(owner, card);
    }
}
