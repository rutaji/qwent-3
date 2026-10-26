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
import org.bukkit.plugin.Plugin;

/**
 * Handles clicks on the game dialog: "qwent:tile/N", "qwent:hand/N", "qwent:keep", "qwent:pass",
 * "qwent:view/{mine|theirs}/{deck|graveyard}", "qwent:back", "qwent:history", "qwent:close".
 */
public final class DialogClickListener implements Listener {
    private final GameManager games;

    public DialogClickListener(GameManager games) {
        this.games = games;
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
        Match match = games.match(player);
        if (match == null || path.equals("close")) {
            // Esc, or a click on a dialog of a game that is over.
            games.closed(player);
            return;
        }
        int side = match.sideOf(player.getUniqueId());
        GameEngine engine = match.engine();
        if (path.startsWith("view/")) {
            String[] parts = path.split("/");
            int pileSide = parts.length > 1 && parts[1].equals("theirs") ? GameState.other(side) : side;
            Pile pile = parts.length > 2 && parts[2].equals("deck") ? Pile.DECK : Pile.GRAVEYARD;
            // The opponent's deck is hidden.
            games.setView(player, pileSide != side && pile == Pile.DECK ? null : new PileView(pileSide, pile));
            games.show(player);
            return;
        }
        if (path.equals("history")) {
            games.toggleHistory(player);
            games.show(player);
            return;
        }
        if (path.equals("back")) {
            games.setView(player, null);
            games.show(player);
            return;
        }
        switch (path) {
            case "keep" -> engine.endMulligan(side);
            case "pass" -> engine.pass(side);
            default -> {
                int slash = path.indexOf('/');
                int index = -1;
                if (slash > 0) {
                    try {
                        index = Integer.parseInt(path.substring(slash + 1));
                    } catch (NumberFormatException e) {
                        index = -1;
                    }
                }
                if (path.startsWith("hand/")) {
                    engine.clickHand(side, index);
                } else if (path.startsWith("tile/")) {
                    engine.clickTile(side, index);
                }
            }
        }
        games.afterAction(player, match);
    }
}
