package com.rutaji.changingitems.cards;

import com.rutaji.changingitems.cards.effect.Effect;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * State of one card game. Board index = row * COLUMNS + column.
 *
 * Effects go into a FIFO queue and resolve one by one. Effects without a valid target are cancelled; a targeted
 * effect stops the queue until the player picks a tile ({@link #pendingEffect()}). While an effect is pending,
 * the player cannot do anything else.
 */
public final class GameState {
    public static final int HAND_SIZE = 9;
    public static final int ROWS = 2;
    public static final int COLUMNS = 5;
    public static final int BOARD_SIZE = ROWS * COLUMNS;
    /** Guard against effects that keep triggering each other forever. */
    private static final int MAX_RESOLVE_STEPS = 1000;

    private final @Nullable CardType[] hand;
    private final @Nullable Unit[] board = new Unit[BOARD_SIZE];
    private final Deque<Effect> queue = new ArrayDeque<>();
    private int selectedHandIndex = -1;

    public GameState(CardType[] hand) {
        if (hand.length != HAND_SIZE) {
            throw new IllegalArgumentException("Hand must have " + HAND_SIZE + " cards");
        }
        this.hand = hand.clone();
    }

    public @Nullable CardType handCard(int index) {
        return hand[index];
    }

    public @Nullable Unit unit(int tile) {
        return tile >= 0 && tile < BOARD_SIZE ? board[tile] : null;
    }

    public int selectedHandIndex() {
        return selectedHandIndex;
    }

    public @Nullable CardType selectedCard() {
        return selectedHandIndex < 0 ? null : hand[selectedHandIndex];
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

    /** Selects a card in hand; clicking the selected card again deselects it. Ignored while an effect is pending. */
    public void selectHand(int index) {
        if (pendingEffect() != null || index < 0 || index >= HAND_SIZE || hand[index] == null) {
            return;
        }
        selectedHandIndex = selectedHandIndex == index ? -1 : index;
    }

    /**
     * A click on a board tile: picks the target of the pending effect, or plays the selected card
     * (a monster on an empty tile, a spell on any tile).
     */
    public void clickTile(int tile) {
        if (tile < 0 || tile >= BOARD_SIZE) {
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
            queue.addAll(card.onPlay(null));
        }
        resolveQueue();
    }

    private void removeSelectedFromHand() {
        hand[selectedHandIndex] = null;
        selectedHandIndex = -1;
    }

    // ---- Used by effects ----

    /** Puts a monster on an empty tile and queues its on-play effects. */
    public void play(CardType monster, int tile) {
        if (!monster.isMonster() || board[tile] != null) {
            throw new IllegalStateException("Cannot play " + monster + " on tile " + tile);
        }
        Unit unit = new Unit(monster);
        board[tile] = unit;
        queue.addAll(monster.onPlay(unit));
    }

    /** Reduces a unit's power; below 1 it is destroyed. */
    public void damage(int tile, int amount) {
        Unit unit = board[tile];
        if (unit == null) {
            return;
        }
        unit.addPower(-amount);
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

    /** Removes a unit from the board and queues its on-death effects. */
    public void destroy(int tile) {
        Unit unit = board[tile];
        if (unit == null) {
            return;
        }
        board[tile] = null;
        queue.addAll(unit.type().onDeath(unit));
    }

    /** Adds effects to the end of the queue (they resolve after everything already queued). */
    public void queue(List<Effect> effects) {
        queue.addAll(effects);
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
                return;
            } else {
                queue.removeFirst();
                effect.resolve(this, -1);
            }
        }
    }
}
