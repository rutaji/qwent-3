package com.rutaji.qwent3.cards.effect;

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
 * Moves every matching card from one deck or graveyard to another, of either side (seen from the owner), e.g. "discard
 * every Brown Mushroom from your deck". Each goes on top or to a random position. Cancelled if there is no such card.
 * No target to pick.
 */
public final class MoveBetweenPacksEffect implements Effect {
    private final CardType source;
    /** Null = any card. */
    private final @Nullable CardFilter filter;
    private final String what;
    private final SideTarget fromSide;
    private final Pack fromPack;
    private final SideTarget toSide;
    private final Pack toPack;
    private final boolean onTop;

    /**
     * @param what the cards for the description, e.g. "every Brown Mushroom"
     * @param onTop true = on top (of a graveyard: as if they entered it last), false = shuffled in at random positions
     */
    public MoveBetweenPacksEffect(CardType source, @Nullable CardFilter filter, String what,
                                  SideTarget fromSide, Pack fromPack, SideTarget toSide, Pack toPack, boolean onTop) {
        this.source = source;
        this.filter = filter;
        this.what = what;
        this.fromSide = fromSide;
        this.fromPack = fromPack;
        this.toSide = toSide;
        this.toPack = toPack;
        this.onTop = onTop;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": move " + what + " from " + pileText(fromSide, fromPack)
                + " to " + pileText(toSide, toPack));
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return new PileCount(filter, fromPack, fromSide).count(state, owner) > 0;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        engine.moveBetweenPiles(side(fromSide, owner), pile(fromPack), side(toSide, owner), pile(toPack), filter,
                onTop);
    }

    private static int side(SideTarget target, int owner) {
        return target == SideTarget.Owner ? owner : GameState.other(owner);
    }

    private static Pile pile(Pack pack) {
        return pack == Pack.Deck ? Pile.DECK : Pile.GRAVEYARD;
    }

    /** E.g. "your deck", "your opponent's graveyard". */
    private static String pileText(SideTarget side, Pack pack) {
        return (side == SideTarget.Opponent ? "your opponent's" : "your")
                + (pack == Pack.Deck ? " deck" : " graveyard");
    }
}
