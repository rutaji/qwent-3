package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import net.kyori.adventure.text.Component;

import java.util.Collections;

/** Draws a card if the owner's graveyard holds at least {@code needed} copies of a card. No target to pick. */
public final class DrawIfGraveyardHasEffect implements Effect {
    private final CardType source;
    private final CardType card;
    private final int needed;

    public DrawIfGraveyardHasEffect(CardType source, CardType card, int needed) {
        this.source = source;
        this.card = card;
        this.needed = needed;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": draw a card");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return Collections.frequency(state.side(owner).graveyard(), card) >= needed;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        state.draw(owner, 1);
    }
}
