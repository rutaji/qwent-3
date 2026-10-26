package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Creates a monster card and plays it on an empty tile of the owner's side (its on-play effect triggers too).
 * The owner picks the tile, or with {@link #random} it goes to a random one without asking.
 */
public final class SummonEffect implements Effect {
    private final CardType source;
    private final CardType summoned;
    private final boolean random;

    /** The player picks the empty tile. */
    public SummonEffect(CardType source, CardType summoned) {
        this(source, summoned, false);
    }

    private SummonEffect(CardType source, CardType summoned, boolean random) {
        if (!summoned.isMonster()) {
            throw new IllegalArgumentException("Only monsters can be summoned: " + summoned);
        }
        this.source = source;
        this.summoned = summoned;
        this.random = random;
    }

    /** Summons on a random empty tile, without asking the player. */
    public static SummonEffect random(CardType source, CardType summoned) {
        return new SummonEffect(source, summoned, true);
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": summon a " + summoned.displayName()
                + (random ? " on a random empty tile" : " on an empty tile of your side"));
    }

    @Override
    public boolean needsTarget() {
        return !random;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return !emptyTiles(state, owner).isEmpty();
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int tile) {
        return state.unit(tile) == null && GameState.sideOf(tile) == owner;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int tile) {
        if (random) {
            List<Integer> empty = emptyTiles(engine.state(), owner);
            tile = empty.get(ThreadLocalRandom.current().nextInt(empty.size()));
        }
        engine.play(owner, summoned, tile);
    }

    private List<Integer> emptyTiles(GameState state, int owner) {
        List<Integer> empty = new ArrayList<>();
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            if (isValidTarget(state, owner, tile)) {
                empty.add(tile);
            }
        }
        return empty;
    }
}
