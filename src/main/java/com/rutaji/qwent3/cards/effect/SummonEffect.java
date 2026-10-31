package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Creates {@code count} copies of a monster card and plays them, one at a time, on empty tiles of the owner's side
 * (their on-play effects trigger too). The owner picks each tile, or with {@link #random} they go to random ones
 * without asking. Stops when the side is full; cancelled if it is full at the start.
 */
public final class SummonEffect implements Effect {
    private final CardType source;
    private final CardType summoned;
    private final int count;
    private final boolean random;
    private int done;

    /** One copy; the player picks the empty tile. */
    public SummonEffect(CardType source, CardType summoned) {
        this(source, summoned, 1);
    }

    /** The player picks the empty tile for each copy. */
    public SummonEffect(CardType source, CardType summoned, int count) {
        this(source, summoned, count, false);
    }

    private SummonEffect(CardType source, CardType summoned, int count, boolean random) {
        if (!summoned.isMonster()) {
            throw new IllegalArgumentException("Only monsters can be summoned: " + summoned);
        }
        this.source = source;
        this.summoned = summoned;
        this.count = count;
        this.random = random;
    }

    /** One copy on a random empty tile, without asking the player. */
    public static SummonEffect random(CardType source, CardType summoned) {
        return random(source, summoned, 1);
    }

    /** Copies on random empty tiles, without asking the player. */
    public static SummonEffect random(CardType source, CardType summoned, int count) {
        return new SummonEffect(source, summoned, count, true);
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": summon a " + summoned.displayName()
                + (random ? " on a random empty tile" : " on an empty tile of your side")
                + (count > 1 ? " (" + (done + 1) + " of " + count + ")" : ""));
    }

    @Override
    public boolean needsTarget() {
        return !random;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return done < count && !emptyTiles(state, owner).isEmpty();
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
        if (++done < count && hasValidTargets(engine.state(), owner)) {
            engine.queueNext(owner, this);
        }
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
