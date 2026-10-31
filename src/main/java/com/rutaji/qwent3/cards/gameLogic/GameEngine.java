package com.rutaji.qwent3.cards.gameLogic;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.CardFilter;
import com.rutaji.qwent3.cards.gameLogic.GameState.LogEntry;
import com.rutaji.qwent3.cards.gameLogic.GameState.Phase;
import com.rutaji.qwent3.cards.gameLogic.GameState.QueuedEffect;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static com.rutaji.qwent3.cards.gameLogic.GameState.BOARD_SIZE;
import static com.rutaji.qwent3.cards.gameLogic.GameState.COLUMNS;
import static com.rutaji.qwent3.cards.gameLogic.GameState.MAX_HAND;
import static com.rutaji.qwent3.cards.gameLogic.GameState.MULLIGAN_REDRAWS;
import static com.rutaji.qwent3.cards.gameLogic.GameState.POINTS_TO_WIN;
import static com.rutaji.qwent3.cards.gameLogic.GameState.ROUND_DRAW;
import static com.rutaji.qwent3.cards.gameLogic.GameState.ROUND_REDRAWS;
import static com.rutaji.qwent3.cards.gameLogic.GameState.STARTING_HAND;
import static com.rutaji.qwent3.cards.gameLogic.GameState.other;

/**
 * The rules of the game: the only class that changes a {@link GameState}. Player input and the operations effects
 * use go through here.
 *
 * Flow: both players mulligan at the same time; then players take turns, playing one card per turn. A player can
 * pass, which ends their turns for the rest of the round; the other keeps playing. When both have passed the round
 * ends: the higher total power gets a point (equal = both get one) and the board is cleared into the graveyards
 * without on-death effects. The first to {@link GameState#POINTS_TO_WIN} points wins (both at once = draw).
 * Otherwise each player draws {@link GameState#ROUND_DRAW} card and mulligans {@link GameState#ROUND_REDRAWS}, and
 * the next round starts with the other player than the previous one.
 *
 * Effects go into one FIFO queue, each with an owner: the player who played the card, or the player on whose
 * side a monster was damaged / died. Effects without a valid target are cancelled; a targeted effect stops the queue
 * until its owner picks a tile. The turn passes only when the queue is empty.
 */
public final class GameEngine {
    /** Guard against effects that keep triggering each other forever. */
    private static final int MAX_RESOLVE_STEPS = 1000;

    private final GameState state;

    /** @param deck0 side 0's deck, top card first (already shuffled); copied. Same for deck1. */
    public GameEngine(List<CardType> deck0, List<CardType> deck1, int firstPlayer) {
        this.state = new GameState(deck0, deck1, firstPlayer);
    }

    /**
     * Starts a game with shuffled copies of both players' decks (the given lists are not changed): both draw the
     * starting hand and start their mulligans; the first player is random.
     */
    public static GameEngine start(List<CardType> deck0, List<CardType> deck1) {
        GameEngine engine = new GameEngine(shuffled(deck0), shuffled(deck1), ThreadLocalRandom.current().nextInt(2));
        for (int side = 0; side < 2; side++) {
            engine.draw(side, STARTING_HAND);
        }
        engine.beginMulligan(MULLIGAN_REDRAWS);
        return engine;
    }

    private static List<CardType> shuffled(List<CardType> cards) {
        List<CardType> deck = new ArrayList<>(cards);
        Collections.shuffle(deck);
        return deck;
    }

    /** The game's data, for reading. */
    public GameState state() {
        return state;
    }

    //region [History]
    private void log(int actor, String text, boolean played) {
        state.log.add(new LogEntry(actor, text, played));
    }

    /** Logs an effect about to resolve (or being cancelled), with what it targets. */
    private void logEffect(Effect effect, int owner, int target, boolean cancelled) {
        String text = PlainTextComponentSerializer.plainText().serialize(effect.description());
        if (cancelled) {
            text += " - no target";
        } else if (target >= 0) {
            text += " -> " + targetName(effect, owner, target);
        } else {
            String detail = effect.logDetail(state, owner);
            if (detail != null) {
                text += " -> " + detail;
            }
        }
        log(owner, text, false);
    }

    private String targetName(Effect effect, int owner, int target) {
        if (effect.targetKind() == Effect.TargetKind.HAND) {
            return state.sides[owner].hand.get(target).displayName();
        }
        Unit unit = state.board[target];
        return unit != null ? unit.type().displayName() : "empty tile";
    }
    //endregion

    //region [Player input]

    /**
     * A click on a hand card: redraws it during the mulligan, picks it as the target of a pending hand-target effect
     * (by its owner), otherwise selects it (on your turn).
     */
    public void clickHand(int side, int index) {
        Side s = state.sides[side];
        if (index < 0 || index >= s.hand.size()) {
            return;
        }
        if (state.phase == Phase.MULLIGAN) {
            redraw(side, index);
        } else if (state.phase == Phase.PLAYING) {
            QueuedEffect pending = state.queue.peekFirst();
            if (pending != null) {
                pick(pending, Effect.TargetKind.HAND, side, index);
            } else if (state.current_player == side) {
                s.selectedHandIndex = s.selectedHandIndex == index ? -1 : index;
            }
        }
    }

    /** Resolves the pending effect with this target, if it is the owner's valid target of the right kind. */
    private void pick(QueuedEffect pending, Effect.TargetKind kind, int side, int target) {
        Effect effect = pending.effect();
        if (pending.owner() == side && effect.targetKind() == kind && effect.isValidTarget(state, side, target)) {
            state.queue.removeFirst();
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
        state.phase = Phase.MULLIGAN;
        for (Side s : state.sides) {
            s.redrawsLeft = redraws;
            s.mulliganDone = false;
            s.redrawn.clear();
        }
    }

    /** Ends a player's mulligan ("keep hand", or no redraws left). The game starts when both are done. */
    public void endMulligan(int side) {
        if (state.phase != Phase.MULLIGAN) {
            return;
        }
        Side s = state.sides[side];
        s.mulliganDone = true;
        s.redrawsLeft = 0;
        s.redrawn.clear();
        if (state.sides[0].mulliganDone && state.sides[1].mulliganDone) {
            state.phase = Phase.PLAYING;
            state.current_player = state.starter;
        }
    }

    /**
     * Mulligan: shuffles a hand card back into the deck and puts a new card in its place. The new card is the
     * top-most card whose type was not redrawn in this mulligan; if there is none, simply the top card.
     */
    private void redraw(int side, int index) {
        Side s = state.sides[side];
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
        if (state.phase != Phase.PLAYING || tile < 0 || tile >= BOARD_SIZE) {
            return;
        }
        QueuedEffect pending = state.queue.peekFirst();
        if (pending != null) {
            pick(pending, Effect.TargetKind.TILE, side, tile);
            return;
        }
        CardType card = state.selectedCard(side);
        if (state.current_player != side || card == null) {
            return;
        }
        if (card.isMonster()) {
            if (!state.canPlay(side, card, tile)) {
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
            state.sides[side].graveyard.add(card);
            castSpell(side, card, false);
        }
        resolveQueue();
    }

    /**
     * Plays a hand card directly (used by the AI): like selecting it and clicking the tile. For a spell the tile does
     * not matter (any value). Ignored if it isn't that player's turn or the move is not allowed.
     */
    public void playCard(int side, int handIndex, int tile) {
        Side s = state.sides[side];
        if (state.phase != Phase.PLAYING || state.current_player != side || state.pendingEffect() != null || s.passed
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
        if (state.phase != Phase.PLAYING || state.current_player != side || state.pendingEffect() != null) {
            return;
        }
        state.sides[side].passed = true;
        state.sides[side].selectedHandIndex = -1;
        log(side, "Passed", false);
        nextTurn();
    }

    /** Ends the game now (e.g. surrender); the winner is decided by the caller. */
    public void end() {
        state.phase = Phase.OVER;
        state.queue.clear();
    }
    //endregion

    //region [Turns and rounds]
    private void removeSelectedFromHand(int side) {
        Side s = state.sides[side];
        s.hand.remove(s.selectedHandIndex);
        s.selectedHandIndex = -1;
    }

    /** After a card is fully resolved: the other player's turn, unless they passed. Both passed = end of round. */
    private void nextTurn() {
        if (state.sides[0].passed && state.sides[1].passed) {
            endRound();
        } else if (!state.sides[other(state.current_player)].passed) {
            state.current_player = other(state.current_player);
        }
    }

    private void endRound() {
        int[] power = {state.power(0), state.power(1)};
        state.lastRoundPower = power;
        if (power[0] >= power[1]) {
            state.sides[0].points++;
        }
        if (power[1] >= power[0]) {
            state.sides[1].points++;
        }
        // Clear the board; no on-death effects.
        for (int tile = 0; tile < BOARD_SIZE; tile++) {
            Unit unit = state.board[tile];
            if (unit != null) {
                state.sides[unit.owner()].graveyard.add(unit.type());
                state.board[tile] = null;
            }
        }
        if (state.sides[0].points >= POINTS_TO_WIN || state.sides[1].points >= POINTS_TO_WIN) {
            state.phase = Phase.OVER;
            return;
        }
        state.round++;
        log(-1, "--- Round " + state.round + " ---", false);
        state.starter = other(state.starter);
        for (int side = 0; side < 2; side++) {
            state.sides[side].passed = false;
            state.sides[side].selectedHandIndex = -1;
            draw(side, ROUND_DRAW);
        }
        beginMulligan(ROUND_REDRAWS);
    }
    //endregion

    //region [Effect queue]

    /** Adds effects owned by {@code owner} to the end of the queue. */
    public void queue(int owner, List<Effect> effects) {
        for (Effect effect : effects) {
            state.queue.addLast(new QueuedEffect(effect, owner));
        }
    }

    /** Puts an effect at the front of the queue, e.g. an effect that repeats itself ("play all ..."). */
    public void queueNext(int owner, Effect effect) {
        state.queue.addFirst(new QueuedEffect(effect, owner));
    }

    /** Resolves queued effects in order until one needs a target; when the queue is empty, the turn ends. */
    private void resolveQueue() {
        int steps = 0;
        while (!state.queue.isEmpty()) {
            if (++steps > MAX_RESOLVE_STEPS) {
                state.queue.clear();
                break;
            }
            QueuedEffect head = state.queue.peekFirst();
            if (!head.effect().hasValidTargets(state, head.owner())) {
                state.queue.removeFirst();
                logEffect(head.effect(), head.owner(), -1, true);
            } else if (head.effect().needsTarget()) {
                return;
            } else {
                state.queue.removeFirst();
                logEffect(head.effect(), head.owner(), -1, false);
                head.effect().resolve(this, head.owner(), -1);
            }
        }
        nextTurn();
    }
    //endregion

    //region [Operations used by effects (and game setup)]

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
        for (Unit unit : state.board) {
            if (unit != null && unit.owner() != side) {
                queue(unit.owner(), unit.type().onOpponentSpell(unit));
            }
        }
    }

    /**
     * Puts a monster on an empty tile. The unit belongs to the side of the tile (a treacherous monster on the
     * opponent's side is the opponent's unit); its on-play effects belong to {@code player}, who played it.
     */
    public void play(int player, CardType monster, int tile) {
        if (!state.canPlay(player, monster, tile)) {
            throw new IllegalStateException("Cannot play " + monster + " on tile " + tile + " for side " + player);
        }
        Unit unit = new Unit(monster, tile);
        state.board[tile] = unit;
        queue(player, monster.onPlay(unit));
    }

    /**
     * Reduces a unit's power. Below 1 power it is destroyed (on-death effects); otherwise its
     * "damaged and survives" effects are queued.
     */
    public void damage(int tile, int amount) {
        Unit unit = state.board[tile];
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

    public void buff(int tile, int amount) {
        Unit unit = state.board[tile];
        if (unit != null) {
            unit.addPower(amount);
        }
    }

    public void setPower(int tile, int power) {
        Unit unit = state.board[tile];
        if (unit != null) {
            unit.addPower(power - unit.power());
        }
    }

    /** Removes a unit (its card goes to its owner's graveyard) and queues its on-death effects for its owner. */
    public void destroy(int tile) {
        Unit unit = state.board[tile];
        if (unit == null) {
            return;
        }
        state.board[tile] = null;
        state.sides[unit.owner()].graveyard.add(unit.type());
        queue(unit.owner(), unit.type().onDeath(unit));
    }

    /**
     * Moves the unit one step (see {@link GameState#step}); units in the way are pushed along. Units that end on the
     * other side belong to that side. False (nothing moves) if the chain would be pushed off the board.
     */
    public boolean shift(int tile, int lineStep, int columnStep) {
        if (!state.canShift(tile, lineStep, columnStep)) {
            return false;
        }
        for (int to = state.shiftEnd(tile, lineStep, columnStep); to != tile; ) {
            int from = GameState.step(to, -lineStep, -columnStep);
            Unit unit = state.board[from];
            state.board[from] = null;
            state.board[to] = unit;
            unit.moveTo(to);
            to = from;
        }
        return true;
    }

    /** Draws cards from the top of the deck. With a full hand the card is discarded to the graveyard. */
    public void draw(int side, int count) {
        draw(side, count, null, side, Pile.DECK);
    }

    /**
     * {@code side} draws the top-most matching cards of {@code pileSide}'s deck or graveyard (the graveyard's top is
     * the card that entered it last), one at a time; null filter = any card. With a full hand the card goes to
     * {@code side}'s graveyard. Returns how many were drawn: fewer than {@code count} if the pile runs out of such
     * cards.
     */
    public int draw(int side, int count, @Nullable CardFilter filter, int pileSide, Pile pile) {
        List<CardType> from = state.sides[pileSide].pile(pile);
        Side to = state.sides[side];
        boolean fromEnd = pile == Pile.GRAVEYARD;
        int drawn = 0;
        for (int n = 0; n < from.size() && drawn < count; ) {
            if (from == to.graveyard && to.hand.size() >= MAX_HAND) {
                // It would go straight back on top of the same graveyard.
                break;
            }
            int i = fromEnd ? from.size() - 1 - n : n;
            if (filter != null && !filter.check(from.get(i))) {
                n++;
                continue;
            }
            CardType card = from.remove(i);
            if (to.hand.size() >= MAX_HAND) {
                to.graveyard.add(card);
            } else {
                to.hand.add(card);
            }
            drawn++;
        }
        return drawn;
    }

    /** Takes a card out of a side's hand (it goes nowhere; put it somewhere with e.g. {@link #addToPile}). */
    public CardType takeFromHand(int side, int index) {
        return state.sides[side].hand.remove(index);
    }

    public void addToGraveyard(int side, CardType card) {
        state.sides[side].graveyard.add(card);
    }

    /**
     * Puts a card into a side's deck or graveyard: on top (the deck's top is its first card, the graveyard's the one
     * that entered it last) or at a random position.
     */
    public void addToPile(int side, Pile pile, CardType card, boolean onTop) {
        List<CardType> cards = state.sides[side].pile(pile);
        if (!onTop) {
            cards.add(ThreadLocalRandom.current().nextInt(cards.size() + 1), card);
        } else if (pile == Pile.DECK) {
            cards.addFirst(card);
        } else {
            cards.add(card);
        }
    }

    /**
     * Puts a card at the bottom of a side's deck or graveyard (the deck's bottom is its last card, drawn last; the
     * graveyard's the one that entered it first).
     */
    public void addToPileBottom(int side, Pile pile, CardType card) {
        List<CardType> cards = state.sides[side].pile(pile);
        if (pile == Pile.DECK) {
            cards.add(card);
        } else {
            cards.addFirst(card);
        }
    }

    /**
     * Moves every matching card (null filter = any) from a side's deck or graveyard into a deck or graveyard (the same
     * side's or the other's), each one on top or at a random position, see {@link #addToPile}; returns how many.
     */
    public int moveBetweenPiles(int fromSide, Pile from, int toSide, Pile to, @Nullable CardFilter filter,
                                boolean onTop) {
        List<CardType> source = state.sides[fromSide].pile(from);
        // Take them all out first, so moving within one pile doesn't meet the same cards again.
        List<CardType> moved = new ArrayList<>();
        for (int i = 0; i < source.size(); ) {
            if (filter == null || filter.check(source.get(i))) {
                moved.add(source.remove(i));
            } else {
                i++;
            }
        }
        for (CardType card : moved) {
            addToPile(toSide, to, card, onTop);
        }
        return moved.size();
    }

    /**
     * Swaps the contents of two piles (a deck or graveyard of either side), keeping both orders: the cards keep their
     * list positions, e.g. a graveyard's first card becomes the deck's top card. Nothing happens for the same pile.
     */
    public void swapPiles(int sideA, Pile pileA, int sideB, Pile pileB) {
        List<CardType> a = state.sides[sideA].pile(pileA);
        List<CardType> b = state.sides[sideB].pile(pileB);
        if (a == b) {
            return;
        }
        List<CardType> oldA = new ArrayList<>(a);
        a.clear();
        a.addAll(b);
        b.clear();
        b.addAll(oldA);
    }

    /** Takes one copy of a card out of a side's deck or graveyard; false if there is none. */
    public boolean removeFromPile(int side, Pile pile, CardType card) {
        return state.sides[side].pile(pile).remove(card);
    }

    /** Removes a random matching card (null filter = any) from a side's deck or graveyard for good; null if none. */
    public @Nullable CardType banishRandom(int side, Pile pile, @Nullable CardFilter filter) {
        return removeRandom(state.sides[side].pile(pile), card -> filter == null || filter.check(card));
    }

    private static @Nullable CardType removeRandom(List<CardType> cards, Predicate<CardType> filter) {
        List<Integer> matching = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            if (filter.test(cards.get(i))) {
                matching.add(i);
            }
        }
        if (matching.isEmpty()) {
            return null;
        }
        return cards.remove((int) matching.get(ThreadLocalRandom.current().nextInt(matching.size())));
    }
    //endregion
}
