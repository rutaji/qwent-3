package com.rutaji.qwent3.cards.gameLogic;

import com.rutaji.qwent3.ai.QwentAi;

import java.util.UUID;

/**
 * Who plays one side of a {@link Match}. Plain data: the behaviour for each kind lives in {@link GameManager}, chosen
 * with a {@code switch}; being sealed, the compiler reports every switch that misses a kind.
 */
public sealed interface Participant permits Participant.Human, Participant.Computer {
    /** Shown to the other player (dialog title, history, result). */
    String name();

    /** A player on the server, identified by UUID (their {@code Player} object is looked up when needed). */
    record Human(UUID id, String name) implements Participant {
    }

    /** An AI from the {@link com.rutaji.qwent3.ai.AiRegistry}; {@code name} is its registered name. */
    record Computer(QwentAi ai, String name) implements Participant {
    }
}
