package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Set;

/** Reduces the power of a monster the owner picks; it is destroyed when its power drops below 1. */
public final class DamageEffect implements Effect {
    private final CardType source;
    private final int amount;
    private final Set<Unit> alreadyHit;
    private final Targets targets;

    /** Damage any monster (either side). */
    public DamageEffect(CardType source, int amount) {
        this(source, amount, new HashSet<>());
    }

    /**
     * Damage a monster not in {@code alreadyHit}; the target is added to it. Share one set between several
     * effects to make them hit different monsters.
     */
    public DamageEffect(CardType source, int amount, Set<Unit> alreadyHit) {
        this(source, amount, alreadyHit, Targets.ANY);
    }

    public DamageEffect(CardType source, int amount, Set<Unit> alreadyHit, Targets targets) {
        this.source = source;
        this.amount = amount;
        this.alreadyHit = alreadyHit;
        this.targets = targets;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": deal " + amount + " damage to " + targets.text()
                + (alreadyHit.isEmpty() ? "" : " (a different one)"));
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int tile) {
        Unit unit = state.unit(tile);
        return unit != null && !alreadyHit.contains(unit) && targets.allows(unit, owner);
    }

    @Override
    public void resolve(GameState state, int owner, int tile) {
        Unit unit = state.unit(tile);
        if (unit != null) {
            alreadyHit.add(unit);
        }
        state.damage(tile, amount);
    }
}
