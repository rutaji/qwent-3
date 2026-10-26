package com.rutaji.qwent3.cards.deck;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** What makes a starting deck valid. Players with an invalid deck cannot start a game. */
public final class DeckRules {
    public static final int MIN_CARDS = 15;
    public static final int MAX_CARDS = 30;
    public static final int MAX_COPIES = 3;
    /** Cards that may not be in a deck at all. */
    public static final Set<CardType> BLACKLIST = Set.of(CardType.ENDER_DRAGON);

    private DeckRules() {
    }

    /** Why the deck is invalid; empty if it is valid. */
    public static List<String> problems(List<CardType> deck) {
        List<String> problems = new ArrayList<>();
        if (deck.size() < MIN_CARDS) {
            problems.add("It has " + deck.size() + " cards, it needs at least " + MIN_CARDS + ".");
        }
        if (deck.size() > MAX_CARDS) {
            problems.add("It has " + deck.size() + " cards, the maximum is " + MAX_CARDS + ".");
        }
        Map<CardType, Integer> copies = new EnumMap<>(CardType.class);
        for (CardType card : deck) {
            copies.merge(card, 1, Integer::sum);
        }
        copies.forEach((card, count) -> {
            if (count > MAX_COPIES) {
                problems.add("It has " + count + " copies of " + card.displayName() + ", the maximum is " + MAX_COPIES + ".");
            }
            if (BLACKLIST.contains(card)) {
                problems.add(card.displayName() + " is not allowed in decks.");
            }
        });
        return problems;
    }

    public static boolean isValid(List<CardType> deck) {
        return isValid(deck, null);
    }

    /** Checks the deck; if it is invalid and a player is given, tells them why (they can't start a game). */
    public static boolean isValid(List<CardType> deck, @Nullable Player player) {
        List<String> problems = problems(deck);
        if (!problems.isEmpty() && player != null) {
            player.sendMessage(Component.text("Your deck is not valid, you can't start a game:", NamedTextColor.RED));
            for (String problem : problems) {
                player.sendMessage(Component.text(" - " + problem, NamedTextColor.RED));
            }
        }
        return problems.isEmpty();
    }

    /** The message confirming a valid deck, e.g. after editing it. */
    public static Component validMessage(List<CardType> deck) {
        return Component.text("Your deck is valid (" + deck.size() + " cards).", NamedTextColor.GREEN);
    }
}
