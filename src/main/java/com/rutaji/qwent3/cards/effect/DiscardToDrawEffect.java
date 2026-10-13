package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Tag;
import net.kyori.adventure.text.Component;

/**
 * The owner picks a card with the given tag in their hand and discards it, then draws the top-most spell from their
 * deck (nothing if there is none, the card is still discarded). Cancelled if they have no such card in hand.
 */
public final class DiscardToDrawEffect implements Effect {
    private final CardType source;
    private final Tag discardTag;

    public DiscardToDrawEffect(CardType source, Tag discardTag) {
        this.source = source;
        this.discardTag = discardTag;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": discard a " + discardTag.displayName() + " card to draw a spell");
    }

    @Override
    public TargetKind targetKind() {
        return TargetKind.HAND;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return target >= 0 && target < state.side(owner).handSize()
                && state.side(owner).handCard(target).hasTag(discardTag);
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        state.discard(owner, target);
        state.drawFirst(owner, card -> !card.isMonster());
    }
}
