package com.rutaji.qwent3.cards.effect.effectTargets.filters;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.Unit;

import java.util.function.Function;

public class FilterPower implements CardFilter{
    private final Function<Integer,Boolean> comparePower;

    public FilterPower(Function<Integer, Boolean> comparePower) {
        this.comparePower = comparePower;
    }


    @Override
    public boolean check(CardType cardType) {
        return comparePower.apply(cardType.power());
    }

    @Override
    public boolean check(Unit unit) {
        return comparePower.apply(unit.power());
    }
}
