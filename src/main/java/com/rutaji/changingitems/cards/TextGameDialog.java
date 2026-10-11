package com.rutaji.changingitems.cards;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Game screen made of clickable body text instead of buttons, so there are no gaps and no grid.
 *
 * Every card is {@link #LINES} text lines tall (9 px each, no gaps). The first line draws the card glyph
 * (it hangs down over the following lines); every line of the card carries the same click event on a
 * card-wide run of text, so the whole card is clickable. Each line of a row has the same width, and the
 * text is centred, so the lines stay aligned. Board and hand are separate body elements.
 */
public final class TextGameDialog implements GameView {
    /** Card cell width; the art glyph is 40 x 60 (2:3), drawn from the top of the first line. */
    private static final int CARD_W = 40;
    /** 7 lines x 9 px = 63 px clickable height; the 3 px below the art separate the rows. */
    private static final int LINES = 7;
    private static final int GAP = 3;
    /** A bitmap glyph advances by its width + 1. */
    private static final int GLYPH_ADVANCE = CARD_W + 1;
    /** Empty lines between the board and the hand. */
    private static final int HAND_GAP_LINES = 2;
    private static final int BODY_WIDTH = 1024;

    private final Glyphs glyphs;
    private final CardArt art;

    public TextGameDialog(Glyphs glyphs) {
        this.glyphs = glyphs;
        this.art = new CardArt(glyphs);
    }

    @Override
    public void show(Player player, GameState state) {
        player.showDialog(build(state));
    }

    private Dialog build(GameState state) {
        CardType selected = state.selectedCard();
        Component selectedLine = selected == null
                ? Component.text("Pick a card from your hand.", NamedTextColor.GRAY)
                : Component.text("Selected: " + selected.displayName() + " (" + selected.power() + ") - click an empty tile.", NamedTextColor.YELLOW);

        TextComponent.Builder board = Component.text();
        for (int row = 0; row < GameState.ROWS; row++) {
            if (row > 0) {
                board.append(Component.newline());
            }
            Cell[] cells = new Cell[GameState.COLUMNS];
            for (int col = 0; col < GameState.COLUMNS; col++) {
                cells[col] = tileCell(state, row * GameState.COLUMNS + col);
            }
            board.append(rowLines(cells));
        }

        TextComponent.Builder hand = Component.text();
        for (int i = 0; i < HAND_GAP_LINES; i++) {
            hand.append(Component.text(" ")).append(Component.newline());
        }
        Cell[] handCells = new Cell[GameState.HAND_SIZE];
        for (int i = 0; i < GameState.HAND_SIZE; i++) {
            handCells[i] = handCell(state, i);
        }
        hand.append(rowLines(handCells));

        DialogBase base = DialogBase.builder(Component.text("Qwent"))
                .canCloseWithEscape(true)
                .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                .body(List.of(
                        DialogBody.plainMessage(Component.text("Total power: " + state.totalPower(), NamedTextColor.GOLD)),
                        DialogBody.plainMessage(selectedLine),
                        DialogBody.plainMessage(board.build().shadowColor(ShadowColor.none()), BODY_WIDTH),
                        DialogBody.plainMessage(hand.build().shadowColor(ShadowColor.none()), BODY_WIDTH)
                ))
                .build();

        // Every dialog type needs at least one button: a 2 px one is invisible with the button override.
        // Esc runs this button's action, so it must close instead of redrawing the dialog.
        ActionButton hidden = ActionButton.builder(Component.empty())
                .width(2)
                .action(DialogAction.customClick(Key.key(GameDialog.NAMESPACE, "close"), null))
                .build();
        return Dialog.create(builder -> builder.empty().base(base).type(DialogType.notice(hidden)));
    }

    /** What one card cell shows: the art for the first line (null = nothing) and its width, click and hover. */
    private record Cell(@Nullable Component art, int artWidth, @Nullable String action, @Nullable Component tooltip) {
    }

    /** Art from a glyph, or a short text when the resource pack is missing. */
    private static Cell cell(@Nullable Component glyph, String fallback, String action, Component tooltip) {
        if (glyph != null) {
            return new Cell(glyph, GLYPH_ADVANCE, action, tooltip);
        }
        // Default font: most letters and digits are 5 px + 1 px spacing.
        return new Cell(Component.text(fallback, NamedTextColor.AQUA), fallback.length() * 6, action, tooltip);
    }

    private Cell tileCell(GameState state, int tile) {
        CardType card = state.boardCard(tile);
        if (card == null) {
            return cell(glyph("qwent:tile_empty_text"), "--", "tile/" + tile, Component.text("Empty tile"));
        }
        return cell(glyph(card.textArtGlyphId()), card.shortName() + card.power(), "tile/" + tile, art.tooltip(card));
    }

    private Cell handCell(GameState state, int index) {
        CardType card = state.handCard(index);
        if (card == null) {
            return new Cell(null, 0, null, null);
        }
        Component cardArt = glyph(card.textArtGlyphId());
        if (cardArt != null && index == state.selectedHandIndex()) {
            Component frame = glyph("qwent:select_frame_text");
            if (frame != null) {
                cardArt = Component.text().append(cardArt).append(Glyphs.space(-GLYPH_ADVANCE)).append(frame).build();
            }
        }
        String fallback = (index == state.selectedHandIndex() ? ">" : "") + card.shortName() + card.power();
        return cell(cardArt, fallback, "hand/" + index, art.tooltip(card));
    }

    private @Nullable Component glyph(String id) {
        return glyphs.resolve(id);
    }

    /** The {@link #LINES} text lines of one card row; every line is exactly as wide as the row. */
    private Component rowLines(Cell[] cells) {
        TextComponent.Builder lines = Component.text();
        for (int line = 0; line < LINES; line++) {
            if (line > 0) {
                lines.append(Component.newline());
            }
            for (int col = 0; col < cells.length; col++) {
                if (col > 0) {
                    lines.append(Glyphs.space(GAP));
                }
                lines.append(cellLine(cells[col], line));
            }
        }
        return lines.build();
    }

    /** One line of a card cell, exactly CARD_W wide, clickable over its whole width. */
    private Component cellLine(Cell cell, int line) {
        Component content;
        if (line == 0 && cell.art() != null) {
            // Pad (or trim) the art to exactly CARD_W.
            content = Component.text().append(cell.art()).append(Glyphs.space(CARD_W - cell.artWidth())).build();
        } else {
            content = Glyphs.space(CARD_W);
        }
        if (cell.action() == null) {
            return content;
        }
        Component styled = content.clickEvent(ClickEvent.custom(Key.key(GameDialog.NAMESPACE, cell.action())));
        return cell.tooltip() == null ? styled : styled.hoverEvent(HoverEvent.showText(cell.tooltip()));
    }
}
