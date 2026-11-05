import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

def main():
    base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
    csv_path = os.path.join(base_dir, "contact_grasp_multiradius_summary.csv")
    images_dir = os.path.join(base_dir, "images")
    os.makedirs(images_dir, exist_ok=True)

    if not os.path.exists(csv_path):
        print(f"CSV not found: {csv_path}")
        return

    df = pd.read_csv(csv_path)
    print("Loaded data:")
    print(df.head(20))

    radii = sorted(df['R_cyl_mm'].unique())
    colors = {12.0: '#1f77b4', 15.0: '#ff7f0e', 18.0: '#2ca02c', 22.0: '#d62728'}
    markers = {12.0: 'o', 15.0: 's', 18.0: '^', 22.0: 'D'}

    plt.rcParams.update({
        'font.size': 11,
        'axes.labelsize': 12,
        'axes.titlesize': 13,
        'xtick.labelsize': 11,
        'ytick.labelsize': 11,
        'legend.fontsize': 11,
        'figure.titlesize': 14
    })

    # -------------------------------------------------------------
    # Plot 1: Normal Contact Force Fn & Friction Force Ff vs Pressure
    # -------------------------------------------------------------
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5), dpi=300)

    for r in radii:
        sub = df[df['R_cyl_mm'] == r].sort_values('P_step_kPa')
        c = colors.get(r, 'black')
        m = markers.get(r, 'o')
        label = f"$R_{{cyl}} = {int(r)}$ mm"
        ax1.plot(sub['P_step_kPa'], sub['Fn_total_N'], marker=m, color=c, linewidth=2.2, label=label)
        ax2.plot(sub['P_step_kPa'], sub['Ff_total_N'], marker=m, color=c, linewidth=2.2, label=label)

    ax1.set_xlabel("Internal Chamber Pressure $P$ (kPa)")
    ax1.set_ylabel("Total Normal Contact Force $F_N$ (N)")
    ax1.set_title("(a) Normal Contact Force vs. Actuation Pressure")
    ax1.grid(True, linestyle="--", alpha=0.6)
    ax1.legend(loc="upper left", frameon=True)

    ax2.set_xlabel("Internal Chamber Pressure $P$ (kPa)")
    ax2.set_ylabel("Coulomb Friction Force $F_f$ (N, $\\mu = 0.50$)")
    ax2.set_title("(b) Tangential Holding Friction Force vs. Pressure")
    ax2.grid(True, linestyle="--", alpha=0.6)
    ax2.legend(loc="upper left", frameon=True)

    plt.tight_layout()
    p1_path = os.path.join(images_dir, "contact_force_vs_pressure_multiradius.png")
    fig.savefig(p1_path, dpi=300)
    plt.close(fig)
    print(f"Saved plot: {p1_path}")

    # -------------------------------------------------------------
    # Plot 2: Peak Normal Force vs Cylinder Radius
    # -------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(8, 5.5), dpi=300)
    peak_fn = []
    peak_ff = []
    peak_p = []
    for r in radii:
        sub = df[df['R_cyl_mm'] == r]
        idx_max = sub['Fn_total_N'].idxmax()
        peak_fn.append(sub.loc[idx_max, 'Fn_total_N'])
        peak_ff.append(sub.loc[idx_max, 'Ff_total_N'])
        peak_p.append(sub.loc[idx_max, 'P_step_kPa'])

    ax.plot(radii, peak_fn, 'o-', color='#1a5276', linewidth=2.5, markersize=8, label="Peak Normal Force $F_{N,max}$")
    ax.plot(radii, peak_ff, 's--', color='#b03a2e', linewidth=2.0, markersize=8, label="Holding Friction Capacity $F_{f,max}$ ($\\mu=0.50$)")

    for i, r in enumerate(radii):
        ax.annotate(f"{peak_fn[i]:.3f} N\n(@ {peak_p[i]:.0f} kPa)",
                    (r, peak_fn[i]), textcoords="offset points", xytext=(0, 10),
                    ha='center', fontsize=9, fontweight='bold', color='#1a5276')

    ax.set_xlabel("Target Cylinder Radius $R_{cyl}$ (mm)")
    ax.set_ylabel("Maximum Grip Force (N)")
    ax.set_title("Soft Actuator Grasping Capacity vs. Target Object Radius")
    ax.grid(True, linestyle="--", alpha=0.6)
    ax.set_xticks(radii)
    ax.legend(loc="upper left", frameon=True)

    plt.tight_layout()
    p2_path = os.path.join(images_dir, "peak_grasp_force_vs_cylinder_radius.png")
    fig.savefig(p2_path, dpi=300)
    plt.close(fig)
    print(f"Saved plot: {p2_path}")

if __name__ == "__main__":
    main()

