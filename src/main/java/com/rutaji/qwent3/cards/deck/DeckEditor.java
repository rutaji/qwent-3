package com.rutaji.qwent3.cards.deck;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import com.rutaji.qwent3.cards.chat.ChatSender;
import net.kyori.adventure.text.Component;
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
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.rutaji.qwent3.movout.inventory.giveBack;

/**
 * /qwent deck: a chest with the player's starting deck as card items. The player can take cards out and put
 * cards in. On close the chest becomes the new deck; everything that can't be in it goes back to the player:
 * non-card items, blacklisted cards, copies above {@link DeckRules#MAX_COPIES} and cards above
 * {@link DeckRules#MAX_CARDS}.
 */
public final class DeckEditor implements Listener {
    private static final int SIZE = 54;

    private final DeckSaveNBT decks;
    private final CardItems items;

    public DeckEditor(DeckSaveNBT decks, CardItems items) {
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

    /**
     * Called by command, to open inventory. Doesn't open if a card of the deck can't be made into an item: the chest
     * would miss that card, and closing it would remove the card from the saved deck.
     */
    public void open(Player player) {
        Map<CardType, Integer> copies = new EnumMap<>(CardType.class);
        for (CardType card : decks.get(player)) {
            copies.merge(card, 1, Integer::sum);
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<CardType, Integer> entry : copies.entrySet()) {
            ItemStack item = items.CardToItem(entry.getKey(), entry.getValue());
            if (item == null) {
                ChatSender.deckEditorUnavailable(player);
                return;
            }
            stacks.add(item);
        }

        DeckHolder holder = new DeckHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Component.text("Qwent deck"));
        holder.inventory = inventory;
        stacks.forEach(inventory::addItem);
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

    /**
     * The chest becomes the new deck: its items are turned into cards, {@link DeckRules#fit} decides which of them
     * fit, the deck is saved and everything else goes back to the player (non-card items as they were, rejected cards
     * as new card items).
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DeckHolder) || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory inventory = event.getInventory();
        List<CardType> cards = new ArrayList<>();
        List<ItemStack> giveBack = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            CardType card = items.ItemToCard(item);
            if (card == null) {
                giveBack.add(item);
            } else {
                cards.addAll(Collections.nCopies(item.getAmount(), card));
            }
        }
        inventory.clear();

        DeckRules.DeckFit fit = DeckRules.fit(cards, player);
        decks.setDeck(player, fit.deck());

        Map<CardType, Integer> rejected = new EnumMap<>(CardType.class);
        for (CardType card : fit.rejected()) {
            rejected.merge(card, 1, Integer::sum);
        }
        rejected.forEach((card, count) -> {
            giveBack.addAll(items.cardStacks(card, count));
        });

        giveBack(player, giveBack);

        DeckRules.tellValidity(fit.deck(), player);
    }

}
