package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Does something to several tiles at once: the selector finds the tiles (e.g. a unit's row), the action is done to
 * each of them it accepts (e.g. destroy the monster there). All tiles are chosen first, then acted on in board order.
 * Cancelled if the action accepts none of them. No target to pick.
 */
public final class AreaEffect implements Effect {
    private final CardType source;
    private final TileSelector selector;
    private final TileAction action;
    /** Replaces the description built from the selector and action; null = built. */
    private final @Nullable String text;

    public AreaEffect(CardType source, TileSelector selector, TileAction action) {
        this(source, selector, action, null);
    }

    /** @param text the description, when the built one reads badly, e.g. "+6 power" */
    public AreaEffect(CardType source, TileSelector selector, TileAction action, @Nullable String text) {
        this.source = source;
        this.selector = selector;
        this.action = action;
        this.text = text;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": " + (text != null ? text : action.text(selector.text())));
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return !tiles(state, owner).isEmpty();
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    /** The monsters on the chosen tiles, with their power, e.g. "Bear (7), Bear (7)"; null if there are none. */
    @Override
    public @Nullable String logDetail(GameState state, int owner) {
        List<String> names = new ArrayList<>();
        for (int tile : tiles(state, owner)) {
            Unit unit = state.unit(tile);
            if (unit != null) {
                names.add(unit.type().displayName() + " (" + unit.power() + ")");
            }
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        // Pick all first: acting on one must not change which others are chosen (e.g. ties for the lowest power).
        for (int tile : tiles(engine.state(), owner)) {
            if (action.accepts(engine.state(), owner, tile)) {
                action.apply(engine, owner, tile);
            }
        }
    }

    private List<Integer> tiles(GameState state, int owner) {
        List<Integer> tiles = new ArrayList<>();
        for (int tile : selector.select(state, owner)) {
            if (action.accepts(state, owner, tile)) {
                tiles.add(tile);
            }
        }
        return tiles;
    }
}
