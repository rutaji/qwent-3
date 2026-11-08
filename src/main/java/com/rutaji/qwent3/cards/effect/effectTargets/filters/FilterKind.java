package com.rutaji.qwent3.cards.effect.effectTargets.filters;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.Unit;

public class FilterKind implements CardFilter{
    private final CardType.Kind kind;

    public FilterKind(CardType.Kind kind) {
        this.kind = kind;
    }


    @Override
    public boolean check(CardType cardType) {
        return cardType.kind() == kind;
    }

    @Override
    public boolean check(Unit unit) {
        return unit.type().kind() == kind;
    }
}
