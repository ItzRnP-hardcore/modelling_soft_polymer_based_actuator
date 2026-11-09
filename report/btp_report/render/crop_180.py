"""Crop the 180-degree renders (render/raw/, written by Render180.java) into figures/comsol_*.png.

Run from report/btp_report:  python render/crop_180.py
"""
from PIL import Image, ImageChops


def plain(k, m=12):
    im = Image.open(f'render/raw/{k}.png').convert('RGB')
    bg = Image.new('RGB', im.size, (255, 255, 255))
    l, t, r, b = ImageChops.difference(im, bg).point(lambda v: 255 if v > 8 else 0).getbbox()
    im.crop((max(l - m, 0), max(t - m, 0), min(r + m, im.width), min(b + m, im.height))).save(f'figures/comsol_{k}.png')


for k in ['fc_sleeved_side_P20', 'fc_sleeved_mises_side_P20', 'fc_sleeved_over_iso_P20', 'fc_sleeved_over_zoom_P20']:
    plain(k)
