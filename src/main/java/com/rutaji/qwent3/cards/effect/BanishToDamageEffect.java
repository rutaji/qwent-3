package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.cardsRepository.Tag;
import net.kyori.adventure.text.Component;

/**
 * Banishes a random card with the tag from the owner's graveyard to deal damage to a monster the owner picks (either
 * side). Cancelled, without banishing, if there is no such card or no monster.
 */
public final class BanishToDamageEffect implements Effect {
    private final CardType source;
    private final Tag tag;
    private final int amount;

    public BanishToDamageEffect(CardType source, Tag tag, int amount) {
        this.source = source;
        this.tag = tag;
        this.amount = amount;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": banish a random " + tag.displayName()
                + " from your graveyard to deal " + amount + " damage to a monster");
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return state.side(owner).graveyard().stream().anyMatch(card -> card.hasTag(tag))
                && Effect.super.hasValidTargets(state, owner);
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return state.unit(target) != null;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        if (engine.banishRandom(owner, card -> card.hasTag(tag)) != null) {
            engine.damage(target, amount);
        }
    }
}
