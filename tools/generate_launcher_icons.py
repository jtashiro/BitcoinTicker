#!/usr/bin/env python3
"""
Generate ic_launcher.png and ic_launcher_round.png for all standard mipmap densities
based on app/src/main/res/mipmap-xxxhdpi/ic_launcher_play_512.png.
Backs up any existing files to *.bak.<timestamp> and prints a verification table.
"""
from pathlib import Path
import sys
import time

try:
    from PIL import Image, ImageDraw
except Exception as e:
    print('Pillow is required: pip install pillow')
    raise

ROOT = Path(__file__).resolve().parents[1] / 'app' / 'src' / 'main' / 'res'
SRC = ROOT / 'mipmap-xxxhdpi' / 'ic_launcher_play_512.png'
if not SRC.exists():
    print('ERROR: source not found:', SRC)
    sys.exit(1)

sizes = [
    ('mipmap-ldpi', 36),
    ('mipmap-mdpi', 48),
    ('mipmap-hdpi', 72),
    ('mipmap-xhdpi', 96),
    ('mipmap-xxhdpi', 144),
    ('mipmap-xxxhdpi', 192),
]

ts = int(time.time())
img = Image.open(SRC).convert('RGBA')
results = []

for folder, size in sizes:
    out_dir = ROOT / folder
    out_dir.mkdir(parents=True, exist_ok=True)
    out_sq = out_dir / 'ic_launcher.png'
    out_round = out_dir / 'ic_launcher_round.png'
    # backup existing
    for p in (out_sq, out_round):
        if p.exists():
            bak = p.parent / (p.name + f'.bak.{ts}')
            if not bak.exists():
                p.rename(bak)
    # create square resized
    sq = img.resize((size, size), Image.LANCZOS)
    sq.save(out_sq, format='PNG')
    # create round with circular alpha
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    sq_round = sq.copy()
    sq_round.putalpha(mask)
    sq_round.save(out_round, format='PNG')
    # verify
    try:
        r = Image.open(out_round)
        has_alpha = 'A' in r.getbands()
        results.append((folder, size, out_sq.exists(), out_round.exists(), f'{r.width}x{r.height}', has_alpha))
    except Exception:
        results.append((folder, size, out_sq.exists(), out_round.exists(), 'ERR', False))

print('folder,size,ic_launcher_exists,ic_launcher_round_exists,round_dims,round_has_alpha')
for r in results:
    print(','.join(map(str, r)))

print('\nBackups (if any) saved as *.bak.<timestamp> in the same folders (timestamp={}).'.format(ts))

