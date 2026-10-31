package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.effect.effectTargets.Pack;
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
    private final Pack packA;
    private final SideTarget sideB;
    private final Pack packB;

    public SwapPileEffect(CardType source, SideTarget sideA, Pack packA, SideTarget sideB, Pack packB) {
        this.source = source;
        this.sideA = sideA;
        this.packA = packA;
        this.sideB = sideB;
        this.packB = packB;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": swap " + pileText(sideA, packA) + " and "
                + pileText(sideB, packB));
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
        engine.swapPiles(side(sideA, owner), pile(packA), side(sideB, owner), pile(packB));
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
