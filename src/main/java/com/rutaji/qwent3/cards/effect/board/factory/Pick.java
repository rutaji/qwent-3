package com.rutaji.qwent3.cards.effect.board.factory;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.cardsRepository.Keyword;
import com.rutaji.qwent3.cards.effect.board.PickTileEffect;
import com.rutaji.qwent3.cards.effect.board.PickUnitEffect;
import com.rutaji.qwent3.cards.effect.effectTargets.Targets;
import com.rutaji.qwent3.cards.gameLogic.Unit;

import java.util.Set;

/**
 * {@link PickUnitEffect}s and {@link PickTileEffect}s: the owner picks a monster or a tile and something happens to
 * it.
 */
public final class Pick {
    private Pick() {
    }

    /**
     * Changes the power of any monster (either side): a buff, or damage when negative (the monster is destroyed when
     * its power drops below 1, otherwise its "damaged and survives" effects trigger).
     */
    public static PickUnitEffect addPower(CardType source, int amount) {
        return addPower(source, amount, Targets.ANY);
    }

    public static PickUnitEffect addPower(CardType source, int amount, Targets targets) {
        return new PickUnitEffect(source, targets, Act.addPower(amount), addPowerText(amount, targets));
    }

    /**
     * Only a monster not in {@code alreadyHit} can be picked; the target is added to it. Share one set between several
     * effects to make them hit different monsters.
     */
    public static PickUnitEffect addPower(CardType source, int amount, Targets targets, Set<Unit> alreadyHit) {
        return new PickUnitEffect(source, targets, Act.addPower(amount), addPowerText(amount, targets), alreadyHit);
    }

    /** Sets the power of a monster to a fixed value. */
    public static PickUnitEffect setPower(CardType source, int power, Targets targets) {
        return new PickUnitEffect(source, targets, Act.setPower(power),
                "set the power of " + targets.text() + " to " + power);
    }

    /**
     * Creates {@code count} copies of a monster card and plays them, one at a time, on tiles where the owner can play
     * it: an empty tile of their side, or either side if it is treacherous (their on-play effects trigger too). The
     * owner picks each tile, or with {@code random} it goes to a random one without asking. Stops when there is no
     * such tile; cancelled if there is none at the start.
     */
    public static PickTileEffect summon(CardType source, CardType card, int count, boolean random) {
        String where = card.hasKeyword(Keyword.TREACHEROUS) ? "an empty tile" : "an empty tile of your side";
        return new PickTileEffect(source, Act.summon(card), "summon a " + card.displayName() + " on "
                + (random ? "a random " + where.substring(3) : where), count, random);
    }

    private static String addPowerText(int amount, Targets targets) {
        return amount < 0
                ? "deal " + -amount + " damage to " + targets.text()
                : "give " + targets.text() + " +" + amount + " power";
    }
}
