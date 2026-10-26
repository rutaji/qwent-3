package com.rutaji.qwent3.ai;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.deck.DeckRules;
import com.rutaji.qwent3.cards.deck.DeckStore;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.rutaji.qwent3.TestGames.game;
import static com.rutaji.qwent3.TestGames.play;
import static com.rutaji.qwent3.TestGames.t;
import static com.rutaji.qwent3.TestGames.withDecks;
import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Legal actions, validation of AI answers, and whole games played by AIs. */
class AiDriverTest {

    /** A logger that keeps its warnings instead of printing them. */
    private static final class CapturingLogger {
        final List<String> warnings = new ArrayList<>();
        final Logger logger = Logger.getAnonymousLogger();

        CapturingLogger() {
            logger.setUseParentHandlers(false);
            logger.addHandler(new Handler() {
                @Override
                public void publish(LogRecord record) {
                    if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                        warnings.add(record.getMessage());
                    }
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            });
        }
    }

    @Test
    void mulliganActionsAreRedrawsAndKeep() {
        GameEngine engine = withDecks(List.of(BEAR, ARROW, GRAVEL), 2, List.of(BEAR), 1, 0);
        engine.beginMulligan(2);
        assertEquals(List.of(new AiAction.Redraw(0), new AiAction.Redraw(1), new AiAction.KeepHand()),
                AiDriver.legalActions(engine.state(), 0));
    }

    @Test
    void turnActionsArePlaysOnLegalTilesSpellsAndPass() {
        GameEngine engine = game(List.of(BEAR, ARROW), List.of(BEAR), 0);
        engine.play(0, BASIC_ZOMBIE, t(0, 0, 0));
        List<AiAction> legal = AiDriver.legalActions(engine.state(), 0);
        assertEquals(GameState.TILES_PER_SIDE - 1 + 1 + 1, legal.size(), "9 tiles for the Bear, the Arrow, Pass");
        assertTrue(legal.contains(new AiAction.Play(1, -1)));
        assertTrue(legal.contains(new AiAction.Pass()));
        assertFalse(legal.contains(new AiAction.Play(0, t(0, 0, 0))), "occupied");
        assertFalse(legal.contains(new AiAction.Play(0, t(1, 0, 0))), "enemy side");
    }

    @Test
    void treacherousMonstersMayGoOnEitherSide() {
        GameEngine engine = game(List.of(BEETROOT), List.of(BEAR), 0);
        assertEquals(GameState.BOARD_SIZE + 1, AiDriver.legalActions(engine.state(), 0).size());
    }

    @Test
    void pendingEffectGivesTargetActions() {
        GameEngine engine = game(List.of(ENDERMAN, BEAR, ARROW), List.of(BEAR), 0);
        play(engine, 0, ENDERMAN, t(0, 0, 0));
        assertEquals(List.of(new AiAction.PickHand(0), new AiAction.PickHand(1)), AiDriver.legalActions(engine.state(), 0));

        GameEngine other = game(List.of(ARROW), List.of(BEAR), 0);
        other.play(1, BEAR, t(1, 0, 2));
        play(other, 0, ARROW, t(0, 0, 0));
        assertEquals(List.of(new AiAction.PickTile(t(1, 0, 2))), AiDriver.legalActions(other.state(), 0));
    }

    @Test
    void noActionsWhenNotWaitedFor() {
        GameEngine engine = game(List.of(BEAR), List.of(BEAR), 0);
        assertTrue(AiDriver.legalActions(engine.state(), 1).isEmpty());
    }

    @Test
    void illegalAnswerIsReplacedByASafeMove() {
        GameEngine engine = game(List.of(BEAR), List.of(BEAR), 0);
        CapturingLogger log = new CapturingLogger();
        AiDriver.run(engine, 0, view -> new AiAction.Play(0, t(1, 0, 0)), "Cheater", log.logger);
        assertTrue(engine.state().side(0).passed(), "fallback: pass");
        assertNull(engine.state().unit(t(1, 0, 0)));
        assertEquals(1, log.warnings.size());
        assertTrue(log.warnings.getFirst().contains("illegal"));
    }

    @Test
    void exceptionIsReplacedByASafeMove() {
        GameEngine engine = withDecks(List.of(BEAR, BEAR), 1, List.of(BEAR), 1, 0);
        engine.beginMulligan(2);
        CapturingLogger log = new CapturingLogger();
        AiDriver.run(engine, 0, view -> {
            throw new IllegalStateException("bug");
        }, "Broken", log.logger);
        assertTrue(engine.state().side(0).mulliganDone(), "fallback: keep hand");
        assertEquals(1, log.warnings.size());
    }

    @Test
    void viewHidesOpponentsHandAndOwnDrawOrder() {
        GameEngine engine = withDecks(List.of(BEAR, GRAVEL, ARROW), 1, List.of(SKELETON, BOAR), 1, 0);
        List<AiView> seen = new ArrayList<>();
        AiDriver.run(engine, 0, view -> {
            seen.add(view);
            return new AiAction.Pass();
        }, "Spy", Logger.getAnonymousLogger());
        AiView view = seen.getFirst();
        assertEquals(0, view.me());
        assertEquals(List.of(BEAR), view.hand());
        assertEquals(List.of(ARROW, GRAVEL), view.deck(), "sorted by card, not in draw order");
        assertEquals(1, view.opponentHandSize());
        assertEquals(1, view.opponentDeckSize());
        assertEquals(GameState.BOARD_SIZE, view.board().size());
        assertTrue(view.legalActions().contains(new AiAction.Pass()));
    }

    @Test
    void randomAiIsRegisteredAndFoundCaseInsensitive() {
        assertTrue(AiRegistry.names().contains("Random"));
        assertEquals("Random", AiRegistry.find("random"));
        assertNull(AiRegistry.find("nobody"));
        assertInstanceOf(RandomAi.class, AiRegistry.create("Random"));
    }

    @Test
    void randomAisFinishManyGamesWithLegalMovesOnly() {
        CapturingLogger log = new CapturingLogger();
        List<CardType> allowed = new ArrayList<>();
        for (CardType card : CardType.values()) {
            if (!DeckRules.BLACKLIST.contains(card)) {
                allowed.add(card);
            }
        }
        Random random = new Random(12345);
        for (int game = 0; game < 300; game++) {
            List<CardType> deck0 = game % 3 == 0 ? new ArrayList<>(DeckStore.defaultDeck()) : randomDeck(allowed, random);
            List<CardType> deck1 = randomDeck(allowed, random);
            Collections.shuffle(deck0, random);
            GameEngine engine = GameEngine.start(deck0, deck1);
            QwentAi[] ais = {new RandomAi(), new RandomAi()};
            int rounds = 0;
            while (engine.state().phase() != GameState.Phase.OVER) {
                boolean acted = false;
                for (int side = 0; side < 2; side++) {
                    if (engine.state().awaits(side)) {
                        AiDriver.run(engine, side, ais[side], "Random", log.logger);
                        acted = true;
                    }
                }
                assertTrue(acted, "game " + game + " is stuck");
                assertTrue(++rounds < 2000, "game " + game + " never ends");
            }
            assertTrue(engine.state().points(0) >= GameState.POINTS_TO_WIN || engine.state().points(1) >= GameState.POINTS_TO_WIN);
        }
        assertEquals(List.of(), log.warnings, "RandomAi never chose an illegal move");
    }

    private static List<CardType> randomDeck(List<CardType> allowed, Random random) {
        List<CardType> deck = new ArrayList<>();
        while (deck.size() < DeckRules.MAX_CARDS) {
            CardType card = allowed.get(random.nextInt(allowed.size()));
            if (Collections.frequency(deck, card) < DeckRules.MAX_COPIES) {
                deck.add(card);
            }
        }
        return deck;
    }
}
