package com.rutaji.changingitems.cards.effect;

import com.rutaji.changingitems.cards.CardType;
import com.rutaji.changingitems.cards.GameState;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Creates a monster card and plays it on an empty tile (its on-play effect triggers too).
 * The player picks the tile, or with {@link #random} it goes to a random empty tile without asking.
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
                + (random ? " on a random empty tile" : " on an empty tile"));
    }

    @Override
    public boolean needsTarget() {
        return !random;
    }

    @Override
    public boolean hasValidTargets(GameState state) {
        return !emptyTiles(state).isEmpty();
    }

    @Override
    public boolean isValidTarget(GameState state, int tile) {
        return state.unit(tile) == null;
    }

    @Override
    public void resolve(GameState state, int tile) {
        if (random) {
            List<Integer> empty = emptyTiles(state);
            tile = empty.get(ThreadLocalRandom.current().nextInt(empty.size()));
        }
        state.play(summoned, tile);
    }

    private static List<Integer> emptyTiles(GameState state) {
        List<Integer> empty = new ArrayList<>();
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            if (state.unit(tile) == null) {
                empty.add(tile);
            }
        }
        return empty;
    }
}
