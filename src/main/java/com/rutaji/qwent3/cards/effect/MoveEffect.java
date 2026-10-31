package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.effect.effectTargets.Direction;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;

/**
 * Moves a monster the owner picks (any, or only their own without {@code canTargetEnemy}) {@code distance} tiles in a
 * direction, seen from the owner: forward = towards the opponent, back = towards the owner, left / right along its
 * row. Monsters in the way are pushed along; a monster that crosses to the other side belongs to that side. It moves
 * one tile at a time and stops early when the monsters in front of it would be pushed off the board; a monster that
 * can't move at all is not a valid target.
 */
public final class MoveEffect implements Effect {
    private final CardType source;
    private final Direction direction;
    private final int distance;
    /** False = only the owner's own monsters can be picked. */
    private final boolean canTargetEnemy;

    public MoveEffect(CardType source, Direction direction, int distance, boolean canTargetEnemy) {
        this.source = source;
        this.direction = direction;
        this.distance = distance;
        this.canTargetEnemy = canTargetEnemy;
    }

    @Override
    public Component description() {
        String where = switch (direction) {
            case Forward -> " towards your opponent";
            case Back -> " towards you";
            case Left -> " left";
            case Right -> " right";
        };
        String unit = direction == Direction.Forward || direction == Direction.Back ? " row" : " tile";
        return Component.text(source.displayName() + ": move "
                + (canTargetEnemy ? "a monster " : "one of your monsters ")
                + distance + unit + (distance == 1 ? "" : "s") + where);
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        Unit unit = state.unit(target);
        return unit != null && (canTargetEnemy || unit.owner() == owner)
                && state.canShift(target, lineStep(owner), columnStep());
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        Unit unit = engine.state().unit(target);
        for (int i = 0; i < distance && unit != null; i++) {
            if (!engine.shift(unit.tile(), lineStep(owner), columnStep())) {
                break;
            }
        }
    }

    private int lineStep(int owner) {
        return switch (direction) {
            case Forward -> GameState.directionTowards(GameState.other(owner));
            case Back -> GameState.directionTowards(owner);
            case Left, Right -> 0;
        };
    }

    private int columnStep() {
        return switch (direction) {
            case Left -> -1;
            case Right -> 1;
            case Forward, Back -> 0;
        };
    }
}
