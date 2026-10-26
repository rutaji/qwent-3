package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Condition: if the unit is on the opponent's side of the effect's owner (e.g. a treacherous monster played there),
 * the other effects follow right away (in order); otherwise this is cancelled and they don't happen. No target to pick.
 */
public final class IfOnEnemySideEffect implements Effect {
    private final CardType source;
    private final Unit unit;
    private final List<Effect> then;

    public IfOnEnemySideEffect(CardType source, Unit unit, Effect... then) {
        this.source = source;
        this.unit = unit;
        this.then = List.of(then);
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": played on your opponent's side");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return unit.owner() != owner;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        // Front of the queue, keeping their order.
        for (Effect effect : then.reversed()) {
            engine.queueNext(owner, effect);
        }
    }
}
