package com.rutaji.changingitems.cards.effect;

import com.rutaji.changingitems.cards.CardType;
import com.rutaji.changingitems.cards.GameState;
import net.kyori.adventure.text.Component;

/** Creates a monster card and plays it on an empty tile (its on-play effect triggers too). */
public final class SummonEffect implements Effect {
    private final CardType source;
    private final CardType summoned;

    public SummonEffect(CardType source, CardType summoned) {
        if (!summoned.isMonster()) {
            throw new IllegalArgumentException("Only monsters can be summoned: " + summoned);
        }
        this.source = source;
        this.summoned = summoned;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": summon a " + summoned.displayName() + " on an empty tile");
    }

    @Override
    public boolean isValidTarget(GameState state, int tile) {
        return state.unit(tile) == null;
    }

    @Override
    public void resolve(GameState state, int tile) {
        state.play(summoned, tile);
    }
}
