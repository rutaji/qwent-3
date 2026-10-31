package com.rutaji.qwent3.cards.effect.board;

import com.rutaji.qwent3.cards.effect.Count;
import com.rutaji.qwent3.cards.effect.effectTargets.Targets;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.CardFilter;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;

/**
 * Counts the monsters on the board that the targets allow (any / the owner's / the opponent's) and the filter accepts.
 * The filter checks the unit as it is now (e.g. its current power, not the card's base power).
 */
public final class BoardCount implements Count {
    private final CardFilter filter;
    private final Targets targets;

    public BoardCount(CardFilter filter, Targets targets) {
        this.filter = filter;
        this.targets = targets;
    }

    @Override
    public int count(GameState state, int owner) {
        int count = 0;
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            Unit unit = state.unit(tile);
            if (unit != null && targets.allows(unit, owner) && filter.check(unit)) {
                count++;
            }
        }
        return count;
    }
}
