#!/usr/bin/env python3
"""
Verify app icon and banner assets for Play Store requirements.
Checks:
 - Banner PNGs exist in drawable-xhdpi and drawable, are 320x180, and fill the canvas (no large transparent padding).
 - Play icon exists at app/src/main/res/mipmap-xxxhdpi/ic_launcher_play_512.png and is 512x512 and fills the canvas.
 - Mipmap ic_launcher.png and ic_launcher_round.png exist for standard densities and round icons have alpha with transparent corners and opaque center.

Prints a CSV-like report and final PASS/FAIL summary.
"""
from pathlib import Path
import sys

try:
    from PIL import Image
except Exception:
    print('Pillow is required. Please install: python3 -m pip install --user pillow')
    sys.exit(2)

import struct

ROOT = Path(__file__).resolve().parents[1] / 'app' / 'src' / 'main' / 'res'

def png_iHDR(path:Path):
    data = path.read_bytes()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        return None
    ihdr = 8 + 4 + 4
    w,h = struct.unpack('!II', data[ihdr:ihdr+8])
    color_type = data[ihdr+9]
    return {'w':w,'h':h,'bytes':len(data),'color_type':color_type}

results = []
errors = []

# Banner checks
banner_paths = [ROOT/'drawable-xhdpi'/'banner_tv_320x180_drawable.png', ROOT/'drawable'/'banner_tv_320x180.png']
for p in banner_paths:
    if not p.exists():
        results.append(('banner', str(p), 'MISSING'))
        errors.append(f'banner missing: {p}')
        continue
    ih = png_iHDR(p)
    if not ih:
        results.append(('banner', str(p), 'INVALID_PNG'))
        errors.append(f'banner invalid PNG: {p}')
        continue
    im = Image.open(p).convert('RGBA')
    w,h = im.size
    bbox = im.getbbox()  # bounding box of non-zero pixels across all channels
    # For banner, also check alpha channel trimmed area
    a = im.getchannel('A') if 'A' in im.getbands() else None
    if a:
        non_trans = sum(1 for px in a.getdata() if px>0)
    else:
        # no alpha: count non-white pixels as content
        non_trans = sum(1 for px in im.getdata() if px[:3] != (255,255,255))
    frac = round(non_trans/(w*h),3)
    trimmed = (bbox[2]-bbox[0], bbox[3]-bbox[1]) if bbox else (0,0)
    ok_size = (w==320 and h==180)
    fills = (trimmed[0]==320 and trimmed[1]==180) or (frac>=0.90)
    status = 'OK' if ok_size and fills else 'WARN'
    results.append(('banner', str(p), status, w, h, trimmed[0], trimmed[1], frac))
    if not ok_size:
        errors.append(f'banner wrong size {w}x{h} (expected 320x180): {p}')
    if not fills:
        errors.append(f'banner does not fill canvas (trim={trimmed}, non-transparent fraction={frac}): {p}')

# Play icon
play = ROOT/'mipmap-xxxhdpi'/'ic_launcher_play_512.png'
if not play.exists():
    results.append(('play', str(play), 'MISSING'))
    errors.append(f'play icon missing: {play}')
else:
    ih = png_iHDR(play)
    if not ih:
        results.append(('play', str(play), 'INVALID_PNG'))
        errors.append(f'play icon invalid PNG: {play}')
    else:
        im = Image.open(play).convert('RGBA')
        w,h = im.size
        bbox = im.getbbox()
        a = im.getchannel('A')
        non_trans = sum(1 for px in a.getdata() if px>0)
        frac = round(non_trans/(w*h),3)
        trimmed = (bbox[2]-bbox[0], bbox[3]-bbox[1]) if bbox else (0,0)
        ok_size = (w==512 and h==512)
        fills = (trimmed[0]==512 and trimmed[1]==512) or (frac>=0.95)
        status = 'OK' if ok_size and fills else 'WARN'
        results.append(('play', str(play), status, w, h, trimmed[0], trimmed[1], frac))
        if not ok_size:
            errors.append(f'play icon wrong size {w}x{h} (expected 512x512): {play}')
        if not fills:
            errors.append(f'play icon does not fill canvas (trim={trimmed}, non-transparent fraction={frac}): {play}')

# Mipmap icons (square + round)
standard = [('mipmap-ldpi',36),('mipmap-mdpi',48),('mipmap-hdpi',72),('mipmap-xhdpi',96),('mipmap-xxhdpi',144),('mipmap-xxxhdpi',192)]
for folder, expected in standard:
    d = ROOT/folder
    sq = d/'ic_launcher.png'
    rd = d/'ic_launcher_round.png'
    if not sq.exists():
        results.append(('mipmap_square', folder, 'MISSING'))
        errors.append(f'mipmap square missing: {sq}')
    else:
        ih = png_iHDR(sq)
        results.append(('mipmap_square', folder, 'OK' if ih and ih['w']==expected and ih['h']==expected else 'WARN', ih['w'] if ih else 0, ih['h'] if ih else 0, expected))
        if not ih or ih['w']!=expected or ih['h']!=expected:
            errors.append(f'mipmap square wrong size for {folder}: got {ih if ih else "INVALID PNG"}, expected {expected}x{expected}')
    if not rd.exists():
        results.append(('mipmap_round', folder, 'MISSING'))
        errors.append(f'mipmap round missing: {rd}')
    else:
        ih = png_iHDR(rd)
        im = Image.open(rd).convert('RGBA')
        a = im.getchannel('A')
        corner = a.getpixel((0,0))
        center = a.getpixel((im.width//2, im.height//2))
        non_trans = sum(1 for v in a.getdata() if v>0)
        frac = round(non_trans/(im.width*im.height),3)
        ok_size = (ih and ih['w']==expected and ih['h']==expected)
        ok_alpha = (corner==0 and center>200 and frac>0.7)
        status = 'OK' if ok_size and ok_alpha else 'WARN'
        results.append(('mipmap_round', folder, status, ih['w'] if ih else 0, ih['h'] if ih else 0, corner, center, frac))
        if not ok_size:
            errors.append(f'mipmap round wrong size for {folder}: got {ih if ih else "INVALID PNG"}, expected {expected}x{expected}')
        if not ok_alpha:
            errors.append(f'mipmap round alpha/padding issue for {folder}: corner={corner}, center={center}, alpha_frac={frac}')

# Print report
print('\nCSV report (type, path/info...):')
for r in results:
    print(','.join(map(str,r)))

print('\nSummary:')
if not errors:
    print('PASS: All checked assets meet the basic size and fill heuristics.')
    sys.exit(0)
else:
    print('FAIL: The following issues were detected:')
    for e in errors:
        print(' -', e)
    sys.exit(3)

