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
}
