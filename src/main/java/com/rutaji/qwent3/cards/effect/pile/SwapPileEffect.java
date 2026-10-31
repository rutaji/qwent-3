package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.effect.effectTargets.SideTarget;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

/**
 * Swaps the contents of two piles, each a deck or graveyard of either side (seen from the owner), without shuffling:
 * the cards keep their order. No target to pick.
 */
public final class SwapPileEffect implements Effect {
    private final CardType source;
    private final SideTarget sideA;
    private final Pile pileA;
    private final SideTarget sideB;
    private final Pile pileB;

    public SwapPileEffect(CardType source, SideTarget sideA, Pile pileA, SideTarget sideB, Pile pileB) {
        this.source = source;
        this.sideA = sideA;
        this.pileA = pileA;
        this.sideB = sideB;
        this.pileB = pileB;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": swap " + pileText(sideA, pileA) + " and "
                + pileText(sideB, pileB));
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
        engine.swapPiles(side(sideA, owner), pileA, side(sideB, owner), pileB);
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
