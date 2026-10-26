package com.rutaji.qwent3.cards.cardsRepository;

import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.rutaji.qwent3.TestGames.game;
import static com.rutaji.qwent3.TestGames.handIndex;
import static com.rutaji.qwent3.TestGames.play;
import static com.rutaji.qwent3.TestGames.power;
import static com.rutaji.qwent3.TestGames.t;
import static com.rutaji.qwent3.TestGames.withDecks;
import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Cards that work with hands, decks and graveyards. */
class PileCardsTest {

    @Test
    void zombieLibrarianDiscardsAZombieToDrawFirstSpell() {
        GameEngine engine = withDecks(List.of(ZOMBIE_LIBRARIAN, BEAR, HORDE, BEAR, GRAVEL), 3, List.of(BEAR), 1, 0);
        play(engine, 0, ZOMBIE_LIBRARIAN, t(0, 0, 0));
        engine.clickHand(0, handIndex(engine, 0, BEAR));
        assertNotNull(engine.state().pendingEffect(), "not a Zombie");
        engine.clickHand(0, handIndex(engine, 0, HORDE));     // Horde is a Zombie spell
        assertEquals(List.of(HORDE), engine.state().side(0).graveyard());
        assertTrue(handIndex(engine, 0, GRAVEL) >= 0, "first spell in the deck drawn");
        assertEquals(List.of(BEAR), engine.state().side(0).deck());
    }

    @Test
    void zombieLibrarianWithoutZombieInHandIsCancelled() {
        GameEngine engine = withDecks(List.of(ZOMBIE_LIBRARIAN, BEAR, GRAVEL), 2, List.of(BEAR), 1, 0);
        play(engine, 0, ZOMBIE_LIBRARIAN, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(1, engine.state().side(0).handSize());
        assertEquals(1, engine.state().side(0).deckSize());
    }

    @Test
    void zombieLibrarianStillDiscardsWhenNoSpellInDeck() {
        GameEngine engine = withDecks(List.of(ZOMBIE_LIBRARIAN, BASIC_ZOMBIE, BEAR), 2, List.of(BEAR), 1, 0);
        play(engine, 0, ZOMBIE_LIBRARIAN, t(0, 0, 0));
        engine.clickHand(0, 0);
        assertEquals(0, engine.state().side(0).handSize());
        assertEquals(List.of(BASIC_ZOMBIE), engine.state().side(0).graveyard());
    }

    @Test
    void librarianDiscardMonsterDrawSpellAndViceVersa() {
        GameEngine engine = withDecks(List.of(LIBRARIAN, BEAR, BASIC_ZOMBIE, GRAVEL), 2, List.of(BEAR), 1, 0);
        play(engine, 0, LIBRARIAN, t(0, 0, 0));
        engine.clickHand(0, handIndex(engine, 0, BEAR));
        assertTrue(handIndex(engine, 0, GRAVEL) >= 0);
        assertEquals(List.of(BASIC_ZOMBIE), engine.state().side(0).deck());

        GameEngine other = withDecks(List.of(LIBRARIAN, ARROW, GRAVEL, BEAR), 2, List.of(BEAR), 1, 0);
        play(other, 0, LIBRARIAN, t(0, 0, 0));
        other.clickHand(0, handIndex(other, 0, ARROW));
        assertTrue(handIndex(other, 0, BEAR) >= 0, "discarded a spell: first monster drawn");
        assertEquals(List.of(GRAVEL), other.state().side(0).deck());
    }

    @Test
    void fertilizingDiscards2ThenDraws2() {
        GameEngine engine = withDecks(List.of(FERTILIZING, BEAR, BASIC_ZOMBIE, ARROW, GRAVEL, EVIL_GOBLIN), 4,
                List.of(BEAR), 1, 0);
        play(engine, 0, FERTILIZING, t(0, 0, 0));
        engine.clickHand(0, handIndex(engine, 0, BEAR));
        assertNotNull(engine.state().pendingEffect());
        engine.clickHand(0, handIndex(engine, 0, BASIC_ZOMBIE));
        assertEquals(List.of(FERTILIZING, BEAR, BASIC_ZOMBIE), engine.state().side(0).graveyard());
        assertEquals(3, engine.state().side(0).handSize());
        assertTrue(handIndex(engine, 0, GRAVEL) >= 0 && handIndex(engine, 0, EVIL_GOBLIN) >= 0);
    }

    @Test
    void fertilizingWithOneCardIsCancelled() {
        GameEngine engine = withDecks(List.of(FERTILIZING, BEAR, GRAVEL), 2, List.of(BEAR), 1, 0);
        play(engine, 0, FERTILIZING, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(1, engine.state().side(0).handSize());
    }

    @Test
    void endermanDiscardsAChosenCard() {
        GameEngine engine = game(List.of(ENDERMAN, BEAR, GRAVEL), List.of(BEAR), 0);
        play(engine, 0, ENDERMAN, t(0, 0, 0));
        engine.clickHand(0, handIndex(engine, 0, GRAVEL));
        assertEquals(List.of(GRAVEL), engine.state().side(0).graveyard());
        assertEquals(14, power(engine, t(0, 0, 0)));
    }

    @Test
    void endermanWithEmptyHandDoesNothing() {
        GameEngine engine = game(List.of(ENDERMAN), List.of(BEAR), 0);
        play(engine, 0, ENDERMAN, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(1, engine.state().current());
    }

    @Test
    void musicDiscDrawsWithTwoDiscsInGraveyardThisOneCounts() {
        GameEngine engine = withDecks(List.of(MUSIC_DISC, MUSIC_DISC, BEAR), 2, List.of(BEAR, BEAR), 2, 0);
        play(engine, 0, MUSIC_DISC, t(0, 0, 0));
        assertEquals(1, engine.state().side(0).handSize(), "only 1 disc in graveyard: no draw");
        engine.pass(1);
        play(engine, 0, MUSIC_DISC, t(0, 0, 0));
        assertEquals(List.of(BEAR), List.of(engine.state().side(0).handCard(0)));
    }

    @Test
    void brownMushroomDiscardsAllCopiesFromDeck() {
        GameEngine engine = withDecks(List.of(BROWN_MUSHROOM, BEAR, BROWN_MUSHROOM, GRAVEL, BROWN_MUSHROOM), 1,
                List.of(BEAR), 1, 0);
        play(engine, 0, BROWN_MUSHROOM, t(0, 0, 0));
        assertEquals(List.of(BEAR, GRAVEL), engine.state().side(0).deck());
        assertEquals(2, Collections.frequency(engine.state().side(0).graveyard(), BROWN_MUSHROOM));
    }

    @Test
    void switcharooSwapsDeckAndGraveyardKeepingOrder() {
        GameEngine engine = withDecks(List.of(SWITCHAROO, BEAR, GRAVEL), 1, List.of(BEAR), 1, 0);
        engine.addToGraveyard(0, ARROW);
        engine.addToGraveyard(0, BASIC_ZOMBIE);
        play(engine, 0, SWITCHAROO, t(0, 0, 0));
        assertEquals(List.of(ARROW, BASIC_ZOMBIE, SWITCHAROO), engine.state().side(0).deck());
        assertEquals(List.of(BEAR, GRAVEL), engine.state().side(0).graveyard());
    }

    @Test
    void infestationShufflesFourSilverfishIntoOpponentsDeck() {
        GameEngine engine = withDecks(List.of(INFESTATION), 1, List.of(BEAR, BEAR, BEAR), 1, 0);
        play(engine, 0, INFESTATION, t(0, 0, 0));
        assertEquals(6, engine.state().side(1).deckSize());
        assertEquals(4, Collections.frequency(engine.state().side(1).deck(), SILVERFISH));
        assertEquals(0, engine.state().side(0).deckSize());
    }

    @Test
    void diamondHoePutsThreeCopiesIntoOwnDeck() {
        GameEngine engine = game(List.of(DIAMOND_HOE), List.of(BEAR), 0);
        engine.play(1, EVIL_GOBLIN, t(1, 0, 0));
        play(engine, 0, DIAMOND_HOE, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(3, Collections.frequency(engine.state().side(0).deck(), EVIL_GOBLIN));
        assertEquals(EVIL_GOBLIN, engine.state().unit(t(1, 0, 0)).type(), "the monster stays");
    }

    @Test
    void goldHoePutsThreeCopiesIntoOwnGraveyard() {
        GameEngine engine = game(List.of(GOLD_HOE), List.of(BEAR), 0);
        engine.play(0, BEETROOT, t(0, 0, 0));
        play(engine, 0, GOLD_HOE, t(0, 0, 0));
        engine.clickTile(0, t(0, 0, 0));
        assertEquals(3, Collections.frequency(engine.state().side(0).graveyard(), BEETROOT));
    }

    @Test
    void weedsPutsWeedsOnTopOfTheSpellCastersDeck() {
        GameEngine engine = withDecks(List.of(ARROW, BEAR), 1, List.of(BEAR, GRAVEL), 2, 0);
        engine.play(0, WEEDS, t(1, 0, 0));   // treacherous, on side 1: side 1's Weeds
        engine.play(1, BEAR, t(1, 0, 1));
        play(engine, 0, ARROW, t(0, 0, 0));
        engine.clickTile(0, t(1, 0, 1));
        assertEquals(List.of(WEEDS, BEAR), engine.state().side(0).deck(), "side 0 cast a spell");
        play(engine, 1, GRAVEL, t(1, 0, 0));
        assertEquals(List.of(), engine.state().side(1).deck(), "own spells don't trigger it");
    }

    @Test
    void mirrorBanishesOpponentsLastSpellAndCastsIt() {
        GameEngine engine = game(List.of(MIRROR), List.of(BEAR), 0);
        engine.addToGraveyard(1, ARROW);
        engine.addToGraveyard(1, BEAR);
        engine.addToGraveyard(1, GRAVEL);
        engine.play(1, EVIL_GOBLIN, t(1, 0, 0));
        engine.play(1, BASIC_ZOMBIE, t(1, 0, 1));
        play(engine, 0, MIRROR, t(0, 0, 0));
        assertNull(engine.state().unit(t(1, 0, 1)), "copied Gravel destroyed the lowest");
        assertEquals(List.of(ARROW, BEAR, BASIC_ZOMBIE), engine.state().side(1).graveyard(), "Gravel banished");
        assertEquals(List.of(MIRROR), engine.state().side(0).graveyard());
        assertTrue(engine.state().log().stream().anyMatch(e -> e.text().equals("Casts Gravel")));
    }

    @Test
    void mirrorWithoutSpellsIsCancelled() {
        GameEngine engine = game(List.of(MIRROR), List.of(BEAR), 0);
        engine.addToGraveyard(1, BEAR);
        play(engine, 0, MIRROR, t(0, 0, 0));
        assertNull(engine.state().pendingEffect());
        assertEquals(List.of(BEAR), engine.state().side(1).graveyard());
    }

    @Test
    void mirrorCopiedSpellTargetsAreChosenByTheMirrorPlayer() {
        GameEngine engine = game(List.of(MIRROR), List.of(BEAR), 0);
        engine.addToGraveyard(1, ARROW);
        engine.play(1, BEAR, t(1, 0, 0));
        play(engine, 0, MIRROR, t(0, 0, 0));
        assertEquals(0, engine.state().pendingOwner());
        engine.clickTile(0, t(1, 0, 0));
        assertEquals(3, power(engine, t(1, 0, 0)));
    }
}
