package com.rutaji.qwent3.cards.gameLogic;

import com.rutaji.qwent3.ai.AiDriver;
import com.rutaji.qwent3.ai.AiRegistry;
import com.rutaji.qwent3.ai.QwentAi;
import com.rutaji.qwent3.cards.chat.ChatSender;
import com.rutaji.qwent3.cards.dialog.PileView;
import com.rutaji.qwent3.cards.deck.DeckRules;
import com.rutaji.qwent3.cards.deck.DeckStore;
import com.rutaji.qwent3.cards.dialog.GameDialog;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

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
    /** Players who hid the game history; kept across games until they leave the server. */
    private final Set<UUID> historyHidden = new HashSet<>();

    /** Dialog state of a player: whether the game dialog is open, which pile screen, whether told it's their turn. */
    private static final class Ui {
        boolean open;
        @Nullable PileView view;
        boolean notified;
    }

    private final Logger logger;

    public GameManager(DeckStore decks, GameDialog dialog, Logger logger) {
        this.decks = decks;
        this.dialog = dialog;
        this.logger = logger;
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
            ChatSender.cannotPlayYourself(from);
            return;
        }
        if (matches.containsKey(from.getUniqueId())) {
            ChatSender.alreadyInGame(from);
            return;
        }
        if (matches.containsKey(to.getUniqueId())) {
            ChatSender.otherAlreadyInGame(from, to);
            return;
        }
        if (!DeckRules.isValid(decks.deck(from), from)) {
            return;
        }
        invites.computeIfAbsent(to.getUniqueId(), id -> new HashMap<>())
                .put(from.getUniqueId(), System.currentTimeMillis() + INVITE_MILLIS);
        ChatSender.inviteSent(from, to);
        ChatSender.invite(to, from);
    }

    public void accept(Player player, Player inviter) {
        if (!takeInvite(player, inviter)) {
            ChatSender.noInvite(player, inviter);
            return;
        }
        if (matches.containsKey(player.getUniqueId())) {
            ChatSender.cannotAcceptInGame(player);
            return;
        }
        if (matches.containsKey(inviter.getUniqueId())) {
            ChatSender.inviterInAnotherGame(player, inviter);
            return;
        }
        if (!DeckRules.isValid(decks.deck(player), player)) {
            return;
        }
        if (!DeckRules.isValid(decks.deck(inviter))) {
            ChatSender.inviterDeckInvalid(player, inviter);
            return;
        }
        start(inviter, player);
    }

    public void decline(Player player, Player inviter) {
        if (takeInvite(player, inviter)) {
            ChatSender.inviteDeclined(player, inviter);
        }
    }

    /** Removes the invite; true if it existed and has not expired. */
    private boolean takeInvite(Player player, Player inviter) {
        Map<UUID, Long> received = invites.get(player.getUniqueId());
        Long expiry = received == null ? null : received.remove(inviter.getUniqueId());
        return expiry != null && expiry >= System.currentTimeMillis();
    }

    private void start(Player player0, Player player1) {
        GameEngine engine = GameEngine.start(decks.deck(player0), decks.deck(player1));
        begin(new Match(engine, human(player0), human(player1)));
    }

    private static Participant.Human human(Player player) {
        return new Participant.Human(player.getUniqueId(), player.getName());
    }

    /** Registers a new match for its human players, lets an AI side do its mulligan, and opens the dialogs. */
    private void begin(Match match) {
        for (int side = 0; side < 2; side++) {
            if (match.participant(side) instanceof Participant.Human human) {
                matches.put(human.id(), match);
                invites.remove(human.id());
            }
        }
        runAi(match);
        for (int side = 0; side < 2; side++) {
            if (match.participant(side) instanceof Participant.Human human) {
                Player player = Bukkit.getPlayer(human.id());
                if (player != null) {
                    Ui u = ui(player);
                    u.view = null;
                    u.notified = false;
                    show(player);
                }
            }
        }
    }

    // ---- Singleplayer ----

    /** Starts a game against a registered AI (the player is side 0, the AI side 1). */
    public void startSingleplayer(Player player, String aiName) {
        String name = AiRegistry.find(aiName);
        if (name == null) {
            ChatSender.unknownAi(player, aiName, AiRegistry.names());
            return;
        }
        if (matches.containsKey(player.getUniqueId())) {
            ChatSender.alreadyInGame(player);
            return;
        }
        if (!DeckRules.isValid(decks.deck(player), player)) {
            return;
        }
        QwentAi ai = AiRegistry.create(name);
        GameEngine engine = GameEngine.start(decks.deck(player), ai.deck());
        begin(new Match(engine, human(player), new Participant.Computer(ai, name)));
    }

    /** Lets every AI side act while the game waits for it. */
    private void runAi(Match match) {
        for (int side = 0; side < 2; side++) {
            switch (match.participant(side)) {
                case Participant.Computer computer ->
                        AiDriver.run(match.engine(), side, computer.ai(), computer.name(), logger);
                case Participant.Human human -> {
                    // Humans act through their dialog.
                }
            }
        }
    }

    // ---- Dialog ----

    /** Opens (or re-opens) the player's game dialog. */
    public void open(Player player) {
        Match match = match(player);
        if (match == null) {
            ChatSender.notInGameUseInvite(player);
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

    /** Shows or hides the history column for this player (kept until they leave the server). */
    public void toggleHistory(Player player) {
        if (!historyHidden.remove(player.getUniqueId())) {
            historyHidden.add(player.getUniqueId());
        }
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
        dialog.show(player, match, match.sideOf(player.getUniqueId()), u.view,
                historyHidden.contains(player.getUniqueId()));
    }

    /**
     * After a player's action: ends the game if it is over, otherwise shows the clicking player their dialog and
     * refreshes the opponent's (if open; if closed and it is their move, they get a chat message).
     */
    public void afterAction(Player clicker, Match match) {
        runAi(match);
        GameState state = match.state();
        if (state.phase() == GameState.Phase.OVER) {
            finish(match, state.winner(), "Final points: " + match.name(0) + " " + state.points(0) + " : "
                    + state.points(1) + " " + match.name(1) + ".");
            return;
        }
        for (int side = 0; side < 2; side++) {
            switch (match.participant(side)) {
                case Participant.Human human -> refreshOrNotify(human, clicker, state.awaits(side));
                case Participant.Computer computer -> {
                    // The AI already acted in runAi.
                }
            }
        }
    }

    /**
     * Shows the human player the new state if they clicked or have the dialog open; otherwise, if the game now
     * waits for them, tells them once in chat.
     */
    private void refreshOrNotify(Participant.Human human, Player clicker, boolean awaited) {
        Player player = Bukkit.getPlayer(human.id());
        if (player == null) {
            return;
        }
        Ui u = ui(player);
        if (player.equals(clicker) || u.open) {
            show(player);
        } else if (awaited && !u.notified) {
            u.notified = true;
            ChatSender.yourMove(player);
        }
    }

    // ---- Ending ----

    public void surrender(Player player) {
        Match match = match(player);
        if (match == null) {
            ChatSender.notInGame(player);
            return;
        }
        int side = match.sideOf(player.getUniqueId());
        finish(match, GameState.other(side), match.name(side) + " surrendered.");
    }

    /** Ends the game: removes it and shows both players the result. {@code winner} -1 = draw. */
    private void finish(Match match, int winner, String reason) {
        match.engine().end();
        for (int side = 0; side < 2; side++) {
            if (match.participant(side) instanceof Participant.Human human) {
                matches.remove(human.id());
                ui.remove(human.id());
            }
        }
        for (int side = 0; side < 2; side++) {
            if (!(match.participant(side) instanceof Participant.Human human)) {
                continue;
            }
            Player player = Bukkit.getPlayer(human.id());
            if (player == null) {
                continue;
            }
            String result = winner < 0 ? "Draw!" : winner == side ? "You won!" : "You lost.";
            String score = "Points: you " + match.state().points(side) + " : "
                    + match.state().points(GameState.other(side)) + " " + match.name(GameState.other(side));
            NamedTextColor color = winner < 0 ? NamedTextColor.GOLD
                    : winner == side ? NamedTextColor.GREEN : NamedTextColor.RED;
            ChatSender.gameResult(player, result, reason, color);
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
        historyHidden.remove(id);
    }
}
