#!/usr/bin/env python3
"""
Regenerate ic_launcher_round.png for standard mipmap densities so the artwork fills
the circular area completely. Uses app/src/main/res/mipmap-xxxhdpi/ic_launcher_play_512.png
as source.
Backs up existing ic_launcher_round.png to ic_launcher_round.png.bak.<timestamp>.
Prints CSV: folder,file,bytes,width,height,color_type,has_alpha
"""
from pathlib import Path
import sys, time, struct
try:
    from PIL import Image, ImageOps, ImageDraw
except Exception:
    print('Pillow required: pip install pillow')
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
img_src = Image.open(SRC).convert('RGBA')
results = []

# overscale factor to ensure artwork reaches circle edges; adjust if you want more bleed
OVERSCALE = 1.08

for folder, size in sizes:
    out_dir = ROOT / folder
    out_dir.mkdir(parents=True, exist_ok=True)
    out_round = out_dir / 'ic_launcher_round.png'
    # backup existing
    if out_round.exists():
        bak = out_round.with_name(out_round.name + f'.bak.{ts}')
        if not bak.exists():
            out_round.rename(bak)
    # first, create an overscaled fit to ensure subject fills the circle
    bigger = int(size * OVERSCALE)
    if bigger <= 0:
        bigger = size
    fitted = ImageOps.fit(img_src, (bigger, bigger), method=Image.LANCZOS, centering=(0.5,0.5))
    # center-crop to target size
    left = (bigger - size)//2
    fitted = fitted.crop((left, left, left + size, left + size))
    # create circular mask and apply
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    fitted.putalpha(mask)
    # save
    fitted.save(out_round, format='PNG', optimize=True)
    # verify via PNG IHDR
    data = out_round.read_bytes()
    ihdr = 8 + 4 + 4
    w, h = struct.unpack('!II', data[ihdr:ihdr+8])
    color_type = data[ihdr+9]
    has_alpha = color_type in (4,6)
    results.append((folder, str(out_round), len(data), w, h, color_type, has_alpha))

print('folder,file,bytes,width,height,color_type,has_alpha')
for r in results:
    print(','.join(map(str,r)))

