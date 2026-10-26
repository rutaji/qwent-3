package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Cost "discard N cards to ...": the owner picks cards in their hand to discard, one at a time, then the other effects
 * follow (in order). Cancelled (nothing discarded) if the hand has fewer than N cards or the first of the other
 * effects has no valid target.
 */
public final class DiscardThenEffect implements Effect {
    private final CardType source;
    private final int count;
    private final List<Effect> then;
    private int discarded;

    /** Discard one card, then the effects. */
    public DiscardThenEffect(CardType source, Effect... then) {
        this(source, 1, then);
    }

    public DiscardThenEffect(CardType source, int count, Effect... then) {
        this.source = source;
        this.count = count;
        this.then = List.of(then);
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": discard a card first"
                + (count > 1 ? " (" + (discarded + 1) + " of " + count + ")" : ""));
    }

    @Override
    public TargetKind targetKind() {
        return TargetKind.HAND;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return state.side(owner).handSize() >= count - discarded && then.getFirst().hasValidTargets(state, owner);
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return target >= 0 && target < state.side(owner).handSize();
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        engine.discard(owner, target);
        if (++discarded < count) {
            engine.queueNext(owner, this);
            return;
        }
        // Front of the queue, keeping their order.
        for (Effect effect : then.reversed()) {
            engine.queueNext(owner, effect);
        }
    }
}
