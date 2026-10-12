package com.rutaji.qwent3.cards;

import com.rutaji.qwent3.cards.effect.Effect;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * State of one game between two players, side 0 and side 1. Knows nothing about Bukkit players.
 *
 * Board: {@link #ROWS} rows of {@link #COLUMNS}; each side owns {@link #ROWS_PER_SIDE} rows. Tile index =
 * side * TILES_PER_SIDE + localRow * COLUMNS + column, local row 0 being the front row (towards the opponent).
 *
 * Flow: both players mulligan at the same time; then players take turns, playing one card per turn. A player can
 * pass, which ends their turns for the rest of the round; the other keeps playing. When both have passed the round
 * ends: the higher total power gets a point (equal = both get one) and the board is cleared into the graveyards
 * without on-death effects. The first to {@link #POINTS_TO_WIN} points wins (both at once = draw). Otherwise each
 * player draws {@link #ROUND_DRAW} card and mulligans {@link #ROUND_REDRAWS}, and the next round starts with the
 * other player than the previous one.
 *
 * Effects go into one FIFO queue, each with an owner: the player who played the card, or the player on whose
 * side a monster was hurt / died. Effects without a valid target are cancelled; a targeted effect stops the queue
 * until its owner picks a tile. The turn passes only when the queue is empty.
 */
public final class GameState {
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
    /** Guard against effects that keep triggering each other forever. */
    private static final int MAX_RESOLVE_STEPS = 1000;

    public enum Phase {
        MULLIGAN,
        PLAYING,
        OVER
    }

    private record QueuedEffect(Effect effect, int owner) {
    }

    private final Side[] sides;
    private final @Nullable Unit[] board = new Unit[BOARD_SIZE];
    private final Deque<QueuedEffect> queue = new ArrayDeque<>();
    private final int[] points = new int[2];
    private int round = 1;
    /** Power of both sides at the end of the previous round, or null in the first round. */
    private int @Nullable [] lastRoundPower;
    /** Who starts the current round. */
    private int starter;
    private Phase phase = Phase.PLAYING;
    private int current;

    /** @param deck0 side 0's deck, top card first (already shuffled); copied. Same for deck1. */
    public GameState(List<CardType> deck0, List<CardType> deck1, int firstPlayer) {
        this.sides = new Side[] {new Side(deck0), new Side(deck1)};
        this.starter = firstPlayer;
        this.current = firstPlayer;
    }

    /** Both draw the starting hand and start their mulligans; the first player is random. */
    public static GameState start(List<CardType> deck0, List<CardType> deck1) {
        GameState state = new GameState(deck0, deck1, ThreadLocalRandom.current().nextInt(2));
        for (int side = 0; side < 2; side++) {
            state.draw(side, STARTING_HAND);
        }
        state.beginMulligan(MULLIGAN_REDRAWS);
        return state;
    }

    public static int other(int side) {
        return 1 - side;
    }

    public static int sideOf(int tile) {
        return tile / TILES_PER_SIDE;
    }

    public static int tile(int side, int localRow, int column) {
        return side * TILES_PER_SIDE + localRow * COLUMNS + column;
    }

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
        return current;
    }

    /** Who starts the current round. */
    public int starter() {
        return starter;
    }

    public int round() {
        return round;
    }

    public int points(int side) {
        return points[side];
    }

    /** A side's power at the end of the previous round, or -1 in the first round. */
    public int lastRoundPower(int side) {
        return lastRoundPower == null ? -1 : lastRoundPower[side];
    }

    /** The winning side once the game is over (by points), or -1 for a draw. */
    public int winner() {
        return points[0] > points[1] ? 0 : points[1] > points[0] ? 1 : -1;
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
            case PLAYING -> pendingEffect() != null ? pendingOwner() == side : current == side;
            case OVER -> false;
        };
    }

    // ---- Player input ----

    /** A click on a hand card: redraws it during the mulligan, otherwise selects it (on your turn). */
    public void clickHand(int side, int index) {
        Side s = sides[side];
        if (index < 0 || index >= s.hand.size()) {
            return;
        }
        if (phase == Phase.MULLIGAN) {
            redraw(side, index);
        } else if (phase == Phase.PLAYING && current == side && pendingEffect() == null) {
            s.selectedHandIndex = s.selectedHandIndex == index ? -1 : index;
        }
    }

    /** Starts the mulligan phase for both players (called after the starting hands are drawn). */
    public void beginMulligan(int redraws) {
        if (redraws <= 0) {
            return;
        }
        phase = Phase.MULLIGAN;
        for (Side s : sides) {
            s.redrawsLeft = redraws;
            s.mulliganDone = false;
            s.redrawn.clear();
        }
    }

    /** Ends a player's mulligan ("keep hand", or no redraws left). The game starts when both are done. */
    public void endMulligan(int side) {
        if (phase != Phase.MULLIGAN) {
            return;
        }
        Side s = sides[side];
        s.mulliganDone = true;
        s.redrawsLeft = 0;
        s.redrawn.clear();
        if (sides[0].mulliganDone && sides[1].mulliganDone) {
            phase = Phase.PLAYING;
            current = starter;
        }
    }

    /**
     * Mulligan: shuffles a hand card back into the deck and puts a new card in its place. The new card is the
     * top-most card whose type was not redrawn in this mulligan; if there is none, simply the top card.
     */
    private void redraw(int side, int index) {
        Side s = sides[side];
        if (s.mulliganDone || s.redrawsLeft <= 0) {
            return;
        }
        CardType returned = s.hand.get(index);
        s.redrawn.add(returned);
        s.deck.add(ThreadLocalRandom.current().nextInt(s.deck.size() + 1), returned);
        int pick = 0;
        for (int i = 0; i < s.deck.size(); i++) {
            if (!s.redrawn.contains(s.deck.get(i))) {
                pick = i;
                break;
            }
        }
        s.hand.set(index, s.deck.remove(pick));
        if (--s.redrawsLeft == 0) {
            endMulligan(side);
        }
    }

    /**
     * A click on a board tile: picks the target of a pending effect (by its owner), or plays the selected card
     * on your turn (a monster on an empty tile of your side, a spell on any tile).
     */
    public void clickTile(int side, int tile) {
        if (phase != Phase.PLAYING || tile < 0 || tile >= BOARD_SIZE) {
            return;
        }
        QueuedEffect pending = queue.peekFirst();
        if (pending != null) {
            if (pending.owner() == side && pending.effect().isValidTarget(this, side, tile)) {
                queue.removeFirst();
                pending.effect().resolve(this, side, tile);
                resolveQueue();
            }
            return;
        }
        CardType card = selectedCard(side);
        if (current != side || card == null) {
            return;
        }
        if (card.isMonster()) {
            if (board[tile] != null || sideOf(tile) != side) {
                return;
            }
            removeSelectedFromHand(side);
            play(side, card, tile);
        } else {
            removeSelectedFromHand(side);
            // A used spell goes to the graveyard right away; its effects then resolve from the queue.
            sides[side].graveyard.add(card);
            queue(side, card.onPlay(null));
        }
        resolveQueue();
    }

    /** Ends the player's turns for the rest of the round (only on their turn, with no effect pending). */
    public void pass(int side) {
        if (phase != Phase.PLAYING || current != side || pendingEffect() != null) {
            return;
        }
        sides[side].passed = true;
        sides[side].selectedHandIndex = -1;
        nextTurn();
    }

    /** Ends the game now (e.g. surrender); the winner is decided by the caller. */
    public void end() {
        phase = Phase.OVER;
        queue.clear();
    }

    private void removeSelectedFromHand(int side) {
        Side s = sides[side];
        s.hand.remove(s.selectedHandIndex);
        s.selectedHandIndex = -1;
    }

    /** After a card is fully resolved: the other player's turn, unless they passed. Both passed = end of round. */
    private void nextTurn() {
        if (sides[0].passed && sides[1].passed) {
            endRound();
        } else if (!sides[other(current)].passed) {
            current = other(current);
        }
    }

    private void endRound() {
        lastRoundPower = new int[] {power(0), power(1)};
        if (lastRoundPower[0] >= lastRoundPower[1]) {
            points[0]++;
        }
        if (lastRoundPower[1] >= lastRoundPower[0]) {
            points[1]++;
        }
        // Clear the board; no on-death effects.
        for (int tile = 0; tile < BOARD_SIZE; tile++) {
            Unit unit = board[tile];
            if (unit != null) {
                sides[unit.owner()].graveyard.add(unit.type());
                board[tile] = null;
            }
        }
        if (points[0] >= POINTS_TO_WIN || points[1] >= POINTS_TO_WIN) {
            phase = Phase.OVER;
            return;
        }
        round++;
        starter = other(starter);
        for (int side = 0; side < 2; side++) {
            sides[side].passed = false;
            sides[side].selectedHandIndex = -1;
            draw(side, ROUND_DRAW);
        }
        beginMulligan(ROUND_REDRAWS);
    }

    // ---- Used by effects (and game setup) ----

    /** Draws cards from the top of the deck. With a full hand the card is discarded to the graveyard. */
    public void draw(int side, int count) {
        Side s = sides[side];
        for (int i = 0; i < count && !s.deck.isEmpty(); i++) {
            CardType card = s.deck.remove(0);
            if (s.hand.size() >= MAX_HAND) {
                s.graveyard.add(card);
            } else {
                s.hand.add(card);
            }
        }
    }

    /** Puts a monster on an empty tile of the owner's side and queues its on-play effects. */
    public void play(int owner, CardType monster, int tile) {
        if (!monster.isMonster() || board[tile] != null || sideOf(tile) != owner) {
            throw new IllegalStateException("Cannot play " + monster + " on tile " + tile + " for side " + owner);
        }
        Unit unit = new Unit(monster, owner);
        board[tile] = unit;
        queue(owner, monster.onPlay(unit));
    }

    /** Reduces a unit's power and queues its on-hurt effects; below 1 power it is destroyed (on-death after). */
    public void damage(int tile, int amount) {
        Unit unit = board[tile];
        if (unit == null || amount <= 0) {
            return;
        }
        unit.addPower(-amount);
        queue(unit.owner(), unit.type().onHurt(unit, amount));
        if (unit.power() < 1) {
            destroy(tile);
        }
    }

    public void buff(int tile, int amount) {
        Unit unit = board[tile];
        if (unit != null) {
            unit.addPower(amount);
        }
    }

    /** Removes a unit (its card goes to its owner's graveyard) and queues its on-death effects for its owner. */
    public void destroy(int tile) {
        Unit unit = board[tile];
        if (unit == null) {
            return;
        }
        board[tile] = null;
        sides[unit.owner()].graveyard.add(unit.type());
        queue(unit.owner(), unit.type().onDeath(unit));
    }

    /** Adds effects owned by {@code owner} to the end of the queue. */
    public void queue(int owner, List<Effect> effects) {
        for (Effect effect : effects) {
            queue.addLast(new QueuedEffect(effect, owner));
        }
    }

    /** Puts an effect at the front of the queue, e.g. an effect that repeats itself ("play all ..."). */
    public void queueNext(int owner, Effect effect) {
        queue.addFirst(new QueuedEffect(effect, owner));
    }

    public boolean pileContains(int side, Pile pile, CardType card) {
        return sides[side].pile(pile).contains(card);
    }

    /** Takes one copy of a card out of a side's deck or graveyard; false if there is none. */
    public boolean removeFromPile(int side, Pile pile, CardType card) {
        return sides[side].pile(pile).remove(card);
    }

    /** Resolves queued effects in order until one needs a target; when the queue is empty, the turn ends. */
    private void resolveQueue() {
        int steps = 0;
        while (!queue.isEmpty()) {
            if (++steps > MAX_RESOLVE_STEPS) {
                queue.clear();
                break;
            }
            QueuedEffect head = queue.peekFirst();
            if (!head.effect().hasValidTargets(this, head.owner())) {
                queue.removeFirst();
            } else if (head.effect().needsTarget()) {
                return;
            } else {
                queue.removeFirst();
                head.effect().resolve(this, head.owner(), -1);
            }
        }
        nextTurn();
    }
}
