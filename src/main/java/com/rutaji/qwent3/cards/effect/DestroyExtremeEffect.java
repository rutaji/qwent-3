package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Destroys the monster with the lowest (or highest) power on the whole board (both sides); if several are tied,
 * all of them. No target to pick.
 * Destroying is not damage: on-hurt effects don't trigger, on-death ones do (in board order).
 */
public final class DestroyExtremeEffect implements Effect {
    private final CardType source;
    private final boolean highest;

    private DestroyExtremeEffect(CardType source, boolean highest) {
        this.source = source;
        this.highest = highest;
    }

    public static DestroyExtremeEffect lowest(CardType source) {
        return new DestroyExtremeEffect(source, false);
    }

    public static DestroyExtremeEffect highest(CardType source) {
        return new DestroyExtremeEffect(source, true);
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": destroy the " + (highest ? "highest" : "lowest")
                + " power monsters");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            if (state.unit(tile) != null) {
                return true;
            }
        }
        return false;
    }

    /** The monsters that will be destroyed. */
    @Override
    public boolean isValidTarget(GameState state, int owner, int tile) {
        Unit unit = state.unit(tile);
        return unit != null && unit.power() == extremePower(state);
    }

    @Override
    public void resolve(GameState state, int owner, int tile) {
        // Pick all first: destroying one must not change which others count as tied.
        List<Integer> tiles = new ArrayList<>();
        for (int t = 0; t < GameState.BOARD_SIZE; t++) {
            if (isValidTarget(state, owner, t)) {
                tiles.add(t);
            }
        }
        tiles.forEach(state::destroy);
    }

    private int extremePower(GameState state) {
        int extreme = highest ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            Unit unit = state.unit(tile);
            if (unit != null) {
                extreme = highest ? Math.max(extreme, unit.power()) : Math.min(extreme, unit.power());
            }
        }
        return extreme;
    }
}
