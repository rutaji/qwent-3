package com.rutaji.changingitems.cards.effect;

import com.rutaji.changingitems.cards.CardType;
import com.rutaji.changingitems.cards.GameState;
import com.rutaji.changingitems.cards.Unit;
import net.kyori.adventure.text.Component;

/**
 * Destroys the monster with the lowest (or highest) power. If several are tied, the player picks one of them;
 * otherwise it happens without asking. Destroying is not damage: on-hurt effects don't trigger, on-death ones do.
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
        // Only shown when the player has to choose, i.e. when several monsters are tied.
        return Component.text(source.displayName() + ": destroy one of the tied " + (highest ? "highest" : "lowest")
                + " power monsters");
    }

    @Override
    public boolean autoPickSingleTarget() {
        return true;
    }

    @Override
    public boolean isValidTarget(GameState state, int tile) {
        Unit unit = state.unit(tile);
        return unit != null && unit.power() == extremePower(state);
    }

    @Override
    public void resolve(GameState state, int tile) {
        state.destroy(tile);
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
