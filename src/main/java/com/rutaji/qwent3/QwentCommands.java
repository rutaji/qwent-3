package com.rutaji.qwent3;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.rutaji.qwent3.ai.AiRegistry;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.deck.DeckEditor;
import com.rutaji.qwent3.cards.deck.DeckRules;
import com.rutaji.qwent3.cards.deck.DeckStore;
import com.rutaji.qwent3.cards.gameLogic.GameManager;
import com.rutaji.qwent3.cards.items.CardItems;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The /qwent command (players only):
 * <pre>
 * /qwent                      reopen your running game (e.g. after Esc)
 * /qwent new &lt;player&gt;         invite a player
 * /qwent accept &lt;player&gt;      accept an invite
 * /qwent decline &lt;player&gt;     decline an invite
 * /qwent singleplayer &lt;ai&gt;    play against an AI from AiRegistry
 * /qwent surrender            give up the running game
 * /qwent deck                 edit your starting deck
 * /qwent info                 card / monster / spell counts of your deck
 * /qwent all                  get every allowed card as items (permission qwent.admin, ops by default)
 * </pre>
 */
public final class QwentCommands {
    private static final String PLAYER_ARG = "player";
    private static final String AI_ARG = "ai";
    private static final String ADMIN_PERMISSION = "qwent.admin";

    private final GameManager games;
    private final DeckStore decks;
    private final DeckEditor deckEditor;
    private final CardItems cardItems;

    public QwentCommands(GameManager games, DeckStore decks, DeckEditor deckEditor, CardItems cardItems) {
        this.games = games;
        this.decks = decks;
        this.deckEditor = deckEditor;
        this.cardItems = cardItems;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("qwent")
                .requires(source -> source.getExecutor() instanceof Player)
                .executes(ctx -> run(ctx, games::open))
                .then(withPlayer("new", games::invite))
                .then(withPlayer("accept", games::accept))
                .then(withPlayer("decline", games::decline))
                .then(singleplayer())
                .then(simple("surrender", games::surrender))
                .then(simple("deck", deckEditor::open))
                .then(simple("info", this::deckInfo))
                .then(simple("all", this::giveAllCards)
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION)))
                .build();
    }

    // ---- Subcommands ----

    /** /qwent singleplayer &lt;ai&gt;, with the registered AI names as suggestions. */
    private LiteralArgumentBuilder<CommandSourceStack> singleplayer() {
        return Commands.literal("singleplayer")
                .then(Commands.argument(AI_ARG, StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            AiRegistry.names().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> run(ctx, player ->
                                games.startSingleplayer(player, StringArgumentType.getString(ctx, AI_ARG)))));
    }

    /** Card, monster and spell counts of the player's starting deck, and whether it is valid. */
    private void deckInfo(Player player) {
        List<CardType> deck = decks.deck(player);
        long monsters = deck.stream().filter(CardType::isMonster).count();
        player.sendMessage(Component.text("Your deck: " + deck.size() + " cards (" + monsters + " monsters, "
                + (deck.size() - monsters) + " spells)", NamedTextColor.GOLD));
        if (DeckRules.isValid(deck, player)) {
            player.sendMessage(DeckRules.validMessage(deck));
        }
    }

    /** {@link DeckRules#MAX_COPIES} of every card allowed in decks, as items (leftovers drop at the player's feet). */
    private void giveAllCards(Player player) {
        for (CardType card : CardType.values()) {
            if (DeckRules.BLACKLIST.contains(card)) {
                continue;
            }
            player.getInventory().addItem(cardItems.create(card, DeckRules.MAX_COPIES)).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        player.sendMessage(Component.text("You got " + DeckRules.MAX_COPIES + " copies of every card.",
                NamedTextColor.GREEN));
    }

    // ---- Helpers ----

    /** A subcommand without arguments that runs an action for the executing player. */
    private static LiteralArgumentBuilder<CommandSourceStack> simple(String name, Consumer<Player> action) {
        return Commands.literal(name).executes(ctx -> run(ctx, action));
    }

    /** A subcommand with one online-player argument: action(executing player, that player). */
    private static LiteralArgumentBuilder<CommandSourceStack> withPlayer(String name, BiConsumer<Player, Player> action) {
        return Commands.literal(name)
                .then(Commands.argument(PLAYER_ARG, ArgumentTypes.player())
                        .executes(ctx -> {
                            action.accept(executor(ctx), targetPlayer(ctx));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, Consumer<Player> action) {
        action.accept(executor(ctx));
        return Command.SINGLE_SUCCESS;
    }

    /** The player running the command (the root command only allows players). */
    private static Player executor(CommandContext<CommandSourceStack> ctx) {
        return (Player) ctx.getSource().getExecutor();
    }

    private static Player targetPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getArgument(PLAYER_ARG, PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
    }
}
