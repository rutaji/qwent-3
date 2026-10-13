package com.rutaji.qwent3.cards;

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
        return problems(deck).isEmpty();
    }
}
