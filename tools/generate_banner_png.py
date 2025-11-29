#!/usr/bin/env python3
"""
Generate a 320x180 PNG banner (no external dependencies) with supersampling for higher quality.
Writes:
 - app/src/main/res/drawable/banner_tv_320x180.png
 - app/src/main/res/drawable/banner_tv_320x180_drawable.png
"""
import struct
import zlib
import math

W = 320
H = 180
OUT = "app/src/main/res/drawable/banner_tv_320x180.png"
OUT2 = "app/src/main/res/drawable/banner_tv_320x180_drawable.png"

SCALE = 4  # supersample factor
SW = W * SCALE
SH = H * SCALE

def lerp(a,b,t):
    return int(a + (b-a)*t + 0.5)

def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(v)))

def rgb_tuple_from_hex(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i+2],16) for i in (0,2,4))

# Background gradient stops
top = rgb_tuple_from_hex('#01203F')
mid = rgb_tuple_from_hex('#054A2E')
bot = rgb_tuple_from_hex('#0A7A50')

# coin gradient colors
c1 = rgb_tuple_from_hex('#FFD166')
c2 = rgb_tuple_from_hex('#F2A900')

# coin params scaled
coin_cx = int((16 + 48) * SCALE)
coin_cy = int((22 + 48) * SCALE)
coin_r = int(48 * SCALE)
coin_r2 = coin_r * coin_r

# title rectangle scaled
rect_x0 = int(124 * SCALE)
rect_x1 = int(300 * SCALE)
rect_y0 = int(112 * SCALE)
rect_y1 = int(140 * SCALE)

# create high-res buffer (RGBA)
rows_hr = [bytearray(SW * 4) for _ in range(SH)]

for y in range(SH):
    t = y / (SH - 1)
    # use a slight mid blend for nicer gradient
    # interpolate top->mid for first half, mid->bot for second half
    if t < 0.5:
        tt = t / 0.5
        bg_r = lerp(top[0], mid[0], tt)
        bg_g = lerp(top[1], mid[1], tt)
        bg_b = lerp(top[2], mid[2], tt)
    else:
        tt = (t - 0.5) / 0.5
        bg_r = lerp(mid[0], bot[0], tt)
        bg_g = lerp(mid[1], bot[1], tt)
        bg_b = lerp(mid[2], bot[2], tt)

    row = rows_hr[y]
    for x in range(SW):
        r = bg_r
        g = bg_g
        b = bg_b
        a = 255
        dx = x - coin_cx
        dy = y - coin_cy
        d2 = dx*dx + dy*dy
        if d2 <= coin_r2:
            rt = math.sqrt(d2) / coin_r
            cr = lerp(c1[0], c2[0], rt)
            cg = lerp(c1[1], c2[1], rt)
            cb = lerp(c1[2], c2[2], rt)
            r = cr
            g = cg
            b = cb
            # soft inner highlight towards top-left
            if dx < coin_r * 0.15 and dy < coin_r * 0.15:
                r = clamp(r + 48)
                g = clamp(g + 48)
                b = clamp(b + 48)
        # subtle title-area dark band
        if rect_x0 <= x <= rect_x1 and rect_y0 <= y <= rect_y1:
            r = int(r * 0.88)
            g = int(g * 0.9)
            b = int(b * 0.88)
        idx = x * 4
        row[idx:idx+4] = bytes((clamp(r), clamp(g), clamp(b), a))

# downscale with simple box filter to target size
rows = []
for y in range(H):
    row = bytearray()
    for x in range(W):
        r_sum = g_sum = b_sum = 0
        for yy in range(y*SCALE, (y+1)*SCALE):
            rr = rows_hr[yy]
            for xx in range(x*SCALE, (x+1)*SCALE):
                idx = xx*4
                r_sum += rr[idx]
                g_sum += rr[idx+1]
                b_sum += rr[idx+2]
        area = SCALE*SCALE
        r = clamp(r_sum / area)
        g = clamp(g_sum / area)
        b = clamp(b_sum / area)
        row.extend(bytes((r,g,b,255)))
    rows.append(b"\x00" + bytes(row))

raw = b''.join(rows)
comp = zlib.compress(raw, level=9)

# PNG writer
import sys

def png_chunk(type_bytes, data_bytes):
    chunk = struct.pack('!I', len(data_bytes)) + type_bytes + data_bytes
    crc = zlib.crc32(type_bytes)
    crc = zlib.crc32(data_bytes, crc) & 0xffffffff
    chunk += struct.pack('!I', crc)
    return chunk

for outpath in (OUT, OUT2):
    with open(outpath, 'wb') as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        ihdr = struct.pack('!IIBBBBB', W, H, 8, 6, 0, 0, 0)
        f.write(png_chunk(b'IHDR', ihdr))
        try:
            f.write(png_chunk(b'sRGB', b'\x00'))
        except Exception:
            pass
        f.write(png_chunk(b'IDAT', comp))
        f.write(png_chunk(b'IEND', b''))

print('Wrote', OUT, 'and', OUT2)
