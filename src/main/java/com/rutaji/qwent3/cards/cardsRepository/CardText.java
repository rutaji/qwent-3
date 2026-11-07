package com.rutaji.qwent3.cards.cardsRepository;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The card text shared by tooltips and item lore: tags, then keywords with their explanation, then rules text. */
public final class CardText {
    private CardText() {
    }

    public static List<Component> lines(CardType card) {
        List<Component> lines = new ArrayList<>();
        if (!card.tags().isEmpty()) {
            lines.add(Component.text(card.tagsText(), NamedTextColor.DARK_AQUA));
        }
        for (Keyword keyword : card.keywords()) {
            lines.add(Component.text()
                    .append(Component.text(keyword.displayName(), NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" - " + keyword.explanation(), NamedTextColor.GRAY))
                    .build());
        }
        lines.add(Component.text(card.description(), NamedTextColor.GRAY));
        return lines;
    }

    /**
     * Adds Lore to card ItemStack
     */
    public static void applyTo(ItemStack item, CardType card) {
        item.editMeta(meta -> {
            meta.displayName(notItalic(Component.text(card.displayName(), NamedTextColor.GOLD)));
            List<Component> lore = new ArrayList<>();
            lore.add(notItalic(Component.text(card.isMonster() ? "Power: " + card.power() : "Spell",
                    NamedTextColor.WHITE)));
            for (Component line : lines(card)) {
                lore.add(notItalic(line));
            }
            meta.lore(lore);
        });
    }

    private static Component notItalic(Component text) {
        return text.decoration(TextDecoration.ITALIC, false);
    }
}
