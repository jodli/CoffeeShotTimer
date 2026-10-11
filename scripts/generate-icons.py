"""Generates the app icon, splash icon and website icons from one geometry (108-unit adaptive icon grid).

Mark: the shot timer dial. A 45 s ring (the website chart's time span), the 25-30 s target
band at 7 to 8 o'clock, and the shot's progress arc stopping inside it at 27 s, marked by the
same shot dot the website chart plots. On a 60 s ring the band was too short to read at 48 px.

Usage (from the repo root): python3 scripts/generate-icons.py
Needs rsvg-convert (brew install librsvg) and cwebp (brew install webp) for the raster fallbacks.
"""
import math
import os
import struct
import subprocess
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "app/src/main/res")
OUT = tempfile.mkdtemp(prefix="icons-")

LIGHT = dict(paper="#F6F6F1", ink="#17202B", grid="#E2E3DC", track="#DCDDD6", ideal="#3E8E5A")
DARK = dict(paper="#10161D", ink="#E8E8E1", grid="#1B232D", track="#2C3542", ideal="#5DB37B")

C = 54  # centre
R = 25  # ring radius
RING_W = 7
BAND_W = 12
DIAL_S = 45  # one revolution
SHOT_S = 27  # where the shot stops
HEAD_R = 4.6  # the shot dot, styled like the website chart: paper fill, ink ring
HEAD_RING = 2
TICK_IN, TICK_OUT, TICK_W = 32, 34.5, 1.6


def deg(seconds):
    return seconds / DIAL_S * 360


def pt(angle, r):
    a = math.radians(angle)
    return C + r * math.sin(a), C - r * math.cos(a)


def arc(a0, a1, r=R):
    (x0, y0), (x1, y1) = pt(a0, r), pt(a1, r)
    return f"M{x0:.2f},{y0:.2f}A{r},{r} 0 {1 if a1 - a0 > 180 else 0},1 {x1:.2f},{y1:.2f}"


def circle(cx, cy, r):
    return f"M{cx - r:.2f},{cy:.2f}a{r},{r} 0 1,0 {2 * r:.2f},0a{r},{r} 0 1,0 {-2 * r:.2f},0z"


TRACK = circle(C, C, R)
BAND = arc(deg(25), deg(30))
PROGRESS = arc(0, deg(SHOT_S))
HEAD = pt(deg(SHOT_S), R)
TICKS = "".join(
    "M{:.2f},{:.2f}L{:.2f},{:.2f}".format(*pt(a, TICK_IN), *pt(a, TICK_OUT)) for a in range(0, 360, 40)  # every 5 s
)
GRID = "".join(f"M{v},0V108M0,{v}H108" for v in range(6, 108, 12))


# ---------- SVG (source for rasters and the website) ----------

def svg(c, *, crop=0, grid=True, shape=None):
    """crop: units trimmed from each side of the 108 grid. shape: None | 'round' | 'squircle'."""
    size = 108 - 2 * crop
    clip = ""
    if shape == "round":
        clip = f'<clipPath id="m"><circle cx="{C}" cy="{C}" r="{size / 2}"/></clipPath>'
    elif shape == "squircle":
        clip = f'<clipPath id="m"><rect x="{crop}" y="{crop}" width="{size}" height="{size}" rx="{size * 0.22}"/></clipPath>'
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{crop} {crop} {size} {size}">{clip}'
        f'<g{" clip-path=" + chr(34) + "url(#m)" + chr(34) if shape else ""}>'
        f'<rect width="108" height="108" fill="{c["paper"]}"/>'
        + (f'<path d="{GRID}" stroke="{c["grid"]}" stroke-width="0.7"/>' if grid else "")
        + mark_svg(c)
        + "</g></svg>"
    )


def mark_svg(c, classes=False):
    def attr(cls, **kw):
        if classes:
            return f'class="{cls}"' + "".join(f' {k.replace("_", "-")}="{v}"' for k, v in kw.items() if k == "stroke_width")
        return " ".join(f'{k.replace("_", "-")}="{v}"' for k, v in kw.items())

    hx, hy = HEAD
    return (
        f'<path d="{TRACK}" {attr("track", fill="none", stroke=c["track"], stroke_width=RING_W)}/>'
        f'<path d="{PROGRESS}" {attr("ink", fill="none", stroke=c["ink"], stroke_width=RING_W)}/>'
        f'<path d="{BAND}" {attr("band", fill="none", stroke=c["ideal"], stroke_width=BAND_W)}/>'
        f'<path d="{TICKS}" {attr("ink", fill="none", stroke=c["ink"], stroke_width=TICK_W)}/>'
        f'<circle cx="{hx:.2f}" cy="{hy:.2f}" r="{HEAD_R}" '
        f'{attr("head", fill=c["paper"], stroke=c["ink"], stroke_width=HEAD_RING)}/>'
    )


def svg_favicon():
    """Site favicon: follows the visitor's colour scheme via CSS classes."""
    def rule(c):
        return (
            f".bg{{fill:{c['paper']}}}.track{{fill:none;stroke:{c['track']}}}.band{{fill:none;stroke:{c['ideal']}}}"
            f".ink{{fill:none;stroke:{c['ink']}}}.head{{fill:{c['paper']};stroke:{c['ink']}}}"
        )

    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72">'
        f"<style>{rule(LIGHT)}@media (prefers-color-scheme:dark){{{rule(DARK)}}}</style>"
        '<rect class="bg" x="18" y="18" width="72" height="72" rx="15.84"/>'
        + mark_svg(LIGHT, classes=True)
        + "</svg>\n"
    )


# ---------- Android vector drawables ----------

HEAD_XML = (
    '<?xml version="1.0" encoding="utf-8"?>\n'
    "<!-- Generated; edit scripts/generate-icons.py instead. -->\n"
)
VEC = (
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
    '    android:width="108dp"\n    android:height="108dp"\n'
    '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
)


def p(d, fill=None, stroke=None, sw=None, name=None, extra=""):
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


def head_paths(c):
    hx, hy = HEAD
    return p(circle(hx, hy, HEAD_R), fill=c["paper"], stroke=c["ink"], sw=HEAD_RING)


def mark_paths(c, animated=False):
    # the band sits on top of the progress arc: the ink visibly runs into the green zone
    out = p(TRACK, stroke=c["track"], sw=RING_W)
    if animated:
        out += p(PROGRESS, stroke=c["ink"], sw=RING_W, name="progress", extra='android:trimPathEnd="0"')
    else:
        out += p(PROGRESS, stroke=c["ink"], sw=RING_W)
    out += p(BAND, stroke=c["ideal"], sw=BAND_W)
    out += p(TICKS, stroke=c["ink"], sw=TICK_W)
    return out


def foreground(c):
    return HEAD_XML + VEC + mark_paths(c) + head_paths(c) + "</vector>\n"


def background(c):
    return HEAD_XML + VEC + p("M0,0h108v108h-108z", fill=c["paper"]) + p(GRID, stroke=c["grid"], sw=0.7) + "</vector>\n"


def band_outline(half):
    a0, a1 = deg(25), deg(30)
    (ox0, oy0), (ox1, oy1) = pt(a0, R + half), pt(a1, R + half)
    (ix1, iy1), (ix0, iy0) = pt(a1, R - half), pt(a0, R - half)
    return (
        f"M{ox0:.2f},{oy0:.2f}A{R + half},{R + half} 0 0,1 {ox1:.2f},{oy1:.2f}"
        f"L{ix1:.2f},{iy1:.2f}A{R - half},{R - half} 0 0,0 {ix0:.2f},{iy0:.2f}z"
    )


def monochrome():
    # Themed icons tint everything one colour: the track goes thin so the progress arc,
    # the wide target band and the head still read as separate shapes.
    k = "#FF000000"
    hx, hy = HEAD
    return (
        HEAD_XML + VEC
        + p(TRACK, stroke=k, sw=1.6)
        + p(arc(0, deg(25)), stroke=k, sw=RING_W)
        + p(TICKS, stroke=k, sw=TICK_W)
        # one colour can't separate the head ring from a solid band, so the band is drawn as an
        # outline here and the head sits inside it as a solid dot
        + p(band_outline(BAND_W / 2 - 0.8), stroke=k, sw=1.6)
        + p(circle(hx, hy, HEAD_R + 1), fill=k)
        + "</vector>\n"
    )


def indent(s, n):
    return "".join(" " * n + line if line.strip() else line for line in s.splitlines(True))


def splash_animated(c):
    """Splash icon for API 31+: the shot runs round the dial and stops in the target band."""
    hx, hy = HEAD
    inner = VEC.replace(' xmlns:android="http://schemas.android.com/apk/res/android"\n', "\n")
    out = HEAD_XML + (
        "<animated-vector\n"
        '    xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt">\n'
        '    <aapt:attr name="android:drawable">\n'
    )
    out += indent(inner, 8)
    out += indent(mark_paths(c, animated=True), 8)
    out += f'            <group android:name="head" android:pivotX="{hx:.2f}" android:pivotY="{hy:.2f}"\n'
    out += '                android:scaleX="0" android:scaleY="0">\n'
    out += indent(head_paths(c), 12)
    out += "            </group>\n        </vector>\n    </aapt:attr>\n"
    # timeline (ms): arc sweeps 0-620 with a brake into the band, head lands 560-860
    out += (
        '    <target android:name="progress">\n        <aapt:attr name="android:animation">\n'
        '            <objectAnimator android:propertyName="trimPathEnd"\n'
        '                android:duration="620" android:valueFrom="0" android:valueTo="1"\n'
        '                android:valueType="floatType"\n'
        '                android:interpolator="@android:interpolator/decelerate_cubic" />\n'
        "        </aapt:attr>\n    </target>\n"
        '    <target android:name="head">\n        <aapt:attr name="android:animation">\n'
        "            <set>\n"
    )
    for prop in ("scaleX", "scaleY"):
        out += (
            f'                <objectAnimator android:propertyName="{prop}"\n'
            '                    android:startOffset="560" android:duration="300"\n'
            '                    android:valueFrom="0" android:valueTo="1" android:valueType="floatType"\n'
            '                    android:interpolator="@android:interpolator/overshoot" />\n'
        )
    out += "            </set>\n        </aapt:attr>\n    </target>\n</animated-vector>\n"
    return out


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
    with open(path, "rb") as f:
        pngs.append(f.read())
header = struct.pack("<HHH", 0, 1, len(sizes))
offset, entries = 6 + 16 * len(sizes), b""
for w, data in zip(sizes, pngs):
    entries += struct.pack("<BBBBHHII", w, w, 0, 0, 1, 32, len(data), offset)
    offset += len(data)
with open(f"{REPO}/docs/favicon.ico", "wb") as f:
    f.write(header + entries + b"".join(pngs))

# previews for review, not committed
for name, c in (("light", LIGHT), ("dark", DARK)):
    render(svg(c, crop=18, shape="round"), 216, os.path.join(OUT, f"preview_{name}.png"))
render(svg(LIGHT, crop=18, shape="round"), 48, os.path.join(OUT, "preview_48.png"))
print(OUT)
