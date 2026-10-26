package com.rutaji.qwent3.ai;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.DeckStore;

import java.util.List;

/**
 * A computer opponent for singleplayer ({@code /qwent singleplayer <name>}). Register implementations in
 * {@link AiRegistry}. A new instance is created for every game, so it may keep state between moves.
 *
 * The game calls {@link #decide} whenever it waits for the AI: its mulligan, its turn, or a target for one of its
 * effects. Return one of {@link AiView#legalActions()}; anything else (or an exception) is logged and replaced by a
 * safe legal action. Called on the server thread - keep it fast.
 */
public interface QwentAi {
    /** The AI's deck for a game, top card first is irrelevant (it is shuffled). Default: the default starting deck. */
    default List<CardType> deck() {
        return DeckStore.defaultDeck();
    }

    AiAction decide(AiView view);
}
