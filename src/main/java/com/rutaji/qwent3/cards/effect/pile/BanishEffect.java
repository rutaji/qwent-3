package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.effect.ResultEffect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.cardsRepository.Tag;
import com.rutaji.qwent3.cards.effect.effectTargets.SideTarget;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.CardFilter;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.FilterTags;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Removes a random matching card for good from a deck or graveyard ({@code pile}) of a side ({@code side}, seen from
 * the owner); the result is the banished card. Cancelled if there is no such card. No target to pick.
 * Combine it with {@link #then} for "banish ... to ...".
 */
public final class BanishEffect implements ResultEffect<CardType> {
    private final CardType source;
    /** Null = any card. */
    private final @Nullable CardFilter filter;
    private final String what;
    private final SideTarget side;
    private final Pile pile;

    /** @param what the card for the description, e.g. "a random Plant" */
    public BanishEffect(CardType source, @Nullable CardFilter filter, String what, SideTarget side, Pile pile) {
        this.source = source;
        this.filter = filter;
        this.what = what;
        this.side = side;
        this.pile = pile;
    }

    /** A random card with the tag from the owner's graveyard. */
    public static BanishEffect fromGraveyard(CardType source, Tag tag) {
        return new BanishEffect(source, new FilterTags(Set.of(tag)), "a random " + tag.displayName(),
                SideTarget.Owner, Pile.GRAVEYARD);
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": banish " + what + " from "
                + (side == SideTarget.Opponent ? "your opponent's" : "your")
                + (pile == Pile.DECK ? " deck" : " graveyard"));
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return new PileCount(filter, pile, side).count(state, owner) > 0;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public @Nullable CardType resolveWithResult(GameEngine engine, int owner, int target) {
        int s = side == SideTarget.Owner ? owner : GameState.other(owner);
        return engine.banishRandom(s, pile, filter);
    }
}
