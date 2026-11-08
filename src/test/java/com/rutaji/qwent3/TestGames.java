package com.rutaji.qwent3;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Side;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;

/** Helpers to set up games for tests. Games made here skip the mulligan and start in the PLAYING phase. */
public final class TestGames {
    private TestGames() {
    }

    /** Each side's deck is exactly its hand list; all of it is drawn. */
    public static GameEngine game(List<CardType> hand0, List<CardType> hand1, int firstPlayer) {
        return withDecks(hand0, hand0.size(), hand1, hand1.size(), firstPlayer);
    }

    /** Decks in draw order (top first); {@code draw0} / {@code draw1} cards are drawn into the hands. */
    public static GameEngine withDecks(List<CardType> deck0, int draw0, List<CardType> deck1, int draw1,
                                      int firstPlayer) {
        GameEngine engine = new GameEngine(deck0, deck1, firstPlayer);
        engine.draw(0, draw0);
        engine.draw(1, draw1);
        return engine;
    }

    /** Selects the first copy of the card in the side's hand and clicks the tile (like a player would). */
    public static void play(GameEngine engine, int side, CardType card, int tile) {
        int index = handIndex(engine, side, card);
        if (index < 0) {
            throw new AssertionError(card + " is not in side " + side + "'s hand");
        }
        engine.clickHand(side, index);
        engine.clickTile(side, tile);
    }

    /** Index of the first copy of the card in the side's hand, or -1. */
    public static int handIndex(GameEngine engine, int side, CardType card) {
        Side s = engine.state().side(side);
        for (int i = 0; i < s.handSize(); i++) {
            if (s.handCard(i) == card) {
                return i;
            }
        }
        return -1;
    }

    public static int t(int side, int row, int column) {
        return GameState.tile(side, row, column);
    }

    /** How many units of this card are on the board (both sides). */
    public static int onBoard(GameEngine engine, CardType card) {
        GameState state = engine.state();
        int count = 0;
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            Unit unit = state.unit(tile);
            if (unit != null && unit.type() == card) {
                count++;
            }
        }
        return count;
    }

    public static int power(GameEngine engine, int tile) {
        Unit unit = engine.state().unit(tile);
        if (unit == null) {
            throw new AssertionError("No unit on tile " + tile);
        }
        return unit.power();
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
