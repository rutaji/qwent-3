package com.rutaji.qwent3.cards.effect.board;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.effect.board.factory.Pick;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The owner picks a tile the action accepts (it may be empty, e.g. where a monster can be summoned) and the action is
 * done to it, {@code count} times, one tile at a time; with {@code random} the tiles are picked at random without
 * asking. Stops when no tile is accepted any more; cancelled if none is at the start. Factories in {@link Pick}.
 */
public final class PickTileEffect implements Effect {
    private final CardType source;
    private final TileAction action;
    private final String text;
    private final int count;
    private final boolean random;
    private int done;

    /** @param text the whole effect for the description, e.g. "summon a Silverfish on an empty tile" */
    public PickTileEffect(CardType source, TileAction action, String text, int count, boolean random) {
        this.source = source;
        this.action = action;
        this.text = text;
        this.count = count;
        this.random = random;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": " + text
                + (count > 1 ? " (" + (done + 1) + " of " + count + ")" : ""));
    }

    @Override
    public boolean needsTarget() {
        return !random;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return done < count && !tiles(state, owner).isEmpty();
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int tile) {
        return tile >= 0 && tile < GameState.BOARD_SIZE && action.accepts(state, owner, tile);
    }

    @Override
    public void resolve(GameEngine engine, int owner, int tile) {
        if (random) {
            List<Integer> tiles = tiles(engine.state(), owner);
            tile = tiles.get(ThreadLocalRandom.current().nextInt(tiles.size()));
        }
        action.apply(engine, owner, tile);
        if (++done < count && hasValidTargets(engine.state(), owner)) {
            engine.queueNext(owner, this);
        }
    }

    private List<Integer> tiles(GameState state, int owner) {
        List<Integer> tiles = new ArrayList<>();
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            if (isValidTarget(state, owner, tile)) {
                tiles.add(tile);
            }
        }
        return tiles;
    }
}
