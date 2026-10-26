package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

/** Puts copies of a card into a deck: on top, or shuffled in at random positions. No target to pick. */
public final class AddToDeckEffect implements Effect {
    private final CardType source;
    private final CardType card;
    private final int copies;
    private final boolean opponents;
    private final boolean onTop;

    /**
     * @param opponents true = the opponent's deck, false = the owner's
     * @param onTop true = on top of the deck, false = shuffled in at random positions
     */
    public AddToDeckEffect(CardType source, CardType card, int copies, boolean opponents, boolean onTop) {
        this.source = source;
        this.card = card;
        this.copies = copies;
        this.opponents = opponents;
        this.onTop = onTop;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": put " + copies + " " + card.displayName() + " into "
                + (opponents ? "your opponent's" : "your") + " deck");
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
        int side = opponents ? GameState.other(owner) : owner;
        for (int i = 0; i < copies; i++) {
            if (onTop) {
                engine.putOnTopOfDeck(side, card);
            } else {
                engine.shuffleIntoDeck(side, card);
            }
        }
    }
}
