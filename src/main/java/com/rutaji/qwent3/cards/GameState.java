package com.rutaji.qwent3.cards;

import com.rutaji.qwent3.cards.effect.Effect;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

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
 * side a monster was damaged / died. Effects without a valid target are cancelled; a targeted effect stops the queue
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

    /**
     * One line of the game history: who did it ({@code actor} 0 / 1, or -1 for neutral notes like round changes),
     * the text, and whether it is the card played from hand that turn.
     */
    public record LogEntry(int actor, String text, boolean played) {
    }

    private final Side[] sides;
    private final @Nullable Unit[] board = new Unit[BOARD_SIZE];
    private final Deque<QueuedEffect> queue = new ArrayDeque<>();
    private final List<LogEntry> log = new ArrayList<>();
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

    /** The game history, oldest first. */
    public List<LogEntry> log() {
        return Collections.unmodifiableList(log);
    }

    private void log(int actor, String text, boolean played) {
        log.add(new LogEntry(actor, text, played));
    }

    /** Logs an effect about to resolve (or being cancelled), with what it targets. */
    private void logEffect(Effect effect, int owner, int target, boolean cancelled) {
        String text = PlainTextComponentSerializer.plainText().serialize(effect.description());
        if (cancelled) {
            text += " - no target";
        } else if (target >= 0) {
            text += " -> " + targetName(effect, owner, target);
        } else {
            String detail = effect.logDetail(this, owner);
            if (detail != null) {
                text += " -> " + detail;
            }
        }
        log(owner, text, false);
    }

    private String targetName(Effect effect, int owner, int target) {
        if (effect.targetKind() == Effect.TargetKind.HAND) {
            return sides[owner].hand.get(target).displayName();
        }
        Unit unit = board[target];
        return unit != null ? unit.type().displayName() : "empty tile";
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

    /**
     * A click on a hand card: redraws it during the mulligan, picks it as the target of a pending hand-target effect
     * (by its owner), otherwise selects it (on your turn).
     */
    public void clickHand(int side, int index) {
        Side s = sides[side];
        if (index < 0 || index >= s.hand.size()) {
            return;
        }
        if (phase == Phase.MULLIGAN) {
            redraw(side, index);
        } else if (phase == Phase.PLAYING) {
            QueuedEffect pending = queue.peekFirst();
            if (pending != null) {
                pick(pending, Effect.TargetKind.HAND, side, index);
            } else if (current == side) {
                s.selectedHandIndex = s.selectedHandIndex == index ? -1 : index;
            }
        }
    }

    /** Resolves the pending effect with this target, if it is the owner's valid target of the right kind. */
    private void pick(QueuedEffect pending, Effect.TargetKind kind, int side, int target) {
        Effect effect = pending.effect();
        if (pending.owner() == side && effect.targetKind() == kind && effect.isValidTarget(this, side, target)) {
            queue.removeFirst();
            logEffect(effect, side, target, false);
            effect.resolve(this, side, target);
            resolveQueue();
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
            pick(pending, Effect.TargetKind.TILE, side, tile);
            return;
        }
        CardType card = selectedCard(side);
        if (current != side || card == null) {
            return;
        }
        if (card.isMonster()) {
            if (!canPlay(side, card, tile)) {
                return;
            }
            removeSelectedFromHand(side);
            log(side, card.displayName(), true);
            play(side, card, tile);
        } else {
            removeSelectedFromHand(side);
            log(side, card.displayName(), true);
            // A used spell goes to the graveyard right away; its effects then resolve from the queue,
            // followed by the opponent's units reacting to the spell.
            sides[side].graveyard.add(card);
            castSpell(side, card, false);
        }
        resolveQueue();
    }

    /**
     * Casts a spell for {@code side}: queues its effects, then the opponent's units' reactions to a spell. With
     * {@code next} the spell's effects go to the front of the queue (a spell played by an effect resolves right away)
     * and the cast is logged. The card itself is not moved anywhere here.
     */
    public void castSpell(int side, CardType spell, boolean next) {
        List<Effect> effects = spell.onPlay(null);
        if (next) {
            log(side, "Casts " + spell.displayName(), false);
            for (Effect effect : effects.reversed()) {
                queueNext(side, effect);
            }
        } else {
            queue(side, effects);
        }
        for (Unit unit : board) {
            if (unit != null && unit.owner() != side) {
                queue(unit.owner(), unit.type().onOpponentSpell(unit));
            }
        }
    }

    /** Removes the card that entered a side's graveyard last among the matching ones; null if there is none. */
    public @Nullable CardType banishLast(int side, Predicate<CardType> filter) {
        List<CardType> graveyard = sides[side].graveyard;
        for (int i = graveyard.size() - 1; i >= 0; i--) {
            if (filter.test(graveyard.get(i))) {
                return graveyard.remove(i);
            }
        }
        return null;
    }

    /**
     * Plays a hand card directly (used by the AI): like selecting it and clicking the tile. For a spell the tile does
     * not matter (any value). Ignored if it isn't that player's turn or the move is not allowed.
     */
    public void playCard(int side, int handIndex, int tile) {
        Side s = sides[side];
        if (phase != Phase.PLAYING || current != side || pendingEffect() != null || s.passed
                || handIndex < 0 || handIndex >= s.hand.size()) {
            return;
        }
        s.selectedHandIndex = handIndex;
        clickTile(side, s.hand.get(handIndex).isMonster() ? tile : 0);
        if (s.selectedHandIndex == handIndex && handIndex < s.hand.size()) {
            // Not played (illegal tile): don't leave the card selected.
            s.selectedHandIndex = -1;
        }
    }

    /** Ends the player's turns for the rest of the round (only on their turn, with no effect pending). */
    public void pass(int side) {
        if (phase != Phase.PLAYING || current != side || pendingEffect() != null) {
            return;
        }
        sides[side].passed = true;
        sides[side].selectedHandIndex = -1;
        log(side, "Passed", false);
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
        log(-1, "--- Round " + round + " ---", false);
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

    /** Moves a hand card to the graveyard. */
    public void discard(int side, int index) {
        Side s = sides[side];
        s.graveyard.add(s.hand.remove(index));
    }

    /**
     * Draws the top-most deck card that matches (with a full hand it goes to the graveyard).
     * False if the deck has no such card.
     */
    public boolean drawFirst(int side, Predicate<CardType> filter) {
        Side s = sides[side];
        for (int i = 0; i < s.deck.size(); i++) {
            if (filter.test(s.deck.get(i))) {
                CardType card = s.deck.remove(i);
                if (s.hand.size() >= MAX_HAND) {
                    s.graveyard.add(card);
                } else {
                    s.hand.add(card);
                }
                return true;
            }
        }
        return false;
    }

    /** Puts a card on top of a side's deck. */
    public void putOnTopOfDeck(int side, CardType card) {
        sides[side].deck.addFirst(card);
    }

    /** Moves every copy of a card from a side's deck to their graveyard; returns how many. */
    public int discardFromDeck(int side, CardType card) {
        Side s = sides[side];
        int moved = 0;
        while (s.deck.remove(card)) {
            s.graveyard.add(card);
            moved++;
        }
        return moved;
    }

    /** Swaps a side's deck and graveyard, keeping both orders (the first graveyard card becomes the top card). */
    public void swapDeckAndGraveyard(int side) {
        Side s = sides[side];
        List<CardType> oldDeck = new ArrayList<>(s.deck);
        s.deck.clear();
        s.deck.addAll(s.graveyard);
        s.graveyard.clear();
        s.graveyard.addAll(oldDeck);
    }

    // ---- Moving units (pistons) ----
    // Along a column, "line positions" 0..ROWS-1 go from side 0's back row, side 0's front row, side 1's front row
    // to side 1's back row.

    /** The direction (+1 / -1 in line positions) towards a side. */
    public static int directionTowards(int side) {
        return side == 1 ? 1 : -1;
    }

    private static int linePosition(int tile) {
        int localRow = (tile % TILES_PER_SIDE) / COLUMNS;
        return sideOf(tile) == 0 ? ROWS_PER_SIDE - 1 - localRow : ROWS_PER_SIDE + localRow;
    }

    private static int tileAtLine(int position, int column) {
        return position < ROWS_PER_SIDE
                ? tile(0, ROWS_PER_SIDE - 1 - position, column)
                : tile(1, position - ROWS_PER_SIDE, column);
    }

    /**
     * The line position of the first empty tile from {@code tile} in a direction (the units in between get pushed
     * there), or -1 if the chain of units would be pushed off the board.
     */
    private int shiftEnd(int tile, int direction) {
        int column = tile % COLUMNS;
        for (int position = linePosition(tile) + direction; position >= 0 && position < ROWS; position += direction) {
            if (board[tileAtLine(position, column)] == null) {
                return position;
            }
        }
        return -1;
    }

    /** Whether the unit on the tile can be moved one row in the direction (pushing the units in front of it). */
    public boolean canShift(int tile, int direction) {
        return unit(tile) != null && shiftEnd(tile, direction) >= 0;
    }

    /**
     * Moves the unit one row in the direction; units in the way are pushed along. Units that end on the other
     * side belong to that side.
     */
    public void shift(int tile, int direction) {
        if (!canShift(tile, direction)) {
            return;
        }
        int column = tile % COLUMNS;
        int start = linePosition(tile);
        for (int position = shiftEnd(tile, direction); position != start; position -= direction) {
            int to = tileAtLine(position, column);
            int from = tileAtLine(position - direction, column);
            Unit unit = board[from];
            board[from] = null;
            board[to] = unit;
            unit.moveTo(to);
        }
    }

    /** Shuffles a card into a side's deck at a random position. */
    public void shuffleIntoDeck(int side, CardType card) {
        List<CardType> deck = sides[side].deck;
        deck.add(ThreadLocalRandom.current().nextInt(deck.size() + 1), card);
    }

    public void addToGraveyard(int side, CardType card) {
        sides[side].graveyard.add(card);
    }

    /** Removes a random matching card from a side's graveyard for good; null if there is none. */
    public @Nullable CardType banishRandom(int side, Predicate<CardType> filter) {
        List<CardType> graveyard = sides[side].graveyard;
        List<Integer> matching = new ArrayList<>();
        for (int i = 0; i < graveyard.size(); i++) {
            if (filter.test(graveyard.get(i))) {
                matching.add(i);
            }
        }
        if (matching.isEmpty()) {
            return null;
        }
        int index = matching.get(ThreadLocalRandom.current().nextInt(matching.size()));
        return graveyard.remove(index);
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

    /**
     * Puts a monster on an empty tile. The unit belongs to the side of the tile (a treacherous monster on the
     * opponent's side is the opponent's unit); its on-play effects belong to {@code player}, who played it.
     */
    public void play(int player, CardType monster, int tile) {
        if (!canPlay(player, monster, tile)) {
            throw new IllegalStateException("Cannot play " + monster + " on tile " + tile + " for side " + player);
        }
        Unit unit = new Unit(monster, sideOf(tile), tile);
        board[tile] = unit;
        queue(player, monster.onPlay(unit));
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

    /** Draws the top card of {@code deckSide}'s deck into {@code toSide}'s hand (full hand: their graveyard). */
    public void drawFromDeckOf(int deckSide, int toSide) {
        Side from = sides[deckSide];
        if (from.deck.isEmpty()) {
            return;
        }
        CardType card = from.deck.remove(0);
        Side to = sides[toSide];
        if (to.hand.size() >= MAX_HAND) {
            to.graveyard.add(card);
        } else {
            to.hand.add(card);
        }
    }

    /**
     * Reduces a unit's power. Below 1 power it is destroyed (on-death effects); otherwise its
     * "damaged and survives" effects are queued.
     */
    public void damage(int tile, int amount) {
        Unit unit = board[tile];
        if (unit == null || amount <= 0) {
            return;
        }
        unit.addPower(-amount);
        if (unit.power() < 1) {
            destroy(tile);
        } else {
            queue(unit.owner(), unit.type().onDamagedSurvive(unit, amount));
        }
    }

    public void setPower(int tile, int power) {
        Unit unit = board[tile];
        if (unit != null) {
            unit.addPower(power - unit.power());
        }
    }

    /** Removes a random matching card from a side's deck; null if there is none. */
    public @Nullable CardType takeRandomFromDeck(int side, Predicate<CardType> filter) {
        List<CardType> deck = sides[side].deck;
        List<Integer> matching = new ArrayList<>();
        for (int i = 0; i < deck.size(); i++) {
            if (filter.test(deck.get(i))) {
                matching.add(i);
            }
        }
        if (matching.isEmpty()) {
            return null;
        }
        return deck.remove((int) matching.get(ThreadLocalRandom.current().nextInt(matching.size())));
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
                logEffect(head.effect(), head.owner(), -1, true);
            } else if (head.effect().needsTarget()) {
                return;
            } else {
                queue.removeFirst();
                logEffect(head.effect(), head.owner(), -1, false);
                head.effect().resolve(this, head.owner(), -1);
            }
        }
        nextTurn();
    }
}
