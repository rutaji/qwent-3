package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Plays a monster on every empty tile of a row (left to right): the owner's back row, or the row a unit is / was in.
 * No target to pick.
 */
public final class FillRowEffect implements Effect {
    private final CardType source;
    private final CardType card;
    /** The unit whose row is filled; null = the owner's back row. */
    private final @Nullable Unit unit;

    private FillRowEffect(CardType source, CardType card, @Nullable Unit unit) {
        if (!card.isMonster()) {
            throw new IllegalArgumentException("Only monsters can be played: " + card);
        }
        this.source = source;
        this.card = card;
        this.unit = unit;
    }

    /** The owner's back row. */
    public static FillRowEffect backRow(CardType source, CardType card) {
        return new FillRowEffect(source, card, null);
    }

    /** The row the unit is in (or was in when it died); it must belong to the effect's owner. */
    public static FillRowEffect rowOf(CardType source, CardType card, Unit unit) {
        return new FillRowEffect(source, card, unit);
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": fill " + (unit == null ? "your back row" : "the row")
                + " with " + card.displayName());
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        for (int tile : rowTiles(owner)) {
            if (state.canPlay(owner, card, tile)) {
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
    public void resolve(GameState state, int owner, int target) {
        for (int tile : rowTiles(owner)) {
            if (state.canPlay(owner, card, tile)) {
                state.play(owner, card, tile);
            }
        }
    }

    private int[] rowTiles(int owner) {
        int first = unit == null
                ? GameState.tile(owner, GameState.ROWS_PER_SIDE - 1, 0)
                : unit.tile() - unit.tile() % GameState.COLUMNS;
        int[] tiles = new int[GameState.COLUMNS];
        for (int column = 0; column < GameState.COLUMNS; column++) {
            tiles[column] = first + column;
        }
        return tiles;
    }
}
