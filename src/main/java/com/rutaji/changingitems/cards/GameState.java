package com.rutaji.changingitems.cards;

import com.rutaji.changingitems.cards.effect.Effect;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * State of one card game. Board index = row * COLUMNS + column.
 *
 * Cards move between the deck (a copy made for this game; top = index 0), the hand (at most {@link #MAX_HAND}),
 * the board (as {@link Unit}s) and the graveyard. Destroyed units and used spells go to the graveyard as plain cards.
 *
 * A game starts in the {@link Phase#MULLIGAN} phase: the player may redraw a few cards. A redrawn card's type cannot
 * be drawn again during the mulligan, unless the deck has nothing else.
 *
 * Effects go into a FIFO queue and resolve one by one. Effects without a valid target are cancelled; a targeted
 * effect stops the queue until the player picks a tile ({@link #pendingEffect()}). While an effect is pending,
 * the player cannot do anything else.
 */
public final class GameState {
    public static final int STARTING_HAND = 9;
    public static final int MAX_HAND = 10;
    public static final int MULLIGAN_REDRAWS = 2;

    public enum Phase {
        MULLIGAN,
        PLAYING
    }
    public static final int ROWS = 2;
    public static final int COLUMNS = 5;
    public static final int BOARD_SIZE = ROWS * COLUMNS;
    /** Guard against effects that keep triggering each other forever. */
    private static final int MAX_RESOLVE_STEPS = 1000;

    private final List<CardType> deck;
    private final List<CardType> hand = new ArrayList<>();
    private final List<CardType> graveyard = new ArrayList<>();
    private final @Nullable Unit[] board = new Unit[BOARD_SIZE];
    private final Deque<Effect> queue = new ArrayDeque<>();
    private int selectedHandIndex = -1;
    private Phase phase = Phase.PLAYING;
    private int redrawsLeft;
    /** Card types redrawn during the mulligan; they are not drawn again while it lasts. */
    private final Set<CardType> redrawn = EnumSet.noneOf(CardType.class);

    /** @param deck this game's deck, top card first (already shuffled); it is copied */
    public GameState(List<CardType> deck) {
        this.deck = new ArrayList<>(deck);
    }

    public int handSize() {
        return hand.size();
    }

    public CardType handCard(int index) {
        return hand.get(index);
    }

    public int deckSize() {
        return deck.size();
    }

    /** The deck in draw order (top first). Don't show this order to the player. */
    public List<CardType> deck() {
        return Collections.unmodifiableList(deck);
    }

    public List<CardType> graveyard() {
        return Collections.unmodifiableList(graveyard);
    }

    public @Nullable Unit unit(int tile) {
        return tile >= 0 && tile < BOARD_SIZE ? board[tile] : null;
    }

    public Phase phase() {
        return phase;
    }

    public int redrawsLeft() {
        return redrawsLeft;
    }

    public int selectedHandIndex() {
        return selectedHandIndex;
    }

    public @Nullable CardType selectedCard() {
        return selectedHandIndex < 0 ? null : hand.get(selectedHandIndex);
    }

    /** The effect waiting for the player to pick a target, or null. */
    public @Nullable Effect pendingEffect() {
        return queue.peekFirst();
    }

    public int totalPower() {
        int total = 0;
        for (Unit unit : board) {
            if (unit != null) {
                total += unit.power();
            }
        }
        return total;
    }

    // ---- Player input ----

    /** A click on a hand card: redraws it during the mulligan, otherwise selects it. */
    public void clickHand(int index) {
        if (phase == Phase.MULLIGAN) {
            redraw(index);
        } else {
            selectHand(index);
        }
    }

    /** Starts the mulligan phase (called after the starting hand is drawn). */
    public void beginMulligan(int redraws) {
        phase = redraws > 0 ? Phase.MULLIGAN : Phase.PLAYING;
        redrawsLeft = redraws;
        redrawn.clear();
    }

    /** Ends the mulligan; also happens on its own when no redraws are left. */
    public void endMulligan() {
        phase = Phase.PLAYING;
        redrawsLeft = 0;
        redrawn.clear();
    }

    /**
     * Mulligan: shuffles a hand card back into the deck and puts a new card in its place. The new card is the
     * top-most card whose type was not redrawn in this mulligan; if there is none, simply the top card.
     */
    private void redraw(int index) {
        if (redrawsLeft <= 0 || index < 0 || index >= hand.size()) {
            return;
        }
        CardType returned = hand.get(index);
        redrawn.add(returned);
        deck.add(ThreadLocalRandom.current().nextInt(deck.size() + 1), returned);
        int pick = 0;
        for (int i = 0; i < deck.size(); i++) {
            if (!redrawn.contains(deck.get(i))) {
                pick = i;
                break;
            }
        }
        hand.set(index, deck.remove(pick));
        if (--redrawsLeft == 0) {
            endMulligan();
        }
    }

    /** Selects a card in hand; clicking the selected card again deselects it. Ignored while an effect is pending. */
    public void selectHand(int index) {
        if (phase != Phase.PLAYING || pendingEffect() != null || index < 0 || index >= hand.size()) {
            return;
        }
        selectedHandIndex = selectedHandIndex == index ? -1 : index;
    }

    /**
     * A click on a board tile: picks the target of the pending effect, or plays the selected card
     * (a monster on an empty tile, a spell on any tile).
     */
    public void clickTile(int tile) {
        if (phase != Phase.PLAYING || tile < 0 || tile >= BOARD_SIZE) {
            return;
        }
        Effect pending = pendingEffect();
        if (pending != null) {
            if (pending.isValidTarget(this, tile)) {
                queue.removeFirst();
                pending.resolve(this, tile);
                resolveQueue();
            }
            return;
        }
        CardType card = selectedCard();
        if (card == null) {
            return;
        }
        if (card.isMonster()) {
            if (board[tile] != null) {
                return;
            }
            removeSelectedFromHand();
            play(card, tile);
        } else {
            removeSelectedFromHand();
            // A used spell goes to the graveyard right away; its effects then resolve from the queue.
            graveyard.add(card);
            queue.addAll(card.onPlay(null));
        }
        resolveQueue();
    }

    private void removeSelectedFromHand() {
        hand.remove(selectedHandIndex);
        selectedHandIndex = -1;
    }

    // ---- Used by effects (and game setup) ----

    /** Draws cards from the top of the deck. With a full hand the card is discarded to the graveyard. */
    public void draw(int count) {
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            CardType card = deck.remove(0);
            if (hand.size() >= MAX_HAND) {
                graveyard.add(card);
            } else {
                hand.add(card);
            }
        }
    }

    /** Puts a monster on an empty tile and queues its on-play effects. */
    public void play(CardType monster, int tile) {
        if (!monster.isMonster() || board[tile] != null) {
            throw new IllegalStateException("Cannot play " + monster + " on tile " + tile);
        }
        Unit unit = new Unit(monster);
        board[tile] = unit;
        queue.addAll(monster.onPlay(unit));
    }

    /** Reduces a unit's power and queues its on-hurt effects; below 1 power it is destroyed (on-death after). */
    public void damage(int tile, int amount) {
        Unit unit = board[tile];
        if (unit == null || amount <= 0) {
            return;
        }
        unit.addPower(-amount);
        queue.addAll(unit.type().onHurt(unit, amount));
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

    /** Removes a unit from the board (its card goes to the graveyard) and queues its on-death effects. */
    public void destroy(int tile) {
        Unit unit = board[tile];
        if (unit == null) {
            return;
        }
        board[tile] = null;
        graveyard.add(unit.type());
        queue.addAll(unit.type().onDeath(unit));
    }

    /** Adds effects to the end of the queue (they resolve after everything already queued). */
    public void queue(List<Effect> effects) {
        queue.addAll(effects);
    }

    /** Puts an effect at the front of the queue, e.g. an effect that repeats itself ("play all ..."). */
    public void queueNext(Effect effect) {
        queue.addFirst(effect);
    }

    public boolean pileContains(Pile pile, CardType card) {
        return pileList(pile).contains(card);
    }

    /** Takes one copy of a card out of the deck or graveyard; false if there is none. */
    public boolean removeFromPile(Pile pile, CardType card) {
        return pileList(pile).remove(card);
    }

    private List<CardType> pileList(Pile pile) {
        return pile == Pile.DECK ? deck : graveyard;
    }

    /** The only valid target tile, or -1 if there are none or several. */
    private int singleValidTarget(Effect effect) {
        int found = -1;
        for (int tile = 0; tile < BOARD_SIZE; tile++) {
            if (effect.isValidTarget(this, tile)) {
                if (found >= 0) {
                    return -1;
                }
                found = tile;
            }
        }
        return found;
    }

    /** Resolves queued effects in order until one needs the player to pick a target. */
    private void resolveQueue() {
        int steps = 0;
        while (!queue.isEmpty()) {
            if (++steps > MAX_RESOLVE_STEPS) {
                queue.clear();
                return;
            }
            Effect effect = queue.peekFirst();
            if (!effect.hasValidTargets(this)) {
                queue.removeFirst();
            } else if (effect.needsTarget()) {
                int only = effect.autoPickSingleTarget() ? singleValidTarget(effect) : -1;
                if (only < 0) {
                    return;
                }
                queue.removeFirst();
                effect.resolve(this, only);
            } else {
                queue.removeFirst();
                effect.resolve(this, -1);
            }
        }
    }
}
