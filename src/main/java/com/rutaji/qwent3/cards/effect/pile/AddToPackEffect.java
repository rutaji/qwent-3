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
 * Puts copies of a card into a deck or graveyard ({@code pack}) of a side ({@code side}, seen from the owner): on top,
 * or shuffled in at random positions. No target to pick.
 */
public final class AddToPackEffect implements Effect {
    private final CardType source;
    private final CardType card;
    private final int copies;
    private final SideTarget side;
    private final Pack pack;
    private final boolean onTop;

    /**
     * @param onTop true = on top (of a graveyard: as if it entered it last), false = shuffled in at random positions
     */
    public AddToPackEffect(CardType source, CardType card, int copies, SideTarget side, Pack pack, boolean onTop) {
        this.source = source;
        this.card = card;
        this.copies = copies;
        this.side = side;
        this.pack = pack;
        this.onTop = onTop;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": put " + copies + " " + card.displayName() + " into "
                + (side == SideTarget.Opponent ? "your opponent's" : "your")
                + (pack == Pack.Deck ? " deck" : " graveyard"));
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
        int s = side == SideTarget.Owner ? owner : GameState.other(owner);
        Pile pile = pack == Pack.Deck ? Pile.DECK : Pile.GRAVEYARD;
        for (int i = 0; i < copies; i++) {
            engine.addToPile(s, pile, card, onTop);
        }
    }
}
