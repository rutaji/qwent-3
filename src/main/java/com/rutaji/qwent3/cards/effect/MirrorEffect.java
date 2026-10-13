package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.CardType;
import com.rutaji.qwent3.cards.GameState;
import net.kyori.adventure.text.Component;

/**
 * Banishes the spell that entered the opponent's graveyard last, then casts it for the owner (its effects resolve
 * next; it counts as casting a spell). The banished card goes nowhere. Cancelled if that graveyard has no spell.
 * No target to pick.
 */
public final class MirrorEffect implements Effect {
    private final CardType source;

    public MirrorEffect(CardType source) {
        this.source = source;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": banish the last spell from your opponent's graveyard to play it");
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean hasValidTargets(GameState state, int owner) {
        return state.side(GameState.other(owner)).graveyard().stream().anyMatch(card -> !card.isMonster());
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameState state, int owner, int target) {
        CardType spell = state.banishLast(GameState.other(owner), card -> !card.isMonster());
        if (spell != null) {
            state.castSpell(owner, spell, true);
        }
    }
}
