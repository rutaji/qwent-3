package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import net.kyori.adventure.text.Component;

/** Increases the power of a monster the owner picks. */
public final class BuffEffect implements Effect {
    private final CardType source;
    private final int amount;
    private final Targets targets;

    /** Buff any monster (either side). */
    public BuffEffect(CardType source, int amount) {
        this(source, amount, Targets.ANY);
    }

    public BuffEffect(CardType source, int amount, Targets targets) {
        this.source = source;
        this.amount = amount;
        this.targets = targets;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": give " + targets.text() + " +" + amount + " power");
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int tile) {
        Unit unit = state.unit(tile);
        return unit != null && targets.allows(unit, owner);
    }

    @Override
    public void resolve(GameEngine engine, int owner, int tile) {
        engine.buff(tile, amount);
    }
}
