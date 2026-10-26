package com.rutaji.qwent3.cards.gameLogic;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.deck.DeckStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rutaji.qwent3.TestGames.game;
import static com.rutaji.qwent3.TestGames.play;
import static com.rutaji.qwent3.TestGames.t;
import static com.rutaji.qwent3.TestGames.withDecks;
import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Turns, placing cards, passing, rounds, points, game end, drawing and the mulligan. */
class GameFlowTest {

    @Nested
    class Start {
        @Test
        void startDrawsNineAndStartsMulligan() {
            GameEngine engine = GameEngine.start(DeckStore.defaultDeck(), DeckStore.defaultDeck());
            assertEquals(GameState.Phase.MULLIGAN, engine.state().phase());
            for (int side = 0; side < 2; side++) {
                assertEquals(GameState.STARTING_HAND, engine.state().side(side).handSize());
                assertEquals(DeckStore.defaultDeck().size() - GameState.STARTING_HAND, engine.state().side(side).deckSize());
                assertEquals(GameState.MULLIGAN_REDRAWS, engine.state().side(side).redrawsLeft());
                assertTrue(engine.state().awaits(side));
            }
            assertEquals(1, engine.state().round());
        }

        @Test
        void gameIsPlayedWithACopyOfTheDeck() {
            List<CardType> deck = new ArrayList<>(DeckStore.defaultDeck());
            List<CardType> before = List.copyOf(deck);
            GameEngine engine = new GameEngine(deck, deck, 0);
            engine.draw(0, 5);
            assertEquals(before, deck);
        }
    }

    @Nested
    class Turns {
        @Test
        void firstPlayerStartsAndTurnsAlternate() {
            GameEngine engine = game(List.of(BEAR, BEAR), List.of(BEAR, BEAR), 1);
            assertEquals(1, engine.state().current());
            play(engine, 1, BEAR, t(1, 0, 0));
            assertEquals(0, engine.state().current());
            play(engine, 0, BEAR, t(0, 0, 0));
            assertEquals(1, engine.state().current());
        }

        @Test
        void playerCannotActOnOpponentsTurn() {
            GameEngine engine = game(List.of(BEAR), List.of(BEAR), 0);
            play(engine, 1, BEAR, t(1, 0, 0));
            assertNull(engine.state().unit(t(1, 0, 0)));
            assertEquals(1, engine.state().side(1).handSize());
            assertEquals(0, engine.state().current());
        }

        @Test
        void monsterGoesOnlyOnOwnEmptyTile() {
            GameEngine engine = game(List.of(BEAR, BEAR), List.of(BEAR), 0);
            play(engine, 0, BEAR, t(1, 0, 0));
            assertNull(engine.state().unit(t(1, 0, 0)), "not on the opponent's side");
            engine.clickTile(0, t(0, 0, 0));
            assertEquals(BEAR, engine.state().unit(t(0, 0, 0)).type());
            assertEquals(0, engine.state().unit(t(0, 0, 0)).owner());
            play(engine, 1, BEAR, t(1, 0, 0));
            play(engine, 0, BEAR, t(0, 0, 0));
            assertEquals(1, engine.state().side(0).handSize(), "occupied tile refused");
            assertEquals(0, engine.state().current());
        }

        @Test
        void clickingTheSelectedCardDeselectsIt() {
            GameEngine engine = game(List.of(BEAR, BEAR), List.of(BEAR), 0);
            engine.clickHand(0, 1);
            assertEquals(1, engine.state().side(0).selectedHandIndex());
            engine.clickHand(0, 1);
            assertEquals(-1, engine.state().side(0).selectedHandIndex());
        }

        @Test
        void spellCanBeCastOnAnyTileAndGoesToGraveyard() {
            GameEngine engine = game(List.of(GRAVEL), List.of(BEAR), 0);
            play(engine, 0, GRAVEL, t(1, 1, 4));
            assertEquals(List.of(GRAVEL), engine.state().side(0).graveyard());
            assertEquals(0, engine.state().side(0).handSize());
            assertEquals(1, engine.state().current());
        }

        @Test
        void powerIsCountedPerSide() {
            GameEngine engine = game(List.of(BEAR, BASIC_ZOMBIE), List.of(EVIL_GOBLIN), 0);
            play(engine, 0, BEAR, t(0, 0, 0));
            play(engine, 1, EVIL_GOBLIN, t(1, 1, 3));
            play(engine, 0, BASIC_ZOMBIE, t(0, 1, 2));
            assertEquals(9, engine.state().power(0));
            assertEquals(8, engine.state().power(1));
        }
    }

    @Nested
    class Passing {
        @Test
        void passOnlyOnYourTurn() {
            GameEngine engine = game(List.of(BEAR), List.of(BEAR), 0);
            engine.pass(1);
            assertFalse(engine.state().side(1).passed());
            engine.pass(0);
            assertTrue(engine.state().side(0).passed());
            assertEquals(1, engine.state().current());
        }

        @Test
        void otherPlayerKeepsPlayingAfterPass() {
            GameEngine engine = game(List.of(BEAR, BEAR), List.of(BEAR, BEAR, BEAR), 0);
            engine.pass(0);
            play(engine, 1, BEAR, t(1, 0, 0));
            assertEquals(1, engine.state().current());
            play(engine, 1, BEAR, t(1, 0, 1));
            assertEquals(1, engine.state().current());
            play(engine, 0, BEAR, t(0, 0, 0));
            assertNull(engine.state().unit(t(0, 0, 0)), "a player who passed can't play");
        }
    }

    @Nested
    class Rounds {
        @Test
        void bothPassedEndsRoundHigherPowerScores() {
            GameEngine engine = withDecks(List.of(BEAR, BEAR, BEAR), 2, List.of(BASIC_ZOMBIE, BEAR, BEAR), 2, 0);
            play(engine, 0, BEAR, t(0, 0, 0));
            play(engine, 1, BASIC_ZOMBIE, t(1, 0, 0));
            engine.pass(0);
            engine.pass(1);
            assertEquals(1, engine.state().points(0));
            assertEquals(0, engine.state().points(1));
            assertEquals(2, engine.state().round());
            assertEquals(7, engine.state().lastRoundPower(0));
            assertEquals(2, engine.state().lastRoundPower(1));
        }

        @Test
        void tiedRoundGivesBothAPoint() {
            GameEngine engine = withDecks(List.of(BEAR, BEAR), 1, List.of(BEAR, BEAR), 1, 0);
            play(engine, 0, BEAR, t(0, 0, 0));
            play(engine, 1, BEAR, t(1, 0, 0));
            engine.pass(0);
            engine.pass(1);
            assertEquals(1, engine.state().points(0));
            assertEquals(1, engine.state().points(1));
        }

        @Test
        void roundEndClearsBoardWithoutDeathEffects() {
            GameEngine engine = withDecks(List.of(CREEPER, BEAR, BEAR), 2, List.of(STONE_BRICKS, BEAR), 1, 0);
            play(engine, 0, CREEPER, t(0, 0, 1));
            play(engine, 1, STONE_BRICKS, t(1, 0, 1));
            play(engine, 0, BEAR, t(0, 0, 0));
            engine.pass(1);
            engine.pass(0);
            for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
                assertNull(engine.state().unit(tile));
            }
            assertNull(engine.state().pendingEffect());
            assertTrue(engine.state().side(0).graveyard().containsAll(List.of(CREEPER, BEAR)));
            assertEquals(List.of(STONE_BRICKS), engine.state().side(1).graveyard(), "no Silverfish from Stone Bricks");
        }

        @Test
        void newRoundDrawsOneMulligansOneAndAlternatesStarter() {
            GameEngine engine = withDecks(List.of(BEAR, BEAR, BEAR, BEAR), 2, List.of(BEAR, BEAR, BEAR, BEAR), 2, 0);
            engine.pass(0);
            engine.pass(1);
            assertEquals(GameState.Phase.MULLIGAN, engine.state().phase());
            assertEquals(3, engine.state().side(0).handSize());
            assertEquals(3, engine.state().side(1).handSize());
            assertEquals(GameState.ROUND_REDRAWS, engine.state().side(0).redrawsLeft());
            assertFalse(engine.state().side(0).passed());
            engine.endMulligan(0);
            engine.endMulligan(1);
            assertEquals(1, engine.state().starter());
            assertEquals(1, engine.state().current());
        }

        @Test
        void twoPointsWinTheGame() {
            GameEngine engine = withDecks(List.of(BEAR, BEAR, BEAR), 2, List.of(BASIC_ZOMBIE, BASIC_ZOMBIE, BEAR), 2, 0);
            play(engine, 0, BEAR, t(0, 0, 0));
            play(engine, 1, BASIC_ZOMBIE, t(1, 0, 0));
            engine.pass(0);
            engine.pass(1);
            engine.endMulligan(0);
            engine.endMulligan(1);
            play(engine, 1, BASIC_ZOMBIE, t(1, 0, 0));
            play(engine, 0, BEAR, t(0, 0, 0));
            engine.pass(1);
            engine.pass(0);
            assertEquals(GameState.Phase.OVER, engine.state().phase());
            assertEquals(0, engine.state().winner());
            assertFalse(engine.state().awaits(0));
            assertFalse(engine.state().awaits(1));
        }

        @Test
        void bothReachingTwoIsADraw() {
            GameEngine engine = withDecks(List.of(BEAR, BEAR), 1, List.of(BEAR, BEAR), 1, 0);
            engine.pass(0);
            engine.pass(1);
            engine.endMulligan(0);
            engine.endMulligan(1);
            engine.pass(1);
            engine.pass(0);
            assertEquals(GameState.Phase.OVER, engine.state().phase());
            assertEquals(-1, engine.state().winner());
        }
    }

    @Nested
    class Drawing {
        @Test
        void handLimitDiscardsExtraCards() {
            List<CardType> deck = new ArrayList<>(Collections.nCopies(12, BEAR));
            GameEngine engine = new GameEngine(deck, List.of(BEAR), 0);
            engine.draw(0, 12);
            assertEquals(GameState.MAX_HAND, engine.state().side(0).handSize());
            assertEquals(2, engine.state().side(0).graveyard().size());
            assertEquals(0, engine.state().side(0).deckSize());
        }

        @Test
        void drawingFromEmptyDeckDoesNothing() {
            GameEngine engine = new GameEngine(List.of(BEAR), List.of(BEAR), 0);
            engine.draw(0, 5);
            assertEquals(1, engine.state().side(0).handSize());
        }

        @Test
        void drawTakesTopCardFirst() {
            GameEngine engine = new GameEngine(List.of(GRAVEL, BEAR, ARROW), List.of(BEAR), 0);
            engine.draw(0, 1);
            assertEquals(GRAVEL, engine.state().side(0).handCard(0));
            assertEquals(List.of(BEAR, ARROW), engine.state().side(0).deck());
        }
    }

    @Nested
    class Mulligan {
        private GameEngine mulligan(List<CardType> deck0, int hand0) {
            GameEngine engine = withDecks(deck0, hand0, List.of(BEAR, BEAR, BEAR), 1, 0);
            engine.beginMulligan(2);
            return engine;
        }

        @Test
        void redrawReplacesCardInSameSlotSkippingRedrawnTypes() {
            GameEngine engine = mulligan(List.of(BEAR, BASIC_ZOMBIE, BEAR, ARROW), 1);
            engine.clickHand(0, 0);
            assertEquals(BASIC_ZOMBIE, engine.state().side(0).handCard(0));
            engine.clickHand(0, 0);
            assertEquals(ARROW, engine.state().side(0).handCard(0), "neither Bear nor Basic Zombie may come back");
        }

        @Test
        void mulliganEndsAutomaticallyAfterLastRedraw() {
            GameEngine engine = mulligan(List.of(BEAR, BASIC_ZOMBIE, ARROW, GRAVEL), 1);
            engine.clickHand(0, 0);
            engine.clickHand(0, 0);
            assertTrue(engine.state().side(0).mulliganDone());
            assertEquals(GameState.Phase.MULLIGAN, engine.state().phase(), "still waiting for the other player");
            engine.endMulligan(1);
            assertEquals(GameState.Phase.PLAYING, engine.state().phase());
        }

        @Test
        void onlyBlockedCardsLeftDrawsOneAnyway() {
            GameEngine engine = mulligan(List.of(BEAR, BEAR), 1);
            engine.clickHand(0, 0);
            assertEquals(BEAR, engine.state().side(0).handCard(0));
            assertEquals(1, engine.state().side(0).deckSize());
        }

        @Test
        void boardAndSelectionLockedDuringMulligan() {
            GameEngine engine = mulligan(List.of(BEAR, BEAR, BEAR), 2);
            engine.clickTile(0, t(0, 0, 0));
            engine.pass(0);
            assertNull(engine.state().unit(t(0, 0, 0)));
            assertFalse(engine.state().side(0).passed());
            assertEquals(-1, engine.state().side(0).selectedHandIndex());
        }
    }

    @Nested
    class History {
        @Test
        void logsPlayedCardsEffectsPassesAndRounds() {
            GameEngine engine = withDecks(List.of(SKELETON, BEAR), 1, List.of(BEAR, BEAR), 1, 0);
            play(engine, 0, SKELETON, t(0, 0, 0));
            engine.clickTile(0, t(0, 0, 0));
            engine.pass(1);
            engine.pass(0);
            List<GameState.LogEntry> log = engine.state().log();
            assertEquals(new GameState.LogEntry(0, "Skeleton", true), log.get(0));
            assertEquals(new GameState.LogEntry(0, "Skeleton: deal 3 damage to a monster -> Skeleton", false), log.get(1));
            assertEquals(new GameState.LogEntry(1, "Passed", false), log.get(2));
            assertEquals(new GameState.LogEntry(0, "Passed", false), log.get(3));
            assertEquals(new GameState.LogEntry(-1, "--- Round 2 ---", false), log.get(4));
        }

        @Test
        void cancelledEffectIsLoggedWithNoTarget() {
            GameEngine engine = game(List.of(ARROW), List.of(BEAR), 0);
            play(engine, 0, ARROW, t(0, 0, 0));
            assertEquals("Arrow: deal 4 damage to a monster - no target", engine.state().log().getLast().text());
        }
    }
}
