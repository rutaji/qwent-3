package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

/**
 * The owner discards {@code discard} cards of their choice from hand, one pick at a time, then draws {@code draw}
 * cards. Cancelled if they don't have enough cards in hand when it starts.
 */
public final class DiscardToDrawCardsEffect implements Effect {
    private final CardType source;
    private final int discard;
    private final int draw;
    private int discarded;

    public DiscardToDrawCardsEffect(CardType source, int discard, int draw) {
        this.source = source;
        this.discard = discard;
        this.draw = draw;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": discard a card (" + (discarded + 1) + " of " + discard
                + "), then draw " + draw);
    }

    @Override
    public TargetKind targetKind() {
        return TargetKind.HAND;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return state.side(owner).handSize() >= discard - discarded;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return target >= 0 && target < state.side(owner).handSize();
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        engine.discard(owner, target);
        if (++discarded < discard) {
            engine.queueNext(owner, this);
        } else {
            engine.draw(owner, draw);
        }
    }
}
