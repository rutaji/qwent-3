package com.rutaji.qwent3.cards.cardsRepository;

import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.rutaji.qwent3.TestGames.game;
import static com.rutaji.qwent3.TestGames.onBoard;
import static com.rutaji.qwent3.TestGames.play;
import static com.rutaji.qwent3.TestGames.plain;
import static com.rutaji.qwent3.TestGames.power;
import static com.rutaji.qwent3.TestGames.t;
import static com.rutaji.qwent3.TestGames.withDecks;
import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Cards that put monsters on the board. */
class SummonCardsTest {

    @RepeatedTest(5)
    void leaderZombieSurvivingDamageSummonsZombieOnRandomOwnTile() {
        GameEngine engine = game(List.of(ARROW), List.of(BEAR), 0);
        engine.play(1, LEADER_ZOMBIE, t(1, 0, 0));
        play(engine, 0, ARROW, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(2, power(engine, t(1, 0, 0)));
        assertEquals(1, onBoard(engine, BASIC_ZOMBIE));
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            if (engine.state().unit(tile) != null && engine.state().unit(tile).type() == BASIC_ZOMBIE) {
                assertEquals(1, GameState.sideOf(tile));
            }
        }
        assertNull(engine.state().pendingEffect(), "random: no prompt");
    }

    @Test
    void leaderZombieKilledSummonsNothing() {
        GameEngine engine = game(List.of(LIGHTNING), List.of(BEAR), 0);
        engine.play(1, LEADER_ZOMBIE, t(1, 0, 0));
        play(engine, 0, LIGHTNING, t(0, 0, 0));
        assertEquals(0, onBoard(engine, BASIC_ZOMBIE));
    }

    @Test
    void hordePlaysEveryBasicZombieFromDeckOnePickEach() {
        GameEngine engine = withDecks(List.of(HORDE, BASIC_ZOMBIE, BEAR, BASIC_ZOMBIE), 1, List.of(BEAR), 1, 0);
        play(engine, 0, HORDE, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertNotNull(engine.state().pendingEffect(), "only own side");
        engine.clickTile(0, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 1));
        assertEquals(2, onBoard(engine, BASIC_ZOMBIE));
        assertEquals(List.of(BEAR), engine.state().side(0).deck());
        assertNull(engine.state().pendingEffect());
    }

    @Test
    void hordeLeavesZombiesInDeckWhenSideIsFull() {
        GameEngine engine = withDecks(List.of(HORDE, BASIC_ZOMBIE, BASIC_ZOMBIE), 1, List.of(BEAR), 1, 0);
        for (int tile = 0; tile < GameState.TILES_PER_SIDE - 1; tile++) {
            engine.play(0, BEAR, tile);
        }
        play(engine, 0, HORDE, t(0, 0, 0));
        engine.clickTile(0, t(0, 1, 4));
        assertNull(engine.state().pendingEffect());
        assertEquals(List.of(BASIC_ZOMBIE), engine.state().side(0).deck());
    }

    @Test
    void zombieAwakeningPlaysZombiesFromGraveyard() {
        GameEngine engine = game(List.of(ZOMBIE_AWAKENING), List.of(BEAR), 0);
        engine.addToGraveyard(0, BASIC_ZOMBIE);
        engine.addToGraveyard(0, BEAR);
        engine.addToGraveyard(0, BASIC_ZOMBIE);
        play(engine, 0, ZOMBIE_AWAKENING, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 1));
        assertEquals(2, onBoard(engine, BASIC_ZOMBIE));
        assertEquals(List.of(BEAR, ZOMBIE_AWAKENING), engine.state().side(0).graveyard());
    }

    @Test
    void badFarmingFillsOwnBackRowEmptyTilesOnly() {
        GameEngine engine = game(List.of(BAD_FARMING), List.of(BEAR), 0);
        engine.play(0, BEAR, t(0, 1, 2));
        engine.play(1, BEAR, t(1, 1, 0));
        play(engine, 0, BAD_FARMING, t(1, 0, 0));
        assertEquals(4, onBoard(engine, BEETROOT));
        assertEquals(BEAR, engine.state().unit(t(0, 1, 2)).type());
        assertNull(engine.state().unit(t(1, 1, 1)), "enemy back row untouched");
        assertNull(engine.state().pendingEffect());
    }

    @Test
    void stoneBricksDeathFillsItsRowWithSilverfishForThatSide() {
        GameEngine engine = game(List.of(ARROW), List.of(BEAR), 0);
        engine.play(0, STONE_BRICKS, t(1, 1, 2));
        engine.play(1, BEAR, t(1, 1, 0));
        play(engine, 0, ARROW, t(0, 0, 0));
        engine.clickTile(0, t(1, 1, 2));
        assertEquals(4, onBoard(engine, SILVERFISH));
        for (int column = 1; column < GameState.COLUMNS; column++) {
            assertEquals(1, engine.state().unit(t(1, 1, column)).owner());
        }
        assertTrue(engine.state().side(1).graveyard().contains(STONE_BRICKS), "it was side 1's unit (treacherous)");
    }

    @Test
    void swarmPlays3SilverfishOnPickedTiles() {
        GameEngine engine = game(List.of(SWARM), List.of(BEAR), 0);
        play(engine, 0, SWARM, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 0));
        engine.clickTile(0, t(0, 1, 2));
        engine.clickTile(0, t(0, 1, 4));
        assertEquals(3, onBoard(engine, SILVERFISH));
        assertEquals(3, engine.state().power(0));
    }

    @Test
    void drunkBrawlerSurvivingDamageSummonsAnother() {
        GameEngine engine = game(List.of(BOAR), List.of(BEAR), 0);
        engine.play(1, DRUNK_BRAWLER, t(1, 0, 0));
        play(engine, 0, BOAR, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(1, engine.state().pendingOwner(), "the Brawler's owner picks the tile");
        engine.clickTile(1, t(1, 1, 3));
        assertEquals(2, onBoard(engine, DRUNK_BRAWLER));
    }

    @Test
    void farmerChainsWhilePlantsLast() {
        GameEngine engine = game(List.of(FARMER), List.of(BEAR), 0);
        engine.addToGraveyard(0, BEETROOT);
        engine.addToGraveyard(0, POTATO);
        play(engine, 0, FARMER, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertNotNull(engine.state().pendingEffect(), "only own side");
        engine.clickTile(0, t(0, 0, 1));
        engine.clickTile(0, t(0, 0, 2));
        assertEquals(3, onBoard(engine, FARMER));
        assertNull(engine.state().pendingEffect());
        assertTrue(engine.state().side(0).graveyard().isEmpty(), "both plants banished");
    }

    @Test
    void farmerWithoutPlantsDoesNothing() {
        GameEngine engine = game(List.of(FARMER), List.of(BEAR), 0);
        play(engine, 0, FARMER, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(1, onBoard(engine, FARMER));
    }

    @Test
    void woodenHoeDestroysOwnMonsterThenCopyIsPlacedBeforeDeathEffects() {
        GameEngine engine = game(List.of(WOODEN_HOE), List.of(BEAR), 0);
        engine.play(0, STONE_BRICKS, t(0, 0, 0));
        engine.play(1, BEAR, t(1, 0, 0));
        play(engine, 0, WOODEN_HOE, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(BEAR, engine.state().unit(t(1, 0, 0)).type(), "enemy monster isn't valid");
        engine.clickTile(0, t(0, 0, 0));
        assertTrue(plain(engine.state().pendingEffect().description()).contains("play the copy of Stone Bricks"));
        engine.clickTile(0, t(1, 0, 4));    // Stone Bricks is treacherous: enemy side allowed
        assertEquals(STONE_BRICKS, engine.state().unit(t(1, 0, 4)).type());
        assertEquals(1, engine.state().unit(t(1, 0, 4)).owner());
        // Then the old Stone Bricks' death effect fills its row (side 0, front row) with Silverfish.
        assertEquals(5, onBoard(engine, SILVERFISH));
    }

    @Test
    void woodenHoeCopyOfNormalMonsterOnlyOnOwnSide() {
        GameEngine engine = game(List.of(WOODEN_HOE), List.of(BEAR), 0);
        engine.play(0, BEAR, t(0, 0, 0));
        play(engine, 0, WOODEN_HOE, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertNotNull(engine.state().pendingEffect());
        engine.clickTile(0, t(0, 1, 1));
        assertEquals(BEAR, engine.state().unit(t(0, 1, 1)).type());
        assertEquals(7, power(engine, t(0, 1, 1)));
    }

    @Test
    void endermiteShowsTheMonsterThenItsTileFollowsItsRules() {
        GameEngine engine = withDecks(List.of(ENDERMITE, ARROW, CREEPER, GRAVEL), 2, List.of(BEAR), 1, 0);
        play(engine, 0, ENDERMITE, t(0, 0, 0));
        engine.clickHand(0, 0);   // discard Arrow
        assertTrue(plain(engine.state().pendingEffect().description()).contains("play Creeper"));
        assertEquals(2, engine.state().side(0).deckSize(), "taken from the deck only when placed");
        engine.clickTile(0, t(1, 0, 2));   // treacherous: enemy side ok
        assertEquals(CREEPER, engine.state().unit(t(1, 0, 2)).type());
        assertEquals(List.of(GRAVEL), engine.state().side(0).deck());
        assertNull(engine.state().pendingEffect(), "second play cancelled: no monster left in the deck");
    }

    @Test
    void endermiteWithoutMonstersInDeckDiscardsNothing() {
        GameEngine engine = withDecks(List.of(ENDERMITE, ARROW, GRAVEL), 2, List.of(BEAR), 1, 0);
        play(engine, 0, ENDERMITE, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(1, engine.state().side(0).handSize());
    }

    @Test
    void emeraldOnEnemySideDrawsTheirTopCard() {
        GameEngine engine = withDecks(List.of(EMERALD), 1, List.of(BEAR, GRAVEL, ARROW), 1, 0);
        play(engine, 0, EMERALD, t(1, 0, 0));
        assertEquals(1, engine.state().unit(t(1, 0, 0)).owner());
        assertEquals(List.of(GRAVEL), List.of(engine.state().side(0).handCard(0)));
        assertEquals(List.of(ARROW), engine.state().side(1).deck());
    }

    @Test
    void emeraldOnOwnSideDrawsNothing() {
        GameEngine engine = withDecks(List.of(EMERALD), 1, List.of(BEAR, GRAVEL), 1, 0);
        play(engine, 0, EMERALD, t(0, 0, 0));
        assertEquals(0, engine.state().side(0).handSize());
        assertEquals(1, engine.state().side(1).deckSize());
    }

    @Test
    void summonedMonstersGoToGraveyardAsCards() {
        GameEngine engine = game(List.of(SWARM, LIGHTNING), List.of(BEAR), 0);
        play(engine, 0, SWARM, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 1));
        engine.clickTile(0, t(0, 0, 2));
        engine.pass(1);
        play(engine, 0, LIGHTNING, t(0, 0, 0));
        assertEquals(3, Collections.frequency(engine.state().side(0).graveyard(), SILVERFISH));
    }
}
