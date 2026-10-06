"""
Report figure: finite-element runs against the beam model of actuator_model.py.

    python fea_compare.py results/bare [results/sleeve]      -> fea_comparison.png

The first run sets the geometry and material; a second run (the same actuator
with a hoop-fibre sleeve) is overlaid if given.
"""
from __future__ import annotations
import json
import sys
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

from actuator_model import Geometry, Material
from fea_viewer import beam_curves

# colour follows the model, not the order it is drawn in
FE_BARE, BEAM_LIN, BEAM_HYP, FE_SLEEVE = "#2a78d6", "#eb6834", "#1baf7a", "#eda100"
INK, MUTED, GRID = "#0b0b0b", "#52514e", "#e1e0d9"


def load(run_dir):
    run_dir = Path(run_dir)
    info = json.loads((run_dir / "run.json").read_text())
    tab = np.genfromtxt(run_dir / "summary.csv", delimiter=",", names=True, dtype=None, encoding="utf-8")
    return info, tab


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3):
        sys.exit(__doc__)
    info, bare = load(sys.argv[1])
    sleeve = load(sys.argv[2])[1] if len(sys.argv) == 3 else None
    geo, m = info["geometry"], info["material"]
    g, mat = Geometry(geo["R"], geo["r"], geo["rp"], geo["L"]), Material(m["C10"], m["C20"], m["C30"])
    p_hi = max(bare["P_kPa"].max(), sleeve["P_kPa"].max() if sleeve is not None else 0.0)
    beam = beam_curves(g, mat, np.linspace(0, p_hi / 1e3, 121))

    plt.rcParams.update({"font.family": "sans-serif", "font.size": 10, "axes.edgecolor": "#c3c2b7",
                         "axes.labelcolor": MUTED, "xtick.color": MUTED, "ytick.color": MUTED,
                         "axes.spines.top": False, "axes.spines.right": False})
    fig, axes = plt.subplots(1, 3, figsize=(15, 4.6))
    panels = [("theta_deg", "theta", "tip angle θ [deg]"), ("delta_mm", "delta", "tip deflection δ [mm]")]
    for ax, (col, key, label) in zip(axes, panels):
        ax.plot(beam["P"], beam["t1_" + key], color=BEAM_LIN, lw=2, label="beam model, Tier 1-2 (notes)")
        ax.plot(beam["t3_P"], beam["t3_" + key], color=BEAM_HYP, lw=2, label="beam model, Tier 3")
        if sleeve is not None:
            ax.plot(sleeve["P_kPa"], sleeve[col], color=FE_SLEEVE, lw=2, marker="o", ms=4, label="FE, hoop-fibre sleeve")
        ax.plot(bare["P_kPa"], bare[col], color=FE_BARE, lw=2, marker="o", ms=4, label="FE, bare actuator")
        ax.set_ylabel(label)
        ax.set_ylim(0, 1.08 * max(bare[col].max(), max(beam["t1_" + key])))
    v0 = bare["V_mm3"][0]
    if sleeve is not None:
        axes[2].plot(sleeve["P_kPa"], sleeve["V_mm3"] / v0, color=FE_SLEEVE, lw=2, marker="o", ms=4, label="FE, hoop-fibre sleeve")
    axes[2].plot(bare["P_kPa"], bare["V_mm3"] / v0, color=FE_BARE, lw=2, marker="o", ms=4, label="FE, bare actuator")
    axes[2].set_ylabel("cavity volume V / V₀")
    for ax in axes:
        ax.set_xlabel("pressure P [kPa]")
        ax.set_xlim(0, 1.03 * p_hi)
        ax.grid(color=GRID, lw=0.8)
        ax.set_axisbelow(True)
        if info.get("P_limit_kPa"):
            ax.axvline(info["P_limit_kPa"], color="#d03b3b", lw=1)
            ax.annotate("ballooning limit ", (info["P_limit_kPa"], 0.03), xycoords=("data", "axes fraction"),
                        ha="right", fontsize=8, color=MUTED)
        ax.legend(fontsize=8, frameon=False, loc="upper left")
    law = "neo-Hookean" if mat.C20 == 0 and mat.C30 == 0 else "Yeoh"
    fig.suptitle(f"Eccentric-cavity actuator   R={g.R:g}  r={g.r:g}  r_p={g.rp:g}  L={g.L:g} mm   {law}, E={mat.E:.3g} MPa",
                 color=INK)
    fig.tight_layout()
    out = Path(__file__).with_name("fea_comparison.png")
    fig.savefig(out, dpi=150)
    print(f"saved {out.name}")
