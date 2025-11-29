#!/usr/bin/env python3
"""
Generate Play Store / TV assets:
 - 320x180 banner -> app/src/main/res/drawable-xhdpi/banner_tv_320x180_drawable.png
 - 512x512 store icon -> app/src/main/playstore/icon_512.png
 - copy store icon to mipmap-xxxhdpi for in-app use: app/src/main/res/mipmap-xxxhdpi/ic_launcher_play_512.png

This script uses a supersampling raster generator (no external deps).
"""
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BANNER_PATH = ROOT / 'app' / 'src' / 'main' / 'res' / 'drawable-xhdpi' / 'banner_tv_320x180_drawable.png'
BANNER_PATH.parent.mkdir(parents=True, exist_ok=True)
ICON_PLAYSTORE = ROOT / 'app' / 'src' / 'main' / 'playstore' / 'icon_512.png'
ICON_PLAYSTORE.parent.mkdir(parents=True, exist_ok=True)
MIPMAP_XXX = ROOT / 'app' / 'src' / 'main' / 'res' / 'mipmap-xxxhdpi'
MIPMAP_XXX.mkdir(parents=True, exist_ok=True)
MIPMAP_ICON = MIPMAP_XXX / 'ic_launcher_play_512.png'

# Reuse the generator implementation but parameterize
import struct, zlib, math

def clamp(v):
    return max(0, min(255, int(v)))

def rgb(h):
    h=h.lstrip('#')
    return tuple(int(h[i:i+2],16) for i in (0,2,4))

# banner generator parameters
Wb, Hb = 320, 180
# icon params
Wi, Hi = 512, 512

# gradient colors
top = rgb('#01203F')
mid = rgb('#054A2E')
bot = rgb('#0A7A50')

c1 = rgb('#FFD166')
c2 = rgb('#F2A900')

# drawing function using supersample

def generate_rgba_image(w, h, draw_fn, scale=4):
    sw = w*scale
    sh = h*scale
    rows_hr = [bytearray(sw*4) for _ in range(sh)]
    for y in range(sh):
        ty = y/(sh-1)
        if ty < 0.5:
            tt = ty/0.5
            bg_r = int(top[0] + (mid[0]-top[0])*tt)
            bg_g = int(top[1] + (mid[1]-top[1])*tt)
            bg_b = int(top[2] + (mid[2]-top[2])*tt)
        else:
            tt = (ty-0.5)/0.5
            bg_r = int(mid[0] + (bot[0]-mid[0])*tt)
            bg_g = int(mid[1] + (bot[1]-mid[1])*tt)
            bg_b = int(mid[2] + (bot[2]-mid[2])*tt)
        row = rows_hr[y]
        for x in range(sw):
            r=g=b=0
            a=255
            # default background
            r=bg_r; g=bg_g; b=bg_b
            # call draw_fn at high-res coords to overlay coin/title effects
            rr,gg,bb = draw_fn(x,y,sw,sh)
            if rr is not None:
                r,g,b = rr,gg,bb
            idx = x*4
            row[idx:idx+4] = bytes((clamp(r),clamp(g),clamp(b),a))
    # downscale box filter
    rows = []
    for y in range(h):
        row = bytearray()
        for x in range(w):
            rsum=gsum=bsum=0
            for yy in range(y*scale,(y+1)*scale):
                hr = rows_hr[yy]
                for xx in range(x*scale,(x+1)*scale):
                    idx=xx*4
                    rsum += hr[idx]
                    gsum += hr[idx+1]
                    bsum += hr[idx+2]
            area = scale*scale
            row.extend(bytes((clamp(rsum/area), clamp(gsum/area), clamp(bsum/area), 255)))
        rows.append(b'\x00' + bytes(row))
    raw=b''.join(rows)
    comp=zlib.compress(raw,9)
    return comp

# specific draw functions

def banner_draw(x,y,sw,sh):
    # coin centered at 64,70 in original scaled coords scaled accordingly
    coin_cx = int((16+48)*(sw/(Wb*4))) if False else int((16+48)*(sw/(Wb*4)))
    # easier: map to relative positions
    relx = x/sw
    rely = y/sh
    # compute coin in scaled coordinates: coin center at (64/320, 70/180)
    cx = int( (64/ Wb) * sw )
    cy = int( (70/ Hb) * sh )
    cr = int( (48/ Wb) * sw )
    dx = x - cx
    dy = y - cy
    d2 = dx*dx+dy*dy
    if d2 <= cr*cr:
        rt = math.sqrt(d2)/cr
        rr = int(c1[0] + (c2[0]-c1[0])*rt)
        gg = int(c1[1] + (c2[1]-c1[1])*rt)
        bb = int(c1[2] + (c2[2]-c1[2])*rt)
        return rr,gg,bb
    return None,None,None

def icon_draw(x,y,sw,sh):
    # draw a round coin emblem centered left and a title-like mark
    cx = sw//2
    cy = sh//2
    cr = int(min(sw,sh)*0.36)
    dx = x-cx
    dy = y-cy
    d2 = dx*dx+dy*dy
    if d2 <= cr*cr:
        rt = math.sqrt(d2)/cr
        rr = int(c1[0] + (c2[0]-c1[0])*rt)
        gg = int(c1[1] + (c2[1]-c1[1])*rt)
        bb = int(c1[2] + (c2[2]-c1[2])*rt)
        return rr,gg,bb
    return None,None,None

# PNG writer helper

def write_png(path, comp, w, h):
    path = Path(path)
    with path.open('wb') as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        ihdr = struct.pack('!IIBBBBB', w, h, 8, 6, 0, 0, 0)
        def chunk(t,d):
            ch = struct.pack('!I', len(d)) + t + d
            crc = zlib.crc32(t)
            crc = zlib.crc32(d, crc) & 0xffffffff
            ch += struct.pack('!I', crc)
            return ch
        f.write(chunk(b'IHDR', ihdr))
        f.write(chunk(b'IDAT', comp))
        f.write(chunk(b'IEND', b''))

print('Generating banner and icons...')
comp_banner = generate_rgba_image(Wb,Hb,banner_draw,scale=4)
write_png(BANNER_PATH, comp_banner, Wb, Hb)
print('Wrote', BANNER_PATH)

comp_icon = generate_rgba_image(Wi,Hi,icon_draw,scale=4)
write_png(ICON_PLAYSTORE, comp_icon, Wi, Hi)
print('Wrote', ICON_PLAYSTORE)

# copy playstore icon to mipmap-xxxhdpi
import shutil
shutil.copy2(ICON_PLAYSTORE, MIPMAP_ICON)
print('Copied to', MIPMAP_ICON)

print('Done')

