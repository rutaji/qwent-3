package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Pile;
import com.rutaji.qwent3.cards.effect.effectTargets.SideTarget;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.CardFilter;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Plays up to {@code count} matching cards for the owner from a deck or graveyard ({@code pile}) of a side
 * ({@code side}, seen from the owner), one at a time. Each time the top-most matching card that can be played now is
 * taken (the deck's top is its first card, the graveyard's the one that entered it last):
 * <ul>
 *     <li>a monster is shown to the owner, who picks its tile (their side, or either side if it is treacherous) -
 *     or with {@code random} it goes to a random such tile without asking; it leaves the pile when it is played;</li>
 *     <li>a spell leaves the pile and is cast for the owner right away, as if played from hand: it goes into the
 *     owner's graveyard, then its effects resolve (it counts as casting a spell).</li>
 * </ul>
 * Stops when {@code count} cards were played or none can be; cancelled if none can be at the start. No target to pick
 * (the monster's tile is picked in the next step).
 */
public final class PlayFromPileEffect implements Effect {
    /** For {@code count}: as many as possible ("play all ..."). */
    public static final int ALL = Integer.MAX_VALUE;

    private final CardType source;
    /** Null = any card. */
    private final @Nullable CardFilter filter;
    private final String what;
    private final int count;
    private final SideTarget side;
    private final Pile pile;
    /** True = monsters go to a random tile they can be played on, without asking the owner. */
    private final boolean random;

    /** @param what the cards for the description, e.g. "every Basic Zombie", "2 monsters", "the last spell" */
    public PlayFromPileEffect(CardType source, @Nullable CardFilter filter, String what, int count, SideTarget side,
                              Pile pile, boolean random) {
        this.source = source;
        this.filter = filter;
        this.what = what;
        this.count = count;
        this.side = side;
        this.pile = pile;
        this.random = random;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": play " + what + " from " + pileText());
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return count > 0 && next(state, owner) != null;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        CardType card = next(engine.state(), owner);
        if (card == null) {
            return;
        }
        if (card.isMonster()) {
            engine.queueNext(owner, new PlaceMonster(card));
            return;
        }
        engine.removeFromPile(pileSide(owner), pile, card);
        queueRest(engine, owner);
        // Like a spell played from hand: into the caster's graveyard right away, then its effects resolve.
        engine.addToGraveyard(owner, card);
        engine.castSpell(owner, card, true);
    }

    /** Queues this effect again at the front for the remaining cards, if there are any and one can be played. */
    private void queueRest(GameEngine engine, int owner) {
        PlayFromPileEffect rest = new PlayFromPileEffect(source, filter, what, count == ALL ? ALL : count - 1, side,
                pile, random);
        if (rest.hasValidTargets(engine.state(), owner)) {
            engine.queueNext(owner, rest);
        }
    }

    /** The top-most matching card that can be played now (a spell, or a monster with a free tile); null if none. */
    private @Nullable CardType next(GameState state, int owner) {
        List<CardType> cards = pile == Pile.DECK
                ? state.side(pileSide(owner)).deck()
                : state.side(pileSide(owner)).graveyard().reversed();
        for (CardType card : cards) {
            if ((filter == null || filter.check(card)) && (!card.isMonster() || !tiles(state, owner, card).isEmpty())) {
                return card;
            }
        }
        return null;
    }

    /** The tiles the owner can play the monster on. */
    private static List<Integer> tiles(GameState state, int owner, CardType card) {
        List<Integer> tiles = new ArrayList<>();
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            if (state.canPlay(owner, card, tile)) {
                tiles.add(tile);
            }
        }
        return tiles;
    }

    private int pileSide(int owner) {
        return side == SideTarget.Owner ? owner : GameState.other(owner);
    }

    /** E.g. "your deck", "your opponent's graveyard". */
    private String pileText() {
        return (side == SideTarget.Opponent ? "your opponent's" : "your")
                + (pile == Pile.DECK ? " deck" : " graveyard");
    }

    /** Second step for a monster: the owner sees which one it is and picks its tile (or it goes to a random one). */
    private final class PlaceMonster implements Effect {
        private final CardType card;

        private PlaceMonster(CardType card) {
            this.card = card;
        }

        @Override
        public Component description() {
            return Component.text(source.displayName() + ": play " + card.displayName() + " from " + pileText()
                    + (random ? " on a random tile" : ""));
        }

        @Override
        public boolean needsTarget() {
            return !random;
        }

        @Override
        public boolean hasValidTargets(GameState state, int owner) {
            return state.pileContains(pileSide(owner), pile, card) && !tiles(state, owner, card).isEmpty();
        }

        @Override
        public boolean isValidTarget(GameState state, int owner, int target) {
            return state.canPlay(owner, card, target);
        }

        @Override
        public void resolve(GameEngine engine, int owner, int target) {
            if (random) {
                List<Integer> tiles = tiles(engine.state(), owner, card);
                target = tiles.get(ThreadLocalRandom.current().nextInt(tiles.size()));
            }
            if (engine.removeFromPile(pileSide(owner), pile, card)) {
                engine.play(owner, card, target);
                queueRest(engine, owner);
            }
        }
    }
}
