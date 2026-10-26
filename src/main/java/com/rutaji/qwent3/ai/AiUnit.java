package com.rutaji.qwent3.ai;

import com.rutaji.qwent3.cards.CardType;

/**
 * A monster on the board, as the AI sees it. {@code owner} is 0 or 1 (compare with {@link AiView#me()}), {@code tile}
 * the board index (see {@link com.rutaji.qwent3.cards.GameState#tile} / {@code sideOf}).
 */
public record AiUnit(CardType type, int power, int owner, int tile) {
}
