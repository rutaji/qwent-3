package com.rutaji.qwent3.cards.gameLogic;

import java.util.UUID;

/** A running game and who plays it: participant at index i plays side i (a person or an AI). */
public final class Match {
    private final GameEngine engine;
    private final Participant[] participants;

    Match(GameEngine engine, Participant side0, Participant side1) {
        this.engine = engine;
        this.participants = new Participant[] {side0, side1};
    }

    /** The game's rules: use it to make moves. */
    public GameEngine engine() {
        return engine;
    }

    /** The game's data, for reading. */
    public GameState state() {
        return engine.state();
    }

    public Participant participant(int side) {
        return participants[side];
    }

    /** The name of whoever plays the side. */
    public String name(int side) {
        return participants[side].name();
    }

    /** The side this player plays, or -1. */
    public int sideOf(UUID player) {
        for (int side = 0; side < participants.length; side++) {
            if (participants[side] instanceof Participant.Human human && human.id().equals(player)) {
                return side;
            }
        }
        return -1;
    }
}
