package com.rutaji.qwent3.cards.effect.effectTargets;

/** A direction on the board, seen from the effect's owner. */
public enum Direction {
    /** One row towards the owner's opponent. */
    Forward,
    /** One row towards the owner. */
    Back,
    /** One column left (column 0 is on the left for both players). */
    Left,
    /** One column right. */
    Right
}
