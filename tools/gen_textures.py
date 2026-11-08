#!/usr/bin/env python3
"""Procedurally paints every texture of the Viltrum mod (entity skins, armor layers, item icons, particles)."""
import os, math, random
import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/viltrum/textures')
for d in ('entity', 'item', 'particle', 'models/armor'): os.makedirs(os.path.join(ROOT, d), exist_ok=True)

def clamp(v): return max(0, min(255, int(v)))
def sh(c, k): return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k), c[3] if len(c) > 3 else 255)
def rgba(c): return (c[0], c[1], c[2], c[3] if len(c) > 3 else 255)
def mix(a, b, t): return tuple(clamp(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)

def hnoise(*k):
    return (hash(k) & 0xffff) / 65535.0

# ----------------------------------------------------------------- box-uv painting
def face_rects(u, v, dx, dy, dz):
    return {'top': (u + dz, v, dx, dz), 'bottom': (u + dz + dx, v, dx, dz),
            'right': (u, v + dz, dz, dy), 'front': (u + dz, v + dz, dx, dy),
            'left': (u + dz + dx, v + dz, dz, dy), 'back': (u + 2 * dz + dx, v + dz, dx, dy)}

def paint_box(img, box, fn, tag=''):
    u, v, dx, dy, dz = box
    for face, (x0, y0, w, h) in face_rects(u, v, dx, dy, dz).items():
        for yy in range(h):
            for xx in range(w):
                c = fn(face, xx, yy, w, h)
                if c is None: continue
                c = rgba(c)
                n = (hnoise(tag, u, v, face, xx, yy) - 0.5) * 0.10
                c = sh(c, 1 + n)
                if 0 <= x0 + xx < img.width and 0 <= y0 + yy < img.height:
                    img.putpixel((x0 + xx, y0 + yy), c)

HEAD = (0, 0, 8, 8, 8); HAT = (32, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4); JACKET = (16, 32, 8, 12, 4)
RARM = (40, 16, 4, 12, 4); RSLEEVE = (40, 32, 4, 12, 4)
LARM = (32, 48, 4, 12, 4); LSLEEVE = (48, 48, 4, 12, 4)
RLEG = (0, 16, 4, 12, 4); RPANTS = (0, 32, 4, 12, 4)
LLEG = (16, 48, 4, 12, 4); LPANTS = (0, 48, 4, 12, 4)

def side_shade(face, c):
    return {'top': sh(c, 1.08), 'bottom': sh(c, 0.7), 'right': sh(c, 0.88), 'left': sh(c, 0.88), 'back': sh(c, 0.94), 'front': c}[face]

# ================================================================= ENTITY SKINS
def skin_viltrumite():
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    SKIN = (226, 176, 142); HAIR = (24, 20, 22); WHITE = (240, 241, 244); RED = (198, 28, 38); GOLD = (222, 180, 60)
    def head(face, x, y, w, h):
        if face == 'top' or face == 'back': return side_shade(face, HAIR)
        if face in ('left', 'right'): return side_shade(face, HAIR if y < 3 or x > 4 or x < 1 else SKIN)
        if y < 2: return HAIR
        if y == 2 and (x in (0, 7) or hnoise('h', x) > .6): return HAIR
        if y == 3: return (20, 18, 20) if x in (1, 2, 5, 6) else SKIN
        if y == 4:
            if x in (1, 6): return (250, 250, 250)
            if x in (2, 5): return (20, 24, 40)
        if y == 5 and x in (0, 7): return sh(SKIN, .85)
        if y == 6 and 1 <= x <= 6 and x not in (3, 4): return (30, 24, 24)
        if y == 6 and x in (3, 4): return sh(SKIN, .9)
        if y == 7 and x in (3, 4): return (150, 80, 76)
        return SKIN
    paint_box(img, HEAD, head, 'vh')
    def body(face, x, y, w, h):
        if face in ('top', 'bottom'): return side_shade(face, WHITE)
        if face == 'back':
            return RED if (y < 3 or abs(x - 3.5) < 1.2 + y * 0.05) and y < 11 else sh(RED, .72) if y < 11 else WHITE
        if face == 'front':
            if y <= 5 and abs(abs(x - 3.5) - (4.2 - y * 0.75)) < 0.95: return RED
            if y in (9, 10): return (GOLD if (y == 9 and x in (3, 4)) or (y == 10 and x in (3, 4)) else RED)
            return sh(WHITE, 1 - 0.04 * (y / 11))
        return side_shade(face, WHITE if y < 9 else RED)
    paint_box(img, BODY, body, 'vb')
    def arm(face, x, y, w, h):
        if y >= 8: return side_shade(face, RED)
        if y == 7: return side_shade(face, sh(RED, .8))
        return side_shade(face, WHITE)
    for box in (RARM, LARM): paint_box(img, box, arm, 'va')
    def leg(face, x, y, w, h):
        if y >= 8: return side_shade(face, sh(RED, .95))
        return side_shade(face, WHITE)
    for box in (RLEG, LLEG): paint_box(img, box, leg, 'vl')
    # cape-ish jacket layer: red shoulders pads
    def jacket(face, x, y, w, h):
        if face == 'back' and y < 11: return (170, 22, 30, 255) if (x + y) % 5 else (140, 16, 24, 255)
        if face in ('left', 'right') and y < 2: return (170, 22, 30, 255)
        return None
    paint_box(img, JACKET, jacket, 'vj')
    return img

def skin_reanimen():
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    SKIN = (186, 176, 168); METAL = (120, 126, 136); DARK = (38, 40, 48); CYAN = (60, 230, 255); RED = (255, 40, 30)
    def head(face, x, y, w, h):
        cyber = x >= 4 if face in ('front', 'top') else face == 'left'
        base = METAL if cyber else SKIN
        if face == 'top' and y == 3: base = DARK
        if face == 'front':
            if y == 4 and x in (5, 6): return RED
            if y == 4 and x in (1, 2): return (210, 210, 200) if x == 1 else (40, 20, 20)
            if y == 3 and x in (1, 2): return sh(SKIN, .6)
            if x == 4 and y > 0: return DARK
            if y == 6 and 2 <= x <= 5: return sh(base, .55)
            if y == 7 and x in (4, 5, 6): return sh(METAL, .8)
            if y == 1 and not cyber and x % 2: return sh(SKIN, .85)
            if cyber and (x + y) % 3 == 0: return sh(METAL, 1.18)
        if face == 'back': return side_shade(face, DARK)
        return side_shade(face, base)
    paint_box(img, HEAD, head, 'rh')
    def body(face, x, y, w, h):
        base = DARK
        if face == 'front':
            if x in (2, 5) and 1 <= y <= 9: return CYAN
            if y == 4 and 2 <= x <= 5: return CYAN
            if (x, y) in ((0, 0), (7, 0)): return METAL
            if y in (6, 7, 8) and 1 <= x <= 6 and y % 2 == 0: return sh(METAL, .7)
            if 1 <= x <= 6 and 5 <= y <= 9: return sh(DARK, 1.5)
        if face == 'back' and x == 3 and y % 2 == 0: return CYAN
        return side_shade(face, base)
    paint_box(img, BODY, body, 'rb')
    def rarm(face, x, y, w, h):
        if y < 3: return side_shade(face, sh(DARK, 1.2))
        if y >= 9: return side_shade(face, sh(SKIN, .8))
        return side_shade(face, DARK)
    paint_box(img, RARM, rarm, 'rra')
    def larm(face, x, y, w, h):  # fully robotic arm
        c = METAL
        if y % 4 == 3: return sh(DARK, 1.0)
        if (x + y) % 4 == 0 and face == 'front': return CYAN
        return side_shade(face, c)
    paint_box(img, LARM, larm, 'rla')
    def leg(face, x, y, w, h):
        if y >= 9: return side_shade(face, sh(METAL, .6))
        if face == 'front' and x == 1 and y in (3, 4): return CYAN
        return side_shade(face, DARK)
    for box in (RLEG, LLEG): paint_box(img, box, leg, 'rl')
    return img

def skin_flaxan():
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    GREEN = (112, 168, 84); GREEN_D = (74, 122, 58); ORANGE = (222, 112, 28); ORANGE_D = (160, 72, 14); BLACK = (20, 20, 24); GREY = (70, 72, 82)
    def head(face, x, y, w, h):
        if face == 'front':
            if y in (3, 4, 5) and ((1 <= x <= 2) or (5 <= x <= 6)): return (8, 8, 12) if not (y == 3 and x in (1, 6)) else (200, 255, 220)
            if y == 1 and x in (3, 4): return GREEN_D
            if y == 6 and 2 <= x <= 5: return GREEN_D
            if y == 7 and x in (3, 4): return sh(GREEN_D, .7)
        if face == 'top' and (x in (3, 4)): return GREEN_D
        return side_shade(face, GREEN)
    paint_box(img, HEAD, head, 'fh')
    def helmet(face, x, y, w, h):  # hat layer: orange crest + cheek guards
        if face == 'top' and x in (3, 4): return ORANGE
        if face == 'front' and y == 0: return ORANGE_D
        if face in ('left', 'right') and 3 <= y <= 6 and x < 2: return ORANGE_D
        return None
    paint_box(img, HAT, helmet, 'fhat')
    def body(face, x, y, w, h):
        if face == 'front':
            if y in (0, 1) and x in (0, 1, 6, 7): return GREY
            if x in (3, 4) and 2 <= y <= 8: return BLACK
            if y == 9 or y == 10: return sh(BLACK, 1.2)
            if (x + y) % 6 == 0: return ORANGE_D
        if face == 'back' and (x == 3 or x == 4): return BLACK
        return side_shade(face, ORANGE if y < 9 else BLACK)
    paint_box(img, BODY, body, 'fb')
    def arm(face, x, y, w, h):
        if y >= 9: return side_shade(face, BLACK)
        if y == 8: return side_shade(face, GREY)
        return side_shade(face, ORANGE if y < 3 else GREEN)
    for box in (RARM, LARM): paint_box(img, box, arm, 'fa')
    def leg(face, x, y, w, h):
        if y >= 9: return side_shade(face, ORANGE_D)
        return side_shade(face, BLACK if y > 1 else GREY)
    for box in (RLEG, LLEG): paint_box(img, box, leg, 'fl')
    return img

# ================================================================= ARMOR LAYERS (64x32)
class Suit:
    pass

def armor_layers(name, S):
    l1 = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); l2 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    paint_box(l1, HEAD, S.helmet, name + 'hm')
    paint_box(l1, BODY, S.chest, name + 'ch')
    paint_box(l1, RARM, S.arm, name + 'ar')
    paint_box(l1, RLEG, S.boots, name + 'bt')
    paint_box(l2, BODY, S.belt, name + 'bl')
    paint_box(l2, RLEG, S.legs, name + 'lg')
    l1.save(os.path.join(ROOT, 'models/armor', name + '_layer_1.png'))
    l2.save(os.path.join(ROOT, 'models/armor', name + '_layer_2.png'))

def make_invincible():
    YEL = (248, 200, 30); YELD = (206, 150, 12); BLUE = (26, 76, 196); BLUED = (14, 44, 128); BLK = (16, 16, 26); WHT = (245, 248, 255)
    S = Suit()
    def helmet(face, x, y, w, h):
        if face == 'front':
            if y in (3, 4) and (1 <= x <= 2 or 5 <= x <= 6): return WHT if (y == 3) else YEL
            if y >= 6 and 2 <= x <= 5: return None  # mouth open
            if y == 5 and 1 <= x <= 6 and x not in (2, 5): return BLUED
            if y == 0 or (y == 1 and x in (0, 7)): return BLK
        if face in ('left', 'right') and y == 4: return YEL
        if face == 'top' and x in (3, 4): return YEL
        return side_shade(face, BLUE)
    def chest(face, x, y, w, h):
        if face == 'front':
            if 2 <= x <= 5: return YEL if y != 11 else YELD
            if x in (1, 6): return BLK if y < 11 else BLUED
            return BLUE
        if face == 'back': return YEL if 2 <= x <= 5 else BLUE
        if face in ('top', 'bottom'): return side_shade(face, BLUE)
        return side_shade(face, BLUE if y % 6 != 5 else BLK)
    def arm(face, x, y, w, h):
        if y >= 8: return side_shade(face, YEL)
        if y == 7: return side_shade(face, BLK)
        if y == 0: return side_shade(face, BLUED)
        return side_shade(face, BLUE)
    def boots(face, x, y, w, h):
        if y < 8: return None
        if y == 8: return side_shade(face, BLK)
        if y == 11: return side_shade(face, BLK)
        return side_shade(face, YEL)
    def belt(face, x, y, w, h):
        if y < 9: return None
        if face == 'front' and x in (3, 4) and y == 10: return BLK
        return side_shade(face, YEL if y != 11 else YELD)
    def legs(face, x, y, w, h):
        if face in ('left', 'right') and x == 1: return BLK
        if y == 0: return side_shade(face, BLUED)
        return side_shade(face, BLUE)
    S.helmet, S.chest, S.arm, S.boots, S.belt, S.legs = helmet, chest, arm, boots, belt, legs
    armor_layers('invincible_suit', S)

def make_omniman():
    WHT = (242, 244, 248); WHTD = (200, 206, 216); RED = (200, 30, 40); REDD = (142, 20, 30); GOLD = (226, 184, 62); HAIR = (22, 18, 20)
    S = Suit()
    def helmet(face, x, y, w, h):
        if face in ('top', 'back'): return side_shade(face, HAIR)
        if face == 'front': return HAIR if y < 2 or (y == 2 and x in (0, 7)) else None
        return HAIR if y < 3 or x > 5 else None
    def chest(face, x, y, w, h):
        if face == 'front':
            if y <= 5 and abs(abs(x - 3.5) - (4.2 - y * 0.75)) < 0.95: return RED
            return WHT if y < 10 else WHTD
        if face == 'back': return RED if (x + y // 3) % 7 else REDD
        if face == 'top': return side_shade(face, WHT)
        return side_shade(face, WHT if y < 10 else WHTD)
    def arm(face, x, y, w, h):
        if y >= 8: return side_shade(face, RED)
        if y == 7: return side_shade(face, REDD)
        if y < 2: return side_shade(face, WHTD)
        return side_shade(face, WHT)
    def boots(face, x, y, w, h):
        if y < 8: return None
        return side_shade(face, RED if y != 11 else REDD)
    def belt(face, x, y, w, h):
        if y < 9: return None
        if face == 'front' and x in (3, 4) and y == 10: return GOLD
        return side_shade(face, RED if y != 11 else REDD)
    def legs(face, x, y, w, h):
        if y == 0: return side_shade(face, WHTD)
        return side_shade(face, WHT if y < 10 else WHTD)
    S.helmet, S.chest, S.arm, S.boots, S.belt, S.legs = helmet, chest, arm, boots, belt, legs
    armor_layers('omniman_suit', S)

# ================================================================= ITEM ICONS (16x16 ascii)
SPR = {}
SPR['helmet'] = ["................", "................", ".....OOOOOO.....", "....OAAAAAAO....", "...OAAHAAAAAO...",
                 "..OAAAAAAAAAAO..", "..OAAAAAAAAAAO..", "..OAAAAAAAAAAO..", "..OAEEAAAAEEAO..", "..OAEEEAAEEEAO..",
                 "..OABAAAAAABAO..", "..OAB......BAO..", "..OOB......BOO..", "...OO......OO...", "................", "................"]
SPR['chest'] = ["................", "..OOO......OOO..", ".OAAAO....OAAAO.", "OAAAAAOOOOAAAAAO", "OAAABOAAAAOBAAAO",
                "OAAABOAEEAOBAAAO", "OAAOBAAEEAABOAAO", ".OOOBAAEEAABOOO.", "...OBAAAAAABO...", "...OBAAAAAABO...",
                "...OBAAAAAABO...", "...OSSSSSSSSO...", "...OAAAAAAAAO...", "...OBBBBBBBBO...", "....OOOOOOOO....", "................"]
SPR['legs'] = ["................", "...OOOOOOOOOO...", "..OSSSSSSSSSSO..", "..OAAAAAAAAAAO..", "..OAAAAAAAAAAO..",
               "..OAAAOOOOAAAO..", "..OAAAO..OAAAO..", "..OAAAO..OAAAO..", "..OAAAO..OAAAO..", "..OABAO..OABAO..",
               "..OABAO..OABAO..", "..OABAO..OABAO..", "..OAAAO..OAAAO..", "..OBBBO..OBBBO..", "..OOOOO..OOOOO..", "................"]
SPR['boots'] = ["................", "................", "................", "................", "...OOO....OOO...",
                "..OSSSO..OSSSO..", "..OAAAO..OAAAO..", "..OAAAO..OAAAO..", "..OAAAO..OAAAO..", "..OAAAOOOOAAAO..",
                ".OAAAAAOOAAAAAO.", "OAAAAAAAOAAAAAAO", "OBBBBBBBOBBBBBBO", "OOOOOOOOOOOOOOOO", "................", "................"]
SPR['vial'] = ["................", "......OOOO......", ".....OHHHHO.....", "......OBBO......", "......OAAO......", "......OAAO......",
               ".....OAAAAO.....", "....OAEEEEAO....", "...OAEEEEEEAO...", "...OAEEHEEEAO...", "...OAEEEEEEAO...",
               "...OAEEEEEEAO...", "....OAEEEEAO....", ".....OAAAAO.....", "......OOOO......", "................"]
SPR['blaster'] = ["................", "................", "..........OOOO..", ".........OAAAAO.", "...OOOOOOOAEEAAO",
                  "..OAAAAAAAAEEAAO", ".OAAHHHAAAAAAAO.", ".OAAAAAAAAAAAO..", "..OOBBOOOOOOOO..", "....OBAO........",
                  "....OBAO........", "....OBAO........", "...OBBAO........", "...OBBBO........", "...OOOOO........", "................"]

def sprite(name, pal, outfile):
    rows = SPR[name]; img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        r = (rows[y] + '.' * 16)[:16] if y < len(rows) else '.' * 16
        for x in range(16):
            ch = r[x]
            if ch != '.' and ch in pal: img.putpixel((x, y), rgba(pal[ch]))
    img.save(os.path.join(ROOT, 'item', outfile + '.png'))

def pal(main, sec, emb, hi=None):
    return {'O': sh(main, 0.28), 'A': main, 'B': sec, 'S': sh(main, 0.7), 'H': hi or sh(main, 1.35), 'E': emb}

def items():
    mk = {'invincible': pal((246, 196, 30), (26, 76, 196), (240, 250, 255), (255, 240, 150)),
          'omniman': pal((240, 242, 246), (200, 30, 40), (200, 30, 40), (255, 255, 255))}
    # mask: blue body, yellow lens
    p = pal((26, 76, 196), (246, 196, 30), (252, 214, 60), (110, 150, 255)); sprite('helmet', p, 'invincible_mask')
    p = pal((26, 76, 196), (246, 196, 30), (246, 196, 30), (110, 150, 255)); sprite('chest', p, 'invincible_chest')
    sprite('legs', pal((26, 76, 196), (246, 196, 30), (246, 196, 30)), 'invincible_legs')
    sprite('boots', pal((246, 196, 30), (200, 150, 10), (246, 196, 30)), 'invincible_boots')
    # omni-man: "helmet" is the hair cap
    sprite('helmet', {'O': (10, 8, 10), 'A': (30, 24, 28), 'B': (60, 50, 56), 'S': (18, 14, 16), 'H': (80, 70, 76), 'E': (0, 0, 0, 0)}, 'omniman_hair')
    sprite('chest', pal((240, 242, 246), (200, 30, 40), (200, 30, 40), (255, 255, 255)), 'omniman_chest')
    sprite('legs', pal((240, 242, 246), (200, 30, 40), (200, 30, 40)), 'omniman_legs')
    sprite('boots', pal((200, 30, 40), (150, 20, 28), (200, 30, 40)), 'omniman_boots')
    sprite('vial', {'O': (20, 40, 44), 'A': (176, 200, 214), 'B': (120, 120, 130), 'H': (255, 255, 255), 'E': (255, 214, 64), 'S': (60, 70, 90)}, 'viltrumite_serum')
    sprite('blaster', {'O': (18, 20, 18), 'A': (70, 140, 74), 'B': (46, 52, 50), 'H': (170, 230, 170), 'E': (255, 70, 50), 'S': (40, 90, 44)}, 'flaxan_blaster')

# ================================================================= PARTICLES
def particles():
    n = 64; y, x = np.mgrid[0:n, 0:n]; r = np.hypot(x - n / 2 + 0.5, y - n / 2 + 0.5) / (n / 2)
    a = np.clip(1 - r, 0, 1) ** 2.2
    img = np.zeros((n, n, 4), np.uint8); img[..., :3] = 255; img[..., 3] = (a * 255).astype(np.uint8)
    Image.fromarray(img, 'RGBA').save(os.path.join(ROOT, 'particle/spark.png'))
    # streak: 64 x 16, bright core along its length
    W, H = 64, 16; y, x = np.mgrid[0:H, 0:W]
    ly = np.abs(y - H / 2 + 0.5) / (H / 2); lx = x / (W - 1)
    a = np.clip(1 - ly, 0, 1) ** 1.6 * np.clip(np.sin(np.pi * np.clip(lx, 0, 1)) ** 0.8, 0, 1) * (0.35 + 0.65 * lx)
    img = np.zeros((H, W, 4), np.uint8); img[..., :3] = 255; img[..., 3] = (np.clip(a, 0, 1) * 255).astype(np.uint8)
    Image.fromarray(img, 'RGBA').save(os.path.join(ROOT, 'particle/streak.png'))
    # shock ring: 128x128 soft ring peaking at r=0.82
    n = 128; y, x = np.mgrid[0:n, 0:n]; r = np.hypot(x - n / 2 + 0.5, y - n / 2 + 0.5) / (n / 2)
    ring = np.exp(-((r - 0.80) / 0.075) ** 2) + 0.22 * np.exp(-((r - 0.55) / 0.2) ** 2) * (r < 0.8)
    ring *= np.clip((1 - r) * 12 + 0.0, 0, 1) if False else 1
    ring[r > 1] = 0
    img = np.zeros((n, n, 4), np.uint8); img[..., :3] = 255; img[..., 3] = (np.clip(ring, 0, 1) * 255).astype(np.uint8)
    Image.fromarray(img, 'RGBA').save(os.path.join(ROOT, 'particle/shock_ring.png'))

def main():
    skin_viltrumite().save(os.path.join(ROOT, 'entity/viltrumite.png'))
    skin_reanimen().save(os.path.join(ROOT, 'entity/reanimen.png'))
    skin_flaxan().save(os.path.join(ROOT, 'entity/flaxan.png'))
    make_invincible(); make_omniman(); items(); particles()
    # preview sheet for visual QA
    import glob
    print('done')

main()
