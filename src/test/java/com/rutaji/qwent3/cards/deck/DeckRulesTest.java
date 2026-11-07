package com.rutaji.qwent3.cards.deck;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Deck validity ({@link DeckRules#isValid}) and building a deck from cards ({@link DeckRules#fit}). */
class DeckRulesTest {

    /** {@code size} different cards, none blacklisted. */
    private static List<CardType> differentCards(int size) {
        List<CardType> deck = new ArrayList<>();
        for (CardType card : CardType.values()) {
            if (deck.size() < size && !DeckRules.BLACKLIST.contains(card)) {
                deck.add(card);
            }
        }
        assertEquals(size, deck.size());
        return deck;
    }

    @Test
    void sizeMustBeBetweenMinAndMax() {
        assertTrue(DeckRules.isValid(differentCards(DeckRules.MIN_CARDS)));
        assertTrue(DeckRules.isValid(differentCards(DeckRules.MAX_CARDS)));
        assertFalse(DeckRules.isValid(differentCards(DeckRules.MIN_CARDS - 1)));
        assertFalse(DeckRules.isValid(differentCards(DeckRules.MAX_CARDS + 1)));
    }

    @Test
    void atMostMaxCopies() {
        List<CardType> deck = differentCards(DeckRules.MIN_CARDS);
        deck.set(1, deck.get(0));
        deck.set(2, deck.get(0));
        assertTrue(DeckRules.isValid(deck));                              // 3 copies
        deck.set(3, deck.get(0));
        assertFalse(DeckRules.isValid(deck));                             // 4 copies
    }

    @Test
    void enderDragonIsBlacklisted() {
        assertTrue(DeckRules.BLACKLIST.contains(ENDER_DRAGON));
        List<CardType> deck = differentCards(DeckRules.MIN_CARDS);
        deck.add(ENDER_DRAGON);
        assertFalse(DeckRules.isValid(deck));
    }

    @Test
    void fitKeepsWhatIsAllowedInOrder() {
        List<CardType> cards = new ArrayList<>(Collections.nCopies(4, BEAR));
        cards.add(ENDER_DRAGON);
        cards.addAll(differentCards(DeckRules.MAX_CARDS + 5));          // includes 1 more Bear

        DeckRules.DeckFit fit = DeckRules.fit(cards, null);
        assertEquals(DeckRules.MAX_CARDS, fit.deck().size());
        assertEquals(3, Collections.frequency(fit.deck(), BEAR));
        assertFalse(fit.deck().contains(ENDER_DRAGON));
        assertEquals(List.of(BEAR, BEAR, BEAR), fit.deck().subList(0, 3)); // first cards first
        assertEquals(cards.size(), fit.deck().size() + fit.rejected().size());
        assertTrue(fit.rejected().contains(ENDER_DRAGON));
        assertEquals(2, Collections.frequency(fit.rejected(), BEAR));
        assertTrue(DeckRules.isValid(fit.deck()));
    }
}
