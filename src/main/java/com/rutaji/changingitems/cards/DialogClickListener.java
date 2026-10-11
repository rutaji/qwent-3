package com.rutaji.changingitems.cards;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/** Handles clicks on the game dialog buttons ("qwent:tile/N", "qwent:hand/N", "qwent:noop"). */
public final class DialogClickListener implements Listener {
    private final Plugin plugin;
    private final GameManager games;
    private final GameDialog dialog;

    public DialogClickListener(Plugin plugin, GameManager games, GameDialog dialog) {
        this.plugin = plugin;
        this.games = games;
        this.dialog = dialog;
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
        plugin.getServer().getScheduler().runTask(plugin, () -> handle(player, path));
    }

    private void handle(Player player, String path) {
        if (!player.isOnline()) {
            return;
        }
        GameState state = games.get(player);
        if (state == null) {
            games.end(player);
            player.closeDialog();
            return;
        }
        int slash = path.indexOf('/');
        if (slash > 0) {
            String kind = path.substring(0, slash);
            int index;
            try {
                index = Integer.parseInt(path.substring(slash + 1));
            } catch (NumberFormatException e) {
                index = -1;
            }
            switch (kind) {
                case "hand" -> state.selectHand(index);
                case "tile" -> state.placeOn(index);
                default -> { }
            }
        }
        dialog.show(player, state);
    }
}
