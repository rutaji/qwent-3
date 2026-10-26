package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;

/**
 * Destroys the monsters directly left and right of the tile a unit is (or was) on, same row. Works after the unit
 * died (for on-death effects). No target to pick.
 */
public final class AdjacentDestroyEffect implements Effect {
    private final CardType source;
    private final Unit center;

    public AdjacentDestroyEffect(CardType source, Unit center) {
        this.source = source;
        this.center = center;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": destroy the monsters left and right");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        for (int neighbour : GameState.neighbours(center.tile())) {
            if (state.unit(neighbour) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        for (int tile : GameState.neighbours(center.tile())) {
            engine.destroy(tile);
        }
    }
}
