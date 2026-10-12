package com.rutaji.qwent3.cards;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Resolves ItemsAdder font images to text components.
 * Uses reflection so the plugin compiles and runs without ItemsAdder installed;
 * in that case (or when a glyph is missing) {@link #resolve} returns null and callers use a text fallback.
 */
public final class Glyphs {
    /** Font with "space" provider: U+F801.. = -1,-2,-4,...,-128 px; U+F821.. = +1,+2,...,+128 px. */
    private static final Key SPACE_FONT = Key.key("qwent", "space");
    private static final char NEGATIVE_BASE = '\uF801';
    private static final char POSITIVE_BASE = '\uF821';

    private final @Nullable Constructor<?> wrapperConstructor;
    private final @Nullable Method existsMethod;
    private final @Nullable Method getStringMethod;

    public Glyphs(Logger logger) {
        Constructor<?> constructor = null;
        Method exists = null;
        Method getString = null;
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            try {
                Class<?> wrapper = Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
                constructor = wrapper.getConstructor(String.class);
                exists = wrapper.getMethod("exists");
                getString = wrapper.getMethod("getString");
            } catch (ReflectiveOperationException e) {
                logger.warning("ItemsAdder found but FontImageWrapper API is unavailable: " + e);
                constructor = null;
            }
        } else {
            logger.info("ItemsAdder not found, cards will use text fallback.");
        }
        this.wrapperConstructor = constructor;
        this.existsMethod = exists;
        this.getStringMethod = getString;
    }

    /** Returns the glyph as a component, or null if it is not available. */
    public @Nullable Component resolve(String namespacedId) {
        if (wrapperConstructor == null || existsMethod == null || getStringMethod == null) {
            return null;
        }
        try {
            Object wrapper = wrapperConstructor.newInstance(namespacedId);
            if (!(boolean) existsMethod.invoke(wrapper)) {
                return null;
            }
            String text = (String) getStringMethod.invoke(wrapper);
            return LegacyComponentSerializer.legacySection().deserialize(text);
        } catch (ReflectiveOperationException e) {
            return null;
        }
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
