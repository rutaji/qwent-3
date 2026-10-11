package com.rutaji.changingitems.cards;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the game screen as a multi-action dialog: board (2 rows x 5) on top, the hand (9 cards in one row)
 * lower down. Every button sends a custom click with id "qwent:tile/N", "qwent:hand/N" or "qwent:noop".
 *
 * The grid has one column count for all rows, so it is {@link GameState#HAND_SIZE} wide; board rows are
 * centred with 2 px filler buttons, which are fully transparent with the resource pack's button override
 * (a 2 px nine-slice button is drawn only from its transparent edge slices).
 *
 * With ItemsAdder glyphs available, each card is a stack of {@link CardArt#STACK} buttons whose labels
 * together show the big card art. Otherwise every card is one button with a text label.
 */
public final class GameDialog {
    public static final String NAMESPACE = "qwent";
    private static final int GRID_COLUMNS = GameState.HAND_SIZE;
    private static final int BOARD_OFFSET = (GRID_COLUMNS - GameState.COLUMNS) / 2;
    /** Invisible filler button rows between the board and the hand (22 px each). */
    private static final int HAND_GAP_ROWS = 2;
    private static final int FILLER_W = 2;
    private static final int TEXT_BUTTON_W = 48;

    private final Glyphs glyphs;
    private final CardArt art;

    public GameDialog(Glyphs glyphs) {
        this.glyphs = glyphs;
        this.art = new CardArt(glyphs);
    }

    public void show(Player player, GameState state) {
        player.showDialog(build(state));
    }

    private Dialog build(GameState state) {
        CardType selected = state.selectedCard();
        Component selectedLine = selected == null
                ? Component.text("Pick a card from your hand.", NamedTextColor.GRAY)
                : Component.text("Selected: " + selected.displayName() + " (" + selected.power() + ") - click an empty tile.", NamedTextColor.YELLOW);

        DialogBase base = DialogBase.builder(Component.text("Qwent"))
                .canCloseWithEscape(true)
                .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                .body(List.of(
                        DialogBody.plainMessage(Component.text("Total power: " + state.totalPower(), NamedTextColor.GOLD)),
                        DialogBody.plainMessage(selectedLine)
                ))
                .build();

        boolean artMode = art.available();
        int stack = artMode ? CardArt.STACK : 1;
        List<ActionButton> buttons = new ArrayList<>();
        for (int boardRow = 0; boardRow < GameState.ROWS; boardRow++) {
            for (int part = 0; part < stack; part++) {
                for (int col = 0; col < GRID_COLUMNS; col++) {
                    int boardCol = col - BOARD_OFFSET;
                    buttons.add(boardCol >= 0 && boardCol < GameState.COLUMNS
                            ? tileButton(state, boardRow * GameState.COLUMNS + boardCol, artMode ? part : -1)
                            : filler());
                }
            }
        }
        for (int i = 0; i < HAND_GAP_ROWS * GRID_COLUMNS; i++) {
            buttons.add(filler());
        }
        for (int part = 0; part < stack; part++) {
            for (int index = 0; index < GameState.HAND_SIZE; index++) {
                buttons.add(handButton(state, index, artMode ? part : -1));
            }
        }

        // No exit button: Esc closes the dialog.
        DialogType type = DialogType.multiAction(buttons)
                .columns(GRID_COLUMNS)
                .build();

        return Dialog.create(builder -> builder.empty().base(base).type(type));
    }

    /** {@code part} is the slice of the card art, or -1 for text mode. */
    private ActionButton tileButton(GameState state, int tile, int part) {
        CardType card = state.boardCard(tile);
        if (card == null) {
            Component label = part >= 0 ? art.emptyTile(part) : Component.text("·", NamedTextColor.DARK_GRAY);
            return cardButton(label, Component.text("Empty tile"), "tile/" + tile, null, part);
        }
        Component label = part >= 0 ? art.card(card, part, false) : cardLabel(card);
        return cardButton(label, tooltip(card), "tile/" + tile, card, part);
    }

    private ActionButton handButton(GameState state, int index, int part) {
        CardType card = state.handCard(index);
        if (card == null) {
            Component label = part >= 0 ? Component.empty() : Component.text("-", NamedTextColor.DARK_GRAY);
            return cardButton(label, null, "noop", null, part);
        }
        boolean selected = index == state.selectedHandIndex();
        Component label = part >= 0 ? art.card(card, part, selected) : textLabel(card, selected);
        return cardButton(label, tooltip(card), "hand/" + index, card, part);
    }

    private ActionButton cardButton(@Nullable Component label, @Nullable Component tooltip, String action,
                                    @Nullable CardType card, int part) {
        if (label == null) {
            // Glyph missing for this card: show its name on the middle button only.
            label = part == 1 && card != null ? Component.text(card.shortName() + " " + card.power()) : Component.empty();
        }
        return ActionButton.builder(label)
                .tooltip(tooltip)
                .width(part >= 0 ? CardArt.BUTTON_W : TEXT_BUTTON_W)
                .action(click(action))
                .build();
    }

    /** Invisible spacer: only the transparent edge slices of the button sprite are drawn. */
    private static ActionButton filler() {
        return ActionButton.builder(Component.empty())
                .width(FILLER_W)
                .action(click("noop"))
                .build();
    }

    private Component textLabel(CardType card, boolean selected) {
        Component label = cardLabel(card);
        if (!selected) {
            return label;
        }
        return Component.text()
                .append(Component.text("[", NamedTextColor.YELLOW))
                .append(label)
                .append(Component.text("]", NamedTextColor.YELLOW))
                .decorate(TextDecoration.BOLD)
                .build();
    }

    /** Small icon (or short name) followed by the power. */
    private Component cardLabel(CardType card) {
        Component icon = glyphs.resolve(card.iconGlyphId());
        if (icon == null) {
            icon = Component.text(card.shortName(), NamedTextColor.AQUA);
        }
        return Component.text().append(icon).append(Component.text(" " + card.power(), NamedTextColor.WHITE)).build();
    }

    /** Big card art (if available), name and power. */
    private Component tooltip(CardType card) {
        Component info = Component.text(card.displayName() + "\nPower: " + card.power());
        Component art = glyphs.resolve(card.bigGlyphId());
        return art == null ? info : Component.text().append(art).append(Component.newline()).append(info).build();
    }

    private static DialogAction click(String path) {
        return DialogAction.customClick(Key.key(NAMESPACE, path), null);
    }
}
