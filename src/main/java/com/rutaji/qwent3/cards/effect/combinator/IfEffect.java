package com.rutaji.qwent3.cards.effect.combinator;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Condition: if it holds (for the game state and the effect's owner) when it is this effect's turn, the other effects
 * follow right away (in order); otherwise this is cancelled and they don't happen. No target to pick.
 */
public final class IfEffect implements Effect {
    private final CardType source;
    private final BiPredicate<GameState, Integer> condition;
    private final String text;
    private final List<Effect> then;

    /** @param text the condition for the description, e.g. "played on your opponent's side" */
    public IfEffect(CardType source, BiPredicate<GameState, Integer> condition, String text, Effect... then) {
        this.source = source;
        this.condition = condition;
        this.text = text;
        this.then = List.of(then);
    }

    /** If the unit is on the opponent's side of the effect's owner (e.g. a treacherous monster played there). */
    public static IfEffect onEnemySide(CardType source, Unit unit, Effect... then) {
        return new IfEffect(source, (state, owner) -> unit.owner() != owner, "played on your opponent's side", then);
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": " + text);
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return condition.test(state, owner);
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
