package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.cardsRepository.Tag;
import net.kyori.adventure.text.Component;

/**
 * Banishes a random card with the tag from the owner's graveyard to play a monster on an empty tile of their side
 * (the owner picks the tile). Cancelled, without banishing, if there is no such card or no empty tile.
 */
public final class BanishToSummonEffect implements Effect {
    private final CardType source;
    private final Tag tag;
    private final CardType summoned;

    public BanishToSummonEffect(CardType source, Tag tag, CardType summoned) {
        if (!summoned.isMonster()) {
            throw new IllegalArgumentException("Only monsters can be played: " + summoned);
        }
        this.source = source;
        this.tag = tag;
        this.summoned = summoned;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": banish a random " + tag.displayName()
                + " from your graveyard to play a " + summoned.displayName() + " on an empty tile of your side");
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return state.side(owner).graveyard().stream().anyMatch(card -> card.hasTag(tag))
                && Effect.super.hasValidTargets(state, owner);
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return state.unit(target) == null && GameState.sideOf(target) == owner;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        if (engine.banishRandom(owner, card -> card.hasTag(tag)) != null) {
            engine.play(owner, summoned, target);
        }
    }
}
