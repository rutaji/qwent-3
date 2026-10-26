package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;

/**
 * Moves any monster (either side) one row: push = towards the owner's opponent, pull = towards the owner. Monsters in the way are pushed along; a monster that crosses to the other side belongs to that side. A
 * monster is not a valid target if the last monster in its column would be pushed off the board.
 */
public final class ShiftEffect implements Effect {
    private final CardType source;
    private final boolean pull;

    private ShiftEffect(CardType source, boolean pull) {
        this.source = source;
        this.pull = pull;
    }

    /** Push any monster one row towards your opponent. */
    public static ShiftEffect push(CardType source) {
        return new ShiftEffect(source, false);
    }

    /** Pull any monster one row towards you. */
    public static ShiftEffect pull(CardType source) {
        return new ShiftEffect(source, true);
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + (pull
                ? ": pull a monster one row towards you"
                : ": push a monster one row towards your opponent"));
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        Unit unit = state.unit(target);
        return unit != null && state.canShift(target, direction(owner));
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        engine.shift(target, direction(owner));
    }

    private int direction(int owner) {
        return GameState.directionTowards(pull ? owner : GameState.other(owner));
    }
}
