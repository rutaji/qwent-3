package com.rutaji.qwent3.cards;

import com.rutaji.qwent3.cards.effect.Effect;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A player's view of a game, made of clickable body text: the board (opponent's two rows on top, own two rows at the
 * bottom, front rows facing each other, with labels on both sides), the own hand, and a pass text. During the
 * mulligan only the hand is shown. Clicks send "qwent:tile/N" (absolute tile), "qwent:hand/N", "qwent:keep",
 * "qwent:pass", "qwent:view/...", "qwent:back"; Esc sends "qwent:close".
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
    private static final int HAND_GAP_LINES = 1;
    /**
     * Width of the label areas left and right of the board. Both sides are equally wide,
     * so the (centred) board stays in the middle.
     */
    private static final int SIDE_W = 100;
    private static final int SIDE_MARGIN = 8;
    private static final int BODY_WIDTH = 1024;

    private final Glyphs glyphs;

    public GameDialog(Glyphs glyphs) {
        this.glyphs = glyphs;
    }

    /** Shows the viewer (playing {@code side}) the game, their mulligan, or a pile screen. */
    public void show(Player player, Match match, int side, @Nullable PileView view) {
        GameState state = match.state();
        Dialog dialog;
        if (view != null) {
            dialog = pile(match, side, view);
        } else if (state.phase() == GameState.Phase.MULLIGAN) {
            dialog = mulligan(match, side);
        } else {
            dialog = game(match, side);
        }
        player.showDialog(dialog);
    }

    /** The end of a game; {@code color} for the result line (won / lost / draw). */
    public void showResult(Player player, String result, TextColor color, String reason, String score) {
        DialogBase base = DialogBase.builder(Component.text("Qwent - " + result))
                .canCloseWithEscape(true)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(List.of(
                        DialogBody.plainMessage(Component.text(result, color)),
                        DialogBody.plainMessage(Component.text(reason, NamedTextColor.GRAY)),
                        DialogBody.plainMessage(Component.text(score, NamedTextColor.WHITE))))
                .build();
        player.showDialog(Dialog.create(builder -> builder.empty().base(base).type(DialogType.notice())));
    }

    // ---- Screens ----

    /** Mulligan: own hand to redraw from and "keep hand"; after keeping, a waiting message. */
    private Dialog mulligan(Match match, int side) {
        GameState state = match.state();
        Side own = state.side(side);
        int other = GameState.other(side);
        String opponent = match.name(other);
        List<DialogBody> body = new ArrayList<>();
        if (state.round() > 1) {
            int mine = state.lastRoundPower(side);
            int theirs = state.lastRoundPower(other);
            String result = mine > theirs ? "you won" : mine < theirs ? "you lost" : "draw";
            NamedTextColor color = mine > theirs ? NamedTextColor.GREEN
                    : mine < theirs ? NamedTextColor.RED : NamedTextColor.GOLD;
            body.add(DialogBody.plainMessage(Component.text("Round " + (state.round() - 1) + ": " + result
                    + " (" + mine + " : " + theirs + ")", color)));
            body.add(DialogBody.plainMessage(Component.text("Points: you " + state.points(side) + " : "
                    + state.points(other) + " " + opponent, NamedTextColor.GOLD)));
        }
        if (own.mulliganDone()) {
            body.add(DialogBody.plainMessage(Component.text("Waiting for " + opponent + " to finish their mulligan.",
                    NamedTextColor.GRAY)));
            body.add(DialogBody.plainMessage(handBlock(match.state(), side, 0), BODY_WIDTH));
            return dialog("Qwent - Round " + state.round() + " mulligan", body);
        }
        Component keep = Component.text("[ Keep hand ]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.custom(Key.key(NAMESPACE, "keep")))
                .hoverEvent(HoverEvent.showText(Component.text("Start the game with this hand")));
        body.add(DialogBody.plainMessage(Component.text("Click a card to redraw it (" + own.redrawsLeft()
                + (own.redrawsLeft() == 1 ? " redraw" : " redraws") + " left).", NamedTextColor.YELLOW)));
        body.add(DialogBody.plainMessage(Component.text(
                "A redrawn card won't come back during the mulligan.", NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(handBlock(match.state(), side, 0), BODY_WIDTH));
        body.add(DialogBody.plainMessage(keep));
        return dialog("Qwent - Round " + state.round() + " mulligan vs " + opponent, body);
    }

    private Dialog game(Match match, int side) {
        GameState state = match.state();
        int opponent = GameState.other(side);
        Side own = state.side(side);
        Side theirs = state.side(opponent);
        Effect pending = state.pendingEffect();
        boolean picking = pending != null && state.pendingOwner() == side;

        // Rows from top to bottom: opponent back, opponent front, own front, own back.
        int[][] rows = {{opponent, 1}, {opponent, 0}, {side, 0}, {side, 1}};
        List<Component> boardLines = new ArrayList<>();
        for (int[] row : rows) {
            Cell[] cells = new Cell[GameState.COLUMNS];
            for (int col = 0; col < GameState.COLUMNS; col++) {
                int tile = GameState.tile(row[0], row[1], col);
                cells[col] = tileCell(state, tile, picking && pending.isValidTarget(state, side, tile));
            }
            boardLines.addAll(rowLines(cells));
        }
        boolean myTurn = state.current() == side;
        Component[] left = new Component[boardLines.size()];
        Component[] right = new Component[boardLines.size()];
        left[3] = sideLabel("Power: " + state.power(opponent), NamedTextColor.GOLD, null, true);
        right[1] = sideLabel("Points: " + state.points(opponent), NamedTextColor.GOLD, null, false);
        right[3] = sideLabel("Hand: " + theirs.handSize(), NamedTextColor.GRAY, null, false);
        if (theirs.passed()) {
            right[5] = sideLabel("Passed", NamedTextColor.RED, null, false);
        }
        left[10] = sideLabel("Graveyard: " + theirs.graveyard().size(), NamedTextColor.GRAY, "view/theirs/graveyard", true);
        right[10] = sideLabel("Deck: " + theirs.deckSize(), NamedTextColor.GRAY, null, false);
        left[13] = sideLabel("Round " + state.round(), NamedTextColor.GRAY, null, true);
        right[13] = sideLabel(myTurn ? "Your turn" : "Opponent's turn",
                myTurn ? NamedTextColor.GREEN : NamedTextColor.RED, null, false);
        left[17] = sideLabel("Graveyard: " + own.graveyard().size(), NamedTextColor.GRAY, "view/mine/graveyard", true);
        right[17] = sideLabel("Deck: " + own.deckSize(), NamedTextColor.GRAY, "view/mine/deck", false);
        left[24] = sideLabel("Power: " + state.power(side), NamedTextColor.GOLD, null, true);
        right[22] = sideLabel("Points: " + state.points(side), NamedTextColor.GOLD, null, false);
        right[24] = sideLabel("Hand: " + own.handSize(), NamedTextColor.GRAY, null, false);
        if (own.passed()) {
            right[26] = sideLabel("Passed", NamedTextColor.RED, null, false);
        }
        TextComponent.Builder board = Component.text();
        for (int line = 0; line < boardLines.size(); line++) {
            if (line > 0) {
                board.append(Component.newline());
            }
            board.append(left[line] != null ? left[line] : Glyphs.space(SIDE_W))
                    .append(boardLines.get(line))
                    .append(right[line] != null ? right[line] : Glyphs.space(SIDE_W));
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(statusLine(match, side)));
        body.add(DialogBody.plainMessage(board.build().shadowColor(ShadowColor.none()), BODY_WIDTH));
        body.add(DialogBody.plainMessage(handBlock(state, side, HAND_GAP_LINES), BODY_WIDTH));
        if (myTurn && pending == null && !own.passed()) {
            body.add(DialogBody.plainMessage(Component.text("[ Pass ]", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.custom(Key.key(NAMESPACE, "pass")))
                    .hoverEvent(HoverEvent.showText(Component.text(
                            "End your turns for the rest of the round.\nWhen both players passed, the higher power"
                                    + " gets a point.\n" + GameState.POINTS_TO_WIN + " points win the game.")))));
        }
        return dialog("Qwent vs " + match.name(opponent), body);
    }

    private static Component statusLine(Match match, int side) {
        GameState state = match.state();
        String opponent = match.name(GameState.other(side));
        Effect pending = state.pendingEffect();
        if (pending != null) {
            if (state.pendingOwner() == side) {
                return Component.text().append(pending.description()).append(Component.text(" - pick a target."))
                        .color(NamedTextColor.LIGHT_PURPLE).build();
            }
            return Component.text().append(Component.text(opponent + " is choosing: "))
                    .append(pending.description()).color(NamedTextColor.GRAY).build();
        }
        if (state.current() != side) {
            return Component.text("Waiting for " + opponent + "...", NamedTextColor.GRAY);
        }
        CardType selected = state.selectedCard(side);
        if (selected == null) {
            String passed = state.side(GameState.other(side)).passed() ? " " + opponent + " has passed." : "";
            return Component.text("Your turn: play a card or pass." + passed, NamedTextColor.YELLOW);
        }
        String hint = selected.isMonster()
                ? selected.displayName() + " (" + selected.power() + ") - click an empty tile on your side."
                : selected.displayName() + " - click any tile to cast it.";
        return Component.text("Selected: " + hint, NamedTextColor.YELLOW);
    }

    /** All cards of a pile in rows of {@link GameState#MAX_HAND}, with hover tooltips, and a "back" text. */
    private Dialog pile(Match match, int side, PileView view) {
        Side owner = match.state().side(view.side());
        List<CardType> cards = new ArrayList<>(view.pile() == Pile.DECK ? owner.deck() : owner.graveyard());
        if (view.pile() == Pile.DECK) {
            // Sorted, so looking at the deck does not reveal the draw order.
            cards.sort(Comparator.naturalOrder());
        }
        String pileName = view.pile() == Pile.DECK ? "deck" : "graveyard";
        String name = view.side() == side ? "Your " + pileName : match.name(view.side()) + "'s " + pileName;
        TextComponent.Builder rows = Component.text();
        boolean firstLine = true;
        for (int start = 0; start < cards.size(); start += GameState.MAX_HAND) {
            List<CardType> rowCards = cards.subList(start, Math.min(start + GameState.MAX_HAND, cards.size()));
            Cell[] cells = new Cell[rowCards.size()];
            for (int i = 0; i < cells.length; i++) {
                CardType card = rowCards.get(i);
                Integer power = card.isMonster() ? card.power() : null;
                cells[i] = cell(art(card.textArtGlyphId(), false), card.shortName(), power, NamedTextColor.WHITE,
                        null, tooltip(card, null));
            }
            for (Component line : rowLines(cells)) {
                if (!firstLine) {
                    rows.append(Component.newline());
                }
                rows.append(line);
                firstLine = false;
            }
        }
        Component back = Component.text("[ Back ]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.custom(Key.key(NAMESPACE, "back")))
                .hoverEvent(HoverEvent.showText(Component.text("Back to the game")));
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(name + ": " + cards.size()
                + (cards.size() == 1 ? " card" : " cards"), NamedTextColor.YELLOW)));
        if (view.pile() == Pile.DECK) {
            body.add(DialogBody.plainMessage(Component.text("Shown sorted, not in draw order.", NamedTextColor.GRAY)));
        }
        if (!cards.isEmpty()) {
            body.add(DialogBody.plainMessage(rows.build().shadowColor(ShadowColor.none()), BODY_WIDTH));
        }
        body.add(DialogBody.plainMessage(back));
        return dialog("Qwent - " + name, body);
    }

    /** The viewer's hand as one text block, after {@code gapLines} empty lines. */
    private Component handBlock(GameState state, int side, int gapLines) {
        TextComponent.Builder hand = Component.text();
        for (int i = 0; i < gapLines; i++) {
            hand.append(Component.text(" ")).append(Component.newline());
        }
        Side own = state.side(side);
        Cell[] handCells = new Cell[own.handSize()];
        for (int i = 0; i < handCells.length; i++) {
            handCells[i] = handCell(own, i);
        }
        List<Component> handLines = rowLines(handCells);
        for (int line = 0; line < handLines.size(); line++) {
            if (line > 0) {
                hand.append(Component.newline());
            }
            hand.append(handLines.get(line));
        }
        return hand.build().shadowColor(ShadowColor.none());
    }

    private static Dialog dialog(String title, List<DialogBody> body) {
        DialogBase base = DialogBase.builder(Component.text(title))
                .canCloseWithEscape(true)
                .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                .body(body)
                .build();
        // Every dialog type needs at least one button: a 2 px one is invisible with the button override.
        // Esc runs this button's action, so it must close instead of redrawing the dialog.
        ActionButton hidden = ActionButton.builder(Component.empty())
                .width(2)
                .action(DialogAction.customClick(Key.key(NAMESPACE, "close"), null))
                .build();
        return Dialog.create(builder -> builder.empty().base(base).type(DialogType.notice(hidden)));
    }

    // ---- Cards ----

    /**
     * What one card cell shows: the art for the first line (null = nothing) and its width,
     * an optional power number, click and hover.
     */
    private record Cell(@Nullable Component art, int artWidth, @Nullable Component power, int powerWidth,
                        @Nullable String action, @Nullable Component tooltip) {
    }

    private Cell tileCell(GameState state, int tile, boolean target) {
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

    private Cell handCell(Side side, int index) {
        CardType card = side.handCard(index);
        boolean selected = index == side.selectedHandIndex();
        Integer power = card.isMonster() ? card.power() : null;
        return cell(art(card.textArtGlyphId(), selected), (selected ? ">" : "") + card.shortName(), power,
                NamedTextColor.WHITE, "hand/" + index, tooltip(card, null));
    }

    /** Art from a glyph (or a short text when the resource pack is missing) and an optional power number. */
    private static Cell cell(@Nullable Component glyph, String fallback, @Nullable Integer power, TextColor powerColor,
                             @Nullable String action, Component tooltip) {
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

    /** Hover text: name, power and rules text. */
    private static Component tooltip(CardType card, @Nullable Unit unit) {
        TextComponent.Builder text = Component.text();
        text.append(Component.text(card.displayName(), NamedTextColor.GOLD)).append(Component.newline());
        if (!card.isMonster()) {
            text.append(Component.text("Spell", NamedTextColor.AQUA));
        } else if (unit != null && unit.power() != card.power()) {
            text.append(Component.text("Power: " + unit.power() + " (base " + card.power() + ")"));
        } else {
            text.append(Component.text("Power: " + card.power()));
        }
        if (!card.tags().isEmpty()) {
            text.append(Component.newline()).append(Component.text(card.tagsText(), NamedTextColor.DARK_AQUA));
        }
        return text.append(Component.newline())
                .append(Component.text(card.description(), NamedTextColor.GRAY))
                .build();
    }
    /** The {@link #LINES} text lines of one card row; every line is exactly as wide as the row. */
    private static List<Component> rowLines(Cell[] cells) {
        List<Component> lines = new ArrayList<>();
        for (int line = 0; line < LINES; line++) {
            TextComponent.Builder text = Component.text();
            for (int col = 0; col < cells.length; col++) {
                if (col > 0) {
                    text.append(Glyphs.space(GAP));
                }
                text.append(cellLine(cells[col], line));
            }
            lines.add(text.build());
        }
        return lines;
    }

    /** A label exactly {@link #SIDE_W} wide, aligned towards the board; clickable if {@code action} is set. */
    private static Component sideLabel(String text, TextColor color, @Nullable String action, boolean leftOfBoard) {
        int width = textWidth(text);
        Component label = Component.text(text, color).shadowColor(ShadowColor.shadowColor(0xFF2A2A2A));
        if (action != null) {
            label = label.clickEvent(ClickEvent.custom(Key.key(NAMESPACE, action)))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to view")));
        }
        Component outer = Glyphs.space(SIDE_W - SIDE_MARGIN - width);
        Component inner = Glyphs.space(SIDE_MARGIN);
        return leftOfBoard
                ? Component.text().append(outer).append(label).append(inner).build()
                : Component.text().append(inner).append(label).append(outer).build();
    }

    /** Width in the default font (glyph width + 1 px spacing); exact for the characters used in labels. */
    private static int textWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            width += switch (c) {
                case 'i', ':', '.', ',', '!', '\'', '|' -> 2;
                case 'l' -> 3;
                case 'I', 't', ' ' -> 4;
                case 'f', 'k', '<', '>' -> 5;
                default -> CHAR_W;
            };
        }
        return width;
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
        if (cell.action() != null) {
            content = content.clickEvent(ClickEvent.custom(Key.key(NAMESPACE, cell.action())));
        }
        return cell.tooltip() == null ? content : content.hoverEvent(HoverEvent.showText(cell.tooltip()));
    }
}
