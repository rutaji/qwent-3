package com.rutaji.qwent3.cards.gameLogic;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Keyword;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.effect.Effect;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * The data of one game between two players, side 0 and side 1, plus read-only questions about it. Knows nothing about
 * Bukkit players. Only {@link GameEngine} (same package) changes it; everyone else only reads.
 *
 * Board: {@link #ROWS} rows of {@link #COLUMNS}; each side owns {@link #ROWS_PER_SIDE} rows. Tile index =
 * side * TILES_PER_SIDE + localRow * COLUMNS + column, local row 0 being the front row (towards the opponent).
 */
public final class GameState {
    //region [Constants]
    public static final int STARTING_HAND = 9;
    public static final int MAX_HAND = 10;
    public static final int MULLIGAN_REDRAWS = 2;
    public static final int POINTS_TO_WIN = 2;
    public static final int ROUND_DRAW = 1;
    public static final int ROUND_REDRAWS = 1;
    public static final int ROWS_PER_SIDE = 2;
    public static final int ROWS = 2 * ROWS_PER_SIDE;
    public static final int COLUMNS = 5;
    public static final int TILES_PER_SIDE = ROWS_PER_SIDE * COLUMNS;
    public static final int BOARD_SIZE = ROWS * COLUMNS;

    public enum Phase {
        MULLIGAN,
        PLAYING,
        OVER
    }

    /** An effect waiting in the queue, with the side that owns it. */
    record QueuedEffect(Effect effect, int owner) {
    }

    /**
     * One line of the game history: who did it ({@code actor} 0 / 1, or -1 for neutral notes like round changes),
     * the text, and whether it is the card played from hand that turn.
     */
    public record LogEntry(int actor, String text, boolean played) {
    }
    //endregion [ constants]

    final Side[] sides;
    final Unit[] board = new Unit[BOARD_SIZE];
    final Deque<QueuedEffect> queue = new ArrayDeque<>();
    final List<LogEntry> log = new ArrayList<>();
    int round = 1;
    /** Power of both sides at the end of the previous round, or null in the first round. */
    int @Nullable [] lastRoundPower;
    /** Who starts the current round. */
    int starter;
    Phase phase = Phase.PLAYING;
    int current_player;

    /** @param deck0 side 0's deck, top card first (already shuffled); copied. Same for deck1. */
    GameState(List<CardType> deck0, List<CardType> deck1, int firstPlayer) {
        this.sides = new Side[] {new Side(deck0), new Side(deck1)};
        this.starter = firstPlayer;
        this.current_player = firstPlayer;
    }

    //region [Board geometry]
    public static int other(int side) {
        return 1 - side;
    }

    public static int sideOf(int tile) {
        return tile / TILES_PER_SIDE;
    }

    public static int tile(int side, int localRow, int column) {
        return side * TILES_PER_SIDE + localRow * COLUMNS + column;
    }

    /** The tiles directly left and right of a tile, in the same row. */
    public static int[] neighbours(int tile) {
        int column = tile % COLUMNS;
        if (column == 0) {
            return new int[] {tile + 1};
        }
        if (column == COLUMNS - 1) {
            return new int[] {tile - 1};
        }
        return new int[] {tile - 1, tile + 1};
    }

    // Along a column, "line positions" 0..ROWS-1 go from side 0's back row, side 0's front row, side 1's front row
    // to side 1's back row.

    /** The direction (+1 / -1 in line positions) towards a side. */
    public static int directionTowards(int side) {
        return side == 1 ? 1 : -1;
    }

    static int linePosition(int tile) {
        int localRow = (tile % TILES_PER_SIDE) / COLUMNS;
        return sideOf(tile) == 0 ? ROWS_PER_SIDE - 1 - localRow : ROWS_PER_SIDE + localRow;
    }

    static int tileAtLine(int position, int column) {
        return position < ROWS_PER_SIDE
                ? tile(0, ROWS_PER_SIDE - 1 - position, column)
                : tile(1, position - ROWS_PER_SIDE, column);
    }
    //endregion

    //region [Queries]
    public Side side(int side) {
        return sides[side];
    }

    public @Nullable Unit unit(int tile) {
        return tile >= 0 && tile < BOARD_SIZE ? board[tile] : null;
    }

    public Phase phase() {
        return phase;
    }

    /** Whose turn it is (while playing). */
    public int current() {
        return current_player;
    }

    /** Who starts the current round. */
    public int starter() {
        return starter;
    }

    public int round() {
        return round;
    }

    public int points(int side) {
        return sides[side].points;
    }

    /** A side's power at the end of the previous round, or -1 in the first round. */
    public int lastRoundPower(int side) {
        return lastRoundPower == null ? -1 : lastRoundPower[side];
    }

    /** The winning side once the game is over (by points), or -1 for a draw. */
    public int winner() {
        int difference = sides[0].points - sides[1].points;
        return difference > 0 ? 0 : difference < 0 ? 1 : -1;
    }

    /** The game history, oldest first. */
    public List<LogEntry> log() {
        return Collections.unmodifiableList(log);
    }

    /** The effect waiting for its owner to pick a target, or null. */
    public @Nullable Effect pendingEffect() {
        QueuedEffect head = queue.peekFirst();
        return head == null ? null : head.effect();
    }

    /** Who has to pick the pending effect's target, or -1. */
    public int pendingOwner() {
        QueuedEffect head = queue.peekFirst();
        return head == null ? -1 : head.owner();
    }

    public @Nullable CardType selectedCard(int side) {
        Side s = sides[side];
        return s.selectedHandIndex < 0 ? null : s.hand.get(s.selectedHandIndex);
    }

    /** Total power of a side's monsters. */
    public int power(int side) {
        int total = 0;
        for (int tile = side * TILES_PER_SIDE; tile < (side + 1) * TILES_PER_SIDE; tile++) {
            if (board[tile] != null) {
                total += board[tile].power();
            }
        }
        return total;
    }

    /** Whether the game waits for this player to do something (mulligan, their turn, or pick a target). */
    public boolean awaits(int side) {
        return switch (phase) {
            case MULLIGAN -> !sides[side].mulliganDone;
            case PLAYING -> pendingEffect() != null ? pendingOwner() == side : current_player == side;
            case OVER -> false;
        };
    }

    /** The tile a unit is on, or -1 if it is not on the board. */
    public int tileOf(Unit unit) {
        for (int tile = 0; tile < BOARD_SIZE; tile++) {
            if (board[tile] == unit) {
                return tile;
            }
        }
        return -1;
    }

    /** Whether {@code player} may put this monster on the tile: empty, and their side (or either, if treacherous). */
    public boolean canPlay(int player, CardType monster, int tile) {
        return monster.isMonster() && tile >= 0 && tile < BOARD_SIZE && board[tile] == null
                && (sideOf(tile) == player || monster.hasKeyword(Keyword.TREACHEROUS));
    }

    /** Whether the unit on the tile can be moved one row in the direction (pushing the units in front of it). */
    /**
     * The tile one step from {@code tile}: {@code lineStep} along its column in line positions (towards a side, see
     * {@link #directionTowards}), {@code columnStep} along its row (-1 = left, +1 = right). -1 if that is off the
     * board.
     */
    public static int step(int tile, int lineStep, int columnStep) {
        int position = linePosition(tile) + lineStep;
        int column = tile % COLUMNS + columnStep;
        if (position < 0 || position >= ROWS || column < 0 || column >= COLUMNS) {
            return -1;
        }
        return tileAtLine(position, column);
    }

    /** Whether the unit on the tile can move one step (see {@link #step}); units in the way would be pushed along. */
    public boolean canShift(int tile, int lineStep, int columnStep) {
        return unit(tile) != null && shiftEnd(tile, lineStep, columnStep) >= 0;
    }

    /**
     * The first empty tile from {@code tile} in a direction (the units in between get pushed there), or -1 if the
     * chain of units would be pushed off the board.
     */
    int shiftEnd(int tile, int lineStep, int columnStep) {
        for (int t = step(tile, lineStep, columnStep); t >= 0; t = step(t, lineStep, columnStep)) {
            if (board[t] == null) {
                return t;
            }
        }
        return -1;
    }

    public boolean pileContains(int side, Pile pile, CardType card) {
        return sides[side].pile(pile).contains(card);
    }
    //endregion
}
