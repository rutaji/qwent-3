package com.rutaji.changingitems.cards;

import com.rutaji.changingitems.cards.effect.BuffEffect;
import com.rutaji.changingitems.cards.effect.DamageEffect;
import com.rutaji.changingitems.cards.effect.Effect;
import com.rutaji.changingitems.cards.effect.SummonEffect;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * All cards, defined at compile time. A card gives itself effects by overriding
 * {@link #onPlay} and/or {@link #onDeath} in its own body; the returned effects go into the effect queue.
 *
 * Art: ItemsAdder font images "qwent:card_<name>_text" (board/hand) and "qwent:card_<name>_big" (tooltip).
 */
public enum CardType {
    PEASANT(Kind.MONSTER, "Peasant", 1, "P", "No effect."),
    ARCHER(Kind.MONSTER, "Archer", 3, "A", "No effect."),
    KNIGHT(Kind.MONSTER, "Knight", 5, "K", "On play: summon a Peasant.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new SummonEffect(this, PEASANT));
        }
    },
    MAGE(Kind.MONSTER, "Mage", 7, "M", "On play: deal 2 damage to a monster (it can be itself).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DamageEffect(this, 2));
        }
    },
    DRAGON(Kind.MONSTER, "Dragon", 10, "D", "On death: give a monster +3 power.") {
        @Override
        public List<Effect> onDeath(Unit self) {
            return List.of(new BuffEffect(this, 3, null));
        }
    },
    ARROW(Kind.SPELL, "Arrow", 0, "Ar", "Deal 4 damage to a monster.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DamageEffect(this, 4));
        }
    },
    TWO_ARROWS(Kind.SPELL, "Two Arrows", 0, "2A", "Deal 4 damage to two different monsters.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            // Shared set: the second arrow cannot hit the first one's target (cancelled if no other monster).
            Set<Unit> hit = new HashSet<>();
            return List.of(new DamageEffect(this, 4, hit), new DamageEffect(this, 4, hit));
        }
    };

    public enum Kind {
        /** Has power and is placed on an empty tile. */
        MONSTER,
        /** No power; played by clicking any tile, then its effects resolve. */
        SPELL
    }

    private final Kind kind;
    private final String displayName;
    private final int power;
    private final String shortName;
    private final String description;

    CardType(Kind kind, String displayName, int power, String shortName, String description) {
        this.kind = kind;
        this.displayName = displayName;
        this.power = power;
        this.shortName = shortName;
        this.description = description;
    }

    /**
     * Effects when the card is played: a monster from hand or summoned (self = the new unit),
     * or a spell being cast (self = null).
     */
    public List<Effect> onPlay(@Nullable Unit self) {
        return List.of();
    }

    /** Effects when this monster is destroyed. {@code self} is already off the board. */
    public List<Effect> onDeath(Unit self) {
        return List.of();
    }

    public Kind kind() {
        return kind;
    }

    public boolean isMonster() {
        return kind == Kind.MONSTER;
    }

    public String displayName() {
        return displayName;
    }

    /** Base power; 0 for spells. */
    public int power() {
        return power;
    }

    /** Text used when the ItemsAdder glyph is not available. */
    public String shortName() {
        return shortName;
    }

    /** Rules text shown in the tooltip. */
    public String description() {
        return description;
    }

    public String bigGlyphId() {
        return "qwent:card_" + name().toLowerCase() + "_big";
    }

    public String textArtGlyphId() {
        return "qwent:card_" + name().toLowerCase() + "_text";
    }
}
