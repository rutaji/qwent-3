package com.rutaji.changingitems.cards;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Each player's starting deck (outside of any game). In memory only for now; deck editing and saving come later.
 * A game always works on a copy, so nothing that happens in a game changes the stored deck.
 */
public final class DeckStore {
    private static final int DEFAULT_COPIES = 4;

    private final Map<UUID, List<CardType>> decks = new HashMap<>();

    public List<CardType> deck(Player player) {
        return Collections.unmodifiableList(decks.computeIfAbsent(player.getUniqueId(), id -> defaultDeck()));
    }

    public void setDeck(Player player, List<CardType> deck) {
        decks.put(player.getUniqueId(), new ArrayList<>(deck));
    }

    /** {@value #DEFAULT_COPIES} copies of every card. */
    public static List<CardType> defaultDeck() {
        List<CardType> deck = new ArrayList<>();
        for (CardType type : CardType.values()) {
            for (int i = 0; i < DEFAULT_COPIES; i++) {
                deck.add(type);
            }
        }
        return deck;
    }
}
