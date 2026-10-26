package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;

/**
 * The owner picks a monster on either side; copies of its card go into the owner's deck (shuffled in) or graveyard.
 */
public final class CopyToPileEffect implements Effect {
    private final CardType source;
    private final int copies;
    private final Pile pile;

    public CopyToPileEffect(CardType source, int copies, Pile pile) {
        this.source = source;
        this.copies = copies;
        this.pile = pile;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": put " + copies + " copies of a monster into your "
                + (pile == Pile.DECK ? "deck" : "graveyard"));
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return state.unit(target) != null;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        Unit unit = engine.state().unit(target);
        if (unit == null) {
            return;
        }
        for (int i = 0; i < copies; i++) {
            if (pile == Pile.DECK) {
                engine.shuffleIntoDeck(owner, unit.type());
            } else {
                engine.addToGraveyard(owner, unit.type());
            }
        }
    }
}
