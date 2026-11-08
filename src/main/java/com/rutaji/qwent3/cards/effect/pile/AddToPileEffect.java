package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.effect.effectTargets.Destination;
import com.rutaji.qwent3.cards.effect.effectTargets.SideTarget;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

/**
 * Puts copies of a card into a deck or graveyard of a side ({@code side}, seen from the owner), as {@code destination}
 * says. No target to pick.
 */
public final class AddToPileEffect implements Effect {
    private final CardType source;
    private final CardType card;
    private final int copies;
    private final SideTarget side;
    private final Destination destination;

    public AddToPileEffect(CardType source, CardType card, int copies, SideTarget side, Destination destination) {
        this.source = source;
        this.card = card;
        this.copies = copies;
        this.side = side;
        this.destination = destination;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": put " + copies + " " + card.displayName() + " into "
                + (side == SideTarget.Opponent ? "your opponent's" : "your")
                + (destination.pile() == Pile.DECK ? " deck" : " graveyard"));
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
        int s = side == SideTarget.Owner ? owner : GameState.other(owner);
        for (int i = 0; i < copies; i++) {
            engine.addToPile(s, card, destination);
        }
    }
}
