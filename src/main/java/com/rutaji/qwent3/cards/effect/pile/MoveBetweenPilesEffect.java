package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.effect.effectTargets.Destination;
import com.rutaji.qwent3.cards.effect.effectTargets.SideTarget;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.CardFilter;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Moves every matching card from one deck or graveyard to another, of either side (seen from the owner), e.g. "discard
 * every Brown Mushroom from your deck". Each goes to {@code destination}. Cancelled if there is no such card.
 * No target to pick.
 */
public final class MoveBetweenPilesEffect implements Effect {
    private final CardType source;
    /** Null = any card. */
    private final @Nullable CardFilter filter;
    private final String what;
    private final SideTarget fromSide;
    private final Pile fromPile;
    private final SideTarget toSide;
    private final Destination destination;

    /**
     * @param what the cards for the description, e.g. "every Brown Mushroom"
     */
    public MoveBetweenPilesEffect(CardType source, @Nullable CardFilter filter, String what,
                                  SideTarget fromSide, Pile fromPile, SideTarget toSide, Destination destination) {
        this.source = source;
        this.filter = filter;
        this.what = what;
        this.fromSide = fromSide;
        this.fromPile = fromPile;
        this.toSide = toSide;
        this.destination = destination;
    }

    @Override
    public Component description() {
        String from = pileText(fromSide, fromPile);
        Pile toPile = destination.pile();
        String text = toPile == null
                ? "banish " + what + " from " + from
                : "move " + what + " from " + from + " to " + pileText(toSide, toPile);
        return Component.text(source.displayName() + ": " + text);
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return new PileCount(filter, fromPile, fromSide).count(state, owner) > 0;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        engine.moveBetweenPiles(side(fromSide, owner), fromPile, side(toSide, owner), filter, destination);
    }

    private static int side(SideTarget target, int owner) {
        return target == SideTarget.Owner ? owner : GameState.other(owner);
    }


    /** E.g. "your deck", "your opponent's graveyard". */
    private static String pileText(SideTarget side, Pile pile) {
        return (side == SideTarget.Opponent ? "your opponent's" : "your")
                + (pile == Pile.DECK ? " deck" : " graveyard");
    }
}
