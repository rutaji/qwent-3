package com.rutaji.changingitems.cards.effect;

import com.rutaji.changingitems.cards.CardType;
import com.rutaji.changingitems.cards.GameState;
import com.rutaji.changingitems.cards.Unit;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Set;

/** Reduces a monster's power; it is destroyed when its power drops below 1. */
public final class DamageEffect implements Effect {
    private final CardType source;
    private final int amount;
    private final Set<Unit> alreadyHit;

    /** Damage any monster. */
    public DamageEffect(CardType source, int amount) {
        this(source, amount, new HashSet<>());
    }

    /**
     * Damage a monster not in {@code alreadyHit}; the target is added to it. Share one set between several
     * effects to make them hit different monsters.
     */
    public DamageEffect(CardType source, int amount, Set<Unit> alreadyHit) {
        this.source = source;
        this.amount = amount;
        this.alreadyHit = alreadyHit;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": deal " + amount + " damage to "
                + (alreadyHit.isEmpty() ? "a monster" : "a different monster"));
    }

    @Override
    public boolean isValidTarget(GameState state, int tile) {
        Unit unit = state.unit(tile);
        return unit != null && !alreadyHit.contains(unit);
    }

    @Override
    public void resolve(GameState state, int tile) {
        Unit unit = state.unit(tile);
        if (unit != null) {
            alreadyHit.add(unit);
        }
        state.damage(tile, amount);
    }
}
