package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.effect.combinator.IfEffect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.effect.effectTargets.Pack;
import com.rutaji.qwent3.cards.effect.effectTargets.SideTarget;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.CardFilter;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.FilterCard;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Side;
import org.jspecify.annotations.Nullable;

import java.util.function.BiPredicate;

/**
 * Counts the matching cards in a deck or graveyard ({@code pack}) of a side ({@code side}, seen from the effect's
 * owner), e.g. "every Potato in your graveyard". Use it with {@link CountEffect} or, via {@link #atLeast},
 * {@link IfEffect}.
 */
public final class PileCount {
    /** Null = any card. */
    private final @Nullable CardFilter filter;
    private final Pack pack;
    private final SideTarget side;

    public PileCount(@Nullable CardFilter filter, Pack pack, SideTarget side) {
        this.filter = filter;
        this.pack = pack;
        this.side = side;
    }

    /** The copies of a card in the owner's graveyard. */
    public static PileCount graveyard(CardType card) {
        return new PileCount(new FilterCard(card), Pack.Graveyard, SideTarget.Owner);
    }

    public int count(GameState state, int owner) {
        Side s = state.side(side == SideTarget.Owner ? owner : GameState.other(owner));
        int count = 0;
        for (CardType card : pack == Pack.Deck ? s.deck() : s.graveyard()) {
            if (filter == null || filter.check(card)) {
                count++;
            }
        }
        return count;
    }

    /** A condition for {@link IfEffect}: at least {@code n} such cards. */
    public BiPredicate<GameState, Integer> atLeast(int n) {
        return (state, owner) -> count(state, owner) >= n;
    }
}
