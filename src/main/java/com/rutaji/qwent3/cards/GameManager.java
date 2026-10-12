package com.rutaji.qwent3.cards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

/**
 * Running games, invites, and what each player's dialog shows. A player can be in one game at a time.
 * Leaving the server during a game counts as surrender.
 */
public final class GameManager implements Listener {
    private static final long INVITE_MILLIS = 60_000;

    private final DeckStore decks;
    private final GameDialog dialog;
    private final Map<UUID, Match> matches = new HashMap<>();
    /** Pending invites: invited player -> (inviter -> expiry time). */
    private final Map<UUID, Map<UUID, Long>> invites = new HashMap<>();
    private final Map<UUID, Ui> ui = new HashMap<>();

    /** Dialog state of a player: whether the game dialog is open, which pile screen, whether told it's their turn. */
    private static final class Ui {
        boolean open;
        @Nullable PileView view;
        boolean notified;
    }

    public GameManager(DeckStore decks, GameDialog dialog) {
        this.decks = decks;
        this.dialog = dialog;
    }

    public @Nullable Match match(Player player) {
        return matches.get(player.getUniqueId());
    }

    private Ui ui(Player player) {
        return ui.computeIfAbsent(player.getUniqueId(), id -> new Ui());
    }

    // ---- Invites ----

    public void invite(Player from, Player to) {
        if (from.equals(to)) {
            from.sendMessage(Component.text("You can't play against yourself.", NamedTextColor.RED));
            return;
        }
        if (matches.containsKey(from.getUniqueId())) {
            from.sendMessage(Component.text("You are already in a game. /qwent surrender to leave it.", NamedTextColor.RED));
            return;
        }
        if (matches.containsKey(to.getUniqueId())) {
            from.sendMessage(Component.text(to.getName() + " is already in a game.", NamedTextColor.RED));
            return;
        }
        if (!checkDeck(from)) {
            return;
        }
        invites.computeIfAbsent(to.getUniqueId(), id -> new HashMap<>())
                .put(from.getUniqueId(), System.currentTimeMillis() + INVITE_MILLIS);
        from.sendMessage(Component.text("Invite sent to " + to.getName() + ".", NamedTextColor.GREEN));
        to.sendMessage(Component.text()
                .append(Component.text(from.getName() + " invites you to a game of Qwent. ", NamedTextColor.GOLD))
                .append(Component.text("[Accept]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/qwent accept " + from.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Start the game"))))
                .append(Component.text(" "))
                .append(Component.text("[Decline]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/qwent decline " + from.getName())))
                .build());
    }

    public void accept(Player player, Player inviter) {
        if (!takeInvite(player, inviter)) {
            player.sendMessage(Component.text("You have no invite from " + inviter.getName() + " (it may have expired).",
                    NamedTextColor.RED));
            return;
        }
        if (matches.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a game.", NamedTextColor.RED));
            return;
        }
        if (matches.containsKey(inviter.getUniqueId())) {
            player.sendMessage(Component.text(inviter.getName() + " is already in another game.", NamedTextColor.RED));
            return;
        }
        if (!checkDeck(player)) {
            return;
        }
        if (!DeckRules.isValid(decks.deck(inviter))) {
            player.sendMessage(Component.text(inviter.getName() + "'s deck is not valid anymore.", NamedTextColor.RED));
            inviter.sendMessage(Component.text(player.getName() + " accepted, but your deck is not valid.",
                    NamedTextColor.RED));
            return;
        }
        start(inviter, player);
    }

    public void decline(Player player, Player inviter) {
        if (takeInvite(player, inviter)) {
            player.sendMessage(Component.text("Invite declined.", NamedTextColor.GRAY));
            inviter.sendMessage(Component.text(player.getName() + " declined your invite.", NamedTextColor.GRAY));
        }
    }

    /** Removes the invite; true if it existed and has not expired. */
    private boolean takeInvite(Player player, Player inviter) {
        Map<UUID, Long> received = invites.get(player.getUniqueId());
        Long expiry = received == null ? null : received.remove(inviter.getUniqueId());
        return expiry != null && expiry >= System.currentTimeMillis();
    }

    private boolean checkDeck(Player player) {
        if (DeckRules.isValid(decks.deck(player))) {
            return true;
        }
        DeckEditor.tellValidity(player, decks.deck(player));
        return false;
    }

    private void start(Player player0, Player player1) {
        GameState state = GameState.start(shuffledDeck(player0), shuffledDeck(player1));
        Match match = new Match(state, player0.getUniqueId(), player0.getName(), player1.getUniqueId(),
                player1.getName());
        for (Player player : List.of(player0, player1)) {
            matches.put(player.getUniqueId(), match);
            invites.remove(player.getUniqueId());
            Ui u = ui(player);
            u.view = null;
            u.notified = false;
            show(player);
        }
    }

    private List<CardType> shuffledDeck(Player player) {
        List<CardType> deck = new ArrayList<>(decks.deck(player));
        Collections.shuffle(deck);
        return deck;
    }

    // ---- Dialog ----

    /** Opens (or re-opens) the player's game dialog. */
    public void open(Player player) {
        Match match = match(player);
        if (match == null) {
            player.sendMessage(Component.text("You are not in a game. Invite someone with /qwent new <player>.",
                    NamedTextColor.GRAY));
            return;
        }
        ui(player).view = null;
        show(player);
    }

    /** The player closed the dialog (Esc). */
    public void closed(Player player) {
        Ui u = ui(player);
        u.open = false;
        u.view = null;
        player.closeDialog();
    }

    public void setView(Player player, @Nullable PileView view) {
        ui(player).view = view;
    }

    /** Shows the player their game dialog (game, mulligan or pile screen). */
    public void show(Player player) {
        Match match = match(player);
        if (match == null) {
            return;
        }
        Ui u = ui(player);
        u.open = true;
        u.notified = false;
        dialog.show(player, match, match.sideOf(player.getUniqueId()), u.view);
    }

    /**
     * After a player's action: ends the game if it is over, otherwise shows the clicking player their dialog and
     * refreshes the opponent's (if open; if closed and it is their move, they get a chat message).
     */
    public void afterAction(Player clicker, Match match) {
        GameState state = match.state();
        if (state.phase() == GameState.Phase.OVER) {
            finish(match, state.winner(), "Final points: " + match.name(0) + " " + state.points(0) + " : "
                    + state.points(1) + " " + match.name(1) + ".");
            return;
        }
        for (int side = 0; side < 2; side++) {
            Player player = Bukkit.getPlayer(match.player(side));
            if (player == null) {
                continue;
            }
            Ui u = ui(player);
            if (player.equals(clicker) || u.open) {
                show(player);
            } else if (state.awaits(side) && !u.notified) {
                u.notified = true;
                player.sendMessage(Component.text("Qwent: it's your move. ", NamedTextColor.GOLD)
                        .append(Component.text("[Open]", NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/qwent"))));
            }
        }
    }

    // ---- Ending ----

    public void surrender(Player player) {
        Match match = match(player);
        if (match == null) {
            player.sendMessage(Component.text("You are not in a game.", NamedTextColor.GRAY));
            return;
        }
        int side = match.sideOf(player.getUniqueId());
        finish(match, GameState.other(side), match.name(side) + " surrendered.");
    }

    /** Ends the game: removes it and shows both players the result. {@code winner} -1 = draw. */
    private void finish(Match match, int winner, String reason) {
        match.state().end();
        for (int side = 0; side < 2; side++) {
            matches.remove(match.player(side));
            ui.remove(match.player(side));
        }
        for (int side = 0; side < 2; side++) {
            Player player = Bukkit.getPlayer(match.player(side));
            if (player == null) {
                continue;
            }
            String result = winner < 0 ? "Draw!" : winner == side ? "You won!" : "You lost.";
            String score = "Points: you " + match.state().points(side) + " : "
                    + match.state().points(GameState.other(side)) + " " + match.name(GameState.other(side));
            NamedTextColor color = winner < 0 ? NamedTextColor.GOLD
                    : winner == side ? NamedTextColor.GREEN : NamedTextColor.RED;
            player.sendMessage(Component.text("Qwent: " + result + " " + reason, color));
            dialog.showResult(player, result, color, reason, score);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        invites.remove(id);
        invites.values().forEach(received -> received.remove(id));
        Match match = matches.get(id);
        if (match != null) {
            int side = match.sideOf(id);
            finish(match, GameState.other(side), match.name(side) + " left the game.");
        }
        ui.remove(id);
    }
}
