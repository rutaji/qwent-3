package com.rutaji.qwent3;

import com.rutaji.qwent3.cards.CardItems;
import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.DeckEditor;
import com.rutaji.qwent3.cards.DeckRules;
import com.rutaji.qwent3.cards.DeckStore;
import com.rutaji.qwent3.cards.DialogClickListener;
import com.rutaji.qwent3.cards.GameDialog;
import com.rutaji.qwent3.cards.GameManager;
import com.rutaji.qwent3.cards.Glyphs;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class Qwent3 extends JavaPlugin {
    private @Nullable DeckEditor deckEditor;

    @Override
    public void onEnable() {
        DeckStore decks = new DeckStore(this);
        CardItems cardItems = new CardItems(this);
        DeckEditor deckEditor = new DeckEditor(decks, cardItems);
        this.deckEditor = deckEditor;
        GameManager games = new GameManager(decks, new GameDialog(new Glyphs(getLogger())));

        getServer().getPluginManager().registerEvents(games, this);
        getServer().getPluginManager().registerEvents(deckEditor, this);
        getServer().getPluginManager().registerEvents(new DialogClickListener(this, games), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("qwent")
                                .requires(source -> source.getExecutor() instanceof Player)
                                // Opens the running game (e.g. after Esc).
                                .executes(ctx -> {
                                    games.open(executor(ctx));
                                    return 1;
                                })
                                .then(Commands.literal("new")
                                        .then(Commands.argument("player", ArgumentTypes.player())
                                                .executes(ctx -> {
                                                    games.invite(executor(ctx), target(ctx));
                                                    return 1;
                                                })))
                                .then(Commands.literal("accept")
                                        .then(Commands.argument("player", ArgumentTypes.player())
                                                .executes(ctx -> {
                                                    games.accept(executor(ctx), target(ctx));
                                                    return 1;
                                                })))
                                .then(Commands.literal("decline")
                                        .then(Commands.argument("player", ArgumentTypes.player())
                                                .executes(ctx -> {
                                                    games.decline(executor(ctx), target(ctx));
                                                    return 1;
                                                })))
                                .then(Commands.literal("surrender")
                                        .executes(ctx -> {
                                            games.surrender(executor(ctx));
                                            return 1;
                                        }))
                                // Gives every card that is allowed in decks (DeckRules.MAX_COPIES of each) as items. Ops only by default.
                                .then(Commands.literal("all")
                                        .requires(source -> source.getSender().hasPermission("qwent.admin"))
                                        .executes(ctx -> {
                                            giveAllCards(executor(ctx), cardItems);
                                            return 1;
                                        }))
                                .then(Commands.literal("info")
                                        .executes(ctx -> {
                                            deckInfo(executor(ctx), decks);
                                            return 1;
                                        }))
                                .then(Commands.literal("deck")
                                        .executes(ctx -> {
                                            deckEditor.open(executor(ctx));
                                            return 1;
                                        }))
                                .build(),
                        "Qwent card game"));

        getLogger().info("qwent3 enabled!");
    }

    /** Card, monster and spell counts of the player's starting deck, and whether it is valid. */
    private static void deckInfo(Player player, DeckStore decks) {
        List<CardType> deck = decks.deck(player);
        long monsters = deck.stream().filter(CardType::isMonster).count();
        player.sendMessage(Component.text("Your deck: " + deck.size() + " cards (" + monsters + " monsters, "
                + (deck.size() - monsters) + " spells)", NamedTextColor.GOLD));
        DeckEditor.tellValidity(player, deck);
    }

    private static void giveAllCards(Player player, CardItems cardItems) {
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

    private static Player executor(CommandContext<CommandSourceStack> ctx) {
        return (Player) ctx.getSource().getExecutor();
    }

    private static Player target(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
    }

    @Override
    public void onDisable() {
        if (deckEditor != null) {
            deckEditor.closeAll();
        }
        getLogger().info("qwent3 disabled!");
    }
}
