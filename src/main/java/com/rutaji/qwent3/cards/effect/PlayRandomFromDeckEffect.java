package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Plays a random monster from the owner's deck. First the monster is chosen at random (among the deck's monsters that
 * have a free tile they can go on) and shown to the owner; then they pick its tile - their side, or either side if it
 * is treacherous. The card leaves the deck when it is played. Cancelled if no monster in the deck can be played.
 */
public final class PlayRandomFromDeckEffect implements Effect {
    private final CardType source;

    public PlayRandomFromDeckEffect(CardType source) {
        this.source = source;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": play a random monster from your deck");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return !playable(state, owner).isEmpty();
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        List<CardType> playable = playable(engine.state(), owner);
        CardType card = playable.get(ThreadLocalRandom.current().nextInt(playable.size()));
        engine.queueNext(owner, new PlaceChosen(card));
    }

    /** Monsters in the deck (one entry per copy) that have at least one tile they can be played on. */
    private static List<CardType> playable(GameState state, int owner) {
        return state.side(owner).deck().stream()
                .filter(card -> card.isMonster() && hasTile(state, owner, card))
                .toList();
    }

    private static boolean hasTile(GameState state, int owner, CardType card) {
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            if (state.canPlay(owner, card, tile)) {
                return true;
            }
        }
        return false;
    }

    /** Second step: the owner sees which monster it is and picks its tile. */
    private final class PlaceChosen implements Effect {
        private final CardType card;

        private PlaceChosen(CardType card) {
            this.card = card;
        }

        @Override
        public Component description() {
            return Component.text(source.displayName() + ": play " + card.displayName() + " (random from your deck)");
        }

        @Override
        public boolean hasValidTargets(GameState state, int owner) {
            return state.pileContains(owner, Pile.DECK, card) && Effect.super.hasValidTargets(state, owner);
        }

        @Override
        public boolean isValidTarget(GameState state, int owner, int target) {
            return state.canPlay(owner, card, target);
        }

        @Override
        public void resolve(GameEngine engine, int owner, int target) {
            if (engine.removeFromPile(owner, Pile.DECK, card)) {
                engine.play(owner, card, target);
            }
        }
    }
}
