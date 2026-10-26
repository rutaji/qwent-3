package com.rutaji.qwent3.ai;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * All AI opponents, registered at compile time: add a line to the static block. The name is what players type in
 * {@code /qwent singleplayer <name>} (case-insensitive) and what is shown as the opponent's name.
 */
public final class AiRegistry {
    private static final Map<String, Supplier<QwentAi>> AIS = new LinkedHashMap<>();

    static {
        register("Random", RandomAi::new);
    }

    private AiRegistry() {
    }

    private static void register(String name, Supplier<QwentAi> factory) {
        AIS.put(name, factory);
    }

    public static Set<String> names() {
        return Collections.unmodifiableSet(AIS.keySet());
    }

    /** The registered name matching (case-insensitive), or null. */
    public static @Nullable String find(String name) {
        return AIS.keySet().stream().filter(n -> n.equalsIgnoreCase(name.trim())).findFirst().orElse(null);
    }

    /** A new instance of the AI; the name must be registered. */
    public static QwentAi create(String name) {
        return AIS.get(name).get();
    }
}
