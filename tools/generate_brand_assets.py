"""Derive static app artwork from the approved logo. Requires Pillow.

Run: python3 tools/generate_brand_assets.py
The material master is rendered once, never at app startup or during navigation.
"""
from pathlib import Path
from io import BytesIO
import base64
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
BRAND = ROOT / "docs/branding"
SVG_NS = "http://www.w3.org/2000/svg"
ET.register_namespace("", SVG_NS)


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.suffix == ".webp":
        image.save(path, lossless=True, method=6)
    else:
        image.save(path, optimize=True)


def scaled(image, size):
    return image.resize((size, size), Image.Resampling.LANCZOS)


def main():
    master = Image.open(BRAND / "pebble-logo-master.png").convert("RGBA")
    # Fixed approved crop: all visible grain and transmitted light are retained.
    # Solid mark occupies x44..468, y23..444, leaving space for downward light.
    assert master.size == (704, 704), "Unexpected master; review crop before replacing it"
    logo = master.crop((96, 120, 608, 632))
    save(logo, BRAND / "pebble-logo.png")
    save(scaled(logo, 256), ROOT / "core/design/src/main/res/drawable-nodpi/pebble_logo.webp")

    foreground = Image.new("RGBA", (432, 432))
    # On the 108dp adaptive layer the solid mark is 60dp, within its safe zone.
    foreground.alpha_composite(scaled(logo, 288), (72, 72))
    save(foreground, ROOT / "app/src/main/res/drawable-nodpi/pebble_launcher_foreground.webp")

    for density, size in [("mdpi", 80), ("hdpi", 120), ("xhdpi", 160), ("xxhdpi", 240), ("xxxhdpi", 320)]:
        icon = Image.new("RGBA", (size, size), "#0D1A1C")
        logo_size = round(size * .9)
        offset = (size - logo_size) // 2
        icon.alpha_composite(scaled(logo, logo_size), (offset, offset))
        save(icon.convert("RGB"), ROOT / f"app/src/main/res/mipmap-{density}/ic_launcher.png")

    font_path = ROOT / "core/design/src/main/res/font/plus_jakarta_sans.ttf"
    for density, width in [("mdpi", 160), ("hdpi", 240), ("xhdpi", 320), ("xxhdpi", 480), ("xxxhdpi", 640)]:
        factor = width / 320
        banner = Image.new("RGBA", (width, round(width * 9 / 16)), "#0D1A1C")
        banner.alpha_composite(scaled(logo, round(112 * factor)), (round(16 * factor), round(40 * factor)))
        font = ImageFont.truetype(str(font_path), round(48 * factor))
        font.set_variation_by_axes([700])
        draw = ImageDraw.Draw(banner)
        draw.text((round(138 * factor), round(92 * factor)), "Pebble", font=font, anchor="lm", fill="#E7F3EC")
        save(banner.convert("RGB"), ROOT / f"app/src/main/res/drawable-{density}/pebble_tv_banner.png")

    png = BytesIO()
    logo.save(png, format="PNG", optimize=True)
    uri = "data:image/png;base64," + base64.b64encode(png.getvalue()).decode("ascii")
    for theme in ["light", "dark"]:
        path = ROOT / f"docs/media/hero-{theme}.svg"
        tree = ET.parse(path)
        root = tree.getroot()
        for child in list(root):
            if child.tag == f"{{{SVG_NS}}}circle" or child.get("id") == "pebble-logo":
                root.remove(child)
        for defs in root.findall(f"{{{SVG_NS}}}defs"):
            for child in list(defs):
                if child.tag == f"{{{SVG_NS}}}radialGradient":
                    defs.remove(child)
        ET.SubElement(root, f"{{{SVG_NS}}}image", {
            "id": "pebble-logo", "x": "1088", "y": "80", "width": "304", "height": "304",
            "href": uri, "preserveAspectRatio": "xMidYMid meet",
        })
        tree.write(path, encoding="unicode", xml_declaration=False)
    print("Generated native logo, legacy/adaptive icons, TV banners and hero SVG sources.")


if __name__ == "__main__":
    main()
