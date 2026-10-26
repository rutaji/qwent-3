package com.rutaji.qwent3.cards.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.Collection;

/** All chat messages about games, invites and singleplayer. */
public final class ChatSender {
    private ChatSender() {
    }

    //region [Invites]
    public static void cannotPlayYourself(Player player) {
        player.sendMessage(Component.text("You can't play against yourself.", NamedTextColor.RED));
    }

    /** The player can't start or join a game because they are already in one. */
    public static void alreadyInGame(Player player) {
        player.sendMessage(Component.text("You are already in a game. /qwent surrender to leave it.", NamedTextColor.RED));
    }

    public static void otherAlreadyInGame(Player player, Player other) {
        player.sendMessage(Component.text(other.getName() + " is already in a game.", NamedTextColor.RED));
    }

    public static void inviteSent(Player from, Player to) {
        from.sendMessage(Component.text("Invite sent to " + to.getName() + ".", NamedTextColor.GREEN));
    }

    /** The invite with clickable [Accept] / [Decline], which run /qwent accept|decline &lt;inviter&gt;. */
    public static void invite(Player to, Player from) {
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

    public static void noInvite(Player player, Player inviter) {
        player.sendMessage(Component.text("You have no invite from " + inviter.getName() + " (it may have expired).",
                NamedTextColor.RED));
    }

    /** Accepting an invite while already in a game. */
    public static void cannotAcceptInGame(Player player) {
        player.sendMessage(Component.text("You are already in a game.", NamedTextColor.RED));
    }

    public static void inviterInAnotherGame(Player player, Player inviter) {
        player.sendMessage(Component.text(inviter.getName() + " is already in another game.", NamedTextColor.RED));
    }

    /** The invite was accepted, but the inviter's deck is no longer valid: tells both. */
    public static void inviterDeckInvalid(Player player, Player inviter) {
        player.sendMessage(Component.text(inviter.getName() + "'s deck is not valid anymore.", NamedTextColor.RED));
        inviter.sendMessage(Component.text(player.getName() + " accepted, but your deck is not valid.",
                NamedTextColor.RED));
    }

    /** The invite was declined: tells both. */
    public static void inviteDeclined(Player player, Player inviter) {
        player.sendMessage(Component.text("Invite declined.", NamedTextColor.GRAY));
        inviter.sendMessage(Component.text(player.getName() + " declined your invite.", NamedTextColor.GRAY));
    }
    //endregion

    //region [Singleplayer]
    public static void unknownAi(Player player, String aiName, Collection<String> available) {
        player.sendMessage(Component.text("Unknown AI '" + aiName + "'. Available: " + String.join(", ", available),
                NamedTextColor.RED));
    }
    //endregion

    //region [Game]
    /** /qwent without a running game. */
    public static void notInGameUseInvite(Player player) {
        player.sendMessage(Component.text("You are not in a game. Invite someone with /qwent new <player>.",
                NamedTextColor.GRAY));
    }

    /** E.g. /qwent surrender without a running game. */
    public static void notInGame(Player player) {
        player.sendMessage(Component.text("You are not in a game.", NamedTextColor.GRAY));
    }

    /** It's the player's move while their game dialog is closed; [Open] runs /qwent. */
    public static void yourMove(Player player) {
        player.sendMessage(Component.text("Qwent: it's your move. ", NamedTextColor.GOLD)
                .append(Component.text("[Open]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/qwent"))));
    }

    /** The end of a game, e.g. "Qwent: You won! Final points: ...". */
    public static void gameResult(Player player, String result, String reason, TextColor color) {
        player.sendMessage(Component.text("Qwent: " + result + " " + reason, color));
    }
    //endregion
}
