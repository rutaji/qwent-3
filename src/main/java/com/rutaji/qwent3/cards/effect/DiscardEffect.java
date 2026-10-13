package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import net.kyori.adventure.text.Component;

/** The owner discards a card of their choice from hand. Cancelled if the hand is empty. */
public final class DiscardEffect implements Effect {
    private final CardType source;

    public DiscardEffect(CardType source) {
        this.source = source;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": discard a card");
    }

    @Override
    public TargetKind targetKind() {
        return TargetKind.HAND;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return target >= 0 && target < state.side(owner).handSize();
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        state.discard(owner, target);
    }
}
