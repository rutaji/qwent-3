package com.rutaji.qwent3.cards.dialog;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jspecify.annotations.Nullable;

/**
 * Resolves ItemsAdder font images to text components. When a glyph is missing (e.g. its texture wasn't generated,
 * or ItemsAdder hasn't finished loading) {@link #resolve} returns null and callers use a text fallback.
 */
public final class Glyphs {
    /** Font with "space" provider: U+F801.. = -1,-2,-4,...,-128 px; U+F821.. = +1,+2,...,+128 px. */
    private static final Key SPACE_FONT = Key.key("qwent", "space");
    private static final char NEGATIVE_BASE = '\uF801';
    private static final char POSITIVE_BASE = '\uF821';

    /** Returns the glyph as a component, or null if it is not available. */
    public @Nullable Component resolve(String namespacedId) {
        FontImageWrapper wrapper;
        try {
            wrapper = new FontImageWrapper(namespacedId);
        } catch (IllegalArgumentException e) { // malformed id
            return null;
        }
        if (!wrapper.exists()) {
            return null;
        }
        return LegacyComponentSerializer.legacySection().deserialize(wrapper.getString());
    }

    /** Moves the text cursor by the given number of pixels (negative = left). */
    public static Component space(int pixels) {
        if (pixels == 0) {
            return Component.empty();
        }
        char base = pixels < 0 ? NEGATIVE_BASE : POSITIVE_BASE;
        int remaining = Math.abs(pixels);
        StringBuilder chars = new StringBuilder();
        while (remaining >= 128) {
            chars.append((char) (base + 7));
            remaining -= 128;
        }
        for (int bit = 6; bit >= 0; bit--) {
            if ((remaining & (1 << bit)) != 0) {
                chars.append((char) (base + bit));
            }
        }
        return Component.text(chars.toString()).font(SPACE_FONT);
    }
}
