package com.rutaji.changingitems.cards;

import com.rutaji.changingitems.cards.effect.BuffEffect;
import com.rutaji.changingitems.cards.effect.DamageEffect;
import com.rutaji.changingitems.cards.effect.DestroyExtremeEffect;
import com.rutaji.changingitems.cards.effect.Effect;
import com.rutaji.changingitems.cards.effect.PlayFromPileEffect;
import com.rutaji.changingitems.cards.effect.SummonEffect;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * All cards, defined at compile time. A card gives itself effects by overriding
 * {@link #onPlay}, {@link #onHurt} and/or {@link #onDeath} in its own body; the returned effects go into the
 * effect queue.
 *
 * Art: ItemsAdder font image "qwent:card_<name>_text", made from textures/font/cards/<name>.png.
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
    },
    GRAVEL(Kind.SPELL, "Gravel", 0, "Gr", "Destroy the lowest power monster.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(DestroyExtremeEffect.lowest(this));
        }
    },
    LIGHTNING(Kind.SPELL, "Lightning", 0, "Li", "Destroy the highest power monster.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(DestroyExtremeEffect.highest(this));
        }
    },
    SKELETON(Kind.MONSTER, "Skeleton", 4, "Sk", "On play: deal 3 damage to a monster.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DamageEffect(this, 3));
        }
    },
    EVIL_GOBLIN(Kind.MONSTER, "Evil Goblin", 8, "EG", "No effect."),
    BOAR(Kind.MONSTER, "Boar", 4, "Bo", "On play: deal 1 damage to a monster.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DamageEffect(this, 1));
        }
    },
    BEAR(Kind.MONSTER, "Bear", 7, "Be", "No effect."),
    BASIC_ZOMBIE(Kind.MONSTER, "Basic Zombie", 2, "BZ", "No effect."),
    LEADER_ZOMBIE(Kind.MONSTER, "Leader Zombie", 6, "LZ", "When hurt: summon a Basic Zombie on a random empty tile.") {
        @Override
        public List<Effect> onHurt(Unit self, int amount) {
            return List.of(SummonEffect.random(this, BASIC_ZOMBIE));
        }
    },
    HORDE(Kind.SPELL, "Horde", 0, "Ho", "Play all Basic Zombies from your deck.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new PlayFromPileEffect(this, BASIC_ZOMBIE, Pile.DECK));
        }
    },
    ZOMBIE_AWAKENING(Kind.SPELL, "Zombie Awakening", 0, "ZA", "Play all Basic Zombies from your graveyard.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new PlayFromPileEffect(this, BASIC_ZOMBIE, Pile.GRAVEYARD));
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

    /**
     * Effects when this monster takes damage, also when the damage destroys it. They are queued before the
     * on-death effects. {@code self.power()} is the power after the damage; below 1 means it does not survive
     * (by the time the effects resolve, it is off the board).
     */
    public List<Effect> onHurt(Unit self, int amount) {
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

    public String textArtGlyphId() {
        return "qwent:card_" + name().toLowerCase() + "_text";
    }
}
