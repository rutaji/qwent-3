package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;

/** Damages the monsters directly left and right of a unit (same row). No target to pick. */
public final class AdjacentDamageEffect implements Effect {
    private final CardType source;
    private final int amount;
    private final Unit center;

    public AdjacentDamageEffect(CardType source, int amount, Unit center) {
        this.source = source;
        this.amount = amount;
        this.center = center;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": deal " + amount + " damage to the monsters left and right");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    /** Cancelled if the unit is no longer on the board or has no neighbours. */
    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        int tile = state.tileOf(center);
        if (tile < 0) {
            return false;
        }
        for (int neighbour : GameState.neighbours(tile)) {
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
        // Both neighbours are found before either is damaged.
        int[] tiles = GameState.neighbours(engine.state().tileOf(center));
        for (int tile : tiles) {
            engine.damage(tile, amount);
        }
    }
}
