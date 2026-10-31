package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.effect.effectTargets.Pack;
import com.rutaji.qwent3.cards.effect.effectTargets.SideTarget;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.CardFilter;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * A player ({@code who}, seen from the owner) draws cards from the top of a deck or graveyard ({@code pack}) of a side
 * ({@code packSide}); with a filter, the top-most matching ones (fewer if the pile runs out of them). No target to pick.
 */
public final class DrawEffect implements Effect {
    private final CardType source;
    private final int count;
    /** Null = any card. */
    private final @Nullable CardFilter filter;
    private final  SideTarget who; //who draws the card
    private final SideTarget packSide; // is it from opponents or from owners deck
    private final  Pack pack; // is it from deck or graveyard

    public DrawEffect(CardType source, int count, @Nullable CardFilter filter, SideTarget who, Pack pack, SideTarget packSide) {
        this.source = source;
        this.count = count;
        this.filter = filter;
        this.who = who;
        this.packSide = packSide;
        this.pack = pack;
    }
    /** The owner draws from their deck. */
    public DrawEffect(CardType source, int count) {
        this(source, count, null);
    }

    /** The owner draws matching cards from their deck. */
    public DrawEffect(CardType source, int count, @Nullable CardFilter filter) {
        this(source, count, filter, SideTarget.Owner, Pack.Deck, SideTarget.Owner);
    }


    @Override
    public Component description() {
        return Component.text(source.displayName() + ": draw " + count + (count == 1 ? " card" : " cards"));
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
    public void resolve(GameEngine engine, int owner, int target) {
        engine.draw(side(who, owner), count, filter, side(packSide, owner),
                pack == Pack.Deck ? Pile.DECK : Pile.GRAVEYARD);
    }

    private static int side(SideTarget target, int owner) {
        return target == SideTarget.Owner ? owner : GameState.other(owner);
    }
}
