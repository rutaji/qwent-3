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
 * Buffs or damages several monsters at once: all of them / all of the owner's, or everything in a unit's row. The
 * monsters are chosen first, then affected in board order. Cancelled if there is none. No target to pick.
 */
public final class MassEffect implements Effect {
    private final CardType source;
    private final int amount;
    private final boolean damage;
    private final Targets targets;
    /** Only monsters in this unit's row; null = the whole board. */
    private final @Nullable Unit rowOf;
    private final String what;

    private MassEffect(CardType source, int amount, boolean damage, Targets targets, @Nullable Unit rowOf,
                       String what) {
        this.source = source;
        this.amount = amount;
        this.damage = damage;
        this.targets = targets;
        this.rowOf = rowOf;
        this.what = what;
    }

    public static MassEffect buffOwn(CardType source, int amount) {
        return new MassEffect(source, amount, false, Targets.OWN, null, "all your monsters");
    }

    public static MassEffect damageAll(CardType source, int amount) {
        return new MassEffect(source, amount, true, Targets.ANY, null, "all monsters");
    }

    /** Every monster in the unit's row, the unit included. */
    public static MassEffect buffRow(CardType source, int amount, Unit unit) {
        return new MassEffect(source, amount, false, Targets.ANY, unit, "every monster in its row");
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + (damage ? ": deal " + amount + " damage to " : ": give ")
                + what + (damage ? "" : " +" + amount + " power"));
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

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        for (int tile : tiles(engine.state(), owner)) {
            if (damage) {
                engine.damage(tile, amount);
            } else {
                engine.buff(tile, amount);
            }
        }
    }

    private List<Integer> tiles(GameState state, int owner) {
        List<Integer> tiles = new ArrayList<>();
        int rowStart = rowOf == null ? -1 : rowOf.tile() - rowOf.tile() % GameState.COLUMNS;
        for (int tile = 0; tile < GameState.BOARD_SIZE; tile++) {
            Unit unit = state.unit(tile);
            boolean inRow = rowOf == null || tile - tile % GameState.COLUMNS == rowStart;
            if (unit != null && inRow && targets.allows(unit, owner)) {
                tiles.add(tile);
            }
        }
        return tiles;
    }
}
