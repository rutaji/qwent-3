package com.rutaji.qwent3.cards.deck;

import com.rutaji.qwent3.cards.cardsRepository.CardText;
import com.rutaji.qwent3.cards.cardsRepository.CardType;
import org.bukkit.inventory.ItemStack;
import dev.lone.itemsadder.api.CustomStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Cards as items. Each card is the ItemsAdder item "qwent:card_&lt;name&gt;" and is recognised by that id (so items
 * from /iagive work too). ItemsAdder is a required dependency.
 */
public final class CardItems {
    private static final String ID_PREFIX = "qwent:card_";

    private final Logger logger;

    public CardItems(Logger logger) {
        this.logger = logger;
    }

    public static String itemId(CardType card) {
        return ID_PREFIX + card.name().toLowerCase();
    }

    /** The card as items, or null (logged) if ItemsAdder doesn't know its item (cards.yml not generated/loaded). */
    public @Nullable ItemStack CardToItem(CardType card, int amount) {
        ItemStack item = itemsAdderItem(card);
        if (item == null) {
            return null;
        }
        item.setAmount(amount);
        CardText.applyTo(item, card);
        return item;
    }

    /** Returns card from item */
    public @Nullable CardType ItemToCard(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        String id = itemsAdderId(item);
        if (id == null || !id.startsWith(ID_PREFIX)) {
            return null;
        }
        try {
            return CardType.valueOf(id.substring(ID_PREFIX.length()).toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Card to item but checks max stack Size
     */
    public List<ItemStack> cardStacks(CardType card, int count) {
        ItemStack item = CardToItem(card, 1);
        if (item == null) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (int left = count; left > 0; left -= item.getMaxStackSize()) {
            ItemStack stack = item.clone();
            stack.setAmount(Math.min(left, item.getMaxStackSize()));
            stacks.add(stack);
        }
        return stacks;
    }

    /** The card's ItemsAdder item, or null (and an error in the log) if ItemsAdder doesn't know it. */
    private @Nullable ItemStack itemsAdderItem(CardType card) {
        String id = itemId(card);
        CustomStack stack = CustomStack.getInstance(id);
        if (stack == null) {
            logger.severe("ItemsAdder has no item " + id + " (card " + card.name() + ")");
            return null;
        }
        return stack.getItemStack();
    }

    /** The ItemsAdder id of the item, or null if it is not an ItemsAdder item. */
    private static @Nullable String itemsAdderId(ItemStack item) {
        CustomStack stack = CustomStack.byItemStack(item);
        return stack == null ? null : stack.getNamespacedID();
    }
}
