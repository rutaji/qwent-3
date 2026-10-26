package com.rutaji.qwent3.cards.cardsRepository;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
}
