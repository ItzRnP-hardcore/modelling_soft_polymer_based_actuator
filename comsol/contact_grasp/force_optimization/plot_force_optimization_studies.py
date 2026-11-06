#!/usr/bin/env python3
"""
plot_force_optimization_studies.py

Generates publication-quality figures for:
1. Normal Force & Friction vs. Pressure up to 100 kPa across Geometric Scales (1.0x, 1.5x, 2.0x).
2. Contact Force Scaling Laws (Empirical vs. Theoretical Area Scaling S^2).
3. Material Von Mises Stress vs. 1.2 MPa Rupture Limit across Pressures.
4. Full-Wrap (> 270 deg) Circumferential Coverage & Clamping Force.
5. Floating Object Kinematic Trajectories under High-Pressure Enclosure.
"""

import os
import sys
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

BASE_DIR = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\force_optimization"
SCALE_CSV = os.path.join(BASE_DIR, "scale_high_pressure_sweep.csv")
WRAP_CSV = os.path.join(BASE_DIR, "full_wrap_grasp_summary.csv")
IMG_DIR = os.path.join(BASE_DIR, "images")
os.makedirs(IMG_DIR, exist_ok=True)

# Publication styling
plt.rcParams.update({
    "font.family": "serif",
    "font.size": 11,
    "axes.labelsize": 12,
    "axes.titlesize": 13,
    "xtick.labelsize": 10,
    "ytick.labelsize": 10,
    "legend.fontsize": 10,
    "figure.titlesize": 14,
    "figure.dpi": 300,
    "savefig.dpi": 300,
    "savefig.bbox": "tight",
    "axes.grid": True,
    "grid.alpha": 0.35,
    "grid.linestyle": "--"
})

def plot_scale_studies():
    if not os.path.exists(SCALE_CSV):
        print(f"[WARN] Scale CSV not found: {SCALE_CSV}")
        return

    df = pd.read_csv(SCALE_CSV)
    if df.empty:
        return

    print(f"Loaded {len(df)} scale records.")
    scales = sorted(df["Scale"].unique())
    scale_colors = {1.0: "#1f77b4", 1.5: "#ff7f0e", 2.0: "#2ca02c"}
    scale_markers = {1.0: "o", 1.5: "s", 2.0: "^"}

    # 1. Normal Force & Friction vs Pressure up to 100 kPa (Dual Panel)
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))
    df_r22 = df[df["R_mm"] == 22.0]

    for s in scales:
        sub = df_r22[df_r22["Scale"] == s].sort_values("P_kPa")
        if not sub.empty:
            label = f"Scale {s:.1f}x ($L={s*140:.0f}$ mm)"
            ax1.plot(sub["P_kPa"], sub["Fn_N"], marker=scale_markers.get(s, "o"),
                     color=scale_colors.get(s, "k"), lw=2.4, ms=7, label=label)
            ax2.plot(sub["P_kPa"], sub["Ffric_N"], marker=scale_markers.get(s, "o"),
                     color=scale_colors.get(s, "k"), lw=2.4, ms=7, label=label)

    ax1.set_xlabel("Internal Pressure $P$ (kPa)")
    ax1.set_ylabel("Normal Contact Force $F_N$ (N)")
    ax1.set_title("Normal Clamping Force vs Pressure (to 100 kPa)", fontweight="bold")
    ax1.legend(frameon=True)
    ax1.set_xlim(-2, 105)
    ax1.set_ylim(bottom=-0.02)

    ax2.set_xlabel("Internal Pressure $P$ (kPa)")
    ax2.set_ylabel("Frictional Holding Capacity $F_f = \\mu F_N$ (N) [$\\mu=0.50$]")
    ax2.set_title("Tangential Friction Holding Capacity", fontweight="bold")
    ax2.legend(frameon=True)
    ax2.set_xlim(-2, 105)
    ax2.set_ylim(bottom=-0.01)

    fig.suptitle("Scaling Analysis: Contact Force & Friction Scaling up to 100 kPa ($R_{\\text{cyl}} = 22$ mm)",
                 fontweight="bold", y=0.98)
    plt.tight_layout()
    p1 = os.path.join(IMG_DIR, "scale_force_vs_pressure_100kpa.png")
    plt.savefig(p1)
    plt.close()
    print(f"Saved: {p1}")

    # 2. Force Scaling Law: Peak Fn vs Scale Factor S
    fig, ax = plt.subplots(figsize=(8, 6))
    peak_forces = []
    for s in scales:
        sub = df_r22[df_r22["Scale"] == s]
        if not sub.empty:
            peak_fn = sub["Fn_N"].max()
            peak_forces.append((s, peak_fn))

    if peak_forces:
        s_vals = [p[0] for p in peak_forces]
        fn_vals = [p[1] for p in peak_forces]
        ax.plot(s_vals, fn_vals, marker="o", color="#d62728", lw=2.5, ms=9, label="COMSOL Continuum FEA")

        # Theoretical S^2 scaling curve baseline
        s_ref = s_vals[0]
        fn_ref = fn_vals[0]
        s_theory = np.linspace(min(s_vals)*0.9, max(s_vals)*1.1, 50)
        fn_theory = fn_ref * (s_theory / s_ref) ** 2
        ax.plot(s_theory, fn_theory, ls="--", color="gray", lw=1.8, label="Theoretical Area Scaling ($F \\propto S^2$)")

        for s, fn in peak_forces:
            ax.annotate(f"Scale {s:.1f}x\n$F_N = {fn:.3f}$ N", (s, fn),
                        textcoords="offset points", xytext=(0, 10), ha="center", fontweight="bold")

        ax.set_xlabel("Geometric Scale Factor $S$")
        ax.set_ylabel("Peak Normal Contact Force $F_{N,\\max}$ (N)")
        ax.set_title("Continuum Force Scaling Law with Geometric Scale Factor", fontweight="bold")
        ax.legend(frameon=True)
        ax.set_xlim(0.8, 2.2)
        plt.tight_layout()
        p2 = os.path.join(IMG_DIR, "scale_factor_force_scaling_law.png")
        plt.savefig(p2)
        plt.close()
        print(f"Saved: {p2}")

    # 3. Peak Stress vs Pressure & Safety Limit
    fig, ax = plt.subplots(figsize=(8.5, 6))
    for s in scales:
        sub = df_r22[df_r22["Scale"] == s].sort_values("P_kPa")
        if not sub.empty:
            ax.plot(sub["P_kPa"], sub["VmMax_kPa"], marker=scale_markers.get(s, "o"),
                    color=scale_colors.get(s, "k"), lw=2.2, ms=7,
                    label=f"Scale {s:.1f}x (Peak $\\sigma_{{\\text{{vm}}}}$)")

    ax.axhline(1200, color="red", ls="--", lw=2.0, label="Silicone Rupture Limit ($\\sigma_{\\text{uts}} = 1.2$ MPa)")
    ax.fill_between([-5, 110], 1200, 2000, color="red", alpha=0.10, label="Material Rupture Zone")

    ax.set_xlabel("Internal Pressure $P$ (kPa)")
    ax.set_ylabel("Peak von Mises Stress $\\sigma_{\\text{vm,max}}$ (kPa)")
    ax.set_title("Material Stress vs Pressure: Safety & Rupture Boundary Check", fontweight="bold")
    ax.set_xlim(-2, 105)
    ax.set_ylim(0, 1400)
    ax.legend(frameon=True, loc="upper left")
    plt.tight_layout()
    p3 = os.path.join(IMG_DIR, "scale_stress_vs_pressure_safety.png")
    plt.savefig(p3)
    plt.close()
    print(f"Saved: {p3}")


def plot_wrap_studies():
    if not os.path.exists(WRAP_CSV):
        print(f"[WARN] Wrap CSV not found: {WRAP_CSV}")
        return

    df = pd.read_csv(WRAP_CSV)
    if df.empty:
        return

    print(f"Loaded {len(df)} full-wrap records.")
    radii = sorted(df["R_mm"].unique())
    r_colors = {15.0: "#1f77b4", 20.0: "#ff7f0e"}
    r_markers = {15.0: "o", 20.0: "s"}

    # 4. Wrap Angle & Circumference Coverage vs Pressure
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

    for r in radii:
        sub = df[df["R_mm"] == r].sort_values("P_kPa")
        if not sub.empty:
            label = f"$R_{{\\text{{cyl}}}} = {r:.0f}$ mm"
            # Bending / wrap angle
            ax1.plot(sub["P_kPa"], sub["TipAngle_deg"], marker=r_markers.get(r, "o"),
                     color=r_colors.get(r, "k"), lw=2.4, ms=7, label=label)

            # Normal clamping force
            ax2.plot(sub["P_kPa"], sub["Fn_N"], marker=r_markers.get(r, "o"),
                     color=r_colors.get(r, "k"), lw=2.4, ms=7, label=label)

    ax1.axhline(270, color="red", ls="--", lw=2.0, label="Target $270^\\circ$ ($3/4$ Circumference)")
    ax1.axhline(360, color="purple", ls=":", lw=1.8, label="Full $360^\\circ$ Enclosure Loop")
    ax1.set_xlabel("Internal Pressure $P$ (kPa)")
    ax1.set_ylabel("Curvature Wrap Angle $\\theta_{\\text{wrap}}$ (deg)")
    ax1.set_title("Circumferential Wrap Angle vs Pressure", fontweight="bold")
    ax1.legend(frameon=True, loc="upper left")
    ax1.set_xlim(-2, 105)
    ax1.set_ylim(0, 400)

    ax2.set_xlabel("Internal Pressure $P$ (kPa)")
    ax2.set_ylabel("Normal Contact Force $F_N$ (N)")
    ax2.set_title("Full-Wrap Contact Clamping Force vs Pressure", fontweight="bold")
    ax2.legend(frameon=True, loc="upper left")
    ax2.set_xlim(-2, 105)
    ax2.set_ylim(bottom=-0.02)

    fig.suptitle("Full-Wrap (> 270 deg) Grasp Mechanics: Angle Enclosure & Clamping Force",
                 fontweight="bold", y=0.98)
    plt.tight_layout()
    p4 = os.path.join(IMG_DIR, "full_wrap_grasp_mechanics.png")
    plt.savefig(p4)
    plt.close()
    print(f"Saved: {p4}")

    # 5. Floating Object Displacement Trajectories (ux, uy)
    fig, ax = plt.subplots(figsize=(8, 6))
    for r in radii:
        sub = df[df["R_mm"] == r].sort_values("P_kPa")
        if not sub.empty:
            ax.plot(sub["ux_mm"], sub["uy_mm"], marker=r_markers.get(r, "o"),
                    color=r_colors.get(r, "k"), lw=2.2, ms=7, label=f"$R_{{\\text{{cyl}}}} = {r:.0f}$ mm")
            last_pt = sub.iloc[-1]
            ax.scatter([last_pt["ux_mm"]], [last_pt["uy_mm"]], color=r_colors.get(r, "k"), s=140, edgecolors="black", zorder=5)
            ax.annotate(f"P={last_pt['P_kPa']:.0f} kPa\n({last_pt['ux_mm']:.2f}, {last_pt['uy_mm']:.2f}) mm",
                        (last_pt["ux_mm"], last_pt["uy_mm"]),
                        textcoords="offset points", xytext=(8, -12), fontweight="bold", fontsize=8.5)

    ax.axhline(0, color="gray", lw=0.8, ls=":")
    ax.axvline(0, color="gray", lw=0.8, ls=":")
    ax.set_xlabel("Horizontal Displacement $\\Delta X_c$ (mm)")
    ax.set_ylabel("Vertical Displacement $\\Delta Y_c$ (mm)")
    ax.set_title("Floating Cylinder Kinematic Trajectory Under Full Wrap Grasp", fontweight="bold")
    ax.legend(frameon=True)
    plt.tight_layout()
    p5 = os.path.join(IMG_DIR, "full_wrap_floating_trajectories.png")
    plt.savefig(p5)
    plt.close()
    print(f"Saved: {p5}")

def main():
    print("Generating Force Optimization and Full-Wrap Figures...")
    plot_scale_studies()
    plot_wrap_studies()
    print("Done generating figures.")

if __name__ == "__main__":
    main()

