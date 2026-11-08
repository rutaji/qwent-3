package com.rutaji.qwent3.cards.effect.effectTargets.filters;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Tag;
import com.rutaji.qwent3.cards.gameLogic.Unit;

import java.util.List;
import java.util.function.Function;

public interface CardFilter {

    public boolean check(CardType cardType);

    public boolean check(Unit unit);

}
