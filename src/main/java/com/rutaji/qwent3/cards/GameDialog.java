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
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
    private static final int HAND_GAP_LINES = 2;
    /**
     * Width of the label areas left and right of the board. Both sides are equally wide,
     * so the (centred) board stays in the middle.
     */
    private static final int SIDE_W = 100;
    private static final int SIDE_MARGIN = 8;
    private static final int BODY_WIDTH = 1024;
    /** History column left of the board (outside the centred line), and the gap between it and the left labels. */
    // Sized for 1920x1080 at GUI scale 2 (960 px wide, 480 px from the centre): shift 10 + board line 206 + gap 15
    // + column 235 = 466 px, leaving a small margin at the screen edge.
    private static final int HISTORY_W = 235;
    private static final int HISTORY_GAP = 15;
    /**
     * Lines below the hand: an empty line, the [ Pass ] line, then room for the history to continue down (the
     * dialog still fits 1080p at GUI scale 2).
     */
    private static final int BOTTOM_LINES = 7;
    /** Lines above the board in the board block: the status line and an empty line. */
    private static final int TOP_LINES = 2;
    /** Width of a board line: labels on both sides and five cards. */
    private static final int BOARD_LINE_W = 2 * SIDE_W + GameState.COLUMNS * CARD_W + (GameState.COLUMNS - 1) * GAP;
    private static final int EXIT_W = 60;
    /** Distance of the turn text and the hearts from the board. */
    private static final int TURN_MARGIN = 120;
    /** Distance of the hearts from the board. */
    private static final int HEARTS_MARGIN = 140;
    /**
     * How far all card areas (game board + hand + history, mulligan hand, deck / graveyard screens) are moved left
     * of the dialog's centre: in game the dialog content shows up this much right of the screen centre.
     */
    private static final int BOARD_SHIFT = 15;
    /** Minecraft's ASCII font at 2x, and the heart glyphs (U+E000 full, U+E001 empty, 18 px + 1). */
    private static final Key BIG_FONT = Key.key("qwent", "big");
    private static final Key HEARTS_FONT = Key.key("qwent", "hearts");
    private static final char HEART_FULL = '\uE000';
    private static final char HEART_EMPTY = '\uE001';
    private static final int HEART_ADVANCE = 19;

    private final Glyphs glyphs;

    public GameDialog(Glyphs glyphs) {
        this.glyphs = glyphs;
    }

    /** Shows the viewer (playing {@code side}) the game, their mulligan, or a pile screen. */
    public void show(Player player, Match match, int side, @Nullable PileView view, boolean historyHidden) {
        GameState state = match.state();
        Dialog dialog;
        if (view != null) {
            dialog = pile(match, side, view);
        } else if (state.phase() == GameState.Phase.MULLIGAN) {
            dialog = mulligan(match, side);
        } else {
            dialog = game(match, side, historyHidden);
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
            body.add(DialogBody.plainMessage(handBlock(match.state(), side, 0, 0), BODY_WIDTH));
            return dialog("Qwent - Round " + state.round() + " mulligan", body);
        }
        Component keep = Component.text("[ Keep hand ]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.custom(Key.key(NAMESPACE, "keep")))
                .hoverEvent(HoverEvent.showText(Component.text("Start the game with this hand")));
        body.add(DialogBody.plainMessage(Component.text("Click a card to redraw it (" + own.redrawsLeft()
                + (own.redrawsLeft() == 1 ? " redraw" : " redraws") + " left).", NamedTextColor.YELLOW)));
        body.add(DialogBody.plainMessage(Component.text(
                "A redrawn card won't come back during the mulligan.", NamedTextColor.GRAY)));
        body.add(DialogBody.plainMessage(handBlock(match.state(), side, 0, 0), BODY_WIDTH));
        body.add(DialogBody.plainMessage(keep));
        return dialog("Qwent - Round " + state.round() + " mulligan vs " + opponent, body);
    }

    private Dialog game(Match match, int side, boolean historyHidden) {
        GameState state = match.state();
        int opponent = GameState.other(side);
        Side own = state.side(side);
        Side theirs = state.side(opponent);
        Effect pending = state.pendingEffect();
        boolean pickingTile = pending != null && state.pendingOwner() == side
                && pending.targetKind() == Effect.TargetKind.TILE;

        // Rows from top to bottom: opponent back, opponent front, own front, own back.
        int[][] rows = {{opponent, 1}, {opponent, 0}, {side, 0}, {side, 1}};
        List<Component> boardLines = new ArrayList<>();
        for (int[] row : rows) {
            Cell[] cells = new Cell[GameState.COLUMNS];
            for (int col = 0; col < GameState.COLUMNS; col++) {
                int tile = GameState.tile(row[0], row[1], col);
                cells[col] = tileCell(state, tile, pickingTile && pending.isValidTarget(state, side, tile));
            }
            boardLines.addAll(rowLines(cells));
        }
        boolean myTurn = state.current() == side;
        Component[] left = new Component[boardLines.size()];
        Component[] right = new Component[boardLines.size()];
        Component lead = leadText(state.power(side) - state.power(opponent));
        left[3] = sideLabel("Power: " + state.power(opponent), NamedTextColor.GOLD, null, lead, true);
        right[3] = sideLabel("Hand: " + theirs.handSize(), NamedTextColor.GRAY, null, false);
        if (theirs.passed()) {
            right[5] = sideLabel("Passed", NamedTextColor.RED, null, false);
        }
        left[10] = sideLabel("Graveyard: " + theirs.graveyard().size(), NamedTextColor.GRAY, "view/theirs/graveyard", true);
        right[10] = sideLabel("Deck: " + theirs.deckSize(), NamedTextColor.GRAY, null, false);
        // Hearts on the last line of the back / front row: the 2x glyph is centred on the line between the rows.
        right[LINES - 1] = heartsLabel(state, opponent);
        right[13] = turnLabel(myTurn);
        right[3 * LINES - 1] = heartsLabel(state, side);
        left[17] = sideLabel("Graveyard: " + own.graveyard().size(), NamedTextColor.GRAY, "view/mine/graveyard", true);
        left[12] = plantsLabel(theirs);
        left[19] = plantsLabel(own);
        right[17] = sideLabel("Deck: " + own.deckSize(), NamedTextColor.GRAY, "view/mine/deck", false);
        left[24] = sideLabel("Power: " + state.power(side), NamedTextColor.GOLD, null, lead, true);
        right[24] = sideLabel("Hand: " + own.handSize(), NamedTextColor.GRAY, null, false);
        if (own.passed()) {
            right[26] = sideLabel("Passed", NamedTextColor.RED, null, false);
        }
        // Every line of both blocks is exactly BOARD_LINE_W wide, so the history column (drawn left of each line)
        // lines up from the status line at the top to the bottom of the hand.
        List<Component> handLines = handLines(state, side);
        int handWidth = own.handSize() == 0 ? 0 : own.handSize() * CARD_W + (own.handSize() - 1) * GAP;
        int historyLineCount = TOP_LINES + boardLines.size() + HAND_GAP_LINES + handLines.size() + BOTTOM_LINES;
        Iterator<Component> history = historyHidden
                ? Collections.nCopies(historyLineCount, (Component) Component.empty()).iterator()
                : historyLines(state, side, historyLineCount).iterator();

        Component status = statusLine(match, side).shadowColor(ShadowColor.shadowColor(0xFF2A2A2A));
        // Every line ends with shiftPad, which moves the whole centred layout BOARD_SHIFT px to the left.
        Component shiftPad = shiftPad();
        TextComponent.Builder board = Component.text()
                .append(history.next()).append(padToBoard(status, textWidth(plain(status)))).append(shiftPad)
                .append(Component.newline())
                .append(history.next()).append(Glyphs.space(BOARD_LINE_W)).append(shiftPad);
        for (int line = 0; line < boardLines.size(); line++) {
            board.append(Component.newline())
                    .append(history.next())
                    .append(left[line] != null ? left[line] : Glyphs.space(SIDE_W))
                    .append(boardLines.get(line))
                    .append(right[line] != null ? right[line] : Glyphs.space(SIDE_W))
                    .append(shiftPad);
        }
        // The hand is in the same text field as the board (separate fields get a gap between them, which would
        // break the history column).
        for (int i = 0; i < HAND_GAP_LINES; i++) {
            board.append(Component.newline())
                    .append(history.next()).append(Glyphs.space(BOARD_LINE_W)).append(shiftPad);
        }
        for (Component handLine : handLines) {
            board.append(Component.newline())
                    .append(history.next()).append(padToBoard(handLine, handWidth)).append(shiftPad);
        }
        // Below the hand: [ Pass ] on the second line, the rest empty - the history continues down beside them.
        boolean canPass = myTurn && pending == null && !own.passed();
        for (int i = 0; i < BOTTOM_LINES; i++) {
            Component content = Glyphs.space(BOARD_LINE_W);
            if (i == 1 && canPass) {
                Component pass = Component.text("[ Pass ]", NamedTextColor.GOLD)
                        .shadowColor(ShadowColor.shadowColor(0xFF2A2A2A))
                        .clickEvent(ClickEvent.custom(Key.key(NAMESPACE, "pass")))
                        .hoverEvent(HoverEvent.showText(Component.text(
                                "End your turns for the rest of the round.\nWhen both players passed, the higher power"
                                        + " gets a point.\n" + GameState.POINTS_TO_WIN + " points win the game.")));
                content = padToBoard(pass, textWidth("[ Pass ]"));
            }
            board.append(Component.newline()).append(history.next()).append(content).append(shiftPad);
        }
        // Under the history column: show / hide it.
        String toggle = historyHidden ? "[ Show history ]" : "[ Hide history ]";
        Component toggleText = Component.text(toggle, NamedTextColor.GOLD)
                .shadowColor(ShadowColor.shadowColor(0xFF2A2A2A))
                .clickEvent(ClickEvent.custom(Key.key(NAMESPACE, "history")));
        board.append(Component.newline())
                .append(historyCell(toggleText, textWidth(toggle)))
                .append(Glyphs.space(BOARD_LINE_W)).append(shiftPad);

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(board.build().shadowColor(ShadowColor.none()), BODY_WIDTH));
        return dialog("Qwent vs " + match.name(opponent), body);
    }

    private static Component statusLine(Match match, int side) {
        GameState state = match.state();
        String opponent = match.name(GameState.other(side));
        Effect pending = state.pendingEffect();
        if (pending != null) {
            if (state.pendingOwner() == side) {
                String pick = pending.targetKind() == Effect.TargetKind.HAND
                        ? " - pick a card in your hand." : " - pick a target.";
                return Component.text().append(pending.description()).append(Component.text(pick))
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
        String where = selected.hasKeyword(Keyword.TREACHEROUS) ? "on either side" : "on your side";
        String hint = selected.isMonster()
                ? selected.displayName() + " (" + selected.power() + ") - click an empty tile " + where + "."
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
                rows.append(line).append(shiftPad());
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

    /** The viewer's hand as one text block, after {@code gapLines} empty lines, each line indented by {@code indent}. */
    private Component handBlock(GameState state, int side, int gapLines, int indent) {
        TextComponent.Builder hand = Component.text();
        for (int i = 0; i < gapLines; i++) {
            hand.append(Component.text(" ")).append(Component.newline());
        }
        List<Component> handLines = handLines(state, side);
        for (int line = 0; line < handLines.size(); line++) {
            if (line > 0) {
                hand.append(Component.newline());
            }
            hand.append(Glyphs.space(indent)).append(handLines.get(line)).append(shiftPad());
        }
        return hand.build().shadowColor(ShadowColor.none());
    }

    /** The text lines of the viewer's hand row (selected card / hand targets highlighted). */
    private List<Component> handLines(GameState state, int side) {
        Side own = state.side(side);
        Effect pending = state.pendingEffect();
        boolean pickingHand = pending != null && state.pendingOwner() == side
                && pending.targetKind() == Effect.TargetKind.HAND;
        Cell[] handCells = new Cell[own.handSize()];
        for (int i = 0; i < handCells.length; i++) {
            boolean highlighted = pickingHand ? pending.isValidTarget(state, side, i) : i == own.selectedHandIndex();
            handCells[i] = handCell(own, i, highlighted);
        }
        return rowLines(handCells);
    }

    /** Trailing space that moves a centred line {@link #BOARD_SHIFT} px to the left. */
    private static Component shiftPad() {
        return Glyphs.space(2 * BOARD_SHIFT);
    }

    /** Centres a line of known width inside exactly {@link #BOARD_LINE_W} (wider lines get negative padding). */
    private static Component padToBoard(Component line, int width) {
        int before = (BOARD_LINE_W - width) / 2;
        return Component.text().append(Glyphs.space(before)).append(line)
                .append(Glyphs.space(BOARD_LINE_W - width - before)).build();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static Dialog dialog(String title, List<DialogBody> body) {
        DialogBase base = DialogBase.builder(Component.text(title))
                .canCloseWithEscape(true)
                .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                .body(body)
                .build();
        // Every dialog type needs at least one button (the client centres it in the footer).
        // Esc runs this button's action too, so both just close the dialog.
        ActionButton exit = ActionButton.builder(Component.text("Exit"))
                .width(EXIT_W)
                .action(DialogAction.customClick(Key.key(NAMESPACE, "close"), null))
                .build();
        return Dialog.create(builder -> builder.empty().base(base).type(DialogType.notice(exit)));
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

    /** @param highlighted selected, or a valid target of a pending hand-target effect */
    private Cell handCell(Side side, int index, boolean highlighted) {
        CardType card = side.handCard(index);
        boolean selected = highlighted;
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
        for (Component line : CardText.lines(card)) {
            text.append(Component.newline()).append(line);
        }
        return text.build();
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

    /** "Plants: n" under a graveyard label: Plant cards in that graveyard; null (no label) if there are none. */
    private static @Nullable Component plantsLabel(Side side) {
        long plants = side.graveyard().stream().filter(card -> card.hasTag(Tag.PLANT)).count();
        return plants == 0 ? null : sideLabel("Plants: " + plants, NamedTextColor.GREEN, null, true);
    }

    /** A label exactly {@link #SIDE_W} wide, aligned towards the board; clickable if {@code action} is set. */
    private static Component sideLabel(String text, TextColor color, @Nullable String action, boolean leftOfBoard) {
        return sideLabel(text, color, action, null, leftOfBoard);
    }

    /** Same, with a hover text (a clickable label always shows "Click to view"). */
    private static Component sideLabel(String text, TextColor color, @Nullable String action,
                                       @Nullable Component hover, boolean leftOfBoard) {
        Component label = Component.text(text, color).shadowColor(ShadowColor.shadowColor(0xFF2A2A2A));
        if (action != null) {
            label = label.clickEvent(ClickEvent.custom(Key.key(NAMESPACE, action)));
            hover = Component.text("Click to view");
        }
        if (hover != null) {
            label = label.hoverEvent(HoverEvent.showText(hover));
        }
        return placeLabel(label, textWidth(text), SIDE_MARGIN, leftOfBoard);
    }

    /**
     * Puts a label of known width into a side area: exactly {@link #SIDE_W} wide, {@code margin} px from the board.
     * A wider label runs past the area; the line still counts as {@link #SIDE_W} wide, so the board stays centred.
     */
    private static Component placeLabel(Component label, int width, int margin, boolean leftOfBoard) {
        Component outer = Glyphs.space(SIDE_W - margin - width);
        Component inner = Glyphs.space(margin);
        return leftOfBoard
                ? Component.text().append(outer).append(label).append(inner).build()
                : Component.text().append(inner).append(label).append(outer).build();
    }

    /**
     * The history column: exactly {@code lines} lines, drawn left of the board line they start (each steps back by
     * {@link #HISTORY_W} + {@link #HISTORY_GAP} and returns, so it adds no width and the board stays centred). The newest
     * entries that fit, oldest at the top; long entries wrap. Green = the viewer, red = the opponent, grey = notes;
     * the card played from hand each turn is bold.
     */
    private static List<Component> historyLines(GameState state, int side, int lines) {
        List<Component> result = new ArrayList<>();
        List<GameState.LogEntry> log = state.log();
        for (int i = log.size() - 1; i >= 0 && result.size() < lines; i--) {
            GameState.LogEntry entry = log.get(i);
            NamedTextColor color = entry.actor() < 0 ? NamedTextColor.GRAY
                    : entry.actor() == side ? NamedTextColor.GREEN : NamedTextColor.RED;
            List<String> wrapped = wrap(entry.text(), entry.played());
            // Adding the entry's lines in reverse, as the whole list is reversed at the end.
            for (int j = wrapped.size() - 1; j >= 0; j--) {
                String text = wrapped.get(j);
                int width = textWidth(text) + (entry.played() ? text.length() : 0);
                Component line = Component.text(text, color).decoration(TextDecoration.BOLD, entry.played())
                        .shadowColor(ShadowColor.shadowColor(0xFF2A2A2A));
                result.add(historyCell(line, width));
            }
        }
        // An entry that didn't fit completely loses its oldest lines.
        while (result.size() > lines) {
            result.removeLast();
        }
        Collections.reverse(result);
        while (result.size() < lines) {
            result.add(Component.empty());
        }
        return result;
    }

    /** Text drawn in the history column, left of the line it starts; adds no width to the line. */
    private static Component historyCell(Component text, int width) {
        return Component.text().append(Glyphs.space(-HISTORY_W - HISTORY_GAP)).append(text)
                .append(Glyphs.space(HISTORY_W + HISTORY_GAP - width)).build();
    }

    /** Splits a history text into lines of at most {@link #HISTORY_W} px, at spaces where possible. */
    private static List<String> wrap(String text, boolean bold) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (width(candidate, bold) <= HISTORY_W) {
                line = new StringBuilder(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder();
            }
            // A word too long for a line on its own is cut.
            for (char c : word.toCharArray()) {
                if (width(line + String.valueOf(c), bold) > HISTORY_W) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
                line.append(c);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static int width(String text, boolean bold) {
        return textWidth(text) + (bold ? text.length() : 0);
    }

    /** Hover text of the power labels: how much the viewer is winning or losing by. */
    private static Component leadText(int lead) {
        if (lead > 0) {
            return Component.text("You are winning by " + lead, NamedTextColor.GREEN);
        }
        if (lead < 0) {
            return Component.text("You are losing by " + -lead, NamedTextColor.RED);
        }
        return Component.text("Tied", NamedTextColor.GOLD);
    }

    /** "Your turn" / "Opponent's turn" in the 2x font. */
    private static Component turnLabel(boolean myTurn) {
        String text = myTurn ? "Your turn" : "Opponent's turn";
        Component label = Component.text(text, myTurn ? NamedTextColor.GREEN : NamedTextColor.RED)
                .font(BIG_FONT).shadowColor(ShadowColor.shadowColor(0xFF2A2A2A));
        return placeLabel(label, bigTextWidth(text), TURN_MARGIN, false);
    }

    /**
     * A side's hearts: one full heart per round the opponent still has to win ({@link GameState#POINTS_TO_WIN}
     * minus the opponent's points), the rest empty.
     */
    private static Component heartsLabel(GameState state, int side) {
        int full = Math.max(0, GameState.POINTS_TO_WIN - state.points(GameState.other(side)));
        String hearts = String.valueOf(HEART_FULL).repeat(full)
                + String.valueOf(HEART_EMPTY).repeat(GameState.POINTS_TO_WIN - full);
        Component label = Component.text(hearts).font(HEARTS_FONT).shadowColor(ShadowColor.none())
                .hoverEvent(HoverEvent.showText(Component.text(
                        "A heart is lost for every round the opponent wins.\nNo hearts left = game lost.")));
        return placeLabel(label, GameState.POINTS_TO_WIN * HEART_ADVANCE, HEARTS_MARGIN, false);
    }

    /** Width in the 2x font (qwent:big): every glyph is twice as wide plus 1 px spacing; a space is 8 px. */
    private static int bigTextWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            width += c == ' ' ? 8 : (textWidth(String.valueOf(c)) - 1) * 2 + 1;
        }
        return width;
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
