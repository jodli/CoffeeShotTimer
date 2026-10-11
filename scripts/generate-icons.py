"""Generates the app icon, splash icon and website icons from one geometry (108-unit adaptive icon grid).

Mark: a sour shot and a dashed trail into the target box, where the dialed-in shot sits.
Same language as the website hero chart.

Usage (from the repo root): python3 scripts/generate-icons.py
Needs rsvg-convert (brew install librsvg) and cwebp (brew install webp) for the raster fallbacks.
"""
import math
import os
import struct
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = tempfile.mkdtemp(prefix="icons-")
RES = os.path.join(REPO, "app/src/main/res")

LIGHT = dict(paper="#F6F6F1", ink="#17202B", grid="#D5D6D0", ideal="#3E8E5A", tint="#D5E3D6", dialed="#2C6E44")
DARK = dict(paper="#10161D", ink="#E8E8E1", grid="#2C3542", ideal="#5DB37B", tint="#1C2F2C", dialed="#5DB37B")

BOX = (57, 34, 81, 58)  # x0, y0, x1, y1
SOUR = (34, 67)  # first shot, ring
DIALED = (69, 46)  # dialed-in shot, solid, inside the box
R_SOUR, R_DIALED = 7, 8
SW = 3
RING_SW = 3.5
DASH, GAP = 4.5, 3.2


def dashes(a, b):
    (x0, y0), (x1, y1) = a, b
    length = math.hypot(x1 - x0, y1 - y0)
    ux, uy = (x1 - x0) / length, (y1 - y0) / length
    # start/stop at the dot edges so every visible dash is a full dash
    t, end, segs = R_SOUR + 3, length - R_DIALED - 2.5, []
    while t < end:
        t2 = min(t + DASH, end)
        segs.append(f"M{x0 + ux * t:.2f},{y0 + uy * t:.2f}L{x0 + ux * t2:.2f},{y0 + uy * t2:.2f}")
        t = t2 + GAP
    return "".join(segs)


def circle(cx, cy, r):
    return f"M{cx - r:.2f},{cy}a{r},{r} 0 1,0 {2 * r},0a{r},{r} 0 1,0 {-2 * r},0z"


def box_path():
    x0, y0, x1, y1 = BOX
    return f"M{x0},{y0}H{x1}V{y1}H{x0}z"


TRAIL = dashes(SOUR, DIALED)
GRID = "".join(f"M{v},0V108M0,{v}H108" for v in range(6, 108, 12))


# ---------- SVG (source for rasters and the website) ----------

def svg(c, *, crop=0, grid=True, shape=None):
    """crop: units trimmed from each side of the 108 grid. shape: None | 'round' | 'squircle'."""
    size = 108 - 2 * crop
    clip = ""
    if shape == "round":
        clip = f'<clipPath id="m"><circle cx="54" cy="54" r="{size / 2}"/></clipPath>'
    elif shape == "squircle":
        rr = size * 0.22
        clip = f'<clipPath id="m"><rect x="{crop}" y="{crop}" width="{size}" height="{size}" rx="{rr}"/></clipPath>'
    g = f'<path d="{GRID}" stroke="{c["grid"]}" stroke-width="0.6"/>' if grid else ""
    (x1, y1), (x3, y3) = SOUR, DIALED
    dots = f'<circle cx="{x1}" cy="{y1}" r="{R_SOUR}" fill="{c["paper"]}" stroke="{c["ink"]}" stroke-width="{RING_SW}"/>'
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{crop} {crop} {size} {size}">{clip}'
        f'<g{" clip-path=" + chr(34) + "url(#m)" + chr(34) if shape else ""}>'
        f'<rect x="0" y="0" width="108" height="108" fill="{c["paper"]}"/>{g}'
        f'<path d="{box_path()}" fill="{c["tint"]}" stroke="{c["ideal"]}" stroke-width="{SW}"/>'
        f'<path d="{TRAIL}" fill="none" stroke="{c["ink"]}" stroke-width="{SW}"/>'
        f'{dots}<circle cx="{x3}" cy="{y3}" r="{R_DIALED}" fill="{c["dialed"]}"/>'
        "</g></svg>"
    )


def svg_favicon():
    """Site favicon: follows the visitor's colour scheme via CSS classes."""
    x0, y0, x1, y1 = BOX
    (sx, sy), (dx, dy) = SOUR, DIALED
    rule = lambda c: (
        f".bg{{fill:{c['paper']}}}.box{{fill:{c['tint']};stroke:{c['ideal']}}}"
        f".ink{{stroke:{c['ink']}}}.ring{{fill:{c['paper']};stroke:{c['ink']}}}.ok{{fill:{c['dialed']}}}"
    )
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72">'
        f"<style>{rule(LIGHT)}@media (prefers-color-scheme:dark){{{rule(DARK)}}}</style>"
        '<rect class="bg" x="18" y="18" width="72" height="72" rx="15.84"/>'
        f'<path class="box" d="{box_path()}" stroke-width="{SW}"/>'
        f'<path class="ink" d="{TRAIL}" fill="none" stroke-width="{SW}"/>'
        f'<circle class="ring" cx="{sx}" cy="{sy}" r="{R_SOUR}" stroke-width="{RING_SW}"/>'
        f'<circle class="ok" cx="{dx}" cy="{dy}" r="{R_DIALED}"/>'
        "</svg>\n"
    )


# ---------- Android vector drawables ----------

HEAD = (
    '<?xml version="1.0" encoding="utf-8"?>\n'
    "<!-- Generated from the icon geometry; edit scripts/generate-icons.py instead. -->\n"
)
VEC = (
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
    '    android:width="108dp"\n    android:height="108dp"\n'
    '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
)


def p(d, fill=None, stroke=None, name=None, extra="", sw=SW):
    attrs = [f'android:pathData="{d}"']
    if name:
        attrs.insert(0, f'android:name="{name}"')
    if fill:
        attrs.append(f'android:fillColor="{fill}"')
    if stroke:
        attrs.append(f'android:strokeColor="{stroke}"')
        attrs.append(f'android:strokeWidth="{sw}"')
    if extra:
        attrs.append(extra)
    return "    <path\n        " + "\n        ".join(attrs) + " />\n"


def dot_paths(c, i):
    if i == 1:
        return p(circle(*DIALED, R_DIALED), fill=c["dialed"])
    return p(circle(*SOUR, R_SOUR), fill=c["paper"], stroke=c["ink"], sw=RING_SW)


def foreground(c):
    out = HEAD + VEC
    out += p(box_path(), fill=c["tint"], stroke=c["ideal"])
    out += p(TRAIL, stroke=c["ink"])
    for i in range(2):
        out += dot_paths(c, i)
    return out + "</vector>\n"


def background(c):
    return HEAD + VEC + p("M0,0h108v108h-108z", fill=c["paper"]) + p(GRID, stroke=c["grid"], sw=0.6) + "</vector>\n"


def monochrome():
    # Themed icons tint this as a single colour, so meaning has to come from shape:
    # an open ring for the sour shot, a filled dot inside the box for the dialed-in one.
    k = "#FF000000"
    out = HEAD + VEC
    out += p(box_path(), stroke=k)
    out += p(TRAIL, stroke=k)
    out += p(circle(*SOUR, R_SOUR), stroke=k, sw=RING_SW)
    out += p(circle(*DIALED, R_DIALED), fill=k)
    return out + "</vector>\n"


def splash_animated(c):
    """Splash icon for API 31+: the sour shot lands, the trail draws, the dialed-in shot lands in the box."""
    out = HEAD.replace("-->", "Animated splash, API 31+. -->") + "<animated-vector\n"
    out += '    xmlns:android="http://schemas.android.com/apk/res/android"\n'
    out += '    xmlns:aapt="http://schemas.android.com/aapt">\n'
    out += '    <aapt:attr name="android:drawable">\n'
    inner = VEC.replace(' xmlns:android="http://schemas.android.com/apk/res/android"\n', "\n")
    inner = inner.replace("\n    ", "\n            ").replace("<vector", "        <vector")
    out += inner
    out += indent(p(box_path(), fill=c["tint"], stroke=c["ideal"]), 8)
    out += indent(p(TRAIL, stroke=c["ink"], name="trail", extra='android:trimPathEnd="0"'), 8)
    for i, (x, y) in enumerate((SOUR, DIALED)):
        out += (
            f'            <group android:name="dot{i}" android:pivotX="{x}" android:pivotY="{y}"\n'
            '                android:scaleX="0" android:scaleY="0">\n'
        )
        out += indent(dot_paths(c, i), 12)
        out += "            </group>\n"
    out += "        </vector>\n    </aapt:attr>\n"
    # timeline (ms): sour shot 0-200, trail 200-640, dialed-in shot 640-880
    steps = [("dot0", 0, 200), ("trail", 200, 440), ("dot1", 640, 240)]
    for name, start, dur in steps:
        out += f'    <target android:name="{name}">\n        <aapt:attr name="android:animation">\n'
        if name.startswith("dot"):
            out += "            <set>\n"
            for prop in ("scaleX", "scaleY"):
                out += (
                    f'                <objectAnimator android:propertyName="{prop}"\n'
                    f'                    android:startOffset="{start}" android:duration="{dur}"\n'
                    '                    android:valueFrom="0" android:valueTo="1" android:valueType="floatType"\n'
                    '                    android:interpolator="@android:interpolator/overshoot" />\n'
                )
            out += "            </set>\n"
        else:
            out += (
                '            <objectAnimator android:propertyName="trimPathEnd"\n'
                f'                android:startOffset="{start}" android:duration="{dur}"\n'
                '                android:valueFrom="0" android:valueTo="1" android:valueType="floatType"\n'
                '                android:interpolator="@android:interpolator/decelerate_cubic" />\n'
            )
        out += "        </aapt:attr>\n    </target>\n"
    return out + "</animated-vector>\n"


def indent(s, n):
    return "".join((" " * (n - 4)) + line if line.strip() else line for line in s.splitlines(True))


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(text)


write(f"{RES}/drawable/ic_launcher_foreground.xml", foreground(LIGHT))
write(f"{RES}/drawable/ic_launcher_background.xml", background(LIGHT))
write(f"{RES}/drawable/ic_launcher_monochrome.xml", monochrome())
write(f"{RES}/drawable/splash_icon.xml", foreground(LIGHT))
write(f"{RES}/drawable-night/splash_icon.xml", foreground(DARK))
write(f"{RES}/drawable-v31/splash_icon_animated.xml", splash_animated(LIGHT))
write(f"{RES}/drawable-night-v31/splash_icon_animated.xml", splash_animated(DARK))

# raster fallbacks: legacy launchers (API 24/25), Play Store listing, website


def render(svg_text, width, dest, webp=False):
    src = os.path.join(OUT, "src.svg")
    write(src, svg_text)
    png = os.path.join(OUT, "out.png") if webp else dest
    subprocess.run(["rsvg-convert", "-w", str(width), src, "-o", png], check=True)
    if webp:
        subprocess.run(["cwebp", "-quiet", "-lossless", png, "-o", dest], check=True)


for density, width in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
    render(svg(LIGHT, crop=18, shape="squircle"), width, f"{RES}/mipmap-{density}/ic_launcher.webp", webp=True)
    render(svg(LIGHT, crop=18, shape="round"), width, f"{RES}/mipmap-{density}/ic_launcher_round.webp", webp=True)
render(svg(LIGHT, crop=18), 512, f"{REPO}/app/src/main/ic_launcher-playstore.png")
render(svg(LIGHT, crop=18, shape="squircle"), 512, f"{REPO}/docs/icon.png")
write(f"{REPO}/docs/icon.svg", svg_favicon())

# favicon.ico: PNG-compressed entries, no grid (it turns to noise below 64 px)
sizes = (16, 32, 48)
pngs = []
for w in sizes:
    path = os.path.join(OUT, f"fav{w}.png")
    render(svg(LIGHT, crop=18, grid=False, shape="squircle"), w, path)
    pngs.append(open(path, "rb").read())
header = struct.pack("<HHH", 0, 1, len(sizes))
offset, entries = 6 + 16 * len(sizes), b""
for w, data in zip(sizes, pngs):
    entries += struct.pack("<BBBBHHII", w, w, 0, 0, 1, 32, len(data), offset)
    offset += len(data)
with open(f"{REPO}/docs/favicon.ico", "wb") as f:
    f.write(header + entries + b"".join(pngs))
print("icons written")
