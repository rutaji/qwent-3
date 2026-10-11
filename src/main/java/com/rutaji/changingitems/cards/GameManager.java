package com.rutaji.changingitems.cards;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Keeps the running game of each player. */
public final class GameManager implements Listener {
    private final Map<UUID, GameState> games = new HashMap<>();
    private final Map<UUID, GameView> views = new HashMap<>();

    public GameState start(Player player, GameView view) {
        CardType[] types = CardType.values();
        CardType[] hand = new CardType[GameState.HAND_SIZE];
        for (int i = 0; i < hand.length; i++) {
            hand[i] = types[ThreadLocalRandom.current().nextInt(types.length)];
        }
        GameState state = new GameState(hand);
        games.put(player.getUniqueId(), state);
        views.put(player.getUniqueId(), view);
        return state;
    }

    public @Nullable GameState get(Player player) {
        return games.get(player.getUniqueId());
    }

    /** The view the player's game was started with. */
    public @Nullable GameView view(Player player) {
        return views.get(player.getUniqueId());
    }

    public void end(Player player) {
        games.remove(player.getUniqueId());
        views.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        end(event.getPlayer());
    }
}
