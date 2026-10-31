package com.rutaji.qwent3.cards.gameLogic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.rutaji.qwent3.TestGames.game;
import static com.rutaji.qwent3.TestGames.play;
import static com.rutaji.qwent3.TestGames.t;
import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tile numbering, placement rules, ownership and moving units. */
class BoardTest {

    @Test
    void tileNumbering() {
        assertEquals(20, GameState.BOARD_SIZE);
        assertEquals(0, GameState.tile(0, 0, 0));
        assertEquals(9, GameState.tile(0, 1, 4));
        assertEquals(10, GameState.tile(1, 0, 0));
        assertEquals(19, GameState.tile(1, 1, 4));
        assertEquals(0, GameState.sideOf(9));
        assertEquals(1, GameState.sideOf(10));
        assertEquals(1, GameState.other(0));
    }

    @Test
    void neighboursAreInTheSameRow() {
        assertArrayEquals(new int[] {1}, GameState.neighbours(0));
        assertArrayEquals(new int[] {1, 3}, GameState.neighbours(2));
        assertArrayEquals(new int[] {3}, GameState.neighbours(4));
        assertArrayEquals(new int[] {16}, GameState.neighbours(15), "column 0 has only a right neighbour");
        assertArrayEquals(new int[] {16, 18}, GameState.neighbours(17));
    }

    @Test
    void treacherousMonsterMayGoOnEitherSideNormalOnlyOwn() {
        GameEngine engine = game(List.of(BEETROOT), List.of(BEAR), 0);
        assertTrue(engine.state().canPlay(0, BEETROOT, t(1, 0, 0)));
        assertTrue(engine.state().canPlay(0, BEETROOT, t(0, 0, 0)));
        assertFalse(engine.state().canPlay(0, BEAR, t(1, 0, 0)));
        assertTrue(engine.state().canPlay(0, BEAR, t(0, 1, 4)));
        assertFalse(engine.state().canPlay(0, ARROW, t(0, 0, 0)), "spells are not placed");
    }

    @Test
    void treacherousMonsterOnEnemySideIsTheirs() {
        GameEngine engine = game(List.of(BEETROOT), List.of(BEAR), 0);
        play(engine, 0, BEETROOT, t(1, 0, 3));
        assertEquals(1, engine.state().unit(t(1, 0, 3)).owner());
        assertEquals(1, engine.state().power(1));
        assertEquals(0, engine.state().power(0));
    }

    @Test
    void shiftPushesChainAndChangesOwnership() {
        GameEngine engine = game(List.of(BEAR, BASIC_ZOMBIE), List.of(EVIL_GOBLIN), 0);
        play(engine, 0, BEAR, t(0, 1, 0));
        play(engine, 1, EVIL_GOBLIN, t(1, 0, 0));
        play(engine, 0, BASIC_ZOMBIE, t(0, 0, 0));
        int towardsSide1 = GameState.directionTowards(1);
        assertTrue(engine.state().canShift(t(0, 1, 0), towardsSide1, 0));
        engine.shift(t(0, 1, 0), towardsSide1, 0);
        assertEquals(BEAR, engine.state().unit(t(0, 0, 0)).type());
        assertEquals(BASIC_ZOMBIE, engine.state().unit(t(1, 0, 0)).type());
        assertEquals(1, engine.state().unit(t(1, 0, 0)).owner(), "crossed to side 1");
        assertEquals(EVIL_GOBLIN, engine.state().unit(t(1, 1, 0)).type());
        assertNull(engine.state().unit(t(0, 1, 0)));
        assertEquals(t(1, 0, 0), engine.state().unit(t(1, 0, 0)).tile());
    }

    @Test
    void cannotShiftOffTheBoard() {
        GameEngine engine = game(List.of(BEAR), List.of(BEAR), 1);
        play(engine, 1, BEAR, t(1, 1, 2));
        assertFalse(engine.state().canShift(t(1, 1, 2), GameState.directionTowards(1), 0));
        assertTrue(engine.state().canShift(t(1, 1, 2), GameState.directionTowards(0), 0));
        assertFalse(engine.state().canShift(t(0, 0, 0), GameState.directionTowards(1), 0), "empty tile");
    }

    @Test
    void tileOfFindsUnitsAndForgetsDeadOnes() {
        GameEngine engine = game(List.of(BEAR), List.of(BEAR), 0);
        play(engine, 0, BEAR, t(0, 1, 3));
        Unit bear = engine.state().unit(t(0, 1, 3));
        assertEquals(t(0, 1, 3), engine.state().tileOf(bear));
        engine.destroy(t(0, 1, 3));
        assertEquals(-1, engine.state().tileOf(bear));
        assertEquals(t(0, 1, 3), bear.tile(), "a dead unit still knows where it died");
    }
}
