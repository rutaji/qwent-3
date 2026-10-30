package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * "Do {@code first}, then something depending on what it did", e.g. draw a card of the other kind than the discarded
 * one. The other effects are built from {@code first}'s result and follow right away (in order); none if the result
 * is null. Cancelled if {@code first} has no valid target. {@code first} must resolve in one go (not re-queue itself).
 */
public final class SwitchEffect<T> implements Effect {
    private final ResultEffect<T> first;
    private final Function<T, List<Effect>> next;

    public SwitchEffect(ResultEffect<T> first, Function<T, List<Effect>> next) {
        this.first = first;
        this.next = next;
    }

    @Override
    public Component description() {
        return first.description();
    }

    @Override
    public boolean needsTarget() {
        return first.needsTarget();
    }

    @Override
    public TargetKind targetKind() {
        return first.targetKind();
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return first.hasValidTargets(state, owner);
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return first.isValidTarget(state, owner, target);
    }

    @Override
    public @Nullable String logDetail(GameState state, int owner) {
        return first.logDetail(state, owner);
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        T result = first.resolveWithResult(engine, owner, target);
        if (result == null) {
            return;
        }
        // Front of the queue, keeping their order.
        for (Effect effect : next.apply(result).reversed()) {
            engine.queueNext(owner, effect);
        }
    }
}
