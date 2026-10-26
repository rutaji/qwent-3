package com.rutaji.qwent3.cards.cardsRepository;

/** Rules keywords of a card (unlike {@link Tag}s, which are just labels effects can look for). */
public enum Keyword {
    /**
     * Can also be played on an empty tile of the opponent's side. It then is the opponent's unit (its power counts
     * for them, its on-damaged / on-death effects are theirs, it goes to their graveyard); only its on-play effects
     * belong to the player who played it.
     */
    TREACHEROUS("Treacherous", "can also be played on your opponent's side.");

    private final String displayName;
    private final String explanation;

    Keyword(String displayName, String explanation) {
        this.displayName = displayName;
        this.explanation = explanation;
    }

    public String displayName() {
        return displayName;
    }

    /** Short reminder shown after the keyword in the card text. */
    public String explanation() {
        return explanation;
    }
}
