package com.rutaji.qwent3.cards;

import java.util.UUID;

/** A running game and the two players in it (player at index i plays side i). */
public final class Match {
    private final GameState state;
    private final UUID[] players;
    private final String[] names;

    Match(GameState state, UUID player0, String name0, UUID player1, String name1) {
        this.state = state;
        this.players = new UUID[] {player0, player1};
        this.names = new String[] {name0, name1};
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
