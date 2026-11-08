package com.rutaji.qwent3.cards.effect.combinator;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * "Do {@code first} to ...": the owner resolves {@code first} (its target, its text), then the other effects follow
 * (in order). Cancelled, so {@code first} doesn't happen either, if {@code first} or the first of the other effects
 * has no valid target.
 */
public final class ToEffect implements Effect {
    private final Effect first;
    private final List<Effect> then;

    public ToEffect(Effect first, Effect... then) {
        this.first = first;
        this.then = List.of(then);
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
        return first.hasValidTargets(state, owner) && (then.isEmpty() || then.getFirst().hasValidTargets(state, owner));
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
        // Front of the queue, keeping their order; queued before first resolves, so whatever first puts at the
        // front (e.g. the next pick of a multi-card discard) still comes before them.
        for (Effect effect : then.reversed()) {
            engine.queueNext(owner, effect);
        }
        first.resolve(engine, owner, target);
    }
}
