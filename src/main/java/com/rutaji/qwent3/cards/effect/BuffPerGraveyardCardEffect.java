package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;

import java.util.Collections;

/** Buffs a unit by {@code perCard} for every copy of a card in the owner's graveyard. No target to pick. */
public final class BuffPerGraveyardCardEffect implements Effect {
    private final CardType source;
    private final Unit unit;
    private final CardType counted;
    private final int perCard;

    public BuffPerGraveyardCardEffect(CardType source, Unit unit, CardType counted, int perCard) {
        this.source = source;
        this.unit = unit;
        this.counted = counted;
        this.perCard = perCard;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": +" + perCard + " power for every " + counted.displayName()
                + " in your graveyard");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    /** Cancelled if the unit is gone or there is no such card in the graveyard. */
    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return state.tileOf(unit) >= 0 && count(state, owner) > 0;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        state.buff(state.tileOf(unit), perCard * count(state, owner));
    }

    private int count(GameState state, int owner) {
        return Collections.frequency(state.side(owner).graveyard(), counted);
    }
}
