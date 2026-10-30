package com.rutaji.qwent3.cards.cardsRepository;

import com.rutaji.qwent3.cards.effect.AddPowerEffect;
import com.rutaji.qwent3.cards.effect.AdjacentDamageEffect;
import com.rutaji.qwent3.cards.effect.AddToDeckEffect;
import com.rutaji.qwent3.cards.effect.AdjacentDestroyEffect;
import com.rutaji.qwent3.cards.effect.BanishToBuffSelfEffect;
import com.rutaji.qwent3.cards.effect.BanishToDamageEffect;
import com.rutaji.qwent3.cards.effect.BanishToSummonEffect;
import com.rutaji.qwent3.cards.effect.BuffPerGraveyardCardEffect;
import com.rutaji.qwent3.cards.effect.ColumnDestroyEffect;
import com.rutaji.qwent3.cards.effect.CopyToPileEffect;
import com.rutaji.qwent3.cards.effect.DestroyExtremeEffect;
import com.rutaji.qwent3.cards.effect.DiscardEffect;
import com.rutaji.qwent3.cards.effect.DiscardFromDeckEffect;
import com.rutaji.qwent3.cards.effect.DrawEffect;
import com.rutaji.qwent3.cards.effect.DrawIfGraveyardHasEffect;
import com.rutaji.qwent3.cards.effect.DrawFromOpponentDeckEffect;
import com.rutaji.qwent3.cards.effect.Effect;
import com.rutaji.qwent3.cards.effect.FillRowEffect;
import com.rutaji.qwent3.cards.effect.IfEffect;
import com.rutaji.qwent3.cards.effect.MassEffect;
import com.rutaji.qwent3.cards.effect.MirrorEffect;
import com.rutaji.qwent3.cards.effect.PlayFromPileEffect;
import com.rutaji.qwent3.cards.effect.PlayRandomFromDeckEffect;
import com.rutaji.qwent3.cards.effect.ReplayEffect;
import com.rutaji.qwent3.cards.effect.SetPowerEffect;
import com.rutaji.qwent3.cards.effect.ShiftEffect;
import com.rutaji.qwent3.cards.effect.SummonEffect;
import com.rutaji.qwent3.cards.effect.SwapDeckAndGraveyardEffect;
import com.rutaji.qwent3.cards.effect.Targets;
import com.rutaji.qwent3.cards.effect.effectTargets.filters.FilterKind;
import com.rutaji.qwent3.cards.gameLogic.Unit;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * All cards, defined at compile time. A card gives itself effects by overriding
 * {@link #onPlay}, {@link #onDamagedSurvive} and/or {@link #onDeath} in its own body; the returned effects go into the
 * effect queue. Cards can have {@link Tag}s and {@link Keyword}s.
 *
 * Art: ItemsAdder font image "qwent:card_<name>_text", made from textures/font/cards/<name>.png.
 */
public enum CardType {
    ARROW(Kind.SPELL, "Arrow", 0, "Ar", "Deal 4 damage to a monster.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new AddPowerEffect(this, -4));
        }
    },
    TWO_ARROWS(Kind.SPELL, "Two Arrows", 0, "2A", "Deal 4 damage to two different monsters.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            // Shared set: the second arrow cannot hit the first one's target (cancelled if no other monster).
            Set<Unit> hit = new HashSet<>();
            return List.of(new AddPowerEffect(this, -4, Targets.ANY, true, hit),
                    new AddPowerEffect(this, -4, Targets.ANY, true, hit));
        }
    },
    GRAVEL(Kind.SPELL, "Gravel", 0, "Gr", "Destroy the lowest power monsters (all of them if tied).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(DestroyExtremeEffect.lowest(this));
        }
    },
    LIGHTNING(Kind.SPELL, "Lightning", 0, "Li", "Destroy the highest power monsters (all of them if tied).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(DestroyExtremeEffect.highest(this));
        }
    },
    SKELETON(Kind.MONSTER, "Skeleton", 4, "Sk", "On play: deal 3 damage to a monster.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new AddPowerEffect(this, -3));
        }
    },
    EVIL_GOBLIN(Kind.MONSTER, "Evil Goblin", 8, "EG", "No effect."),
    BOAR(Kind.MONSTER, "Boar", 4, "Bo", "On play: deal 1 damage to a monster.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new AddPowerEffect(this, -1));
        }
    },
    BEAR(Kind.MONSTER, "Bear", 7, "Be", "No effect."),
    BASIC_ZOMBIE(Kind.MONSTER, "Basic Zombie", 2, "BZ", "No effect.", Tag.ZOMBIE),
    LEADER_ZOMBIE(Kind.MONSTER, "Leader Zombie", 6, "LZ", "When damaged and survives: summon a Basic Zombie on a random empty tile.",
            Tag.ZOMBIE) {
        @Override
        public List<Effect> onDamagedSurvive(Unit self, int amount) {
            return List.of(SummonEffect.random(this, BASIC_ZOMBIE));
        }
    },
    HORDE(Kind.SPELL, "Horde", 0, "Ho", "Play all Basic Zombies from your deck.", Tag.ZOMBIE) {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new PlayFromPileEffect(this, BASIC_ZOMBIE, Pile.DECK));
        }
    },
    ZOMBIE_AWAKENING(Kind.SPELL, "Zombie Awakening", 0, "ZA", "Play all Basic Zombies from your graveyard.",
            Tag.ZOMBIE) {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new PlayFromPileEffect(this, BASIC_ZOMBIE, Pile.GRAVEYARD));
        }
    },
    DESERT_ZOMBIE(Kind.MONSTER, "Desert Zombie", 5, "DZ", "On play: deal 2 damage to a monster.", Tag.ZOMBIE) {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new AddPowerEffect(this, -2));
        }
    },
    ZOMBIE_LIBRARIAN(Kind.MONSTER, "Zombie Librarian", 5, "ZL",
            "On play: discard a Zombie card from your hand to draw a spell.", Tag.ZOMBIE) {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DiscardEffect(this, Tag.ZOMBIE).then(
                    new DrawEffect(this, 1, new FilterKind(Kind.SPELL))));
        }
    },
    BLAZE(Kind.MONSTER, "Blaze", 4, "Bl", "On play: deal 1 damage to a monster, twice (can be the same one).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new AddPowerEffect(this, -1), new AddPowerEffect(this, -1));
        }
    },
    CACTUS(Kind.MONSTER, "Cactus", 7, "Ca", "On play: deal 1 damage to the monsters left and right of it.",
            Set.of(Keyword.TREACHEROUS), Tag.PLANT) {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return self == null ? List.of() : List.of(new AdjacentDamageEffect(this, 1, self));
        }
    },
    BEETROOT(Kind.MONSTER, "Beetroot", 1, "Bt", "No effect.", Set.of(Keyword.TREACHEROUS), Tag.PLANT),
    BAD_FARMING(Kind.SPELL, "Bad Farming", 0, "BF", "Fill your back row with Beetroots.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(FillRowEffect.backRow(this, BEETROOT));
        }
    },
    EMERALD(Kind.MONSTER, "Emerald", 6, "Em", "On play on your opponent's side: draw a card from their deck.",
            Set.of(Keyword.TREACHEROUS)) {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return self == null ? List.of()
                    : List.of(IfEffect.onEnemySide(this, self, new DrawFromOpponentDeckEffect(this)));
        }
    },
    CREEPER(Kind.MONSTER, "Creeper", 6, "Cr", "On death: destroy the monsters left and right of it.",
            Set.of(Keyword.TREACHEROUS)) {
        @Override
        public List<Effect> onDeath(Unit self) {
            return List.of(new AdjacentDestroyEffect(this, self));
        }
    },
    DIAMOND_HOE(Kind.SPELL, "Diamond Hoe", 0, "DH", "Put 3 copies of a monster on the board into your deck.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new CopyToPileEffect(this, 3, Pile.DECK));
        }
    },
    GOLD_HOE(Kind.SPELL, "Gold Hoe", 0, "GH", "Put 3 copies of a monster on the board into your graveyard.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new CopyToPileEffect(this, 3, Pile.GRAVEYARD));
        }
    },
    WOODEN_HOE(Kind.SPELL, "Wooden Hoe", 0, "WH",
            "Destroy your monster to play a copy of it.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new ReplayEffect(this));
        }
    },
    POTATO(Kind.MONSTER, "Potato", 1, "Po", "On play: +3 power for every Potato in your graveyard.", Tag.PLANT) {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return self == null ? List.of() : List.of(new BuffPerGraveyardCardEffect(this, self, POTATO, 3));
        }
    },
    FERTILIZING(Kind.SPELL, "Fertilizing", 0, "Fe", "Discard 2 cards to draw 2 cards.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DiscardEffect(this, 2).then(new DrawEffect(this, 2)));
        }
    },
    FARMER(Kind.MONSTER, "Farmer", 4, "Fa", "On play: banish a random Plant from your graveyard to play a Farmer.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new BanishToSummonEffect(this, Tag.PLANT, FARMER));
        }
    },
    WHEAT(Kind.MONSTER, "Wheat", 2, "Wh", "On play: banish a random Plant from your graveyard for +10 power.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return self == null ? List.of() : List.of(new BanishToBuffSelfEffect(this, Tag.PLANT, self, 10));
        }
    },
    THORNY_ROSE(Kind.MONSTER, "Thorny Rose", 6, "TR",
            "On play: banish a random Plant from your graveyard to deal 4 damage to a monster.", Tag.PLANT) {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new BanishToDamageEffect(this, Tag.PLANT, 4));
        }
    },
    BROWN_MUSHROOM(Kind.MONSTER, "Brown Mushroom", 3, "BM",
            "On play: discard every Brown Mushroom from your deck.", Tag.PLANT) {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DiscardFromDeckEffect(this, BROWN_MUSHROOM));
        }
    },
    WEEDS(Kind.MONSTER, "Weeds", 2, "We", "When your opponent casts a spell: put a Weeds on top of their deck.",
            Set.of(Keyword.TREACHEROUS)) {
        @Override
        public List<Effect> onOpponentSpell(Unit self) {
            return List.of(new AddToDeckEffect(this, WEEDS, 1, true, true));
        }
    },
    SWITCHAROO(Kind.SPELL, "Switcharoo", 0, "Sw", "Swap your deck and your graveyard (no shuffling).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new SwapDeckAndGraveyardEffect(this));
        }
    },
    SILVERFISH(Kind.MONSTER, "Silverfish", 1, "Si", "No effect."),
    STONE_BRICKS(Kind.MONSTER, "Stone Bricks", 3, "SB", "On death: fill its row with Silverfish.",
            Set.of(Keyword.TREACHEROUS)) {
        @Override
        public List<Effect> onDeath(Unit self) {
            return List.of(FillRowEffect.rowOf(this, SILVERFISH, self));
        }
    },
    INFESTATION(Kind.SPELL, "Infestation", 0, "In", "Shuffle 4 Silverfish into your opponent's deck.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new AddToDeckEffect(this, SILVERFISH, 4, true, false));
        }
    },
    PISTON(Kind.SPELL, "Piston", 0, "Pi",
            "Push a monster one row towards your opponent (monsters in the way are pushed along).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(ShiftEffect.push(this));
        }
    },
    SLIME_PISTON(Kind.SPELL, "Slime Piston", 0, "SP",
            "Discard a card to pull a monster one row towards you (monsters in the way are pushed along).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DiscardEffect(this).then(ShiftEffect.pull(this)));
        }
    },
    DRUNK_BRAWLER(Kind.MONSTER, "Drunk Brawler", 3, "DB", "When damaged and survives: summon a Drunk Brawler.") {
        @Override
        public List<Effect> onDamagedSurvive(Unit self, int amount) {
            return List.of(new SummonEffect(this, DRUNK_BRAWLER));
        }
    },
    LIBRARIAN(Kind.MONSTER, "Librarian", 2, "Lb",
            "On play: discard a monster to draw a spell, or a spell to draw a monster.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DiscardEffect(this).thenWith(card -> List.of(
                    new DrawEffect(this, 1, new FilterKind(card.isMonster() ? Kind.SPELL : Kind.MONSTER)))));
        }
    },
    SWARM(Kind.SPELL, "Swarm", 0, "Sm", "Play 3 Silverfish.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new SummonEffect(this, SILVERFISH), new SummonEffect(this, SILVERFISH),
                    new SummonEffect(this, SILVERFISH));
        }
    },
    SPLASH_OF_STRENGTH(Kind.SPELL, "Splash of Strength", 0, "SS", "Give all your monsters +2 power.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(MassEffect.buffOwn(this, 2));
        }
    },
    HARSH_WORLD(Kind.SPELL, "Harsh World", 0, "HW", "Deal 2 damage to all monsters (both sides).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(MassEffect.damageAll(this, 2));
        }
    },
    BEE(Kind.MONSTER, "Bee", 5, "Bee", "On play: give a monster +2 power.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new AddPowerEffect(this, 2));
        }
    },
    HEALER(Kind.MONSTER, "Healer", 6, "He", "On play: set the power of one of your monsters to 5.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new SetPowerEffect(this, 5, Targets.OWN));
        }
    },
    SHAMAN(Kind.MONSTER, "Shaman", 2, "Sh", "On play: give every monster in its row +2 power (itself too).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return self == null ? List.of() : List.of(MassEffect.buffRow(this, 2, self));
        }
    },
    BARD(Kind.MONSTER, "Bard", 3, "Bd", "On play: give a monster +1 power, twice (can be the same one).") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new AddPowerEffect(this, 1), new AddPowerEffect(this, 1));
        }
    },
    WARLOCK(Kind.MONSTER, "Warlock", 9, "Wl", "On play: give an enemy monster +2 power.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new AddPowerEffect(this, 2, Targets.ENEMY));
        }
    },
    MUSIC_DISC(Kind.SPELL, "Music Disc", 0, "MD",
            "If your graveyard has at least 2 Music Discs (this one counts), draw a card.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DrawIfGraveyardHasEffect(this, MUSIC_DISC, 2));
        }
    },
    ENDERMAN(Kind.MONSTER, "Enderman", 14, "En", "On play: discard a card.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DiscardEffect(this));
        }
    },
    SHULKER(Kind.MONSTER, "Shulker", 4, "Su",
            "On play: discard a card to deal 4 damage to 3 different enemy monsters.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            Set<Unit> hit = new HashSet<>();
            return List.of(new DiscardEffect(this).then(
                    new AddPowerEffect(this, -4, Targets.ENEMY, true, hit),
                    new AddPowerEffect(this, -4, Targets.ENEMY, true, hit),
                    new AddPowerEffect(this, -4, Targets.ENEMY, true, hit)));
        }
    },
    ENDERMITE(Kind.MONSTER, "Endermite", 2, "Et",
            "On play: discard a card to play 2 random monsters from your deck.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DiscardEffect(this).then(
                    new PlayRandomFromDeckEffect(this), new PlayRandomFromDeckEffect(this)));
        }
    },
    ENDER_DRAGON(Kind.MONSTER, "Ender Dragon", 27, "ED", "On play: destroy every other monster in its column.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return self == null ? List.of() : List.of(new ColumnDestroyEffect(this, self));
        }
    },
    SUMMON_ENDER_DRAGON(Kind.SPELL, "Summon Ender Dragon", 0, "SE", "Discard 4 cards to play an Ender Dragon.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new DiscardEffect(this, 4).then(new SummonEffect(this, ENDER_DRAGON)));
        }
    },
    MIRROR(Kind.SPELL, "Mirror", 0, "Mi",
            "Banish the last spell that entered your opponent's graveyard to play it.") {
        @Override
        public List<Effect> onPlay(@Nullable Unit self) {
            return List.of(new MirrorEffect(this));
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
    private final Set<Tag> tags;
    private final Set<Keyword> keywords;

    /** @param tags card tags; most cards have none */
    CardType(Kind kind, String displayName, int power, String shortName, String description, Tag... tags) {
        this(kind, displayName, power, shortName, description, Set.of(), tags);
    }

    /** @param keywords rules keywords, e.g. {@code Set.of(Keyword.TREACHEROUS)} */
    CardType(Kind kind, String displayName, int power, String shortName, String description, Set<Keyword> keywords,
             Tag... tags) {
        this.kind = kind;
        this.displayName = displayName;
        this.power = power;
        this.shortName = shortName;
        this.description = description;
        this.tags = tags.length == 0 ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(List.of(tags)));
        this.keywords = keywords.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(keywords));
    }

    /**
     * Effects when the card is played: a monster from hand or summoned (self = the new unit),
     * or a spell being cast (self = null).
     */
    public List<Effect> onPlay(@Nullable Unit self) {
        return List.of();
    }

    /**
     * Effects when this monster takes damage and survives it (damage that destroys it triggers only on-death).
     * {@code self.power()} is the power after the damage.
     */
    public List<Effect> onDamagedSurvive(Unit self, int amount) {
        return List.of();
    }

    /** Effects when the opponent of this monster's owner casts a spell (queued after the spell's own effects). */
    public List<Effect> onOpponentSpell(Unit self) {
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

    public Set<Tag> tags() {
        return tags;
    }

    public boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }
    public boolean hasTag(Set<Tag> tags) {
        for(Tag t : tags){
            if(tags.contains(t)){
                return true;
            }
        }
        return false;
    }

    public Set<Keyword> keywords() {
        return keywords;
    }

    public boolean hasKeyword(Keyword keyword) {
        return keywords.contains(keyword);
    }

    /** Tags as text, e.g. "Zombie, Plant"; empty if none. */
    public String tagsText() {
        return String.join(", ", tags.stream().map(Tag::displayName).toList());
    }

    /** Rules text shown in the tooltip. */
    public String description() {
        return description;
    }

    public String textArtGlyphId() {
        return "qwent:card_" + name().toLowerCase() + "_text";
    }
}
