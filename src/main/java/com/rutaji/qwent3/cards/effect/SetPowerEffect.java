package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;

/** Sets the power of a monster the owner picks to a fixed value. */
public final class SetPowerEffect implements Effect {
    private final CardType source;
    private final int power;
    private final Targets targets;

    public SetPowerEffect(CardType source, int power, Targets targets) {
        this.source = source;
        this.power = power;
        this.targets = targets;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": set the power of " + targets.text() + " to " + power);
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        Unit unit = state.unit(target);
        return unit != null && targets.allows(unit, owner);
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        state.setPower(target, power);
    }
}
