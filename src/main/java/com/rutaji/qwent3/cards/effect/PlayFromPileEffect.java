package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import net.kyori.adventure.text.Component;

/**
 * Plays every copy of a monster from the owner's deck or graveyard, one at a time; the owner picks an empty tile of
 * their side for each. After each one it queues itself again at the front, until no copy or no empty tile is left
 * (remaining copies stay where they are).
 */
public final class PlayFromPileEffect implements Effect {
    private final CardType source;
    private final CardType card;
    private final Pile pile;

    public PlayFromPileEffect(CardType source, CardType card, Pile pile) {
        if (!card.isMonster()) {
            throw new IllegalArgumentException("Only monsters can be played: " + card);
        }
        this.source = source;
        this.card = card;
        this.pile = pile;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": play a " + card.displayName() + " from your "
                + (pile == Pile.DECK ? "deck" : "graveyard") + " on an empty tile of your side");
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int tile) {
        return state.unit(tile) == null && GameState.sideOf(tile) == owner && state.pileContains(owner, pile, card);
    }

    @Override
    public void resolve(GameEngine engine, int owner, int tile) {
        if (!engine.removeFromPile(owner, pile, card)) {
            return;
        }
        engine.play(owner, card, tile);
        if (engine.state().pileContains(owner, pile, card)) {
            engine.queueNext(owner, this);
        }
    }
}
