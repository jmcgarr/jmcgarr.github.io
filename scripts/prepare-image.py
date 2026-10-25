#!/usr/bin/env python3
# /// script
# requires-python = ">=3.9"
# dependencies = ["pillow>=10"]
# ///
"""Prepare an image added by hand under src/jbake/assets/img/ so it passes the site's image rules.

Usage:
  uv run scripts/prepare-image.py <image>...          (uv fetches Pillow itself)
  python3 scripts/prepare-image.py <image>...         (needs: pip install pillow)
  scripts/prepare-image.py --dry-run <image>...       say what would happen, change nothing
  scripts/prepare-image.py --png <image>...           keep a PNG as PNG (a graphic, a screenshot, a logo)
  scripts/prepare-image.py --jpeg <image>...          turn a PNG into a JPEG even if it looks like a graphic
  scripts/prepare-image.py --width N <image>...       cap the width at N px (default: 1920 for a masthead,
                                                      1000 for an inline image, the T016/T017 limits)
  scripts/prepare-image.py --keep-source <image>...   don't remove a new PNG after writing its JPEG

What it does to each image (the method of revival tasks T016 and T017, AGENTS.md Rule 1 "Images"):
- Converts the pixels to sRGB from the embedded colour profile, then drops the profile and every bit of
  camera metadata (EXIF, XMP, GPS). Nothing but pixels is published.
- Resizes to at most the width cap, keeping the aspect ratio.
- A JPEG is re-encoded in place under the same name: progressive, quality stepping down from 82 until the
  file is 400 KB or less (then the width, down to 1200 px, if quality 60 is still too big).
- A PNG that is a photo (more than 50,000 distinct colours and no transparency, or --jpeg) becomes
  <name>.jpg next to it, and every `masthead=`, Markdown image and `src=` that named the PNG in
  src/jbake/content/ and src/jbake/templates/ is pointed at the JPEG. If the PNG was already published
  (it is in `main`), it stays at its URL as a small fallback, 1024 px and 256 colours, because published
  URLs are permanent. A PNG that was never published is removed (--keep-source keeps it).
- A PNG that is a graphic (or --png) stays a PNG: resized losslessly, optimized, transparency kept.
- A masthead (anything under src/jbake/assets/img/masthead/) also gets its 960 and 1440 px variants,
  through scripts/masthead-variants.py; stale variants of the same image are replaced.

Then run `./gradlew reviewChanges` to confirm, and look at the result in `./gradlew bakePreview`.
"""
import argparse
import importlib.util
import io
import os
import re
import subprocess
import sys

ASSETS = "src/jbake/assets"
IMG_DIR = os.path.join(ASSETS, "img")
MASTHEAD_DIR = os.path.join(IMG_DIR, "masthead")
MAX_BYTES = 400 * 1024          # ReviewChanges.MAX_IMAGE_BYTES (E5)
MASTHEAD_WIDTH = 1920           # ReviewChanges.MAX_IMAGE_WIDTH (E5), T016
INLINE_WIDTH = 1000             # T017: twice the widest in-post display width
MIN_WIDTH = 1200                # don't shrink a masthead below this to meet the byte target; warn instead
PHOTO_COLOURS = 50_000          # a PNG with more distinct colours than this is a photo
FALLBACK_WIDTH = 1024           # option B: the small PNG left at a published URL
QUALITIES = range(82, 59, -4)   # 82, 78, 74, 70, 66, 62
TEXT_ROOTS = ("src/jbake/content", "src/jbake/templates")


def log(msg):
    print(msg, flush=True)


def is_masthead(path):
    return os.path.normpath(path).startswith(os.path.normpath(MASTHEAD_DIR) + os.sep)


def published(path):
    """True if the file exists on main or origin/main: its URL is a promise (AGENTS.md Rule 1)."""
    for ref in ("main", "origin/main"):
        r = subprocess.run(["git", "cat-file", "-e", f"{ref}:{path}"], capture_output=True)
        if r.returncode == 0:
            return True
    return False


def to_srgb(im):
    """Return an RGB (or RGBA) copy of im in sRGB, with the embedded profile applied and then discarded."""
    from PIL import Image, ImageCms
    icc = im.info.get("icc_profile")
    has_alpha = im.mode in ("RGBA", "LA") or (im.mode == "P" and "transparency" in im.info)
    mode = "RGBA" if has_alpha else "RGB"
    im = im.convert(mode)
    if icc:
        try:
            srgb = ImageCms.createProfile("sRGB")
            im = ImageCms.profileToProfile(im, ImageCms.ImageCmsProfile(io.BytesIO(icc)), srgb, outputMode=mode)
        except ImageCms.PyCMSError as e:
            log(f"  warning: couldn't apply the colour profile ({e}); treating the pixels as sRGB")
    out = im.copy()
    out.info = {}  # drop what Pillow would otherwise write back: EXIF, XMP, the ICC profile, DPI
    return out


def fit(im, width):
    from PIL import Image
    if im.width <= width:
        return im
    return im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)


def save_jpeg(im, target, width):
    """Write im as a progressive sRGB JPEG of at most MAX_BYTES, lowering quality then width. Returns a note."""
    from PIL import ImageCms
    srgb = ImageCms.ImageCmsProfile(ImageCms.createProfile("sRGB")).tobytes()
    if im.mode != "RGB":
        im = im.convert("RGB")
    w = min(width, im.width)
    while True:
        sized = fit(im, w)
        for q in QUALITIES:
            buf = io.BytesIO()
            sized.save(buf, "JPEG", quality=q, optimize=True, progressive=True, icc_profile=srgb)
            if buf.tell() <= MAX_BYTES:
                open(target, "wb").write(buf.getvalue())
                return f"{sized.width}x{sized.height} q{q} {buf.tell() // 1024}K"
        if w - 200 < MIN_WIDTH:
            open(target, "wb").write(buf.getvalue())
            return f"{sized.width}x{sized.height} q{q} {buf.tell() // 1024}K  WARNING: still over {MAX_BYTES // 1024} KB"
        w -= 200


def save_png(im, target, width, colours=None):
    """Write im as an optimized PNG, resized losslessly; with `colours`, quantized to that palette size."""
    from PIL import Image
    im = fit(im, width)
    if colours:
        im = im.quantize(colours, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG)
    im.save(target, "PNG", optimize=True)
    return f"{im.width}x{im.height} {os.path.getsize(target) // 1024}K"


def looks_like_photo(im):
    if im.mode == "RGBA" and im.getchannel("A").getextrema()[0] < 255:
        return False
    return im.convert("RGB").getcolors(PHOTO_COLOURS) is None


def references(old_name, new_name, dry_run):
    """Point every masthead=, Markdown image and src= at the renamed file. Returns the files changed."""
    old_base, new_base = os.path.basename(old_name), os.path.basename(new_name)
    site_old = "/" + os.path.relpath(old_name, ASSETS)
    site_new = "/" + os.path.relpath(new_name, ASSETS)
    changed = []
    for root in TEXT_ROOTS:
        for dirpath, _, files in os.walk(root):
            for f in files:
                p = os.path.join(dirpath, f)
                try:
                    text = open(p, encoding="utf-8").read()
                except (UnicodeDecodeError, OSError):
                    continue
                new = text.replace(site_old, site_new)
                if is_masthead(old_name):
                    new = re.sub(rf"^(masthead=){re.escape(old_base)}$", rf"\g<1>{new_base}", new, flags=re.M)
                    new = new.replace(f'<#assign masthead = "{old_base}">', f'<#assign masthead = "{new_base}">')
                if new != text:
                    changed.append(p)
                    if not dry_run:
                        open(p, "w", encoding="utf-8", newline="").write(new)
    return changed


def masthead_variants(source, dry_run):
    spec = importlib.util.spec_from_file_location("masthead_variants", os.path.join("scripts", "masthead-variants.py"))
    mv = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mv)
    for w in mv.WIDTHS:
        target = mv.variant_path(source, w)
        if dry_run:
            log(f"  would write {target}")
            continue
        if os.path.exists(target):
            os.remove(target)
        log(f"  wrote {target}  {mv.make_variant(source, target, w)}")


def prepare(path, args):
    from PIL import Image
    path = os.path.normpath(path)
    if not os.path.normpath(path).startswith(os.path.normpath(IMG_DIR) + os.sep):
        log(f"{path}: skipped, not under {IMG_DIR}")
        return False
    stem, ext = os.path.splitext(path)
    ext = ext.lower()
    if ext not in (".jpg", ".jpeg", ".png"):
        log(f"{path}: skipped, only JPEG and PNG are handled")
        return False
    masthead = is_masthead(path)
    width = args.width or (MASTHEAD_WIDTH if masthead else INLINE_WIDTH)
    src = Image.open(path)
    log(f"{path}: {src.width}x{src.height} {src.mode} {os.path.getsize(path) // 1024}K"
        f"{', colour profile' if src.info.get('icc_profile') else ''}"
        f"{', EXIF' if src.info.get('exif') else ''}{', XMP' if 'xmp' in src.info or 'XML:com.adobe.xmp' in src.info else ''}"
        f"{', masthead' if masthead else ''}; width cap {width}")
    im = to_srgb(src)
    final = path

    if ext in (".jpg", ".jpeg"):
        if args.dry_run:
            log(f"  would re-encode in place as sRGB progressive JPEG, no metadata")
        else:
            log(f"  re-encoded in place: {save_jpeg(im, path, width)}")
    elif args.png or (not args.jpeg and not looks_like_photo(im)):
        log("  a graphic (few colours or transparency): stays PNG")
        if args.dry_run:
            log("  would resize losslessly and optimize in place")
        else:
            log(f"  optimized in place: {save_png(im, path, width)}")
    else:
        final = stem + ".jpg"
        was_published = published(path)
        log(f"  a photo: writing {final}" + (" (the PNG is published, so it stays as a small fallback)" if was_published
                                               else " (the PNG was never published, so it goes)"))
        if not args.dry_run:
            log(f"  wrote {final}: {save_jpeg(im, final, width)}")
            if was_published:
                log(f"  fallback {path}: {save_png(im, path, FALLBACK_WIDTH, colours=256)}")
            elif args.keep_source:
                log(f"  kept {path} (--keep-source); it will be published as-is unless you remove it")
            else:
                os.remove(path)
                log(f"  removed {path}")
        for f in references(path, final, args.dry_run):
            log(f"  {'would point' if args.dry_run else 'pointed'} {f} at {os.path.basename(final)}")

    if masthead:
        masthead_variants(final, args.dry_run)
    return True


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0], formatter_class=argparse.RawDescriptionHelpFormatter,
                                 epilog=__doc__.split("\n", 1)[1])
    ap.add_argument("images", nargs="+", help="paths under src/jbake/assets/img/")
    ap.add_argument("--dry-run", action="store_true", help="report, change nothing")
    kind = ap.add_mutually_exclusive_group()
    kind.add_argument("--png", action="store_true", help="keep a PNG as PNG")
    kind.add_argument("--jpeg", action="store_true", help="turn a PNG into a JPEG")
    ap.add_argument("--width", type=int, help=f"width cap in px (default {MASTHEAD_WIDTH} masthead, {INLINE_WIDTH} inline)")
    ap.add_argument("--keep-source", action="store_true", help="keep a never-published PNG after writing its JPEG")
    args = ap.parse_args()
    try:
        import PIL  # noqa: F401
    except ImportError:
        sys.exit("Pillow is missing: run with `uv run scripts/prepare-image.py ...` or `pip install pillow`")
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    ok = [prepare(p, args) for p in args.images]
    if not all(ok):
        sys.exit(1)
    log("Next: ./gradlew reviewChanges, then check the page in ./gradlew bakePreview")


if __name__ == "__main__":
    main()
