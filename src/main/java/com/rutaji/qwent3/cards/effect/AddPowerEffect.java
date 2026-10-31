package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.effect.effectTargets.Targets;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Changes the power of a monster the owner picks. A positive amount is a buff; a negative one is damage: the monster
 * is destroyed when its power drops below 1, otherwise its "damaged and survives" effects trigger.
 */
public final class AddPowerEffect implements Effect {
    private final CardType source;
    private final int amount;
    private final Targets targets;
    private final boolean differentTarget;
    private final Set<Unit> alreadyHit;

    /** Any monster (either side). */
    public AddPowerEffect(CardType source, int amount) {
        this(source, amount, Targets.ANY);
    }

    public AddPowerEffect(CardType source, int amount, Targets targets) {
        this(source, amount, targets, false, new HashSet<>());
    }

    /**
     * With {@code differentTarget}, only a monster not in {@code alreadyHit} can be picked, and the target is added
     * to it. Share one set between several effects to make them hit different monsters.
     */
    public AddPowerEffect(CardType source, int amount, Targets targets, boolean differentTarget, Set<Unit> alreadyHit) {
        this.source = source;
        this.amount = amount;
        this.targets = targets;
        this.differentTarget = differentTarget;
        this.alreadyHit = alreadyHit;
    }

    @Override
    public Component description() {
        String text = amount < 0
                ? ": deal " + -amount + " damage to " + targets.text()
                : ": give " + targets.text() + " +" + amount + " power";
        return Component.text(source.displayName() + text
                + (differentTarget && !alreadyHit.isEmpty() ? " (a different one)" : ""));
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int tile) {
        Unit unit = state.unit(tile);
        return unit != null && !(differentTarget && alreadyHit.contains(unit)) && targets.allows(unit, owner);
    }

    @Override
    public void resolve(GameEngine engine, int owner, int tile) {
        Unit unit = engine.state().unit(tile);
        if (differentTarget && unit != null) {
            alreadyHit.add(unit);
        }
        if (amount < 0) {
            engine.damage(tile, -amount);
        } else {
            engine.buff(tile, amount);
        }
    }
}
