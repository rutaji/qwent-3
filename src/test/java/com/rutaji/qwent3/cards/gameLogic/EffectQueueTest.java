package com.rutaji.qwent3.cards.gameLogic;

import com.rutaji.qwent3.cards.effect.Effect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.rutaji.qwent3.TestGames.game;
import static com.rutaji.qwent3.TestGames.handIndex;
import static com.rutaji.qwent3.TestGames.play;
import static com.rutaji.qwent3.TestGames.power;
import static com.rutaji.qwent3.TestGames.t;
import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** How effects are queued, owned, targeted, cancelled and resolved in order. */
class EffectQueueTest {

    @Test
    void effectWithoutValidTargetIsCancelledAndTurnPasses() {
        GameEngine engine = game(List.of(ARROW), List.of(BEAR), 0);
        play(engine, 0, ARROW, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(1, engine.state().current());
    }

    @Test
    void targetedEffectWaitsForItsOwnerAndTurnWaitsForIt() {
        GameEngine engine = game(List.of(ARROW), List.of(BEAR, BEAR), 1);
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, ARROW, t(0, 0, 0));
        assertNotNull(engine.state().pendingEffect());
        assertEquals(0, engine.state().pendingOwner());
        assertEquals(0, engine.state().current(), "turn doesn't pass while an effect waits");
        assertTrue(engine.state().awaits(0));
        assertFalse(engine.state().awaits(1));
    }

    @Test
    void onlyTheOwnerCanPickAndOnlyValidTargets() {
        GameEngine engine = game(List.of(ARROW), List.of(BEAR, BEAR), 1);
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, ARROW, t(0, 0, 0));
        engine.clickTile(1, t(1, 0, 0));
        assertEquals(7, power(engine, t(1, 0, 0)), "opponent can't pick");
        engine.clickTile(0, t(0, 1, 1));
        assertNotNull(engine.state().pendingEffect(), "empty tile isn't a valid damage target");
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(3, power(engine, t(1, 0, 0)));
        assertNull(engine.state().pendingEffect());
    }

    @Test
    void handIsLockedWhileAnEffectWaits() {
        GameEngine engine = game(List.of(SKELETON, BEAR), List.of(BEAR), 0);
        play(engine, 0, SKELETON, t(0, 0, 0));
        engine.clickHand(0, handIndex(engine, 0, BEAR));
        assertEquals(-1, engine.state().side(0).selectedHandIndex());
    }

    @Test
    void tileClickDoesNotAnswerAHandEffect() {
        GameEngine engine = game(List.of(ENDERMAN, BEAR), List.of(BEAR), 0);
        play(engine, 0, ENDERMAN, t(0, 0, 0));
        assertEquals(Effect.TargetKind.HAND, engine.state().pendingEffect().targetKind());
        engine.clickTile(0, t(0, 0, 1));
        assertNotNull(engine.state().pendingEffect());
        engine.clickHand(0, 0);
        assertNull(engine.state().pendingEffect());
    }

    @Test
    void deathEffectBelongsToTheSideTheMonsterDiedOnEvenOnOpponentsTurn() {
        GameEngine engine = game(List.of(BEAR, GRAVEL), List.of(STONE_BRICKS, EVIL_GOBLIN), 1);
        play(engine, 1, STONE_BRICKS, t(1, 1, 2));
        play(engine, 0, BEAR, t(0, 0, 0));
        play(engine, 1, EVIL_GOBLIN, t(1, 0, 0));
        play(engine, 0, GRAVEL, t(0, 0, 0));
        // Stone Bricks (3) was the lowest: its row (including its own tile) fills with Silverfish for side 1.
        for (int column = 0; column < GameState.COLUMNS; column++) {
            assertEquals(SILVERFISH, engine.state().unit(t(1, 1, column)).type());
            assertEquals(1, engine.state().unit(t(1, 1, column)).owner());
        }
        assertTrue(engine.state().side(1).graveyard().contains(STONE_BRICKS));
    }

    @Test
    void damagedAndSurvivedEffectBelongsToTheMonstersSide() {
        GameEngine engine = game(List.of(ARROW), List.of(DRUNK_BRAWLER, BEAR), 1);
        play(engine, 1, DRUNK_BRAWLER, t(1, 0, 0));
        play(engine, 0, ARROW, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertNull(engine.state().unit(t(1, 0, 0)), "3 power, 4 damage: dies, no new Brawler");
        assertNull(engine.state().pendingEffect());
    }

    @Test
    void effectsResolveInOrder() {
        GameEngine engine = game(List.of(BEAR, TWO_ARROWS), List.of(CREEPER, BASIC_ZOMBIE, EVIL_GOBLIN), 1);
        play(engine, 1, CREEPER, t(1, 0, 1));
        play(engine, 0, BEAR, t(0, 0, 4));
        play(engine, 1, BASIC_ZOMBIE, t(1, 0, 0));
        play(engine, 0, TWO_ARROWS, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 1));   // Creeper 6 -> 2
        engine.clickTile(0, t(1, 0, 0));   // Basic Zombie dies
        assertNull(engine.state().unit(t(1, 0, 0)));
        assertEquals(2, power(engine, t(1, 0, 1)));
        assertNull(engine.state().pendingEffect());
    }

    @Test
    void playedSpellsTriggerOpponentsWeedsAfterTheirOwnEffects() {
        GameEngine engine = game(List.of(BEAR, ARROW), List.of(WEEDS, GRAVEL), 1);
        play(engine, 1, WEEDS, t(1, 0, 0));
        play(engine, 0, BEAR, t(0, 0, 0));
        play(engine, 1, GRAVEL, t(1, 0, 0));       // own spell: own Weeds don't react; Weeds (2) is lowest -> destroyed
        assertTrue(engine.state().side(1).graveyard().contains(WEEDS));
        assertEquals(0, engine.state().side(1).deckSize());
    }
}
