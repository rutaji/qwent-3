package com.rutaji.qwent3.cards.effect.board;

import com.rutaji.qwent3.cards.effect.board.factory.Select;
import com.rutaji.qwent3.cards.gameLogic.GameState;

import java.util.List;

/**
 * Finds the tiles an {@link AreaEffect} applies to, without changing the game; may include empty tiles (e.g. a whole
 * row). Which of them the effect acts on is up to its {@link TileAction}. Factories in {@link Select}.
 */
public interface TileSelector {
    /** The tiles in board order. */
    List<Integer> select(GameState state, int owner);

    /** Where the tiles are, for descriptions, e.g. "in its row", "left and right of it". */
    String text();
}
