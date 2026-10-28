#!/usr/bin/env python3
"""pixeldiff.py: compare before/after screenshots pixel by pixel.

Usage: .venv/bin/python pixeldiff.py <before_dir> <after_dir> [<diff_out_dir>]
For every PNG in before_dir with the same name in after_dir, prints the size, the number of differing
pixels and their bounding box. With diff_out_dir, writes a red-on-grey diff image for each pair that differs.
Exit status 1 if any pair differs. Needs Pillow.
"""
import os
import sys

from PIL import Image, ImageChops


def main():
    before, after = sys.argv[1], sys.argv[2]
    out = sys.argv[3] if len(sys.argv) > 3 else None
    differs = 0
    for name in sorted(os.listdir(before)):
        if not name.endswith(".png"):
            continue
        a = Image.open(os.path.join(before, name)).convert("RGB")
        p = os.path.join(after, name)
        if not os.path.exists(p):
            print(f"{name}: missing in {after}")
            differs += 1
            continue
        b = Image.open(p).convert("RGB")
        if a.size != b.size:
            print(f"{name}: size {a.size} vs {b.size}")
            differs += 1
            continue
        d = ImageChops.difference(a, b)
        box = d.getbbox()
        if box is None:
            print(f"{name}: {a.size[0]}x{a.size[1]}, identical (0 pixels differ)")
            continue
        mask = d.convert("L").point(lambda v: 255 if v else 0)
        n = sum(1 for v in mask.getdata() if v)
        print(f"{name}: {n} pixels differ, bounding box {box}")
        differs += 1
        if out:
            os.makedirs(out, exist_ok=True)
            grey = a.convert("L").convert("RGB")
            red = Image.new("RGB", a.size, (255, 0, 0))
            Image.composite(red, grey, mask).save(os.path.join(out, name))
    return 1 if differs else 0


if __name__ == "__main__":
    sys.exit(main())
