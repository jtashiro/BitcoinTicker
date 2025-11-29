#!/usr/bin/env bash
set -euo pipefail

SVG_PATH="app/src/main/res/drawable/banner_tv_320x180.svg"
OUT_PNG="app/src/main/res/drawable/banner_tv_320x180.png"
WIDTH=320
HEIGHT=180

echo "Converting $SVG_PATH -> $OUT_PNG ($WIDTH x $HEIGHT)"

# Try rsvg-convert
if command -v rsvg-convert >/dev/null 2>&1; then
  echo "Using rsvg-convert"
  rsvg-convert -w $WIDTH -h $HEIGHT -o "$OUT_PNG" "$SVG_PATH"
  exit 0
fi

# Try cairosvg (Python)
if command -v cairosvg >/dev/null 2>&1; then
  echo "Using cairosvg"
  cairosvg -w $WIDTH -h $HEIGHT -o "$OUT_PNG" "$SVG_PATH"
  exit 0
fi

# Try ImageMagick
if command -v convert >/dev/null 2>&1; then
  echo "Using ImageMagick convert"
  convert "$SVG_PATH" -background none -resize ${WIDTH}x${HEIGHT}! "$OUT_PNG"
  exit 0
fi

cat <<EOF
No SVG conversion tool found. Install one of:
  - librsvg (rsvg-convert)
  - cairosvg (pip install cairosvg)
  - ImageMagick (brew install imagemagick)
Then re-run: ./tools/convert_banner.sh
EOF
exit 2

