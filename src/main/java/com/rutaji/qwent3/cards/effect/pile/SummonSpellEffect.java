package com.rutaji.qwent3.cards.effect.pile;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;
import net.kyori.adventure.text.Component;

/**
 * Creates a spell card and casts it for the owner, as if played from hand: it goes into the owner's graveyard, then
 * its effects resolve right away (it counts as casting a spell). No target to pick.
 */
public final class SummonSpellEffect implements Effect {
    private final CardType source;
    private final CardType spell;

    public SummonSpellEffect(CardType source, CardType spell) {
        if (spell.isMonster()) {
            throw new IllegalArgumentException("Only spells can be cast: " + spell);
        }
        this.source = source;
        this.spell = spell;
    }

    @Override
    public Component description() {
        return Component.text(source.displayName() + ": cast " + spell.displayName());
    }

    @Override
    public boolean needsTarget() {
        return false;
    }

    @Override
    public boolean isValidTarget(GameState state, int owner, int target) {
        return false;
    }

    @Override
    public void resolve(GameEngine engine, int owner, int target) {
        engine.addToGraveyard(owner, spell);
        engine.castSpell(owner, spell, true);
    }
}
