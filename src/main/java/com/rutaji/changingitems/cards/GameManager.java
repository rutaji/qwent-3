package com.rutaji.changingitems.cards;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Keeps the running game of each player. */
public final class GameManager implements Listener {
    private final Map<UUID, GameState> games = new HashMap<>();
    private final DeckStore decks;

    public GameManager(DeckStore decks) {
        this.decks = decks;
    }

    /** Starts a game with a shuffled copy of the player's starting deck, draws the starting hand, starts the mulligan. */
    public GameState start(Player player) {
        List<CardType> deck = new ArrayList<>(decks.deck(player));
        Collections.shuffle(deck);
        GameState state = new GameState(deck);
        state.draw(GameState.STARTING_HAND);
        state.beginMulligan(GameState.MULLIGAN_REDRAWS);
        games.put(player.getUniqueId(), state);
        return state;
    }

    public @Nullable GameState get(Player player) {
        return games.get(player.getUniqueId());
    }

    public void end(Player player) {
        games.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        end(event.getPlayer());
    }
}
