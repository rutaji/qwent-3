#!/usr/bin/env python3
"""Generates contents/qwent/configs/cards.yml, placeholder PNGs for missing art, and the raw pack files.

The game dialog (GameDialog) draws each card as one glyph, 60 px tall (40 x 60 for 2:3 art), from the top
of a text line, hanging down over the next lines. If the art sits too high/low against its clickable area,
change TEXT_ASCENT. After a change: rerun, copy contents/qwent and contents/qwent_raw to
plugins/ItemsAdder/contents/ and run /iazip.
"""
import json
import os
import struct
import sys
import zipfile
import zlib

CARDS = ["peasant", "archer", "knight", "mage", "dragon", "arrow", "two_arrows"]  # must match CardType (lower case)
ART_W, ART_H = 64, 96      # size of <card>_big.png; a placeholder is made for missing art
FRAME_W, FRAME_H = 40, 60  # tile_empty / select_frame placeholders

# Vanilla button sprites are overridden with transparent left/right edges, so the dialog's required
# footer button can be made invisible (2 px wide). This affects ALL buttons in the game.
# Pass a different client jar as the first argument if needed.
CLIENT_JAR = os.path.expanduser("~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/"
                                "libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar")
EDGE = 10
BUTTON_SPRITES = ["button", "button_disabled", "button_highlighted"]

# A glyph's top is drawn at (line top + 7 - y_position).
TEXT_CARD_H = 60
TEXT_ASCENT = 7

HERE = os.path.dirname(os.path.abspath(__file__))
CONTENT = os.path.join(HERE, "contents", "qwent")
# Raw resource pack files (vanilla overrides, space font). Must be a separate content folder:
# ItemsAdder 4.1 puts every folder of a "textures/"-style content folder under assets/<namespace>/.
RAW = os.path.join(HERE, "contents", "qwent_raw", "resourcepack")
TEXTURES = os.path.join(CONTENT, "textures", "font", "cards")


def text_glyph(name, texture):
    return [f"  {name}:", f"    path: font/cards/{texture}.png",
            f"    scale_ratio: {TEXT_CARD_H}", f"    y_position: {TEXT_ASCENT}"]


def write_png(path, pixels, w, h):
    raw = b"".join(b"\x00" + bytes(c for px in pixels[y * w:(y + 1) * w] for c in px) for y in range(h))
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


def read_png(data):
    """Minimal decoder for 8-bit non-interlaced grey / grey+alpha / RGB / RGBA PNGs -> (w, h, RGBA list)."""
    pos, idat = 8, b""
    while pos < len(data):
        length, tag = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        if tag == b"IHDR":
            w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            if depth != 8 or interlace or ctype not in (0, 2, 4, 6):
                raise ValueError("unsupported PNG format")
        elif tag == b"IDAT":
            idat += body
        pos += 12 + length
    channels = {0: 1, 2: 3, 4: 2, 6: 4}[ctype]
    raw, stride = zlib.decompress(idat), w * channels
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        ftype, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - channels] if x >= channels else 0
            b = prev[x]
            c = prev[x - channels] if x >= channels else 0
            if ftype == 1:
                line[x] = (line[x] + a) & 0xFF
            elif ftype == 2:
                line[x] = (line[x] + b) & 0xFF
            elif ftype == 3:
                line[x] = (line[x] + (a + b) // 2) & 0xFF
            elif ftype == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 0xFF
        rows.append(line)
        prev = line
    pixels = []
    for line in rows:
        for x in range(w):
            px = line[x * channels:(x + 1) * channels]
            if ctype == 0:
                pixels.append((px[0], px[0], px[0], 255))
            elif ctype == 4:
                pixels.append((px[0], px[0], px[0], px[1]))
            elif ctype == 2:
                pixels.append((px[0], px[1], px[2], 255))
            else:
                pixels.append(tuple(px))
    return w, h, pixels


def override_buttons(client_jar):
    out = os.path.join(RAW, "assets", "minecraft", "textures", "gui", "sprites", "widget")
    os.makedirs(out, exist_ok=True)
    with zipfile.ZipFile(client_jar) as jar:
        for name in BUTTON_SPRITES:
            base = f"assets/minecraft/textures/gui/sprites/widget/{name}"
            w, h, px = read_png(jar.read(base + ".png"))
            px = [(0, 0, 0, 0) if i % w < EDGE or i % w >= w - EDGE else p for i, p in enumerate(px)]
            write_png(os.path.join(out, name + ".png"), px, w, h)
            # The left/right nine-slice border must cover the transparent edge, or the tiled centre shows it
            # again. Top/bottom stay vanilla: top + bottom must be less than the 20 px height.
            meta = json.loads(jar.read(base + ".png.mcmeta"))
            scaling = meta["gui"]["scaling"]
            vanilla = scaling.get("border", 0)
            if isinstance(vanilla, int):
                vanilla = {"left": vanilla, "top": vanilla, "right": vanilla, "bottom": vanilla}
            scaling["border"] = {"left": max(vanilla["left"], EDGE), "top": vanilla["top"],
                                 "right": max(vanilla["right"], EDGE), "bottom": vanilla["bottom"]}
            with open(os.path.join(out, name + ".png.mcmeta"), "w") as f:
                json.dump(meta, f, indent=4)


def write_space_font():
    """qwent:space font: U+F801.. = -1,-2,...,-128 px; U+F821.. = +1,...,+128 px (see Glyphs.space)."""
    advances = {}
    for i in range(8):
        advances[chr(0xF801 + i)] = -(1 << i)
        advances[chr(0xF821 + i)] = 1 << i
    out = os.path.join(RAW, "assets", "qwent", "font")
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "space.json"), "w") as f:
        json.dump({"providers": [{"type": "space", "advances": advances}]}, f, indent=2)


def placeholder(path, fill, border, border_px, w, h):
    """Writes a framed rectangle, unless the file already exists (real art is never overwritten)."""
    if os.path.exists(path):
        return
    px = []
    for y in range(h):
        for x in range(w):
            edge = min(x, y, w - 1 - x, h - 1 - y) < border_px
            px.append(border if edge else fill)
    write_png(path, px, w, h)


def main():
    os.makedirs(TEXTURES, exist_ok=True)
    os.makedirs(os.path.join(CONTENT, "configs"), exist_ok=True)
    lines = ["# Generated by itemsadder/gen_cards.py - edit the script, not this file.",
             "info:", "  namespace: qwent", "font_images:"]
    for card in CARDS:
        placeholder(os.path.join(TEXTURES, f"{card}_big.png"), (60, 60, 90, 255), (230, 230, 230, 255), 3,
                    ART_W, ART_H)
        # Big art in the hover tooltip, and the same art at card size on the board / in the hand.
        lines += [f"  card_{card}_big:", f"    path: font/cards/{card}_big.png", "    scale_ratio: 96",
                  "    y_position: 10"]
        lines += text_glyph(f"card_{card}_text", f"{card}_big")
    placeholder(os.path.join(TEXTURES, "tile_empty.png"), (0, 0, 0, 90), (200, 200, 200, 200), 1, FRAME_W, FRAME_H)
    placeholder(os.path.join(TEXTURES, "select_frame.png"), (0, 0, 0, 0), (255, 215, 0, 255), 2, FRAME_W, FRAME_H)
    lines += text_glyph("tile_empty_text", "tile_empty")
    lines += text_glyph("select_frame_text", "select_frame")
    with open(os.path.join(CONTENT, "configs", "cards.yml"), "w") as f:
        f.write("\n".join(lines) + "\n")

    write_space_font()
    with open(os.path.join(RAW, "pack.mcmeta"), "w") as f:
        json.dump({"pack": {"pack_format": 7, "description": "qwent"}}, f)

    client_jar = sys.argv[1] if len(sys.argv) > 1 else CLIENT_JAR
    if os.path.exists(client_jar):
        override_buttons(client_jar)
    else:
        print(f"Client jar not found ({client_jar}); button sprites not overridden.")


if __name__ == "__main__":
    main()
