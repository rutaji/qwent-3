package com.rutaji.qwent3.cards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Each player's starting deck (outside of any game), stored in the player's own data (persistent data container)
 * as a list of card names. It is saved together with the player's inventory, so moving cards between the deck
 * editor and the inventory can't duplicate or lose cards if the server crashes.
 *
 * Players without a saved deck get {@link #defaultDeck()}. A game always works on a copy.
 *
 * The key has a fixed namespace ("qwent:deck"), so renaming the plugin doesn't lose saved decks.
 */
public final class DeckStore {
    private static final int DEFAULT_COPIES = 2;

    private static final NamespacedKey DECK_KEY = Objects.requireNonNull(NamespacedKey.fromString("qwent:deck"));
    /** Where decks were saved while the plugin was called ChangingItems; moved to {@link #DECK_KEY} when read. */
    private static final NamespacedKey LEGACY_DECK_KEY =
            Objects.requireNonNull(NamespacedKey.fromString("changingitems:deck"));

    private final Plugin plugin;

    public DeckStore(Plugin plugin) {
        this.plugin = plugin;
    }

    public List<CardType> deck(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        List<String> legacy = data.get(LEGACY_DECK_KEY, PersistentDataType.LIST.strings());
        if (legacy != null) {
            data.remove(LEGACY_DECK_KEY);
            if (!data.has(DECK_KEY)) {
                data.set(DECK_KEY, PersistentDataType.LIST.strings(), legacy);
            }
        }
        List<String> names = data.get(DECK_KEY, PersistentDataType.LIST.strings());
        if (names == null) {
            return defaultDeck();
        }
        List<CardType> deck = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (String name : names) {
            try {
                deck.add(CardType.valueOf(name));
            } catch (IllegalArgumentException e) {
                unknown.add(name);
            }
        }
        if (!unknown.isEmpty()) {
            // Cards that were removed from the game: drop them from the saved deck once.
            plugin.getLogger().warning("Removed unknown cards " + unknown + " from the deck of " + player.getName());
            player.sendMessage(Component.text("Some cards in your deck no longer exist and were removed.",
                    NamedTextColor.YELLOW));
            setDeck(player, deck);
        }
        return Collections.unmodifiableList(deck);
    }

    public void setDeck(Player player, List<CardType> deck) {
        List<String> names = deck.stream().map(CardType::name).toList();
        player.getPersistentDataContainer().set(DECK_KEY, PersistentDataType.LIST.strings(), names);
    }

    /** {@value #DEFAULT_COPIES} copies of every card in definition order, cut at {@link DeckRules#MAX_CARDS}. */
    public static List<CardType> defaultDeck() {
        List<CardType> deck = new ArrayList<>();
        for (CardType type : CardType.values()) {
            for (int i = 0; i < DEFAULT_COPIES && deck.size() < DeckRules.MAX_CARDS; i++) {
                deck.add(type);
            }
        }
        return deck;
    }
}
