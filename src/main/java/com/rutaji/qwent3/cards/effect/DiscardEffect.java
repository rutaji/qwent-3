package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Tag;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * The owner discards {@code count} matching cards of their choice from hand, one pick at a time; the result is the
 * card discarded by this pick. Cancelled if they don't have enough matching cards in hand when it starts.
 * Combine it with {@link #then} / {@link #thenWith} for "discard ... to ...".
 */
public final class DiscardEffect implements ResultEffect<CardType> {
    private final CardType source;
    private final int count;
    private final Predicate<CardType> filter;
    private final String what;
    private int discarded;

    /** Discard one card. */
    public DiscardEffect(CardType source) {
        this(source, 1);
    }

    public DiscardEffect(CardType source, int count) {
        this(source, count, card -> true, "a card");
    }

    /** Discard one card with the tag. */
    public DiscardEffect(CardType source, Tag tag) {
        this(source, 1, card -> card.hasTag(tag), "a " + tag.displayName() + " card");
    }

    /** @param what the card for the description, e.g. "a Zombie card" */
    public DiscardEffect(CardType source, int count, Predicate<CardType> filter, String what) {
        this.source = source;
        this.count = count;
        this.filter = filter;
        this.what = what;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": discard " + what
                + (count > 1 ? " (" + (discarded + 1) + " of " + count + ")" : ""));
    }

    @Override
    public TargetKind targetKind() {
        return TargetKind.HAND;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        int matching = 0;
        for (int i = 0; i < state.side(owner).handSize(); i++) {
            if (filter.test(state.side(owner).handCard(i))) {
                matching++;
            }
        }
        return matching >= count - discarded;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return target >= 0 && target < state.side(owner).handSize() && filter.test(state.side(owner).handCard(target));
    }

    @Override
    public @Nullable CardType resolveWithResult(GameEngine engine, int owner, int target) {
        CardType card = engine.state().side(owner).handCard(target);
        engine.discard(owner, target);
        if (++discarded < count) {
            engine.queueNext(owner, this);
        }
        return card;
    }
}
