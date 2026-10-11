package com.rutaji.changingitems.cards.effect;

import com.rutaji.changingitems.cards.CardType;
import com.rutaji.changingitems.cards.GameState;
import com.rutaji.changingitems.cards.Pile;
import net.kyori.adventure.text.Component;

/**
 * Plays every copy of a monster from the deck or graveyard, one at a time; the player picks an empty tile for
 * each. After each one it queues itself again at the front, until no copy or no empty tile is left
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
                + (pile == Pile.DECK ? "deck" : "graveyard") + " on an empty tile");
    }

    @Override
    public boolean isValidTarget(GameState state, int tile) {
        return state.unit(tile) == null && state.pileContains(pile, card);
    }

    @Override
    public void resolve(GameState state, int tile) {
        if (!state.removeFromPile(pile, card)) {
            return;
        }
        state.play(card, tile);
        if (state.pileContains(pile, card)) {
            state.queueNext(this);
        }
    }
}
