"""
Compile the final slide_equations.tex and export each page as a transparent PNG for the
Canva deck (report/final/slides/).  Run from the project root:

    python report/final/render_slides.py
"""
import subprocess
from pathlib import Path
import fitz
from PIL import Image

HERE = Path(__file__).resolve().parent
OUT = HERE / "slides"
NAMES = ["panel_section", "panel_inertia", "panel_loading", "panel_beam", "panel_poisson",
         "panel_wall", "panel_second_order",
         "diag_schematic", "diag_neutral_axis", "diag_free_body", "diag_arc", "diag_inplane", "diag_tphi", "table_results"]

for _ in range(2):
    subprocess.run(["pdflatex", "-interaction=nonstopmode", "-halt-on-error", "slide_equations.tex"],
                   cwd=HERE, check=True, stdout=subprocess.DEVNULL)
for ext in ("aux", "log"):
    (HERE / f"slide_equations.{ext}").unlink(missing_ok=True)

OUT.mkdir(exist_ok=True)
doc = fitz.open(HERE / "slide_equations.pdf")
assert len(doc) == len(NAMES), f"{len(doc)} pages, {len(NAMES)} names"
for page, name in zip(doc, NAMES):
    path = OUT / f"{name}.png"
    page.get_pixmap(dpi=220, alpha=True).save(path)
    im = Image.open(path)
    l, t, r, b = im.getchannel("A").getbbox()
    pad = 12
    im.crop((max(l - pad, 0), max(t - pad, 0), min(r + pad, im.width), min(b + pad, im.height))).save(path)
    print(f"{name:22s} {Image.open(path).size}")
