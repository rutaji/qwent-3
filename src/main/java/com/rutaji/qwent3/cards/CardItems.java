package com.rutaji.qwent3.cards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Cards as items. Each card is the ItemsAdder item "qwent:card_<name>" (falls back to paper without ItemsAdder),
 * tagged with the card type ("qwent:card") so it is recognised even without ItemsAdder. ItemsAdder items without
 * the tag (e.g. from /iagive) are recognised by their ItemsAdder id.
 */
public final class CardItems {
    private static final NamespacedKey CARD_KEY = Objects.requireNonNull(NamespacedKey.fromString("qwent:card"));
    /** Tag on card items made while the plugin was called ChangingItems. */
    private static final NamespacedKey LEGACY_CARD_KEY =
            Objects.requireNonNull(NamespacedKey.fromString("changingitems:card"));

    private final @Nullable Method getInstance;
    private final @Nullable Method getItemStack;
    private final @Nullable Method byItemStack;
    private final @Nullable Method getNamespacedId;

    public CardItems(Plugin plugin) {
        Method instance = null;
        Method itemStack = null;
        Method byItem = null;
        Method namespacedId = null;
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            try {
                Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
                instance = customStack.getMethod("getInstance", String.class);
                itemStack = customStack.getMethod("getItemStack");
                byItem = customStack.getMethod("byItemStack", ItemStack.class);
                namespacedId = customStack.getMethod("getNamespacedID");
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().warning("ItemsAdder found but CustomStack API is unavailable: " + e);
                instance = null;
            }
        }
        this.getInstance = instance;
        this.getItemStack = itemStack;
        this.byItemStack = byItem;
        this.getNamespacedId = namespacedId;
    }

    public static String itemId(CardType card) {
        return "qwent:card_" + card.name().toLowerCase();
    }

    public ItemStack create(CardType card, int amount) {
        ItemStack item = itemsAdderItem(card);
        if (item == null) {
            item = new ItemStack(Material.PAPER);
        }
        item.setAmount(amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(card.displayName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(card.isMonster() ? "Power: " + card.power() : "Spell", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        for (Component line : CardText.lines(card)) {
            lore.add(line.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(CARD_KEY, PersistentDataType.STRING, card.name());
        item.setItemMeta(meta);
        return item;
    }

    /** The card this item is, or null if it is not a card. */
    public @Nullable CardType cardOf(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        String tagged = item.getPersistentDataContainer().get(CARD_KEY, PersistentDataType.STRING);
        if (tagged == null) {
            tagged = item.getPersistentDataContainer().get(LEGACY_CARD_KEY, PersistentDataType.STRING);
        }
        if (tagged != null) {
            return byName(tagged);
        }
        String id = itemsAdderId(item);
        if (id != null && id.startsWith("qwent:card_")) {
            return byName(id.substring("qwent:card_".length()).toUpperCase());
        }
        return null;
    }

    private static @Nullable CardType byName(String name) {
        try {
            return CardType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private @Nullable ItemStack itemsAdderItem(CardType card) {
        if (getInstance == null || getItemStack == null) {
            return null;
        }
        try {
            Object stack = getInstance.invoke(null, itemId(card));
            return stack == null ? null : (ItemStack) getItemStack.invoke(stack);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private @Nullable String itemsAdderId(ItemStack item) {
        if (byItemStack == null || getNamespacedId == null) {
            return null;
        }
        try {
            Object stack = byItemStack.invoke(null, item);
            return stack == null ? null : (String) getNamespacedId.invoke(stack);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
