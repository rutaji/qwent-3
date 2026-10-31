package com.rutaji.qwent3.cards.cardsRepository;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.rutaji.qwent3.TestGames.game;
import static com.rutaji.qwent3.TestGames.handIndex;
import static com.rutaji.qwent3.TestGames.onBoard;
import static com.rutaji.qwent3.TestGames.play;
import static com.rutaji.qwent3.TestGames.power;
import static com.rutaji.qwent3.TestGames.t;
import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Cards that damage or destroy monsters. */
class DamageCardsTest {

    @Test
    void arrowDeals4ToAnyMonster() {
        GameEngine engine = game(List.of(BEAR, ARROW), List.of(EVIL_GOBLIN), 1);
        play(engine, 1, EVIL_GOBLIN, t(1, 0, 0));
        play(engine, 0, BEAR, t(0, 0, 0));
        engine.pass(1);
        play(engine, 0, ARROW, t(0, 0, 4));
        engine.clickTile(0, t(0, 0, 0));
        assertEquals(3, power(engine, t(0, 0, 0)), "own monster is a valid target too");
    }

    @Test
    void arrowKillsAt4() {
        GameEngine engine = game(List.of(ARROW), List.of(BASIC_ZOMBIE, BEAR), 1);
        play(engine, 1, BASIC_ZOMBIE, t(1, 0, 0));
        play(engine, 0, ARROW, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertNull(engine.state().unit(t(1, 0, 0)));
        assertEquals(List.of(BASIC_ZOMBIE), engine.state().side(1).graveyard());
    }

    @Test
    void twoArrowsSecondCannotHitTheSameMonster() {
        GameEngine engine = game(List.of(BEAR, TWO_ARROWS), List.of(EVIL_GOBLIN, BEAR), 1);
        play(engine, 1, EVIL_GOBLIN, t(1, 0, 0));
        play(engine, 0, BEAR, t(0, 0, 0));
        engine.pass(1);
        play(engine, 0, TWO_ARROWS, t(0, 0, 4));
        engine.clickTile(0, t(1, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(4, power(engine, t(1, 0, 0)), "second click on the same monster ignored");
        assertNotNull(engine.state().pendingEffect());
        engine.clickTile(0, t(0, 0, 0));
        assertEquals(3, power(engine, t(0, 0, 0)));
        assertNull(engine.state().pendingEffect());
    }

    @Test
    void twoArrowsWithOneMonsterHitsItOnce() {
        GameEngine engine = game(List.of(TWO_ARROWS), List.of(EVIL_GOBLIN, BEAR), 1);
        play(engine, 1, EVIL_GOBLIN, t(1, 0, 0));
        play(engine, 0, TWO_ARROWS, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(4, power(engine, t(1, 0, 0)));
        assertNull(engine.state().pendingEffect());
    }

    @Test
    void gravelDestroysAllLowestWithoutAsking() {
        GameEngine engine = game(List.of(BASIC_ZOMBIE, GRAVEL), List.of(BASIC_ZOMBIE, BEAR), 0);
        play(engine, 0, BASIC_ZOMBIE, t(0, 0, 0));
        play(engine, 1, BASIC_ZOMBIE, t(1, 0, 0));
        play(engine, 0, GRAVEL, t(0, 0, 0));
        assertEquals(0, onBoard(engine, BASIC_ZOMBIE));
        assertNull(engine.state().pendingEffect());
        assertEquals("Gravel: destroy the monsters with the lowest power -> Basic Zombie (2), Basic Zombie (2)",
                engine.state().log().getLast().text());
    }

    @Test
    void lightningDestroysTiedHighest() {
        GameEngine engine = game(List.of(BEAR, LIGHTNING), List.of(BEAR, BASIC_ZOMBIE), 0);
        play(engine, 0, BEAR, t(0, 0, 0));
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, LIGHTNING, t(0, 0, 0));
        assertEquals(0, onBoard(engine, BEAR));
        assertEquals(List.of(LIGHTNING, BEAR), engine.state().side(0).graveyard());
        assertEquals(List.of(BEAR), engine.state().side(1).graveyard());
    }

    @Test
    void lightningKillingACreeperTriggersItsDeathEffect() {
        GameEngine engine = game(List.of(LIGHTNING), List.of(BEAR), 0);
        // Board set up directly (these monsters have no on-play effects).
        engine.play(1, BASIC_ZOMBIE, t(1, 0, 0));
        engine.play(1, CREEPER, t(1, 0, 1));
        engine.play(1, BASIC_ZOMBIE, t(1, 0, 2));
        play(engine, 0, LIGHTNING, t(0, 0, 0));
        assertEquals(0, onBoard(engine, CREEPER));
        assertEquals(0, onBoard(engine, BASIC_ZOMBIE), "the Creeper's death destroyed both neighbours");
        assertEquals(List.of(CREEPER, BASIC_ZOMBIE, BASIC_ZOMBIE), engine.state().side(1).graveyard());
    }

    @Test
    void gravelOnEmptyBoardIsCancelled() {
        GameEngine engine = game(List.of(GRAVEL), List.of(BEAR), 0);
        play(engine, 0, GRAVEL, t(0, 0, 0));
        assertTrue(engine.state().log().getLast().text().endsWith("- no target"));
    }

    @Test
    void skeletonDeals3() {
        GameEngine engine = game(List.of(SKELETON), List.of(BEAR), 1);
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, SKELETON, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(4, power(engine, t(1, 0, 0)));
    }

    @Test
    void boarDeals1() {
        GameEngine engine = game(List.of(BOAR), List.of(BEAR), 1);
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, BOAR, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(6, power(engine, t(1, 0, 0)));
    }

    @Test
    void desertZombieDeals2AndCanHitItself() {
        GameEngine engine = game(List.of(DESERT_ZOMBIE), List.of(BEAR), 0);
        play(engine, 0, DESERT_ZOMBIE, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 0));
        assertEquals(3, power(engine, t(0, 0, 0)));
    }

    @Test
    void blazeDeals1TwiceSameTargetAllowed() {
        GameEngine engine = game(List.of(BLAZE), List.of(BEAR), 1);
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, BLAZE, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(5, power(engine, t(1, 0, 0)));
    }

    @Test
    void cactusDamagesLeftAndRightOnly() {
        GameEngine engine = game(List.of(BEAR, BEAR, BEAR, CACTUS), List.of(BEAR), 0);
        play(engine, 0, BEAR, t(0, 0, 1));
        engine.pass(1);
        play(engine, 0, BEAR, t(0, 0, 3));
        play(engine, 0, BEAR, t(0, 1, 2));
        play(engine, 0, CACTUS, t(0, 0, 2));
        assertEquals(6, power(engine, t(0, 0, 1)));
        assertEquals(6, power(engine, t(0, 0, 3)));
        assertEquals(7, power(engine, t(0, 1, 2)), "the one behind isn't hit");
    }

    @Test
    void cactusIsTreacherousPlant() {
        assertTrue(CACTUS.hasKeyword(Keyword.TREACHEROUS));
        assertTrue(CACTUS.hasTag(Tag.PLANT));
    }

    @Test
    void harshWorldDamagesEveryone() {
        GameEngine engine = game(List.of(BEAR, HARSH_WORLD), List.of(BASIC_ZOMBIE, BEAR), 0);
        play(engine, 0, BEAR, t(0, 0, 0));
        play(engine, 1, BASIC_ZOMBIE, t(1, 0, 0));
        play(engine, 0, HARSH_WORLD, t(0, 0, 0));
        assertEquals(5, power(engine, t(0, 0, 0)));
        assertNull(engine.state().unit(t(1, 0, 0)), "2 power: destroyed");
    }

    @Test
    void thornyRoseNeedsAPlantInGraveyard() {
        GameEngine engine = game(List.of(THORNY_ROSE), List.of(BEAR, BEAR), 1);
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, THORNY_ROSE, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(7, power(engine, t(1, 0, 0)));
    }

    @Test
    void thornyRoseBanishesAPlantToDeal4() {
        GameEngine engine = game(List.of(THORNY_ROSE), List.of(BEAR, BEAR), 1);
        engine.addToGraveyard(0, BEETROOT);
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, THORNY_ROSE, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(3, power(engine, t(1, 0, 0)));
        assertFalse(engine.state().side(0).graveyard().contains(BEETROOT), "banished");
    }

    @Test
    void shulkerDiscardsThenHits3DifferentEnemies() {
        GameEngine engine = game(List.of(SHULKER, BEAR, BASIC_ZOMBIE), List.of(BEAR, EVIL_GOBLIN, BEAR), 1);
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, BEAR, t(0, 0, 4));
        play(engine, 1, EVIL_GOBLIN, t(1, 0, 1));
        play(engine, 0, SHULKER, t(0, 0, 0));
        assertEquals(Effect.TargetKind.HAND, engine.state().pendingEffect().targetKind());
        engine.clickHand(0, handIndex(engine, 0, BASIC_ZOMBIE));
        engine.clickTile(0, t(0, 0, 4));
        assertEquals(7, power(engine, t(0, 0, 4)), "own monster isn't a valid target");
        engine.clickTile(0, t(1, 0, 0));
        engine.clickTile(0, t(1, 0, 1));
        assertEquals(3, power(engine, t(1, 0, 0)));
        assertEquals(4, power(engine, t(1, 0, 1)));
        assertNull(engine.state().pendingEffect(), "third hit cancelled: only 2 enemies");
        assertTrue(engine.state().side(0).graveyard().contains(BASIC_ZOMBIE));
    }

    @Test
    void shulkerWithoutEnemiesDiscardsNothing() {
        GameEngine engine = game(List.of(SHULKER, BEAR), List.of(BEAR), 0);
        play(engine, 0, SHULKER, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(1, engine.state().side(0).handSize());
    }

    @Test
    void creeperDiesAndDestroysLeftAndRight() {
        GameEngine engine = game(List.of(BEAR, ARROW, ARROW), List.of(BEAR, CREEPER, BASIC_ZOMBIE, BEAR), 1);
        play(engine, 1, BEAR, t(1, 0, 0));
        play(engine, 0, BEAR, t(0, 0, 4));
        play(engine, 1, CREEPER, t(1, 0, 1));
        play(engine, 0, ARROW, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 1));             // Creeper 6 -> 2
        play(engine, 1, BASIC_ZOMBIE, t(1, 0, 2));
        play(engine, 0, ARROW, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 1));             // Creeper dies
        assertNull(engine.state().unit(t(1, 0, 0)));
        assertNull(engine.state().unit(t(1, 0, 2)));
        assertEquals(List.of(CREEPER, BEAR, BASIC_ZOMBIE), engine.state().side(1).graveyard());
    }

    @Test
    void creeperIsTreacherous() {
        assertTrue(CREEPER.hasKeyword(Keyword.TREACHEROUS));
    }

    @Test
    void summonEnderDragonDiscards4ThenPlaysDragonWhichClearsItsColumn() {
        GameEngine engine = game(List.of(SUMMON_ENDER_DRAGON, BEAR, BEAR, BEAR, BEAR, BEAR), List.of(BEAR, BEAR), 1);
        play(engine, 1, BEAR, t(1, 0, 2));
        play(engine, 0, BEAR, t(0, 1, 2));
        play(engine, 1, BEAR, t(1, 0, 3));
        play(engine, 0, SUMMON_ENDER_DRAGON, t(0, 0, 0));
        for (int i = 0; i < 4; i++) {
            assertEquals(Effect.TargetKind.HAND, engine.state().pendingEffect().targetKind());
            engine.clickHand(0, 0);
        }
        engine.clickTile(0, t(0, 0, 2));
        assertEquals(27, power(engine, t(0, 0, 2)));
        assertNull(engine.state().unit(t(0, 1, 2)), "own monster in the column destroyed too");
        assertNull(engine.state().unit(t(1, 0, 2)));
        assertNotNull(engine.state().unit(t(1, 0, 3)), "other columns untouched");
    }

    @Test
    void summonEnderDragonNeedsFourCards() {
        GameEngine engine = game(List.of(SUMMON_ENDER_DRAGON, BEAR, BEAR, BEAR), List.of(BEAR), 0);
        play(engine, 0, SUMMON_ENDER_DRAGON, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(3, engine.state().side(0).handSize(), "nothing discarded");
    }
}
