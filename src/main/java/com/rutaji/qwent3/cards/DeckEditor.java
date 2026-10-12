package com.rutaji.qwent3.cards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * /qwent deck: a chest with the player's starting deck as card items. The player can take cards out and put
 * cards in. On close the chest becomes the new deck; everything that can't be in it goes back to the player:
 * non-card items, blacklisted cards, copies above {@link DeckRules#MAX_COPIES} and cards above
 * {@link DeckRules#MAX_CARDS}.
 */
public final class DeckEditor implements Listener {
    private static final int SIZE = 54;

    private final DeckStore decks;
    private final CardItems items;

    public DeckEditor(DeckStore decks, CardItems items) {
        this.decks = decks;
        this.items = items;
    }

    private static final class DeckHolder implements InventoryHolder {
        private @Nullable Inventory inventory;

        @Override
        public Inventory getInventory() {
            if (inventory == null) {
                throw new IllegalStateException("Inventory not created yet");
            }
            return inventory;
        }
    }

    public void open(Player player) {
        DeckHolder holder = new DeckHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Component.text("Qwent deck"));
        holder.inventory = inventory;
        Map<CardType, Integer> copies = new EnumMap<>(CardType.class);
        for (CardType card : decks.deck(player)) {
            copies.merge(card, 1, Integer::sum);
        }
        copies.forEach((card, count) -> inventory.addItem(items.create(card, count)));
        player.openInventory(inventory);
    }

    /** Closes open deck editors so their contents are saved (e.g. when the plugin is disabled). */
    public void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof DeckHolder) {
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DeckHolder) || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory inventory = event.getInventory();
        List<CardType> deck = new ArrayList<>();
        Map<CardType, Integer> copies = new EnumMap<>(CardType.class);
        List<ItemStack> giveBack = new ArrayList<>();
        Set<CardType> blacklisted = EnumSet.noneOf(CardType.class);
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            CardType card = items.cardOf(item);
            if (card == null) {
                giveBack.add(item);
                continue;
            }
            if (DeckRules.BLACKLIST.contains(card)) {
                blacklisted.add(card);
                giveBack.add(item);
                continue;
            }
            int kept = 0;
            for (int i = 0; i < item.getAmount(); i++) {
                if (copies.getOrDefault(card, 0) < DeckRules.MAX_COPIES && deck.size() < DeckRules.MAX_CARDS) {
                    copies.merge(card, 1, Integer::sum);
                    deck.add(card);
                    kept++;
                }
            }
            if (kept < item.getAmount()) {
                ItemStack extra = item.clone();
                extra.setAmount(item.getAmount() - kept);
                giveBack.add(extra);
            }
        }
        inventory.clear();
        decks.setDeck(player, deck);
        giveBack(player, giveBack);
        for (CardType card : blacklisted) {
            player.sendMessage(Component.text(card.displayName() + " is blacklisted and can't be in a deck.",
                    NamedTextColor.RED));
        }
        tellValidity(player, deck);
    }

    /** Puts items in the player's inventory; what doesn't fit is dropped at their feet. */
    private static void giveBack(Player player, List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stacks.toArray(new ItemStack[0]));
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage(Component.text("Items that can't be in your deck were returned to you.", NamedTextColor.GRAY));
    }

    public static void tellValidity(Player player, List<CardType> deck) {
        List<String> problems = DeckRules.problems(deck);
        if (problems.isEmpty()) {
            player.sendMessage(Component.text("Your deck is valid (" + deck.size() + " cards).", NamedTextColor.GREEN));
            return;
        }
        player.sendMessage(Component.text("Your deck is not valid, you can't start a game:", NamedTextColor.RED));
        for (String problem : problems) {
            player.sendMessage(Component.text(" - " + problem, NamedTextColor.RED));
        }
    }
}
