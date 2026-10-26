package com.rutaji.qwent3.cards.deck;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Deck validity and the fixed default deck. */
class DeckRulesTest {

    private static List<CardType> deckOf(int copiesEach, CardType... cards) {
        List<CardType> deck = new ArrayList<>();
        for (CardType card : cards) {
            deck.addAll(Collections.nCopies(copiesEach, card));
        }
        return deck;
    }

    @Test
    void fifteenToThirtyCardsMaxThreeCopies() {
        assertTrue(DeckRules.isValid(deckOf(3, BEAR, ARROW, GRAVEL, SKELETON, BOAR)));                 // 15
        assertTrue(DeckRules.isValid(deckOf(3, BEAR, ARROW, GRAVEL, SKELETON, BOAR,
                BEE, BARD, SHAMAN, HEALER, WARLOCK)));                                                  // 30
        assertFalse(DeckRules.isValid(deckOf(2, BEAR, ARROW, GRAVEL, SKELETON, BOAR, BEE, BARD)));    // 14
    }

    @Test
    void problemsExplainWhatIsWrong() {
        List<CardType> deck = deckOf(3, BEAR, ARROW, GRAVEL, SKELETON);
        deck.add(BEAR);
        List<String> problems = DeckRules.problems(deck);
        assertTrue(problems.contains("It has 13 cards, it needs at least 15."));
        assertTrue(problems.contains("It has 4 copies of Bear, the maximum is 3."));

        List<CardType> big = new ArrayList<>();
        for (CardType card : CardType.values()) {
            if (big.size() < 31 && !DeckRules.BLACKLIST.contains(card)) {
                big.add(card);
            }
        }
        assertTrue(DeckRules.problems(big).contains("It has 31 cards, the maximum is 30."));
    }

    @Test
    void enderDragonIsBlacklisted() {
        assertTrue(DeckRules.BLACKLIST.contains(ENDER_DRAGON));
        List<CardType> deck = deckOf(3, BEAR, ARROW, GRAVEL, SKELETON, BOAR);
        deck.add(ENDER_DRAGON);
        assertTrue(DeckRules.problems(deck).contains("Ender Dragon is not allowed in decks."));
    }

    @Test
    void defaultDeckIsTheFixedValidList() {
        List<CardType> expected = new ArrayList<>();
        expected.addAll(Collections.nCopies(2, GRAVEL));
        expected.add(LIGHTNING);
        expected.addAll(Collections.nCopies(2, SKELETON));
        expected.add(BLAZE);
        expected.add(BOAR);
        expected.addAll(Collections.nCopies(2, BEE));
        expected.add(LEADER_ZOMBIE);
        expected.addAll(Collections.nCopies(3, BASIC_ZOMBIE));
        expected.addAll(Collections.nCopies(2, DESERT_ZOMBIE));
        expected.addAll(Collections.nCopies(2, CACTUS));
        expected.addAll(Collections.nCopies(2, SHAMAN));
        expected.add(ARROW);
        expected.add(ZOMBIE_AWAKENING);
        assertEquals(expected, DeckStore.defaultDeck());
        assertEquals(21, DeckStore.defaultDeck().size());
        assertTrue(DeckRules.isValid(DeckStore.defaultDeck()));
    }
}
