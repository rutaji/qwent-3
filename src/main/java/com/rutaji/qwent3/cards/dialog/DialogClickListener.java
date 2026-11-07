package com.rutaji.qwent3.cards.dialog;

import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.gameLogic.GameManager;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Match;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;

import java.util.logging.Logger;

/**
 * Handles clicks on the game dialog: "qwent:tile/N", "qwent:hand/N", "qwent:keep", "qwent:pass",
 * "qwent:view/{mine|theirs}/{deck|graveyard}", "qwent:back", "qwent:history", "qwent:close".
 */
public final class DialogClickListener implements Listener {
    private final GameManager gameManager;
    private final Logger logger;

    public DialogClickListener(GameManager games, Logger logger) {
        this.gameManager = games;
        this.logger = logger;
    }

    @EventHandler
    public void onClick(PlayerCustomClickEvent event) {
        if (!event.getIdentifier().namespace().equals(GameDialog.NAMESPACE)) {
            return;
        }
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }
        Player player = connection.getPlayer();
        String path = event.getIdentifier().value();
        // Game state is only touched on the main thread.
        handle(player, path);
    }

    private void handle(Player player, String path) {
        Match match = gameManager.match(player);
        if (match == null) {
            // A click on a dialog of a game that is over.
            gameManager.closed(player);
            return;
        }
        int side = match.sideOf(player.getUniqueId());
        GameEngine engine = match.engine();
        String[] parts = path.split("/");
        switch (parts[0]) {
            // Screen only: the game doesn't change, only this player's screen is redrawn.
            case "close" -> gameManager.closed(player);
            case "history" -> {
                gameManager.toggleHistory(player);
                gameManager.show(player);
            }
            case "back" -> {
                gameManager.setView(player, null);
                gameManager.show(player);
            }
            case "view" -> {
                gameManager.setView(player, pileView(parts, side));
                gameManager.show(player);
            }
            // Moves: change the game, then both players' screens are updated.
            case "keep" -> {
                engine.endMulligan(side);
                gameManager.afterAction(player, match);
            }
            case "pass" -> {
                engine.pass(side);
                gameManager.afterAction(player, match);
            }
            case "hand" -> {
                engine.clickHand(side, index(parts));
                gameManager.afterAction(player, match);
            }
            case "tile" -> {
                engine.clickTile(side, index(parts));
                gameManager.afterAction(player, match);
            }
            default -> logger.warning("Unknown dialog click \"" + GameDialog.NAMESPACE + ":" + path + "\" from "
                    + player.getName() + "; GameDialog doesn't create it.");
        }
    }

    /** "view/{mine|theirs}/{deck|graveyard}"; null (the game screen) for the opponent's deck, which is hidden. */
    private static @Nullable PileView pileView(String[] parts, int side) {
        int pileSide = parts.length > 1 && parts[1].equals("theirs") ? GameState.other(side) : side;
        Pile pile = parts.length > 2 && parts[2].equals("deck") ? Pile.DECK : Pile.GRAVEYARD;
        return pileSide != side && pile == Pile.DECK ? null : new PileView(pileSide, pile);
    }

    /** N of "hand/N" or "tile/N", or -1 if it is missing or not a number (the engine ignores it). */
    private static int index(String[] parts) {
        if (parts.length != 2) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
