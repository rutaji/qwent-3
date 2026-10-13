package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import net.kyori.adventure.text.Component;

/** Swaps the owner's deck and graveyard, without shuffling. No target to pick. */
public final class SwapDeckAndGraveyardEffect implements Effect {
    private final CardType source;

    public SwapDeckAndGraveyardEffect(CardType source) {
        this.source = source;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": swap your deck and graveyard");
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
    public void resolve(GameState state, int owner, int target) {
        state.swapDeckAndGraveyard(owner);
    }
}
