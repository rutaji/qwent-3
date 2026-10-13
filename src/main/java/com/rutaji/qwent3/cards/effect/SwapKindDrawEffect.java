package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import net.kyori.adventure.text.Component;

/**
 * The owner discards a card of their choice: a monster to draw the top-most spell of their deck, or a spell to draw
 * the top-most monster (nothing if there is none). Cancelled if the hand is empty.
 */
public final class SwapKindDrawEffect implements Effect {
    private final CardType source;

    public SwapKindDrawEffect(CardType source) {
        this.source = source;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": discard a monster to draw a spell, or a spell to draw a monster");
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
        boolean discardedMonster = state.side(owner).handCard(target).isMonster();
        state.discard(owner, target);
        state.drawFirst(owner, card -> card.isMonster() != discardedMonster);
    }
}
