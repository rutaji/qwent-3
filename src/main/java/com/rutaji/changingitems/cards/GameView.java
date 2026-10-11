package com.rutaji.changingitems.cards;

import org.bukkit.entity.Player;

/** A way of showing a game to a player (button grid or clickable text). */
public interface GameView {
    void show(Player player, GameState state);
}
