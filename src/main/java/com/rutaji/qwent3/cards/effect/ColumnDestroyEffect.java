package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Destroys every other monster in a unit's column, on both sides (all chosen first, then destroyed in board order;
 * on-death effects follow). Cancelled if there is none. No target to pick.
 */
public final class ColumnDestroyEffect implements Effect {
    private final CardType source;
    private final Unit center;

    public ColumnDestroyEffect(CardType source, Unit center) {
        this.source = source;
        this.center = center;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": destroy every other monster in its column");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return !tiles(state).isEmpty();
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        tiles(state).forEach(state::destroy);
    }

    private List<Integer> tiles(GameState state) {
        List<Integer> tiles = new ArrayList<>();
        int column = center.tile() % GameState.COLUMNS;
        for (int tile = column; tile < GameState.BOARD_SIZE; tile += GameState.COLUMNS) {
            Unit unit = state.unit(tile);
            if (unit != null && unit != center) {
                tiles.add(tile);
            }
        }
        return tiles;
    }
}
