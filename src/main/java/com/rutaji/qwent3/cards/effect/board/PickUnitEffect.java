package com.rutaji.qwent3.cards.effect.board;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.effect.ResultEffect;
import com.rutaji.qwent3.cards.effect.effectTargets.Targets;
import com.rutaji.qwent3.cards.effect.board.factory.Pick;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * The owner picks a monster the targets allow; {@code action} (if any) is done to it, e.g. destroy it. The result is
 * the picked unit, so the effects after it can use it, e.g. play a copy of its card (see {@link #thenWith}).
 * Factories for the common ones in {@link Pick}.
 */
public final class PickUnitEffect implements ResultEffect<Unit> {
    private final CardType source;
    private final Targets targets;
    /** Null = only pick it. */
    private final @Nullable TileAction action;
    private final String text;
    /** Monsters that can't be picked, the picked one is added; null = no such rule. */
    private final @Nullable Set<Unit> alreadyHit;

    /** @param text the whole effect for the description, e.g. "destroy one of your monsters to play a copy of it" */
    public PickUnitEffect(CardType source, Targets targets, @Nullable TileAction action, String text) {
        this(source, targets, action, text, null);
    }

    /**
     * With {@code alreadyHit}, only a monster not in it can be picked, and the picked one is added to it. Share one
     * set between several effects to make them pick different monsters.
     */
    public PickUnitEffect(CardType source, Targets targets, @Nullable TileAction action, String text,
                          @Nullable Set<Unit> alreadyHit) {
        this.source = source;
        this.targets = targets;
        this.action = action;
        this.text = text;
        this.alreadyHit = alreadyHit;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": " + text
                + (alreadyHit != null && !alreadyHit.isEmpty() ? " (a different one)" : ""));
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int tile) {
        Unit unit = state.unit(tile);
        return unit != null && targets.allows(unit, owner) && (alreadyHit == null || !alreadyHit.contains(unit))
                && (action == null || action.accepts(state, owner, tile));
    }

    @Override
    public @Nullable Unit resolveWithResult(GameEngine engine, int owner, int tile) {
        Unit unit = engine.state().unit(tile);
        if (unit == null) {
            return null;
        }
        if (alreadyHit != null) {
            alreadyHit.add(unit);
        }
        if (action != null) {
            action.apply(engine, owner, tile);
        }
        return unit;
    }
}
