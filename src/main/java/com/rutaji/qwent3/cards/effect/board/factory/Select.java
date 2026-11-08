package com.rutaji.qwent3.cards.effect.board.factory;

import com.rutaji.qwent3.cards.effect.board.AreaEffect;
import com.rutaji.qwent3.cards.effect.board.TileSelector;
import com.rutaji.qwent3.cards.effect.effectTargets.Targets;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/** {@link TileSelector}s for {@link AreaEffect}. */
public final class Select {
    private Select() {
    }

    private static TileSelector of(String text, BiFunction<GameState, Integer, List<Integer>> select) {
        return new TileSelector() {
            @Override
            public List<Integer> select(GameState state, int owner) {
                return select.apply(state, owner);
            }

            @Override
            public String text() {
                return text;
            }
        };
    }

    /** The monsters with the lowest power on the whole board (both sides); all of them if tied. */
    public static TileSelector lowestPower() {
        return of("with the lowest power", (state, owner) -> extremePower(state, false));
    }

    /** The monsters with the highest power on the whole board (both sides); all of them if tied. */
    public static TileSelector highestPower() {
        return of("with the highest power", (state, owner) -> extremePower(state, true));
    }

    /** The monsters the targets allow, on the whole board. */
    public static TileSelector all(Targets targets) {
        String text = switch (targets) {
            case ANY -> "on the board";
            case OWN -> "of yours";
            case ENEMY -> "of your opponent";
        };
        return of(text, (state, owner) -> {
            List<Integer> tiles = new ArrayList<>();
            for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
                Unit unit = state.unit(tile);
                if (unit != null && targets.allows(unit, owner)) {
                    tiles.add(tile);
                }
            }
            return tiles;
        });
    }

    /** Every tile of the row the unit is in (or was in when it died), the unit's own included. */
    public static TileSelector row(Unit unit) {
        return of("in its row", (state, owner) -> rowFrom(unit.tile() - unit.tile() % GameState.COLUMNS));
    }

    /** Every tile of the owner's back row. */
    public static TileSelector backRow() {
        return of("in your back row",
                (state, owner) -> rowFrom(GameState.tile(owner, GameState.ROWS_PER_SIDE - 1, 0)));
    }

    /** Every tile of the column the unit is in (or was in), both sides; its own only with {@code includeSelf}. */
    public static TileSelector column(Unit unit, boolean includeSelf) {
        return of(includeSelf ? "in its column" : "in its column (except itself)", (state, owner) -> {
            List<Integer> tiles = new ArrayList<>();
            for (int tile = unit.tile() % GameState.COLUMNS; tile < GameState.BOARD_SIZE; tile += GameState.COLUMNS) {
                if (includeSelf || tile != unit.tile()) {
                    tiles.add(tile);
                }
            }
            return tiles;
        });
    }

    /** The unit's own tile; none if it is no longer on the board. */
    public static TileSelector self(Unit unit) {
        return of("itself", (state, owner) -> {
            int tile = state.tileOf(unit);
            return tile < 0 ? List.of() : List.of(tile);
        });
    }

    /** The tiles directly left and right of the unit (same row); none if it is no longer on the board. */
    public static TileSelector neighbours(Unit unit) {
        return of("left and right of it", (state, owner) -> {
            int tile = state.tileOf(unit);
            return tile < 0 ? List.of() : toList(GameState.neighbours(tile));
        });
    }

    /** The tiles directly left and right of where the unit is or was (same row); works after it died. */
    public static TileSelector neighboursOfLastTile(Unit unit) {
        return of("left and right of it", (state, owner) -> toList(GameState.neighbours(unit.tile())));
    }

    private static List<Integer> extremePower(GameState state, boolean highest) {
        int extreme = highest ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            Unit unit = state.unit(tile);
            if (unit != null) {
                extreme = highest ? Math.max(extreme, unit.power()) : Math.min(extreme, unit.power());
            }
        }
        List<Integer> tiles = new ArrayList<>();
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            Unit unit = state.unit(tile);
            if (unit != null && unit.power() == extreme) {
                tiles.add(tile);
            }
        }
        return tiles;
    }

    private static List<Integer> rowFrom(int first) {
        List<Integer> tiles = new ArrayList<>();
        for (int column = 0; column < GameState.COLUMNS; column++) {
            tiles.add(first + column);
        }
        return tiles;
    }

    private static List<Integer> toList(int[] tiles) {
        List<Integer> list = new ArrayList<>();
        for (int tile : tiles) {
            list.add(tile);
        }
        return list;
    }
}
