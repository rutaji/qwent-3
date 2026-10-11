package com.rutaji.changingitems.cards;

/**
 * All cards, defined at compile time.
 * Glyph ids point to ItemsAdder font_images (namespace "qwent"):
 * a small icon shown inside buttons and a big image shown in the tooltip.
 */
public enum CardType {
    PEASANT("Peasant", 1, "P"),
    ARCHER("Archer", 3, "A"),
    KNIGHT("Knight", 5, "K"),
    MAGE("Mage", 7, "M"),
    DRAGON("Dragon", 10, "D");

    private final String displayName;
    private final int power;
    private final String shortName;

    CardType(String displayName, int power, String shortName) {
        this.displayName = displayName;
        this.power = power;
        this.shortName = shortName;
    }

    public String displayName() {
        return displayName;
    }

    public int power() {
        return power;
    }

    /** Text used when the ItemsAdder glyph is not available. */
    public String shortName() {
        return shortName;
    }

    public String iconGlyphId() {
        return "qwent:card_" + name().toLowerCase();
    }

    public String bigGlyphId() {
        return "qwent:card_" + name().toLowerCase() + "_big";
    }

    /**
     * Big card art for one button of the card's button stack. The vertical offset (y_position)
     * is baked into the resource pack, so there is one glyph variant per part.
     */
    public String artGlyphId(int part) {
        return "qwent:card_" + name().toLowerCase() + "_p" + part;
    }
}
