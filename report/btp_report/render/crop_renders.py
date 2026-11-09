"""Crop the raw COMSOL renders (render/raw/, written by RenderReport.java) into figures/comsol_*.png.

Whole images are trimmed of white margins. Cross-sections also lose their title (given in the caption)
and the blank band between the section and its colour legend.
Run from report/btp_report:  python render/crop_renders.py
"""
from PIL import Image, ImageChops
import numpy as np

PLAIN = ['sc_sleeved_mesh', 'fc_sleeved_mesh', 'fc_sleeved_section_geom_x20', 'fc_sleeved_section_geom_x100',
         'fc_fillet_section_geom_x20', 'sc_sleeved_side_P40', 'sc_sleeved_side_P90', 'sc_bare_side_P40',
         'sc_bare_side_P90', 'sc_sleeved_mises_last', 'fc_sleeved_side_P10', 'fc_sleeved_side_P15',
         'fc_sleeved_mises_P15']
SECTIONS = ['fc_bare_section_P10_x40', 'fc_sleeved_section_P10_x40', 'fc_fillet_section_P10_x40',
            'sc_bare_section_P90', 'sc_sleeved_section_P90']


def plain(k, m=12):
    im = Image.open(f'render/raw/{k}.png').convert('RGB')
    bg = Image.new('RGB', im.size, (255, 255, 255))
    l, t, r, b = ImageChops.difference(im, bg).point(lambda v: 255 if v > 8 else 0).getbbox()
    im.crop((max(l - m, 0), max(t - m, 0), min(r + m, im.width), min(b + m, im.height))).save(f'figures/comsol_{k}.png')


def squeeze(k, top=70, gap=40):
    a = np.asarray(Image.open(f'render/raw/{k}.png').convert('RGB'))[top:]
    ink = (a < 240).any(axis=2)
    cols, rows = ink.any(axis=0), ink.any(axis=1)
    keep, run = [], 0
    for j, c in enumerate(cols):
        run = 0 if c else run + 1
        if run <= gap:
            keep.append(j)
    r0, r1 = np.argmax(rows), len(rows) - np.argmax(rows[::-1])
    out = a[max(r0 - 10, 0):r1 + 10][:, keep]
    c = (out < 240).any(axis=2).any(axis=0)
    c0, c1 = np.argmax(c), len(c) - np.argmax(c[::-1])
    Image.fromarray(out[:, max(c0 - 10, 0):c1 + 10]).save(f'figures/comsol_{k}.png')


for k in PLAIN:
    plain(k)
for k in SECTIONS:
    squeeze(k)
