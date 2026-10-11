package com.rutaji.changingitems.cards;

import com.rutaji.changingitems.cards.effect.Effect;
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
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Game screen made of clickable body text: board (2 x 5) and hand (9) as two text blocks.
 * Clicks send "qwent:tile/N", "qwent:hand/N"; Esc sends "qwent:close".
 *
 * Every card is {@link #LINES} text lines tall (9 px each, no gaps). The first line draws the card glyph
 * (it hangs down over the following lines), {@link #POWER_LINE} shows the power; every line of the card carries
 * the same click event over the card's whole width. All lines of a row are equally wide and the text is centred,
 * so the lines stay aligned.
 */
public final class GameDialog {
    public static final String NAMESPACE = "qwent";
    /** Card cell width; the art glyph is 40 x 60 (2:3), drawn from the top of the first line. */
    private static final int CARD_W = 40;
    /** 7 lines x 9 px = 63 px clickable height; the 3 px below the art separate the rows. */
    private static final int LINES = 7;
    private static final int POWER_LINE = 5;
    private static final int POWER_INDENT = 3;
    private static final int GAP = 3;
    /** A bitmap glyph advances by its width + 1. */
    private static final int GLYPH_ADVANCE = CARD_W + 1;
    /** Default font: digits and most letters are 5 px + 1 px spacing. */
    private static final int CHAR_W = 6;
    /** Empty lines between the board and the hand. */
    private static final int HAND_GAP_LINES = 2;
    private static final int BODY_WIDTH = 1024;

    private final Glyphs glyphs;

    public GameDialog(Glyphs glyphs) {
        this.glyphs = glyphs;
    }

    public void show(Player player, GameState state) {
        player.showDialog(build(state));
    }

    private Dialog build(GameState state) {
        Effect pending = state.pendingEffect();

        TextComponent.Builder board = Component.text();
        for (int row = 0; row < GameState.ROWS; row++) {
            if (row > 0) {
                board.append(Component.newline());
            }
            Cell[] cells = new Cell[GameState.COLUMNS];
            for (int col = 0; col < GameState.COLUMNS; col++) {
                cells[col] = tileCell(state, row * GameState.COLUMNS + col, pending);
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
                        DialogBody.plainMessage(statusLine(state, pending)),
                        DialogBody.plainMessage(board.build().shadowColor(ShadowColor.none()), BODY_WIDTH),
                        DialogBody.plainMessage(hand.build().shadowColor(ShadowColor.none()), BODY_WIDTH)
                ))
                .build();

        // Every dialog type needs at least one button: a 2 px one is invisible with the button override.
        // Esc runs this button's action, so it must close instead of redrawing the dialog.
        ActionButton hidden = ActionButton.builder(Component.empty())
                .width(2)
                .action(DialogAction.customClick(Key.key(NAMESPACE, "close"), null))
                .build();
        return Dialog.create(builder -> builder.empty().base(base).type(DialogType.notice(hidden)));
    }

    private static Component statusLine(GameState state, @Nullable Effect pending) {
        if (pending != null) {
            return Component.text().append(pending.description()).append(Component.text(" - pick a target."))
                    .color(NamedTextColor.LIGHT_PURPLE).build();
        }
        CardType selected = state.selectedCard();
        if (selected == null) {
            return Component.text("Pick a card from your hand.", NamedTextColor.GRAY);
        }
        String hint = selected.isMonster()
                ? selected.displayName() + " (" + selected.power() + ") - click an empty tile."
                : selected.displayName() + " - click any tile to cast it.";
        return Component.text("Selected: " + hint, NamedTextColor.YELLOW);
    }

    /**
     * What one card cell shows: the art for the first line (null = nothing) and its width,
     * an optional power number, click and hover.
     */
    private record Cell(@Nullable Component art, int artWidth, @Nullable Component power, int powerWidth,
                        @Nullable String action, @Nullable Component tooltip) {
        static final Cell EMPTY = new Cell(null, 0, null, 0, null, null);
    }

    private Cell tileCell(GameState state, int tile, @Nullable Effect pending) {
        boolean target = pending != null && pending.isValidTarget(state, tile);
        Unit unit = state.unit(tile);
        if (unit == null) {
            return cell(art("qwent:tile_empty_text", target), "--", null, NamedTextColor.WHITE,
                "tile/" + tile, Component.text("Empty tile"));
        }
        CardType type = unit.type();
        NamedTextColor color = unit.power() > type.power() ? NamedTextColor.GREEN
                : unit.power() < type.power() ? NamedTextColor.RED : NamedTextColor.WHITE;
        return cell(art(type.textArtGlyphId(), target), type.shortName(), unit.power(), color,
                "tile/" + tile, tooltip(type, unit));
    }

    private Cell handCell(GameState state, int index) {
        CardType card = state.handCard(index);
        if (card == null) {
            return Cell.EMPTY;
        }
        boolean selected = index == state.selectedHandIndex();
        Integer power = card.isMonster() ? card.power() : null;
        return cell(art(card.textArtGlyphId(), selected), (selected ? ">" : "") + card.shortName(), power,
                NamedTextColor.WHITE, "hand/" + index, tooltip(card, null));
    }

    /** Art from a glyph (or a short text when the resource pack is missing) and an optional power number. */
    private static Cell cell(@Nullable Component glyph, String fallback, @Nullable Integer power, TextColor powerColor,
                             String action, Component tooltip) {
        Component powerText = null;
        int powerWidth = 0;
        if (power != null) {
            String digits = String.valueOf(power);
            // Keep a dark shadow on the number so it is readable on top of the art.
            powerText = Component.text(digits, powerColor).shadowColor(ShadowColor.shadowColor(0xFF000000));
            powerWidth = digits.length() * CHAR_W;
        }
        if (glyph != null) {
            return new Cell(glyph, GLYPH_ADVANCE, powerText, powerWidth, action, tooltip);
        }
        return new Cell(Component.text(fallback, NamedTextColor.AQUA), fallback.length() * CHAR_W,
                powerText, powerWidth, action, tooltip);
    }

    /** The card glyph, with the gold frame drawn over it when highlighted; null if the glyph is missing. */
    private @Nullable Component art(String glyphId, boolean highlighted) {
        Component art = glyphs.resolve(glyphId);
        if (art == null || !highlighted) {
            return art;
        }
        Component frame = glyphs.resolve("qwent:select_frame_text");
        return frame == null ? art
                : Component.text().append(art).append(Glyphs.space(-GLYPH_ADVANCE)).append(frame).build();
    }

    /** Hover text: big card art (if available), name, power and rules text. */
    private Component tooltip(CardType card, @Nullable Unit unit) {
        TextComponent.Builder text = Component.text();
        Component big = glyphs.resolve(card.bigGlyphId());
        if (big != null) {
            text.append(big).append(Component.newline());
        }
        text.append(Component.text(card.displayName(), NamedTextColor.GOLD)).append(Component.newline());
        if (!card.isMonster()) {
            text.append(Component.text("Spell", NamedTextColor.AQUA));
        } else if (unit != null && unit.power() != card.power()) {
            text.append(Component.text("Power: " + unit.power() + " (base " + card.power() + ")"));
        } else {
            text.append(Component.text("Power: " + card.power()));
        }
        return text.append(Component.newline())
                .append(Component.text(card.description(), NamedTextColor.GRAY))
                .build();
    }

    /** The {@link #LINES} text lines of one card row; every line is exactly as wide as the row. */
    private static Component rowLines(Cell[] cells) {
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
    private static Component cellLine(Cell cell, int line) {
        Component content;
        if (line == 0 && cell.art() != null) {
            content = Component.text().append(cell.art()).append(Glyphs.space(CARD_W - cell.artWidth())).build();
        } else if (line == POWER_LINE && cell.power() != null) {
            content = Component.text()
                    .append(Glyphs.space(POWER_INDENT))
                    .append(cell.power())
                    .append(Glyphs.space(CARD_W - POWER_INDENT - cell.powerWidth()))
                    .build();
        } else {
            content = Glyphs.space(CARD_W);
        }
        if (cell.action() == null) {
            return content;
        }
        Component styled = content.clickEvent(ClickEvent.custom(Key.key(NAMESPACE, cell.action())));
        return cell.tooltip() == null ? styled : styled.hoverEvent(HoverEvent.showText(cell.tooltip()));
    }
}
