"""
comsol_compare.py - Compare COMSOL 6.4 3-D FEA against custom Python FEA (actuator_fea.py)
and analytical beam theory (actuator_model.py).
"""
from __future__ import annotations
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(ROOT))

from actuator_model import Geometry, Material
from fea_viewer import beam_curves

bare_py = np.genfromtxt(ROOT / "results" / "bare" / "summary.csv", delimiter=",", names=True)
comsol_bare = np.genfromtxt(HERE / "comsol_bare_summary.csv", delimiter=",", names=True)

g = Geometry(10.0, 4.0, 3.0, 100.0)
mat = Material.from_E(0.6)
beam = beam_curves(g, mat, np.linspace(0, 0.105, 121))

print("=== BARE ACTUATOR: Python FEA (hex27 Q2/P1) vs COMSOL 6.4 (tet10 mixed u-p) ===")
print(f"{'P [kPa]':>8s} | {'delta_Py [mm]':>13s} | {'delta_COMSOL':>13s} | {'Diff [mm]':>10s} | {'theta_Py [deg]':>14s} | {'theta_COMSOL':>14s}")
print("-" * 86)
for row_c in comsol_bare:
    p = row_c["P_kPa"]
    idx = int(np.argmin(np.abs(bare_py["P_kPa"] - p)))
    dp, dc = bare_py["delta_mm"][idx], row_c["delta_mm"]
    tp, tc = bare_py["theta_deg"][idx], row_c["theta_deg"]
    print(f"{p:8.1f} | {dp:13.3f} | {dc:13.3f} | {dc - dp:+10.3f} | {tp:14.2f} | {tc:14.2f}")

plt.rcParams.update({
    "font.family": "sans-serif", "font.size": 10,
    "axes.edgecolor": "#c3c2b7", "axes.labelcolor": "#52514e",
    "xtick.color": "#52514e", "ytick.color": "#52514e",
    "axes.spines.top": False, "axes.spines.right": False
})

fig, axes = plt.subplots(1, 2, figsize=(11.5, 4.6))

# Panel 1: Tip angle theta [deg]
axes[0].plot(beam["P"], beam["t1_theta"], "--", color="#eb6834", lw=1.8, label="Beam model (Tier 1, uniaxial)")
axes[0].plot(bare_py["P_kPa"], bare_py["theta_deg"], "-", color="#2a78d6", lw=2.2, label="Custom Python FEA (hex27 Q2/P1)")
axes[0].plot(comsol_bare["P_kPa"], comsol_bare["theta_deg"], "o", color="#d03b3b", ms=6, mfc="none", mew=2.0, label="COMSOL 6.4 (mixed u-p)")
axes[0].set_xlabel("Pressure P [kPa]")
axes[0].set_ylabel("Tip angle θ [deg]")
axes[0].set_xlim(0, 108)
axes[0].set_ylim(0, 82)
axes[0].grid(color="#e1e0d9", lw=0.8)
axes[0].legend(frameon=False, loc="upper left")

# Panel 2: Tip deflection delta [mm]
axes[1].plot(beam["P"], beam["t1_delta"], "--", color="#eb6834", lw=1.8, label="Beam model (Tier 1, uniaxial)")
axes[1].plot(bare_py["P_kPa"], bare_py["delta_mm"], "-", color="#2a78d6", lw=2.2, label="Custom Python FEA (hex27 Q2/P1)")
axes[1].plot(comsol_bare["P_kPa"], comsol_bare["delta_mm"], "o", color="#d03b3b", ms=6, mfc="none", mew=2.0, label="COMSOL 6.4 (mixed u-p)")
axes[1].set_xlabel("Pressure P [kPa]")
axes[1].set_ylabel("Tip deflection δ [mm]")
axes[1].set_xlim(0, 108)
axes[1].set_ylim(0, 66)
axes[1].grid(color="#e1e0d9", lw=0.8)
axes[1].legend(frameon=False, loc="upper left")

for ax in axes:
    ax.axvline(104.65, color="#888888", ls=":", lw=1.2)
    ax.annotate("Ballooning limit\n(104.65 kPa)", (104.65, 0.72), xycoords=("data", "axes fraction"),
                ha="right", fontsize=8, color="#52514e")

fig.suptitle("Independent Cross-Verification: Custom 3-D Python FEA vs. COMSOL Multiphysics 6.4\n"
             "Bare Eccentric-Cavity Silicone Actuator (R=10, r=4, r_p=3, L=100 mm, Neo-Hookean E=0.6 MPa, ν=0.4995)",
             fontsize=10.5, color="#0b0b0b")
fig.tight_layout()
out_fig = HERE / "comsol_verification.png"
fig.savefig(out_fig, dpi=160)
print(f"\nSaved {out_fig}")

