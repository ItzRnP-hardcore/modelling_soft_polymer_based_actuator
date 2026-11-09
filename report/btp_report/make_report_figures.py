"""
make_report_figures.py — figures for the B.Tech report, generated only from the project's data files.

Inputs (all read-only):
  comsol/bare/comsol_bare_summary.csv, comsol/sleeved/comsol_sleeve_summary.csv   COMSOL single-cavity models
  comsol/tapered_4cavity_sleeved/sleeved_study_summary.csv, ..._centreline.csv   physical 4-chamber model
Outputs: report/btp_report/figures/*.png
"""
import csv
from pathlib import Path

import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Wedge, Circle
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent / "figures"
OUT.mkdir(exist_ok=True)

# Validated categorical palette (dataviz reference, light mode), fixed order; markers give a
# second encoding so identity never rests on colour alone (print / colour-vision deficiency).
C1, C2, C3, C4 = "#2a78d6", "#eb6834", "#1baf7a", "#eda100"
GREY = "#6b6b66"
plt.rcParams.update({
    "font.family": "serif", "font.serif": ["Times New Roman", "Times", "DejaVu Serif"],
    "mathtext.fontset": "stix", "font.size": 10, "axes.titlesize": 10, "axes.labelsize": 10,
    "legend.fontsize": 8.5, "axes.spines.top": False, "axes.spines.right": False,
    "axes.grid": True, "grid.color": "#e4e3df", "grid.linewidth": 0.6, "lines.linewidth": 1.6,
    "savefig.dpi": 300, "savefig.bbox": "tight",
})


def rows(path, **flt):
    with open(ROOT / path, encoding="utf-8-sig") as f:
        r = list(csv.DictReader(f))
    return [x for x in r if all(x[k] == v for k, v in flt.items())]


def col(rr, k):
    return np.array([float(x[k]) for x in rr])


def beam_theta_uniform(P_kPa, E=0.6, R=10.0, r=4.0, rp=3.0, l=100.0):
    """Eq. (theta) of the report: theta = pi P R^2 r^2 rp l / (E Izz (R^2 - r^2))."""
    Izz = np.pi * (R**4 - r**4) / 4 - np.pi * R**2 * r**2 * rp**2 / (R**2 - r**2)
    return np.degrees(np.pi * (P_kPa / 1000) * R**2 * r**2 * rp * l / (E * Izz * (R**2 - r**2)))


def beam_theta_tapered(P_kPa, E, d0=2.5, dt=1.0, cav_deg=56, L=140, R0=9.5, Rt=3.8, tw0=2.0, twt=0.8, xs=4, xe=136):
    """Hoop-reinforced beam estimate integrated over the tapered 4-chamber section (one chamber pressurised)."""
    al = np.radians(cav_deg / 2)
    x = np.linspace(xs, xe, 400)
    xi = (x - xs) / (xe - xs)
    R = R0 - (R0 - Rt) * x / L
    b = (R0 - tw0) - ((R0 - tw0) - (Rt - twt)) * xi
    a = b - (d0 - (d0 - dt) * xi)
    Ac = al * (b**2 - a**2)
    e = 2 * np.sin(al) / (3 * al) * (b**3 - a**3) / (b**2 - a**2)
    I = np.pi * R**4 / 4 - 4 * al * (b**4 - a**4) / 4
    return np.degrees(np.asarray(P_kPa) / 1000 / E * np.trapezoid(Ac * e / I, x))


# ---------------------------------------------------------------- Fig: single-cavity actuator, COMSOL vs beam
def fig_single_cavity():
    bare, slv = rows("comsol/bare/comsol_bare_summary.csv"), rows("comsol/sleeved/comsol_sleeve_summary.csv")
    fig, ax = plt.subplots(1, 2, figsize=(6.6, 2.8))
    P = np.linspace(0, 110, 100)
    a = ax[0]
    a.plot(P, beam_theta_uniform(P), "--", color=GREY, lw=1.2, label="beam theory")
    a.plot(col(slv, "P_kPa"), col(slv, "theta_deg"), "-s", ms=4, color=C1, label="COMSOL, sleeved")
    a.plot(col(bare, "P_kPa"), col(bare, "theta_deg"), "-o", ms=4, color=C2, label="COMSOL, bare")
    a.set(xlabel="cavity pressure $P$ [kPa]", ylabel=r"tip angle $\theta$ [deg]", title="(a) Tip angle", xlim=(0, 112), ylim=(0, 46))
    a.legend(loc="upper left", frameon=False)
    a = ax[1]
    for r, c, m, lab in [(slv, C1, "s", "sleeved"), (bare, C2, "o", "bare")]:
        p = col(r, "P_kPa"); k = p > 0
        a.plot(p[k] / 600.0, col(r, "theta_deg")[k] / beam_theta_uniform(p[k]), "-" + m, ms=4, color=c, label=lab)
    a.axhline(1.0, color=GREY, lw=0.8, ls="--")
    a.set(xlabel="$P/E$", ylabel=r"$\theta_{\mathrm{FE}}/\theta_{\mathrm{beam}}$", title="(b) Ratio to beam theory", xlim=(0, 0.19), ylim=(0, 1.8))
    a.legend(loc="lower right", frameon=False)
    fig.tight_layout()
    fig.savefig(OUT / "fig_single_cavity.png")
    plt.close(fig)


# ---------------------------------------------------------------- Fig: 4-chamber cross-section
def fig_section():
    fig, ax = plt.subplots(1, 2, figsize=(5.6, 2.9))
    for a, (x, label) in zip(ax, [(4.0, "(a) $x$ = 4 mm (start of chambers)"), (136.0, "(b) $x$ = 136 mm (end of chambers)")]):
        xi = (x - 4) / 132
        R = 9.5 - 5.7 * x / 140
        b = 7.5 - 4.5 * xi
        aa = b - (2.5 - 1.5 * xi)
        a.add_patch(Circle((0, 0), R, fc="#dfe9f6", ec="#333333", lw=1.0))
        for k, c in enumerate([90, 0, -90, 180]):      # chamber centres: +Y (1), +Z (2), -Y (3), -Z (4); drawn with Z right, Y up
            fc = "#f6d2c3" if k == 0 else "white"
            a.add_patch(Wedge((0, 0), b, c - 28, c + 28, width=b - aa, fc=fc, ec="#333333", lw=0.9))
        d = np.radians(45)   # radius drawn along a rib, clear of the chambers
        a.annotate("", xy=(R * np.cos(d), R * np.sin(d)), xytext=(0, 0), arrowprops=dict(arrowstyle="->", lw=0.7))
        a.text(R * np.cos(d) + 0.3, R * np.sin(d) + 0.3, f"$R$ = {R:.2f} mm", fontsize=7.5)
        a.text(0, (aa + b) / 2, "1", ha="center", va="center", fontsize=7 if x > 50 else 8, fontweight="bold")
        a.set(xlim=(-10.5, 10.5), ylim=(-10.5, 10.5), aspect="equal", title=label)
        a.set_xlabel("$z$ [mm]")
        a.set_ylabel("$y$ [mm]")
        a.grid(False)
    fig.tight_layout()
    fig.savefig(OUT / "fig_4ch_section.png")
    plt.close(fig)


# ---------------------------------------------------------------- Fig: physical 4-chamber model
CASES = [("bare_base_E0.07", "bare", C2, "o"),
         ("sleeved_base_E0.07", "sleeved", C1, "s"),
         ("sleeved_fillet_E0.07", "sleeved, filleted chambers", C3, "D"),
         ("sleeved_deep_E0.07", "sleeved, deeper chambers", C4, "^")]
P_USABLE = 15.0     # kPa: last step before the local stress runaway (see report Section 5.4)


def fig_4ch_response():
    summ = rows("comsol/tapered_4cavity_sleeved/sleeved_study_summary.csv")
    fig, a = plt.subplots(figsize=(4.6, 3.2))
    P = np.linspace(0, 20, 50)
    a.plot(P, beam_theta_tapered(P, 0.07), "--", color=GREY, lw=1.2, label="beam estimate (sleeved)")
    for case, lab, c, m in CASES:
        r = [x for x in summ if x["case"] == case]
        p, th, vm = col(r, "P_kPa"), col(r, "theta_tip_deg"), col(r, "vm_max_kPa")
        ok = vm <= 1000.0          # peak von Mises <= 1 MPa
        a.plot(p, th, "-", color=c, lw=1.2)
        a.plot(p[ok], th[ok], m, ms=4.5, color=c, label=lab)
        if (~ok).any():
            a.plot(p[~ok], th[~ok], m, ms=5, mfc="white", mec=c, mew=1.2)
    a.plot([], [], "o", ms=5, mfc="white", mec=GREY, label="peak stress > 1 MPa")
    a.axvspan(P_USABLE, 22, color="#f1f0ec", zorder=0)
    a.text(18.5, 12, "beyond the\nusable range", ha="center", fontsize=8, color=GREY)
    a.set(xlabel="cavity pressure $P$ [kPa]", ylabel="tip rotation [deg]", xlim=(0, 22), ylim=(0, 200))
    a.legend(frameon=False, loc="upper left")
    fig.tight_layout()
    fig.savefig(OUT / "fig_4ch_response.png")
    plt.close(fig)


def fig_4ch_centreline():
    cl = rows("comsol/tapered_4cavity_sleeved/sleeved_study_centreline.csv", case="sleeved_base_E0.07")
    fig, a = plt.subplots(figsize=(4.6, 2.9))
    for Pv, c, m in [(0.0, GREY, None), (5.0, C3, "o"), (10.0, C2, "s"), (15.0, C1, "D")]:
        pts = [x for x in cl if abs(float(x["P_kPa"]) - Pv) < 1e-6]
        a.plot(col(pts, "x_mm"), col(pts, "y_mm"), "-" if Pv else ":", color=c,
               marker=m, ms=3, markevery=4, label=f"{Pv:g} kPa")
    a.set(xlabel="$x$ [mm]", ylabel="$y$ [mm]", aspect="equal", xlim=(-2, 145), ylim=(-95, 8))
    a.legend(frameon=False, loc="lower left", title="sleeved, $P$ =", title_fontsize=8.5)
    fig.tight_layout()
    fig.savefig(OUT / "fig_4ch_centreline.png")
    plt.close(fig)


def crop_render():
    for name in ("comsol_sleeved_P15kPa.png",):
        p = OUT / name
        im = Image.open(p).convert("RGB")
        arr = np.asarray(im)
        mask = (arr < 245).any(axis=2)
        ys, xs = np.where(mask)
        im.crop((max(xs.min() - 20, 0), max(ys.min() - 20, 0), xs.max() + 20, ys.max() + 20)).save(OUT / name.replace(".png", "_crop.png"))


if __name__ == "__main__":
    fig_single_cavity()
    fig_section()
    fig_4ch_response()
    fig_4ch_centreline()
    crop_render()
    print("figures ->", OUT)
