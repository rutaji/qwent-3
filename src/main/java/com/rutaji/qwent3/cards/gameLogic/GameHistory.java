package com.rutaji.qwent3.cards.gameLogic;

import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.gameLogic.GameState.LogEntry;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Writes the history of a game ({@link GameState#log()}): the cards played and how effects resolved. */
final class GameHistory {
    private final GameState state;

    GameHistory(GameState state) {
        this.state = state;
    }

    /**
     * @param actor 0 / 1, or -1 for neutral notes like round changes
     * @param played whether it is the card played from hand that turn
     */
    void log(int actor, String text, boolean played) {
        state.log.add(new LogEntry(actor, text, played));
    }

    /** Logs an effect about to resolve (or being cancelled), with what it targets. */
    void logEffect(Effect effect, int owner, int target, boolean cancelled) {
        String text = PlainTextComponentSerializer.plainText().serialize(effect.description());
        if (cancelled) {
            text += " - no target";
        } else if (target >= 0) {
            text += " -> " + targetName(effect, owner, target);
        } else {
            String detail = effect.logDetail(state, owner);
            if (detail != null) {
                text += " -> " + detail;
            }
        }
        log(owner, text, false);
    }

    private String targetName(Effect effect, int owner, int target) {
        if (effect.targetKind() == Effect.TargetKind.HAND) {
            return state.sides[owner].hand.get(target).displayName();
        }
        Unit unit = state.board[target];
        return unit != null ? unit.type().displayName() : "empty tile";
    }
}
