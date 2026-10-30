package com.rutaji.qwent3.cards.effect.effectTargets.filters;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Tag;
import com.rutaji.qwent3.cards.gameLogic.Unit;

import java.util.Set;

public class FilterTags implements CardFilter{
    private final Set<Tag> tags;

    public FilterTags(Set<Tag> tags) {
        this.tags = tags;
    }


    @Override
    public boolean check(CardType cardType) {
        return cardType.hasTag(tags);
    }

    @Override
    public boolean check(Unit unit) {
        return unit.type().hasTag(tags);
    }
}
