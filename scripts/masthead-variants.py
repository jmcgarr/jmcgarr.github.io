#!/usr/bin/env python3
"""Create the smaller header ("masthead") image variants that masthead.ftl serves by screen width (T071).

Usage:
  scripts/masthead-variants.py           create any missing <stem>-960.jpg and <stem>-1440.jpg
  scripts/masthead-variants.py --check   exit 1 if any masthead in use is missing a variant

A masthead is any local image named by `masthead=` in content front matter or by
`<#assign masthead = "...">` in a template, resolved relative to src/jbake/assets/img/masthead/,
or to src/jbake/assets/ when it starts with "/" (T037). Remote (http) mastheads have no variants.
Requires Pillow. Variants are sRGB, progressive JPEG, with no camera metadata. Sources narrower
than a variant are copied as-is.
Existing variants are never overwritten (delete one to regenerate it).
"""
import io, os, re, shutil, sys

ASSETS = "src/jbake/assets"
MASTHEAD_DIR = os.path.join(ASSETS, "img", "masthead")
WIDTHS = (960, 1440)


def mastheads_in_use():
    names = set()
    for root in ("src/jbake/content", "src/jbake/templates"):
        for dirpath, _, files in os.walk(root):
            for f in files:
                text = open(os.path.join(dirpath, f), encoding="utf-8", errors="replace").read().replace("\r", "\n")
                names.update(m.strip() for m in re.findall(r"^masthead=(.+)$", text, re.M))
                names.update(re.findall(r'<#assign masthead = "([^"]+)">', text))
    return sorted(n for n in names if not n.startswith("http"))


def variant_path(source, width):
    stem, ext = os.path.splitext(source)
    return f"{stem}-{width}{ext}"


def make_variant(source, target, width):
    from PIL import Image, ImageCms
    im = Image.open(source)
    if im.width <= width:
        shutil.copyfile(source, target)
        return "copied (source is narrower)"
    icc = im.info.get("icc_profile")
    im = im.convert("RGB")
    srgb = ImageCms.createProfile("sRGB")
    if icc:
        im = ImageCms.profileToProfile(im, ImageCms.ImageCmsProfile(io.BytesIO(icc)), srgb, outputMode="RGB")
    im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    im.save(target, "JPEG", quality=80, optimize=True, progressive=True, icc_profile=ImageCms.ImageCmsProfile(srgb).tobytes())
    return f"{width}x{im.height}"


def main(check_only):
    missing = []
    for name in mastheads_in_use():
        source = os.path.normpath(os.path.join(ASSETS, name.lstrip("/")) if name.startswith("/") else os.path.join(MASTHEAD_DIR, name))
        if not os.path.isfile(source):
            missing.append(f"{source} (the masthead itself is missing)")
            continue
        for w in WIDTHS:
            target = variant_path(source, w)
            if os.path.isfile(target):
                continue
            if check_only:
                missing.append(target)
            else:
                how = make_variant(source, target, w)
                print(f"created {target}  {os.path.getsize(target) // 1024}K  {how}")
    if missing:
        print("FAIL: missing masthead variants (run scripts/masthead-variants.py):")
        print("\n".join(f"  {m}" for m in missing))
        sys.exit(1)
    if check_only:
        print(f"OK: all {len(mastheads_in_use())} mastheads in use have {'/'.join(map(str, WIDTHS))}px variants")


if __name__ == "__main__":
    main("--check" in sys.argv[1:])
