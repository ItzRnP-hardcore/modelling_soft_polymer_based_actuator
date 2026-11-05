#!/usr/bin/env python3
"""
plot_floating_location_sweep.py

Generates publication-quality figures analyzing:
1. Contact normal force Fn vs Pressure for each cylinder radius across locations (Proximal, Central, Distal).
2. Peak normal force vs Initial cylinder location Xc for each radius.
3. Floating cylinder displacement trajectories (ux, uy) as the actuator curls and wraps around it.
4. Friction capacity Ffric vs Pressure and Radius.
"""

import os
import sys
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker

BASE_DIR = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
CSV_PATH = os.path.join(BASE_DIR, "floating_contact_location_sweep.csv")
IMG_DIR = os.path.join(BASE_DIR, "images")
os.makedirs(IMG_DIR, exist_ok=True)

# Styling
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

def main():
    if not os.path.exists(CSV_PATH):
        print(f"Error: CSV file not found: {CSV_PATH}")
        return

    df = pd.read_csv(CSV_PATH)
    if df.empty:
        print("CSV is empty.")
        return

    print(f"Loaded {len(df)} rows from {CSV_PATH}")
    radii = sorted(df["Radius_mm"].unique())
    locs = ["Proximal", "Central", "Distal"]
    loc_colors = {"Proximal": "#1f77b4", "Central": "#2ca02c", "Distal": "#d62728"}
    loc_markers = {"Proximal": "o", "Central": "s", "Distal": "^"}

    # -------------------------------------------------------------
    # Plot 1: Normal Contact Force Fn vs Pressure across Locations (4-panel)
    # -------------------------------------------------------------
    fig, axes = plt.subplots(2, 2, figsize=(12, 10), sharex=True)
    axes = axes.flatten()

    for idx, r in enumerate(radii):
        ax = axes[idx]
        df_r = df[df["Radius_mm"] == r]
        for loc in locs:
            df_sub = df_r[df_r["Loc"] == loc].sort_values(by="P_kPa")
            if not df_sub.empty:
                xc = df_sub["Xc_mm"].iloc[0]
                yc = df_sub["Yc_mm"].iloc[0]
                label = f"{loc} ($X_c={xc:.0f}, Y_c={yc:.0f}$ mm)"
                ax.plot(df_sub["P_kPa"], df_sub["Fn_N"],
                        marker=loc_markers[loc], color=loc_colors[loc],
                        lw=2.2, ms=6, label=label)

        ax.set_title(f"Cylinder Radius $R = {r:.0f}$ mm")
        ax.set_ylabel("Normal Contact Force $F_N$ (N)")
        ax.set_xlabel("Internal Pressure $P$ (kPa)")
        ax.legend(frameon=True, loc="upper left")
        ax.set_xlim(-2, 42)
        ax.set_ylim(bottom=-0.01)

    fig.suptitle("Floating Cylinder Normal Contact Force vs Pressure: Spatial Location Comparison", fontweight="bold", y=0.98)
    plt.tight_layout()
    p1 = os.path.join(IMG_DIR, "floating_grasp_fn_vs_pressure_locations.png")
    plt.savefig(p1)
    plt.close()
    print(f"Saved: {p1}")

    # -------------------------------------------------------------
    # Plot 2: Peak Normal Force vs Initial Location Xc
    # -------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(8, 6))
    r_colors = {12.0: "#1f77b4", 15.0: "#ff7f0e", 18.0: "#2ca02c", 22.0: "#d62728"}

    for r in radii:
        df_r = df[df["Radius_mm"] == r]
        peak_records = []
        for loc in locs:
            df_sub = df_r[df_r["Loc"] == loc]
            if not df_sub.empty:
                xc = df_sub["Xc_mm"].iloc[0]
                peak_fn = df_sub["Fn_N"].max()
                peak_records.append((xc, peak_fn, loc))

        if peak_records:
            peak_records.sort(key=lambda x: x[0])
            xcs = [p[0] for p in peak_records]
            fns = [p[1] for p in peak_records]
            ax.plot(xcs, fns, marker="o", lw=2.5, ms=8, color=r_colors.get(r, "k"),
                    label=f"$R_{{\\text{{cyl}}}} = {r:.0f}$ mm")
            for x, fn, l in peak_records:
                ax.annotate(f"{l}\n({fn:.3f} N)", (x, fn), textcoords="offset points",
                            xytext=(0, 8), ha="center", fontsize=8.5)

    ax.set_xlabel("Initial Cylinder Horizontal Location $X_c$ (mm)")
    ax.set_ylabel("Peak Normal Contact Force $F_{N,\\max}$ (N)")
    ax.set_title("Peak Grasp Normal Force vs Initial Cylinder Location $X_c$", fontweight="bold")
    ax.legend(frameon=True)
    ax.set_xlim(20, 48)
    plt.tight_layout()
    p2 = os.path.join(IMG_DIR, "floating_grasp_peak_force_vs_location.png")
    plt.savefig(p2)
    plt.close()
    print(f"Saved: {p2}")

    # -------------------------------------------------------------
    # Plot 3: Floating Cylinder Displacement Vectors & Trajectories
    # -------------------------------------------------------------
    fig, axes = plt.subplots(2, 2, figsize=(12, 10))
    axes = axes.flatten()

    for idx, r in enumerate(radii):
        ax = axes[idx]
        df_r = df[df["Radius_mm"] == r]
        for loc in locs:
            df_sub = df_r[df_r["Loc"] == loc].sort_values(by="P_kPa")
            if not df_sub.empty:
                # Plot trajectory in displacement space (ux, uy)
                ax.plot(df_sub["ux_mm"], df_sub["uy_mm"],
                        marker=loc_markers[loc], color=loc_colors[loc],
                        lw=2.0, ms=6, label=f"{loc}")
                # Mark final point at P=40 kPa
                last_pt = df_sub.iloc[-1]
                ax.scatter([last_pt["ux_mm"]], [last_pt["uy_mm"]],
                           color=loc_colors[loc], s=120, edgecolors="black", zorder=5)
                ax.annotate(f"P={last_pt['P_kPa']:.0f}kPa\n({last_pt['ux_mm']:.2f}, {last_pt['uy_mm']:.2f})",
                            (last_pt["ux_mm"], last_pt["uy_mm"]),
                            textcoords="offset points", xytext=(8, -8), fontsize=8)

        ax.set_title(f"Displacement Trajectory: $R = {r:.0f}$ mm")
        ax.set_xlabel("Horizontal Displacement $\\Delta X_c$ (mm)")
        ax.set_ylabel("Vertical Displacement $\\Delta Y_c$ (mm)")
        ax.axhline(0, color="gray", lw=0.8, ls=":")
        ax.axvline(0, color="gray", lw=0.8, ls=":")
        ax.legend(frameon=True, loc="best")

    fig.suptitle("Floating Cylinder Kinematic Trajectories $(\\Delta X_c, \\Delta Y_c)$ Under Actuator Grasp", fontweight="bold", y=0.98)
    plt.tight_layout()
    p3 = os.path.join(IMG_DIR, "floating_grasp_cylinder_displacements.png")
    plt.savefig(p3)
    plt.close()
    print(f"Saved: {p3}")

    # -------------------------------------------------------------
    # Plot 4: Friction Force Capacity vs Radius and Pressure (Optimal Locations)
    # -------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(8, 6))
    for r in radii:
        df_r = df[df["Radius_mm"] == r]
        # Find best location for this radius
        best_loc = None
        max_f = -1
        for loc in locs:
            f = df_r[df_r["Loc"] == loc]["Fn_N"].max()
            if f > max_f:
                max_f = f
                best_loc = loc
        if best_loc:
            df_best = df_r[df_r["Loc"] == best_loc].sort_values(by="P_kPa")
            ax.plot(df_best["P_kPa"], df_best["Ffric_N"],
                    marker="o", lw=2.5, ms=7, color=r_colors.get(r, "k"),
                    label=f"$R={r:.0f}$ mm ({best_loc})")

    ax.set_xlabel("Internal Pressure $P$ (kPa)")
    ax.set_ylabel("Friction Force Capacity $F_f = \\mu F_N$ (N) [$\\mu = 0.50$]")
    ax.set_title("Optimal Location Friction Capacity vs Pressure", fontweight="bold")
    ax.legend(frameon=True)
    ax.set_xlim(-2, 42)
    ax.set_ylim(bottom=-0.005)
    plt.tight_layout()
    p4 = os.path.join(IMG_DIR, "floating_grasp_optimal_friction_capacity.png")
    plt.savefig(p4)
    plt.close()
    print(f"Saved: {p4}")

    print("\nAll 4 publication figures successfully generated.")

if __name__ == "__main__":
    main()

