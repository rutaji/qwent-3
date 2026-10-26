package com.rutaji.qwent3.cards.cardsRepository;

import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.rutaji.qwent3.TestGames.game;
import static com.rutaji.qwent3.TestGames.play;
import static com.rutaji.qwent3.TestGames.power;
import static com.rutaji.qwent3.TestGames.t;
import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Cards that change power. */
class BuffCardsTest {

    @Test
    void beeGives2ToAnyMonsterItselfIncluded() {
        GameEngine engine = game(List.of(BEE), List.of(BEAR), 0);
        play(engine, 0, BEE, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 0));
        assertEquals(7, power(engine, t(0, 0, 0)));
    }

    @Test
    void bardGives1TwiceSameTargetAllowed() {
        GameEngine engine = game(List.of(BARD), List.of(BEAR), 0);
        engine.play(1, BEAR, t(1, 0, 0));
        play(engine, 0, BARD, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(9, power(engine, t(1, 0, 0)));
    }

    @Test
    void warlockOnlyBuffsEnemies() {
        GameEngine engine = game(List.of(WARLOCK), List.of(BEAR), 0);
        engine.play(0, BEAR, t(0, 0, 1));
        engine.play(1, BEAR, t(1, 0, 0));
        play(engine, 0, WARLOCK, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 1));
        assertEquals(7, power(engine, t(0, 0, 1)), "own monster isn't valid");
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(9, power(engine, t(1, 0, 0)));
    }

    @Test
    void healerSetsOwnMonsterTo5() {
        GameEngine engine = game(List.of(HEALER), List.of(BEAR), 0);
        engine.play(0, EVIL_GOBLIN, t(0, 0, 1));
        engine.play(1, BEAR, t(1, 0, 0));
        play(engine, 0, HEALER, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(7, power(engine, t(1, 0, 0)), "enemy monster isn't valid");
        engine.clickTile(0, t(0, 0, 1));
        assertEquals(5, power(engine, t(0, 0, 1)));
    }

    @Test
    void shamanBuffsItsWholeRowIncludingItself() {
        GameEngine engine = game(List.of(SHAMAN), List.of(BEAR), 0);
        engine.play(0, BEAR, t(0, 0, 0));
        engine.play(0, BASIC_ZOMBIE, t(0, 1, 0));
        play(engine, 0, SHAMAN, t(0, 0, 3));
        assertEquals(9, power(engine, t(0, 0, 0)));
        assertEquals(4, power(engine, t(0, 0, 3)));
        assertEquals(2, power(engine, t(0, 1, 0)), "other row unchanged");
    }

    @Test
    void splashOfStrengthBuffsOnlyOwnMonsters() {
        GameEngine engine = game(List.of(SPLASH_OF_STRENGTH), List.of(BEAR), 0);
        engine.play(0, BEAR, t(0, 0, 0));
        engine.play(0, BASIC_ZOMBIE, t(0, 1, 4));
        engine.play(1, BEAR, t(1, 0, 0));
        play(engine, 0, SPLASH_OF_STRENGTH, t(0, 0, 0));
        assertEquals(9, power(engine, t(0, 0, 0)));
        assertEquals(4, power(engine, t(0, 1, 4)));
        assertEquals(7, power(engine, t(1, 0, 0)));
    }

    @Test
    void potatoGets3PerPotatoInGraveyard() {
        GameEngine engine = game(List.of(POTATO, POTATO), List.of(BEAR), 0);
        play(engine, 0, POTATO, t(0, 0, 0));
        assertEquals(1, power(engine, t(0, 0, 0)), "no Potato in the graveyard");
        engine.pass(1);
        engine.addToGraveyard(0, POTATO);
        engine.addToGraveyard(0, POTATO);
        play(engine, 0, POTATO, t(0, 0, 1));
        assertEquals(7, power(engine, t(0, 0, 1)));
    }

    @Test
    void wheatBanishesAPlantFor10() {
        GameEngine engine = game(List.of(WHEAT, WHEAT), List.of(BEAR), 0);
        play(engine, 0, WHEAT, t(0, 0, 0));
        assertEquals(2, power(engine, t(0, 0, 0)), "no Plant in the graveyard");
        engine.pass(1);
        engine.addToGraveyard(0, BEETROOT);
        play(engine, 0, WHEAT, t(0, 0, 1));
        assertEquals(12, power(engine, t(0, 0, 1)));
        assertFalse(engine.state().side(0).graveyard().contains(BEETROOT));
    }

    @Test
    void buffedAndDamagedPowerStaysOnTheUnitNotTheCard() {
        GameEngine engine = game(List.of(BEE, ARROW), List.of(BEAR), 0);
        play(engine, 0, BEE, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 0));
        engine.pass(1);
        play(engine, 0, ARROW, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 0));
        assertEquals(3, power(engine, t(0, 0, 0)));
        assertEquals(5, BEE.power(), "base power unchanged");
    }
}
