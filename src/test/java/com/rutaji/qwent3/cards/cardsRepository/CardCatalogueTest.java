package com.rutaji.qwent3.cards.cardsRepository;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.rutaji.qwent3.cards.cardsRepository.CardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Basic data of every card. */
class CardCatalogueTest {

    @Test
    void everyCardHasNameTextAndSensiblePower() {
        for (CardType card : CardType.values()) {
            assertFalse(card.displayName().isBlank(), card + " name");
            assertFalse(card.description().isBlank(), card + " text");
            if (card.isMonster()) {
                assertTrue(card.power() > 0, card + " power");
            } else {
                assertEquals(0, card.power(), card + " spells have no power");
            }
        }
    }

    @Test
    void shortNamesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (CardType card : CardType.values()) {
            assertTrue(seen.add(card.shortName()), "duplicate short name " + card.shortName());
        }
    }

    @Test
    void artGlyphIdsFollowTheNaming() {
        assertEquals("qwent:card_basic_zombie_text", BASIC_ZOMBIE.textArtGlyphId());
    }

    @Test
    void plainMonstersHaveTheirPower() {
        assertEquals(8, EVIL_GOBLIN.power());
        assertEquals(7, BEAR.power());
        assertEquals(2, BASIC_ZOMBIE.power());
        assertEquals(1, SILVERFISH.power());
        assertEquals(1, BEETROOT.power());
        assertTrue(EVIL_GOBLIN.onPlay(null).isEmpty());
    }

    @Test
    void tagsAndKeywords() {
        assertEquals(Set.of(Tag.ZOMBIE), BASIC_ZOMBIE.tags());
        assertTrue(LEADER_ZOMBIE.hasTag(Tag.ZOMBIE));
        assertTrue(HORDE.hasTag(Tag.ZOMBIE), "spells can have tags");
        assertTrue(ZOMBIE_AWAKENING.hasTag(Tag.ZOMBIE));
        assertTrue(DESERT_ZOMBIE.hasTag(Tag.ZOMBIE));
        assertTrue(ZOMBIE_LIBRARIAN.hasTag(Tag.ZOMBIE));
        for (CardType plant : new CardType[] {CACTUS, BEETROOT, POTATO, THORNY_ROSE, BROWN_MUSHROOM}) {
            assertTrue(plant.hasTag(Tag.PLANT), plant + " is a Plant");
        }
        assertFalse(FARMER.hasTag(Tag.PLANT));
        assertFalse(WHEAT.hasTag(Tag.PLANT));
        for (CardType treacherous : new CardType[] {CACTUS, BEETROOT, EMERALD, CREEPER, WEEDS, STONE_BRICKS}) {
            assertTrue(treacherous.hasKeyword(Keyword.TREACHEROUS), treacherous + " is Treacherous");
        }
        assertTrue(BEAR.tags().isEmpty());
        assertTrue(BEAR.keywords().isEmpty());
    }

    @Test
    void cardTextListsTagsThenKeywordThenRules() {
        assertEquals(3, CardText.lines(CACTUS).size());
        assertEquals(1, CardText.lines(BEAR).size());
    }
}
