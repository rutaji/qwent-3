package com.rutaji.qwent3.cards;

import com.rutaji.qwent3.ai.QwentAi;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A running game and the two players in it (player at index i plays side i). In singleplayer, side
 * {@link #AI_SIDE} is played by an AI; its "player" id is a made-up UUID that is never online.
 */
public final class Match {
    public static final int AI_SIDE = 1;

    private final GameState state;
    private final UUID[] players;
    private final String[] names;
    private final @Nullable QwentAi ai;

    Match(GameState state, UUID player0, String name0, UUID player1, String name1) {
        this(state, player0, name0, player1, name1, null);
    }

    Match(GameState state, UUID player0, String name0, UUID player1, String name1, @Nullable QwentAi ai) {
        this.state = state;
        this.players = new UUID[] {player0, player1};
        this.names = new String[] {name0, name1};
        this.ai = ai;
    }

    /** The AI playing {@link #AI_SIDE}, or null in a game between two players. */
    public @Nullable QwentAi ai() {
        return ai;
    }

    public GameState state() {
        return state;
    }

    public UUID player(int side) {
        return players[side];
    }

    public String name(int side) {
        return names[side];
    }

    /** The side this player plays, or -1. */
    public int sideOf(UUID player) {
        return players[0].equals(player) ? 0 : players[1].equals(player) ? 1 : -1;
    }
}
