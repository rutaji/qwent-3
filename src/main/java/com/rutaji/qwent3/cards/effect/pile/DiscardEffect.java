package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.cardsRepository.Tag;
import com.rutaji.qwent3.cards.effect.ResultEffect;
import com.rutaji.qwent3.cards.effect.effectTargets.Destination;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.CardFilter;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.FilterTags;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * The owner picks {@code count} matching cards from their hand, one at a time, and each goes to {@code destination}:
 * their graveyard (a normal discard), their deck (at a random position, on top or at the bottom), or banished. The
 * result is the card of this pick. Cancelled if they don't have enough matching cards in hand when it starts.
 * Combine it with {@link #then} / {@link #thenWith} for "discard ... to ...".
 */
public final class DiscardEffect implements ResultEffect<CardType> {
    private final CardType source;
    private final int count;
    /** Null = any card. */
    private final @Nullable CardFilter filter;
    private final String what;
    private final Destination destination;
    private int discarded;

    /** Discard one card. */
    public DiscardEffect(CardType source) {
        this(source, 1);
    }

    public DiscardEffect(CardType source, int count) {
        this(source, count, null, "a card", Destination.Graveyard);
    }

    /** Discard one card with the tag. */
    public DiscardEffect(CardType source, Tag tag) {
        this(source, 1, new FilterTags(Set.of(tag)), "a " + tag.displayName() + " card", Destination.Graveyard);
    }

    /** @param what the card for the description, e.g. "a Zombie card" */
    public DiscardEffect(CardType source, int count, @Nullable CardFilter filter, String what,
                         Destination destination) {
        this.source = source;
        this.count = count;
        this.filter = filter;
        this.what = what;
        this.destination = destination;
    }

    @Override
    public Component description() {
        String text = switch (destination) {
            case Graveyard -> "discard " + what;
            case DeckRandom -> "shuffle " + what + " from your hand into your deck";
            case DeckTop -> "put " + what + " from your hand on top of your deck";
            case DeckBottom -> "put " + what + " from your hand at the bottom of your deck";
            case Banished -> "banish " + what + " from your hand";
        };
        return Component.text(source.displayName() + ": " + text
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
            if (matches(state.side(owner).handCard(i))) {
                matching++;
            }
        }
        return matching >= count - discarded;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return target >= 0 && target < state.side(owner).handSize() && matches(state.side(owner).handCard(target));
    }

    @Override
    public @Nullable CardType resolveWithResult(GameEngine engine, int owner, int target) {
        CardType card = engine.takeFromHand(owner, target);
        switch (destination) {
            case Graveyard -> engine.addToGraveyard(owner, card);
            case DeckRandom -> engine.addToPile(owner, Pile.DECK, card, false);
            case DeckTop -> engine.addToPile(owner, Pile.DECK, card, true);
            case DeckBottom -> engine.addToPileBottom(owner, Pile.DECK, card);
            case Banished -> {
                // Gone for good.
            }
        }
        if (++discarded < count) {
            engine.queueNext(owner, this);
        }
        return card;
    }

    private boolean matches(CardType card) {
        return filter == null || filter.check(card);
    }
}
