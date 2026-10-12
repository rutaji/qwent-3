#!/usr/bin/env python3
"""Makes the card textures from the source art in art/<card>.png (any size):

- textures/font/cards/<card>.png: the board / hand picture. Minecraft packs font glyphs into 256 x 256 pages,
  so a bigger glyph is silently not shown. Art that is already 2:3 and at most 256 px is copied unchanged
  (keeps pixel art crisp); anything else is cropped to 2:3 and scaled to FONT_W x FONT_H.
- textures/item/cards/<card>.png: the item icon, the largest square from the upper part, scaled to ICON_SIZE.

Overwrites both. Run gen_cards.py afterwards for new cards. Needs Pillow:  pip install pillow
Usage: python3 make_card_textures.py [card ...]   (no arguments = all cards in gen_cards.CARDS that have art)
"""
import os
import shutil
import sys

from PIL import Image

from gen_cards import CARDS, HERE, ITEM_TEXTURES, TEXTURES

ART = os.path.join(HERE, "art")
MAX_GLYPH = 256
FONT_W, FONT_H = 160, 240  # 4x the 40 x 60 it is shown at
ICON_SIZE = 32
# Where a crop sits vertically when the art is taller than needed: 0 = top, 0.5 = centre.
VERTICAL_POSITION = 0.25


def crop(art, ratio):
    """Largest centred (horizontally) crop with width / height = ratio."""
    w, h = art.size
    if w / h > ratio:
        new_w = round(h * ratio)
        left = (w - new_w) // 2
        return art.crop((left, 0, left + new_w, h))
    new_h = round(w / ratio)
    top = int((h - new_h) * VERTICAL_POSITION)
    return art.crop((0, top, w, top + new_h))


def make(card):
    source = os.path.join(ART, f"{card}.png")
    art = Image.open(source).convert("RGBA")
    w, h = art.size
    os.makedirs(TEXTURES, exist_ok=True)
    font_path = os.path.join(TEXTURES, f"{card}.png")
    if w * 3 == h * 2 and h <= MAX_GLYPH:
        shutil.copyfile(source, font_path)
    else:
        crop(art, 2 / 3).resize((FONT_W, FONT_H), Image.LANCZOS).save(font_path)
    os.makedirs(ITEM_TEXTURES, exist_ok=True)
    crop(art, 1).resize((ICON_SIZE, ICON_SIZE), Image.LANCZOS).save(os.path.join(ITEM_TEXTURES, f"{card}.png"))


def main():
    for card in sys.argv[1:] or CARDS:
        if not os.path.exists(os.path.join(ART, f"{card}.png")):
            print(f"{card}: no art/{card}.png, skipped")
            continue
        make(card)
        print(f"{card}: done")


if __name__ == "__main__":
    main()
