package com.rutaji.qwent3.ai;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.effect.Effect;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Everything the AI may know when it has to act, from its own point of view ({@link #me()} is its side, 0 or 1).
 * A snapshot: it does not change while the AI decides.
 *
 * Board tiles: {@link GameState#BOARD_SIZE} tiles, index = side * {@link GameState#TILES_PER_SIDE}
 * + localRow * {@link GameState#COLUMNS} + column, local row 0 = front row. Helpers: {@link GameState#tile},
 * {@link GameState#sideOf}, {@link GameState#neighbours}.
 *
 * @param phase            MULLIGAN or PLAYING
 * @param board            index = tile, null = empty
 * @param deck             the AI's own deck, sorted (not in draw order)
 * @param pendingEffect    the effect waiting for the AI to pick a target, or null; see {@link Effect#description()}
 *                         and {@link Effect#targetKind()} (TILE: answer with PickTile, HAND: PickHand)
 * @param redrawsLeft      mulligan redraws left
 * @param log              the game history, oldest first (actor 0 / 1, or -1 for notes)
 * @param legalActions     what the AI may do now; {@link QwentAi#decide} must return one of these
 */
public record AiView(
        int me,
        GameState.Phase phase,
        int round,
        int myPoints,
        int opponentPoints,
        int myPower,
        int opponentPower,
        boolean iPassed,
        boolean opponentPassed,
        List<@Nullable AiUnit> board,
        List<CardType> hand,
        List<CardType> deck,
        List<CardType> graveyard,
        int opponentHandSize,
        int opponentDeckSize,
        List<CardType> opponentGraveyard,
        @Nullable Effect pendingEffect,
        int redrawsLeft,
        List<GameState.LogEntry> log,
        List<AiAction> legalActions) {

    public int opponent() {
        return GameState.other(me);
    }

    public @Nullable AiUnit unit(int tile) {
        return board.get(tile);
    }
}
