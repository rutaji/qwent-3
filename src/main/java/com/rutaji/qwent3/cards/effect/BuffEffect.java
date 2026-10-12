package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import com.rutaji.qwent3.cards.Unit;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/** Increases a monster's power (on either side). */
public final class BuffEffect implements Effect {
    private final CardType source;
    private final int amount;
    private final @Nullable Unit exclude;

    /** @param exclude a unit that cannot be targeted (usually the card itself), or null */
    public BuffEffect(CardType source, int amount, @Nullable Unit exclude) {
        this.source = source;
        this.amount = amount;
        this.exclude = exclude;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": give " + (exclude == null ? "a monster" : "another monster")
                + " +" + amount + " power");
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int tile) {
        Unit unit = state.unit(tile);
        return unit != null && unit != exclude;
    }

    @Override
    public void resolve(GameState state, int owner, int tile) {
        state.buff(tile, amount);
    }
}
