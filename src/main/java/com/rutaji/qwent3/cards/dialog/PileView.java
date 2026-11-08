package com.rutaji.qwent3.cards.dialog;

import com.rutaji.qwent3.cards.cardsRepository.Pile;

/** A pile screen a player has open: which side's deck or graveyard. */
public record PileView(int side, Pile pile) {
}
