package com.rutaji.changingitems.cards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import org.jspecify.annotations.Nullable;

/**
 * Card art inside button labels. A card is a stack of {@link #STACK} buttons; every button's label is
 * the whole card glyph, shifted up by one button pitch per part ("_p0", "_p1", "_p2" glyph variants).
 * The label is drawn on top of its button and clipped to it, so each button shows its slice of the card.
 */
public final class CardArt {
    /** Buttons per card (3 x 20 px + 2 x 2 px gap = 64 px tall card). */
    public static final int STACK = 3;
    /** 43 px art plus the 2 px label inset on each side; the label must not be wider or it scrolls. */
    public static final int BUTTON_W = 47;
    private static final int GLYPH_ADVANCE = 44;

    private final Glyphs glyphs;

    public CardArt(Glyphs glyphs) {
        this.glyphs = glyphs;
    }

    /** True when the resource pack glyphs are available. */
    public boolean available() {
        return glyphs.resolve("qwent:tile_empty_p0") != null;
    }

    public @Nullable Component card(CardType card, int part, boolean selected) {
        Component art = glyphs.resolve(card.artGlyphId(part));
        if (art == null) {
            return null;
        }
        if (selected) {
            Component frame = glyphs.resolve("qwent:select_frame_p" + part);
            if (frame != null) {
                // Step back one glyph so the frame is drawn over the card.
                art = Component.text().append(art).append(Glyphs.space(-GLYPH_ADVANCE)).append(frame).build();
            }
        }
        return finish(art);
    }

    public @Nullable Component emptyTile(int part) {
        Component tile = glyphs.resolve("qwent:tile_empty_p" + part);
        return tile == null ? null : finish(tile);
    }

    /** Drops the shadow and trims the glyph's extra 1 px advance, so the label is exactly the art width. */
    /** Hover text: big card art (if available), name and power. */
    public Component tooltip(CardType card) {
        Component info = Component.text(card.displayName() + "\nPower: " + card.power());
        Component big = glyphs.resolve(card.bigGlyphId());
        return big == null ? info : Component.text().append(big).append(Component.newline()).append(info).build();
    }

    private static Component finish(Component glyph) {
        return Component.text().append(glyph).append(Glyphs.space(-1)).build().shadowColor(ShadowColor.none());
    }
}
