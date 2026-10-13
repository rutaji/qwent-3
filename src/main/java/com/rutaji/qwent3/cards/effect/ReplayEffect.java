package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;

/**
 * The owner picks one of their monsters: it is destroyed, then they pick an empty tile where a fresh copy is played -
 * their side (the freed tile counts), or either side if the monster is treacherous. Both steps happen before the destroyed monster's on-death effects; the copy's
 * on-play effects come after those.
 */
public final class ReplayEffect implements Effect {
    private final CardType source;

    public ReplayEffect(CardType source) {
        this.source = source;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": destroy one of your monsters to play a copy of it");
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        Unit unit = state.unit(target);
        return unit != null && unit.owner() == owner;
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        Unit unit = state.unit(target);
        if (unit == null) {
            return;
        }
        state.destroy(target);
        // At the front of the queue: placing the copy is still part of this card, before the on-death effects.
        state.queueNext(owner, new PlaceCopy(unit.type()));
    }

    /** Second step: the owner picks a tile for the copy (where that card may be played). */
    private final class PlaceCopy implements Effect {
        private final CardType copy;

        private PlaceCopy(CardType copy) {
            this.copy = copy;
        }

        @Override
        public Component description() {
            return Component.text(source.displayName() + ": play the copy of " + copy.displayName());
        }

        @Override
        public boolean isValidTarget(GameState state, int owner, int target) {
            return state.canPlay(owner, copy, target);
        }

        @Override
        public void resolve(GameState state, int owner, int target) {
            state.play(owner, copy, target);
        }
    }
}
