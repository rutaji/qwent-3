package com.rutaji.qwent3.cards.deck;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.chat.ChatSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** What makes a starting deck valid. Players with an invalid deck cannot start a game. */
public final class DeckRules {
    public static final int MIN_CARDS = 20;
    public static final int MAX_CARDS = 30;
    public static final int MAX_COPIES = 3;
    /** Cards that may not be in a deck at all. */
    public static final Set<CardType> BLACKLIST = Set.of(CardType.ENDER_DRAGON);

    private DeckRules() {
    }

    /** Cards split into the ones that fit in a deck and the ones that don't (see {@link #fit}). */
    public record DeckFit(List<CardType> deck, List<CardType> rejected) {
    }

    /**
     * Puts the cards into a deck in order. Blacklisted cards are rejected (and the player, if given, is told about
     * each); every other card is kept only if {@link #canAdd} allows it, the rest (copies above {@link #MAX_COPIES},
     * cards above {@link #MAX_CARDS}) are rejected too. The deck may still be too small.
     */
    public static DeckFit fit(List<CardType> cards, @Nullable Player player) {
        List<CardType> deck = new ArrayList<>();
        List<CardType> rejected = new ArrayList<>();
        Set<CardType> blacklisted = EnumSet.noneOf(CardType.class);
        for (CardType card : cards) {
            if (isBlacklisted(card)) {
                blacklisted.add(card);
                rejected.add(card);
            } else if (canAdd(deck, card)) {
                deck.add(card);
            } else {
                rejected.add(card);
            }
        }
        if (player != null) {
            for (CardType card : blacklisted) {
                ChatSender.blacklisted(player, card);
            }
        }
        return new DeckFit(deck, rejected);
    }

    /** Whether the card may not be in a deck at all. */
    private static boolean isBlacklisted(CardType card) {
        return BLACKLIST.contains(card);
    }

    /**
     * Whether one more copy of the card may be added to the deck: it has fewer than {@link #MAX_CARDS} cards and
     * fewer than {@link #MAX_COPIES} copies of it. Doesn't check the blacklist ({@link #fit} does) or too few cards
     * (a deck being built is allowed to be short).
     */
    private static boolean canAdd(List<CardType> deck, CardType card) {
        return deck.size() < MAX_CARDS && Collections.frequency(deck, card) < MAX_COPIES;
    }

    public static boolean isValid(List<CardType> deck) {
        return isValid(deck, null);
    }

    /** Checks the deck; if it is invalid and a player is given, tells them why (they can't start a game). */
    public static boolean isValid(List<CardType> deck, @Nullable Player player) {
        boolean valid = true;
        if (deck.size() < MIN_CARDS) {
            valid = false;
            if (player != null) {
                ChatSender.deckTooSmall(player, deck.size());
            }
        }
        if (deck.size() > MAX_CARDS) {
            valid = false;
            if (player != null) {
                ChatSender.deckTooBig(player, deck.size());
            }
        }
        Map<CardType, Integer> copies = new EnumMap<>(CardType.class);
        for (CardType card : deck) {
            copies.merge(card, 1, Integer::sum);
        }
        for (Map.Entry<CardType, Integer> entry : copies.entrySet()) {
            CardType card = entry.getKey();
            if (entry.getValue() > MAX_COPIES) {
                valid = false;
                if (player != null) {
                    ChatSender.tooManyCopies(player, card, entry.getValue());
                }
            }
            if (isBlacklisted(card)) {
                valid = false;
                if (player != null) {
                    ChatSender.notAllowedInDeck(player, card);
                }
            }
        }
        if (!valid && player != null) {
            ChatSender.deckInvalid(player);
        }
        return valid;
    }

    /** Tells the player whether their deck is valid, and if not, why (e.g. after editing it, /qwent info). */
    public static void tellValidity(List<CardType> deck, Player player) {
        if (isValid(deck, player)) {
            ChatSender.deckValid(player, deck.size());
        }
    }
}
