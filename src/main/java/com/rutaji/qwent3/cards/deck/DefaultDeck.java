package com.rutaji.qwent3.cards.deck;

import com.rutaji.qwent3.cards.cardsRepository.CardType;

import java.util.ArrayList;
import java.util.List;


public final class DefaultDeck {
    private DefaultDeck() {
    }

    private static final List<Entry> DEFAULT_DECK = List.of(
            new Entry(CardType.GRAVEL, 2),
            new Entry(CardType.LIGHTNING, 1),
            new Entry(CardType.SKELETON, 2),
            new Entry(CardType.BLAZE, 1),
            new Entry(CardType.BOAR, 1),
            new Entry(CardType.BEE, 2),
            new Entry(CardType.LEADER_ZOMBIE, 1),
            new Entry(CardType.BASIC_ZOMBIE, 3),
            new Entry(CardType.DESERT_ZOMBIE, 2),
            new Entry(CardType.CACTUS, 2),
            new Entry(CardType.SHAMAN, 2),
            new Entry(CardType.ARROW, 1),
            new Entry(CardType.ZOMBIE_AWAKENING, 1));

    private record Entry(CardType card, int copies) {
    }

    public static List<CardType> defaultDeck() {
        List<CardType> deck = new ArrayList<>();
        for (Entry entry : DEFAULT_DECK) {
            for (int i = 0; i < entry.copies(); i++) {
                deck.add(entry.card());
            }
        }
        return deck;
    }

}
