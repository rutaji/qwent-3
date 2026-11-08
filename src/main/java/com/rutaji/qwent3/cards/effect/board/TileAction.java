package com.rutaji.qwent3.cards.effect.board;

import com.rutaji.qwent3.cards.effect.board.factory.Act;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;

/** What an {@link AreaEffect} does to each tile its {@link TileSelector} found. Factories in {@link Act}. */
public interface TileAction {
    /** Whether it can act on this tile now, e.g. there is a monster on it / the card can be played there. */
    boolean accepts(GameState state, int owner, int tile);

    void apply(GameEngine engine, int owner, int tile);

    /** The description of the whole effect, given where the tiles are ({@link TileSelector#text()}). */
    String text(String where);
}
