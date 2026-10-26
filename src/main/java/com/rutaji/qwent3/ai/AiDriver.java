package com.rutaji.qwent3.ai;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Side;
import com.rutaji.qwent3.cards.Unit;
import com.rutaji.qwent3.cards.effect.Effect;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/** Runs an AI's moves: builds its view and legal actions, asks it, and applies the (validated) answer. */
public final class AiDriver {
    /** Guard against an AI (or a card loop) that never finishes its part. */
    private static final int MAX_ACTIONS = 500;

    private AiDriver() {
    }

    /** Lets the AI act as long as the game waits for it. */
    public static void run(GameState state, int side, QwentAi ai, String aiName, Logger logger) {
        for (int i = 0; i < MAX_ACTIONS && state.phase() != GameState.Phase.OVER && state.awaits(side); i++) {
            List<AiAction> legal = legalActions(state, side);
            if (legal.isEmpty()) {
                return;
            }
            AiAction action;
            try {
                action = ai.decide(view(state, side, legal));
            } catch (RuntimeException e) {
                logger.warning("AI '" + aiName + "' threw an exception, using a fallback move: " + e);
                action = null;
            }
            if (action == null || !legal.contains(action)) {
                if (action != null) {
                    logger.warning("AI '" + aiName + "' chose an illegal action " + action + ", using a fallback move.");
                }
                action = fallback(legal);
            }
            apply(state, side, action);
        }
    }

    /** All actions the side may take right now (empty if the game doesn't wait for it). */
    public static List<AiAction> legalActions(GameState state, int side) {
        List<AiAction> legal = new ArrayList<>();
        Side own = state.side(side);
        if (state.phase() == GameState.Phase.MULLIGAN) {
            if (!own.mulliganDone()) {
                for (int i = 0; i < own.handSize() && own.redrawsLeft() > 0; i++) {
                    legal.add(new AiAction.Redraw(i));
                }
                legal.add(new AiAction.KeepHand());
            }
            return legal;
        }
        if (state.phase() != GameState.Phase.PLAYING) {
            return legal;
        }
        Effect pending = state.pendingEffect();
        if (pending != null) {
            if (state.pendingOwner() == side) {
                boolean hand = pending.targetKind() == Effect.TargetKind.HAND;
                int count = hand ? own.handSize() : GameState.BOARD_SIZE;
                for (int target = 0; target < count; target++) {
                    if (pending.isValidTarget(state, side, target)) {
                        legal.add(hand ? new AiAction.PickHand(target) : new AiAction.PickTile(target));
                    }
                }
            }
            return legal;
        }
        if (state.current() != side || own.passed()) {
            return legal;
        }
        for (int i = 0; i < own.handSize(); i++) {
            CardType card = own.handCard(i);
            if (!card.isMonster()) {
                legal.add(new AiAction.Play(i, -1));
                continue;
            }
            for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
                if (state.canPlay(side, card, tile)) {
                    legal.add(new AiAction.Play(i, tile));
                }
            }
        }
        legal.add(new AiAction.Pass());
        return legal;
    }

    private static AiView view(GameState state, int side, List<AiAction> legal) {
        int opponent = GameState.other(side);
        Side own = state.side(side);
        Side theirs = state.side(opponent);
        List<@Nullable AiUnit> board = new ArrayList<>();
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            Unit unit = state.unit(tile);
            board.add(unit == null ? null : new AiUnit(unit.type(), unit.power(), unit.owner(), tile));
        }
        List<CardType> hand = new ArrayList<>();
        for (int i = 0; i < own.handSize(); i++) {
            hand.add(own.handCard(i));
        }
        List<CardType> deck = new ArrayList<>(own.deck());
        deck.sort(Comparator.naturalOrder());
        Effect pending = state.pendingOwner() == side ? state.pendingEffect() : null;
        return new AiView(side, state.phase(), state.round(), state.points(side), state.points(opponent),
                state.power(side), state.power(opponent), own.passed(), theirs.passed(),
                Collections.unmodifiableList(board), List.copyOf(hand), List.copyOf(deck),
                own.graveyard(), theirs.handSize(), theirs.deckSize(), theirs.graveyard(),
                pending, own.redrawsLeft(), state.log(), List.copyOf(legal));
    }

    /** A safe legal move: keep the hand, pass, or else the first one. */
    private static AiAction fallback(List<AiAction> legal) {
        for (AiAction safe : List.of(new AiAction.KeepHand(), new AiAction.Pass())) {
            if (legal.contains(safe)) {
                return safe;
            }
        }
        return legal.getFirst();
    }

    private static void apply(GameState state, int side, AiAction action) {
        switch (action) {
            case AiAction.Redraw r -> state.clickHand(side, r.handIndex());
            case AiAction.KeepHand k -> state.endMulligan(side);
            case AiAction.Play p -> state.playCard(side, p.handIndex(), p.tile());
            case AiAction.Pass p -> state.pass(side);
            case AiAction.PickTile p -> state.clickTile(side, p.tile());
            case AiAction.PickHand p -> state.clickHand(side, p.handIndex());
        }
    }
}
