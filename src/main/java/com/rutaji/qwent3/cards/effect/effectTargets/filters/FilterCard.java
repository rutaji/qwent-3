package com.rutaji.qwent3.cards.effect.effectTargets.filters;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Tag;
import com.rutaji.qwent3.cards.gameLogic.Unit;

import java.util.List;
import java.util.Set;

public class FilterCard implements CardFilter {
    private final CardType card;

    public FilterCard(CardType card) {
        this.card = card;
    }


    @Override
    public boolean check(CardType cardType) {

        return cardType == card;
    }

    @Override
    public boolean check(Unit unit) {
        return unit.type() == card;
    }
}
