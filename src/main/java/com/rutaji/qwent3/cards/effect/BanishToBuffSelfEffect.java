package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Tag;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;

/**
 * Banishes a random card with the tag from the owner's graveyard to buff a unit. Cancelled, without banishing, if
 * there is no such card or the unit is gone. No target to pick.
 */
public final class BanishToBuffSelfEffect implements Effect {
    private final CardType source;
    private final Tag tag;
    private final Unit unit;
    private final int amount;

    public BanishToBuffSelfEffect(CardType source, Tag tag, Unit unit, int amount) {
        this.source = source;
        this.tag = tag;
        this.unit = unit;
        this.amount = amount;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": banish a random " + tag.displayName()
                + " from your graveyard for +" + amount + " power");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return state.tileOf(unit) >= 0 && state.side(owner).graveyard().stream().anyMatch(card -> card.hasTag(tag));
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        if (state.banishRandom(owner, card -> card.hasTag(tag)) != null) {
            state.buff(state.tileOf(unit), amount);
        }
    }
}
