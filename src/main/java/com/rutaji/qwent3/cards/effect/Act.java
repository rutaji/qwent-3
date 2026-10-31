package com.rutaji.qwent3.cards.effect;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.gameLogic.GameEngine;
import com.rutaji.qwent3.cards.gameLogic.GameState;

/** {@link TileAction}s for {@link AreaEffect}. */
public final class Act {
    private Act() {
    }

    /** Destroys the monster on the tile. Destroying is not damage: on-death effects trigger, on-damaged ones don't. */
    public static TileAction destroy() {
        return new TileAction() {
            @Override
            public boolean accepts(GameState state, int owner, int tile) {
                return state.unit(tile) != null;
            }

            @Override
            public void apply(GameEngine engine, int owner, int tile) {
                engine.destroy(tile);
            }

            @Override
            public String text(String where) {
                return "destroy the monsters " + where;
            }
        };
    }

    /** Changes the power of the monster on the tile: a buff, or damage when negative (see {@link AddPowerEffect}). */
    public static TileAction addPower(int amount) {
        return new TileAction() {
            @Override
            public boolean accepts(GameState state, int owner, int tile) {
                return state.unit(tile) != null;
            }

            @Override
            public void apply(GameEngine engine, int owner, int tile) {
                if (amount < 0) {
                    engine.damage(tile, -amount);
                } else {
                    engine.buff(tile, amount);
                }
            }

            @Override
            public String text(String where) {
                return amount < 0
                        ? "deal " + -amount + " damage to the monsters " + where
                        : "give the monsters " + where + " +" + amount + " power";
            }
        };
    }

    /** Plays the monster for the owner on the tile, if it can be played there (e.g. "fill the row with ..."). */
    public static TileAction summon(CardType card) {
        if (!card.isMonster()) {
            throw new IllegalArgumentException("Only monsters can be played: " + card);
        }
        return new TileAction() {
            @Override
            public boolean accepts(GameState state, int owner, int tile) {
                return state.canPlay(owner, card, tile);
            }

            @Override
            public void apply(GameEngine engine, int owner, int tile) {
                engine.play(owner, card, tile);
            }

            @Override
            public String text(String where) {
                return "play " + card.displayName() + " on every free tile " + where;
            }
        };
    }
}
