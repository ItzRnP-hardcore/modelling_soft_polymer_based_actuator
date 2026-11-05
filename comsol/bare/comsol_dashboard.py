import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
from matplotlib.gridspec import GridSpec
import numpy as np
from pathlib import Path

import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(ROOT))

bare_py = np.genfromtxt(ROOT / "results" / "bare" / "summary.csv", delimiter=",", names=True)
sleeve_py = np.genfromtxt(ROOT / "results" / "sleeve" / "summary.csv", delimiter=",", names=True)
comsol_bare = np.genfromtxt(HERE / "comsol_bare_summary.csv", delimiter=",", names=True)

from actuator_model import Geometry, Material
from fea_viewer import beam_curves

g = Geometry(10.0, 4.0, 3.0, 100.0)
mat = Material.from_E(0.6)
beam = beam_curves(g, mat, np.linspace(0, 0.110, 121))

fig = plt.figure(figsize=(15, 9.5))
gs = GridSpec(2, 2, height_ratios=[1.1, 1.0], hspace=0.32, wspace=0.22)

# Subplot 1: True COMSOL 3D Render
ax_img = fig.add_subplot(gs[0, :])
try:
    img = mpimg.imread(HERE / "comsol_bare_deformed_stress.png")
    ax_img.imshow(img)
    ax_img.axis("off")
    ax_img.set_title("COMSOL Multiphysics 6.4: 3-D Deformed von Mises Stress & Curvature at P = 90 kPa\n"
                     "(Tetrahedral mesh, Nearly Incompressible Neo-Hookean u-p, Follower Cavity Pressure)",
                     fontsize=12, fontweight="bold", pad=8)
except Exception as e:
    ax_img.text(0.5, 0.5, f"Image Load Error: {e}", ha="center", va="center")

# Subplot 2: Tip Deflection delta [mm]
ax_delta = fig.add_subplot(gs[1, 0])
ax_delta.plot(beam["P"], beam["t1_delta"], "--", color="#eb6834", lw=2.0, label="Euler-Bernoulli Beam (Tier 1)")
ax_delta.plot(sleeve_py["P_kPa"], sleeve_py["delta_mm"], "-.", color="#1baf7a", lw=2.2, label="Hoop-Sleeve Python FEA")
ax_delta.plot(bare_py["P_kPa"], bare_py["delta_mm"], "-", color="#2a78d6", lw=2.4, label="Bare Actuator: Python FEA (hex27 Q2/P1)")
ax_delta.plot(comsol_bare["P_kPa"], comsol_bare["delta_mm"], "o", color="#d03b3b", ms=6.5, mfc="none", mew=2.2, label="Bare Actuator: COMSOL 6.4 (tet10 mixed u-p)")

ax_delta.axvline(104.65, color="#888888", ls=":", lw=1.2)
ax_delta.text(103.5, 38, "Ballooning Limit\n(104.65 kPa)", ha="right", fontsize=8.5, color="#52514e")
ax_delta.set_xlabel("Cavity Pressure P [kPa]", fontsize=11)
ax_delta.set_ylabel("Tip Deflection δ [mm]", fontsize=11)
ax_delta.set_xlim(0, 112)
ax_delta.set_ylim(0, 68)
ax_delta.grid(True, linestyle="--", alpha=0.5)
ax_delta.legend(loc="upper left", frameon=False, fontsize=9.5)
ax_delta.set_title("Tip Deflection δ(P) Comparison", fontsize=11.5, fontweight="bold")

# Subplot 3: Tip Angle theta [deg]
ax_theta = fig.add_subplot(gs[1, 1])
ax_theta.plot(beam["P"], beam["t1_theta"], "--", color="#eb6834", lw=2.0, label="Euler-Bernoulli Beam (Tier 1)")
ax_theta.plot(sleeve_py["P_kPa"], sleeve_py["theta_deg"], "-.", color="#1baf7a", lw=2.2, label="Hoop-Sleeve Python FEA")
ax_theta.plot(bare_py["P_kPa"], bare_py["theta_deg"], "-", color="#2a78d6", lw=2.4, label="Bare Actuator: Python FEA (hex27 Q2/P1)")
ax_theta.plot(comsol_bare["P_kPa"], comsol_bare["theta_deg"], "o", color="#d03b3b", ms=6.5, mfc="none", mew=2.2, label="Bare Actuator: COMSOL 6.4 (tet10 mixed u-p)")

ax_theta.axvline(104.65, color="#888888", ls=":", lw=1.2)
ax_theta.text(103.5, 48, "Ballooning Limit\n(104.65 kPa)", ha="right", fontsize=8.5, color="#52514e")
ax_theta.set_xlabel("Cavity Pressure P [kPa]", fontsize=11)
ax_theta.set_ylabel("Tip Bending Angle θ [deg]", fontsize=11)
ax_theta.set_xlim(0, 112)
ax_theta.set_ylim(0, 85)
ax_theta.grid(True, linestyle="--", alpha=0.5)
ax_theta.legend(loc="upper left", frameon=False, fontsize=9.5)
ax_theta.set_title("Tip Bending Angle θ(P) Comparison", fontsize=11.5, fontweight="bold")

out_dash = HERE / "comsol_dashboard.png"
fig.savefig(out_dash, dpi=180, bbox_inches="tight")
print(f"Saved dashboard: {out_dash}")
