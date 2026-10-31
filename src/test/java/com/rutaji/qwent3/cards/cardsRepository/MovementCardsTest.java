package com.rutaji.qwent3.cards.cardsRepository;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.rutaji.qwent3.TestGames.game;
import static com.rutaji.qwent3.TestGames.play;
import static com.rutaji.qwent3.TestGames.t;
import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Piston and Slime Piston. */
class MovementCardsTest {

    @Test
    void pistonPushesOwnMonsterChainTowardsOpponent() {
        GameEngine engine = game(List.of(PISTON), List.of(BEAR), 0);
        engine.play(0, BEAR, t(0, 1, 0));
        engine.play(0, BASIC_ZOMBIE, t(0, 0, 0));
        engine.play(1, EVIL_GOBLIN, t(1, 0, 0));
        play(engine, 0, PISTON, t(0, 0, 4));
        engine.clickTile(0, t(0, 1, 0));
        assertEquals(BEAR, engine.state().unit(t(0, 0, 0)).type());
        assertEquals(BASIC_ZOMBIE, engine.state().unit(t(1, 0, 0)).type());
        assertEquals(1, engine.state().unit(t(1, 0, 0)).owner(), "crossed over: now the opponent's");
        assertEquals(EVIL_GOBLIN, engine.state().unit(t(1, 1, 0)).type());
    }

    @Test
    void pistonCanPushEnemyMonsters() {
        GameEngine engine = game(List.of(PISTON), List.of(BEAR), 0);
        engine.play(1, BEAR, t(1, 0, 2));
        play(engine, 0, PISTON, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 2));
        assertEquals(BEAR, engine.state().unit(t(1, 1, 2)).type());
    }

    @Test
    void pistonCannotPushOffTheBoard() {
        GameEngine engine = game(List.of(PISTON), List.of(BEAR), 0);
        engine.play(1, BEAR, t(1, 1, 2));
        play(engine, 0, PISTON, t(0, 0, 0));
        assertNull(engine.state().pendingEffect(), "the only monster is on the edge: cancelled");
    }

    @Test
    void slimePistonDiscardsThenPullsAnyMonsterTowardsYou() {
        GameEngine engine = game(List.of(SLIME_PISTON, BEAR), List.of(BEAR), 0);
        engine.play(1, EVIL_GOBLIN, t(1, 0, 3));
        play(engine, 0, SLIME_PISTON, t(0, 0, 0));
        assertEquals(Effect.TargetKind.HAND, engine.state().pendingEffect().targetKind());
        engine.clickHand(0, 0);
        engine.clickTile(0, t(1, 0, 3));
        assertEquals(EVIL_GOBLIN, engine.state().unit(t(0, 0, 3)).type());
        assertEquals(0, engine.state().unit(t(0, 0, 3)).owner(), "pulled over: now yours");
        assertEquals(8, engine.state().power(0));
        assertEquals(List.of(SLIME_PISTON, BEAR), engine.state().side(0).graveyard());
    }

    @Test
    void slimePistonWithEmptyHandIsCancelled() {
        GameEngine engine = game(List.of(SLIME_PISTON), List.of(BEAR), 0);
        engine.play(1, BEAR, t(1, 0, 3));
        play(engine, 0, SLIME_PISTON, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(BEAR, engine.state().unit(t(1, 0, 3)).type());
    }

    @Test
    void piddyDirectlyLeftOfDartyMovesAMonsterOneRowBack() {
        GameEngine engine = game(List.of(DARTY, PIDDY), List.of(BEAR), 0);
        engine.play(1, BEAR, t(1, 0, 1));
        play(engine, 0, DARTY, t(0, 0, 3));      // no Piddy on its left yet: its own effect is cancelled
        engine.pass(1);
        play(engine, 0, PIDDY, t(0, 0, 2));
        assertNotNull(engine.state().pendingEffect(), "condition held: the move waits for a target");
        engine.clickTile(0, t(1, 0, 1));         // any monster, even an enemy one
        assertEquals(BEAR, engine.state().unit(t(0, 0, 1)).type(), "pulled one row towards Piddy's owner");
        assertEquals(0, engine.state().unit(t(0, 0, 1)).owner(), "crossed over: now side 0's");
    }

    @Test
    void piddyMovesOwnMonsterToTheBackRow() {
        GameEngine engine = game(List.of(DARTY, PIDDY), List.of(BEAR), 0);
        engine.play(0, BEAR, t(0, 0, 4));
        play(engine, 0, DARTY, t(0, 0, 1));
        engine.pass(1);
        play(engine, 0, PIDDY, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 4));
        assertEquals(BEAR, engine.state().unit(t(0, 1, 4)).type());
        assertNull(engine.state().unit(t(0, 0, 4)));
    }

    @Test
    void piddyWithDartyFurtherRightDoesNothing() {
        GameEngine engine = game(List.of(DARTY, PIDDY), List.of(BEAR), 0);
        engine.play(1, BEAR, t(1, 0, 1));
        play(engine, 0, DARTY, t(0, 0, 4));
        engine.pass(1);
        play(engine, 0, PIDDY, t(0, 0, 2));
        assertNull(engine.state().pendingEffect());
        assertTrue(engine.state().log().getLast().text().endsWith("- no target"));
    }

    @Test
    void piddyRightOfDartyDoesNothing() {
        GameEngine engine = game(List.of(DARTY, PIDDY), List.of(BEAR), 0);
        engine.play(1, BEAR, t(1, 0, 1));
        play(engine, 0, DARTY, t(0, 0, 1));
        engine.pass(1);
        play(engine, 0, PIDDY, t(0, 0, 2));
        assertNull(engine.state().pendingEffect());
    }

    @Test
    void piddyWorksForSide1AndPullsTowardsSide1() {
        GameEngine engine = game(List.of(BEAR), List.of(DARTY, PIDDY), 1);
        engine.play(0, BEAR, t(0, 0, 0));
        play(engine, 1, DARTY, t(1, 1, 3));
        engine.pass(0);
        play(engine, 1, PIDDY, t(1, 1, 2));
        assertEquals(1, engine.state().pendingOwner(), "side 1's Piddy, side 1 picks");
        engine.clickTile(1, t(0, 0, 0));
        assertEquals(BEAR, engine.state().unit(t(1, 0, 0)).type(), "pulled towards side 1");
    }

    @Test
    void dartyAndPiddyStats() {
        assertEquals(2, DARTY.power());
        assertTrue(DARTY.onPlay(null).isEmpty());
        assertEquals(2, PIDDY.power());
        assertTrue(PIDDY.isMonster());
    }
}
