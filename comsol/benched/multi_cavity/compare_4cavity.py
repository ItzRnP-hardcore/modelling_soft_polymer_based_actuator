"""
compare_4cavity.py
Compares the Coded Segmented FEA / Beam Model vs COMSOL 6.4 3D Solid FEA for the 4-Cavity Segmented Actuator.
Includes real 3D COMSOL Total Displacement and von Mises Stress renders alongside centerline trajectories
and a quantitative verification table across all 7 actuation patterns.
"""
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from PIL import Image
from pathlib import Path
import os

OUT_DIR = Path("c:/Users/rudra/Documents/BTP/comsol/multi_cavity")
df_c = pd.read_csv(OUT_DIR / "comsol_4cavity_summary.csv")
df_lines = pd.read_csv(OUT_DIR / "comsol_4cavity_centerlines.csv")
df_py_sleeve = pd.read_csv("c:/Users/rudra/Documents/BTP/results/sleeve/summary.csv")

# From Python 3D FEA of the sleeved eccentric actuator at P = 50 kPa:
# Total pressurized length L_cav_single = 96 mm -> theta_py = 14.364 deg, axial strain eps_py = 0.258 / 96.0
row_py50 = df_py_sleeve[np.isclose(df_py_sleeve["P_kPa"], 50.0)].iloc[0]
theta_py50_rad = np.radians(row_py50["theta_deg"])
kappa_py50 = theta_py50_rad / 96.0  # rad/mm in pressurized cavity section
eps_py50 = (row_py50["x_tip_mm"] - 100.0 + 0.5 * 100.0 * theta_py50_rad**2 / 3.0) / 96.0

# 4-Cavity geometry along x in [0, 101] mm:
# Cavities at [2, 24], [27, 49], [52, 74], [77, 99] (each 22 mm long, with 3 mm septa)
cav_intervals = [(2.0, 24.0), (27.0, 49.0), (52.0, 74.0), (77.0, 99.0)]

def solve_coded_4cavity(weights, n_pts=201):
    s = np.linspace(0.0, 101.0, n_pts)
    ds = s[1] - s[0]
    x = np.zeros(n_pts)
    y = np.zeros(n_pts)
    th = np.zeros(n_pts)
    for i in range(1, n_pts):
        smid = 0.5 * (s[i-1] + s[i])
        w_active = 0.0
        for k, (xs, xe) in enumerate(cav_intervals):
            if xs <= smid <= xe:
                w_active = float(weights[k])
                break
        k_loc = w_active * kappa_py50
        e_loc = w_active * 0.0028
        th[i] = th[i-1] + k_loc * ds
        th_mid = 0.5 * (th[i-1] + th[i])
        x[i] = x[i-1] + (1.0 + e_loc) * np.cos(th_mid) * ds
        y[i] = y[i-1] - (1.0 + e_loc) * np.sin(th_mid) * ds
    return s, x, y, np.degrees(th[-1]), -y[-1], x[-1] - 101.0

cases_info = [
    ("all_4_chambers", [1, 1, 1, 1], "All 4 Chambers [1,1,1,1]", "#38bdf8"),
    ("chambers_1_2_proximal", [1, 1, 0, 0], "Chambers 1+2 Proximal [1,1,0,0]", "#ec4899"),
    ("chambers_3_4_distal", [0, 0, 1, 1], "Chambers 3+4 Distal [0,0,1,1]", "#06b6d4"),
    ("chamber_1_base", [1, 0, 0, 0], "Chamber 1 Base Only [1,0,0,0]", "#f43f5e"),
    ("chamber_2", [0, 1, 0, 0], "Chamber 2 Mid-Base [0,1,0,0]", "#fbbf24"),
    ("chamber_3", [0, 0, 1, 0], "Chamber 3 Mid-Tip [0,0,1,0]", "#a855f7"),
    ("chamber_4_tip", [0, 0, 0, 1], "Chamber 4 Tip Only [0,0,0,1]", "#10b981"),
]

coded_results = {}
for cname, w, label, col in cases_info:
    s, x, y, th_deg, delta_mm, u_tip = solve_coded_4cavity(w)
    coded_results[cname] = {
        "w": w, "label": label, "color": col,
        "s": s, "x": x, "y": y,
        "theta_deg": th_deg, "delta_mm": delta_mm, "u_tip_mm": u_tip
    }

fig = plt.figure(figsize=(20, 13), dpi=200, facecolor='#0f172a')
gs = fig.add_gridspec(3, 3, height_ratios=[1.05, 1.05, 1.15], hspace=0.34, wspace=0.24)

# Row 1: 3D COMSOL Renders for All 4 Chambers (Disp + Stress) & Single-Chamber Comparison
r1_panels = [
    (gs[0, 0], "comsol_all_4_chambers_disp.png",
     "COMSOL 6.4: All 4 Chambers [1,1,1,1] Total Displacement\nFull-length bending (δ = 11.16 mm, θ = 12.16° at P = 50 kPa)"),
    (gs[0, 1], "comsol_all_4_chambers_stress.png",
     "COMSOL 6.4: All 4 Chambers [1,1,1,1] von Mises Stress\n3 mm internal septa isolate cavity hoop expansion"),
    (gs[0, 2], "comsol_chamber_1_base_disp.png",
     "COMSOL 6.4: Chamber 1 (Base Only [1,0,0,0]) Displacement\nRoot bends; unpressurized Chambers 2–4 project as straight tangent"),
]

for spec, fname, title in r1_panels:
    ax = fig.add_subplot(spec)
    p = OUT_DIR / fname
    if p.exists():
        ax.imshow(Image.open(p))
    ax.axis('off')
    ax.set_title(title, color='#f8fafc', fontsize=10.5, fontweight='bold', pad=8)

# Row 2 Left: 3D COMSOL Render for Tip Only [0,0,0,1] Stress/Disp
ax_r2_0 = fig.add_subplot(gs[1, 0])
p_tip = OUT_DIR / "comsol_chamber_4_tip_stress.png"
if p_tip.exists():
    ax_r2_0.imshow(Image.open(p_tip))
ax_r2_0.axis('off')
ax_r2_0.set_title("COMSOL 6.4: Chamber 4 (Tip Only [0,0,0,1]) von Mises Stress\nStress strictly confined to distal chamber (x ∈ [77, 99] mm)",
                  color='#f8fafc', fontsize=10.5, fontweight='bold', pad=8)

# Row 2 Middle + Right: Centerline Deflection Trajectories (Coded vs COMSOL 6.4)
ax_traj = fig.add_subplot(gs[1, 1:])
ax_traj.set_facecolor('#1e293b')
ax_traj.tick_params(colors='#94a3b8')
for spine in ax_traj.spines.values():
    spine.set_color('#334155')

for cname, w, label, col in cases_info:
    cr = coded_results[cname]
    ax_traj.plot(cr["x"], cr["y"], '--', color=col, lw=2.0, alpha=0.85)
    sub = df_lines[df_lines["case_name"] == cname]
    if len(sub) > 0:
        ax_traj.plot(sub["x_mm"], sub["y_mm"], '-', color=col, lw=2.6,
                     label=f"{label}: COMSOL δ={-sub['y_mm'].iloc[-1]:.2f}mm vs Coded {cr['delta_mm']:.2f}mm")

# Mark the 4 chamber zones on y=0
for k, (xs, xe) in enumerate(cav_intervals, 1):
    ax_traj.axvspan(xs, xe, color='#334155', alpha=0.22)
    ax_traj.text(0.5*(xs+xe), 0.45, f"Cav {k}", color='#94a3b8', fontsize=8.5, ha='center')

ax_traj.axhline(0, color='#64748b', linestyle=':', lw=1)
ax_traj.set_xlabel("Axial Coordinate x (mm)", color='#e2e8f0', fontsize=10)
ax_traj.set_ylabel("Vertical Centerline Deflection y (mm)", color='#e2e8f0', fontsize=10)
ax_traj.set_title("Deformed Centerline Trajectories y(x): COMSOL 6.4 3D Solid (Solid) vs. Coded FEA Model (Dashed)",
                  color='#f8fafc', fontsize=11.5, fontweight='bold')
ax_traj.grid(True, linestyle=':', alpha=0.4, color='#64748b')
ax_traj.legend(facecolor='#1e293b', edgecolor='#334155', labelcolor='#f8fafc', fontsize=8.2, ncol=2, loc='lower left')

# Row 3 Left: Bar Chart of Tip Deflection δ(mm) & Angle θ(deg) Across All 7 Patterns
ax_bar = fig.add_subplot(gs[2, :2])
ax_bar.set_facecolor('#1e293b')
ax_bar.tick_params(colors='#94a3b8')
for spine in ax_bar.spines.values():
    spine.set_color('#334155')

x_idx = np.arange(len(cases_info))
width = 0.36
c_deltas = [df_c[df_c["case_name"] == c[0]]["delta_mm"].iloc[0] for c in cases_info]
py_deltas = [coded_results[c[0]]["delta_mm"] for c in cases_info]
c_thetas = [df_c[df_c["case_name"] == c[0]]["theta_deg"].iloc[0] for c in cases_info]
py_thetas = [coded_results[c[0]]["theta_deg"] for c in cases_info]

b1 = ax_bar.bar(x_idx - width/2, py_deltas, width, color='#38bdf8', edgecolor='#0284c7', label='Coded Segmented FEA δ (mm)')
b2 = ax_bar.bar(x_idx + width/2, c_deltas, width, color='#f43f5e', edgecolor='#be123c', label='COMSOL 6.4 3D Solid δ (mm)')

for i in range(len(cases_info)):
    ax_bar.text(x_idx[i] - width/2, py_deltas[i] + 0.25, f"{py_deltas[i]:.2f}\n({py_thetas[i]:.1f}°)",
                ha='center', va='bottom', color='#bae6fd', fontsize=7.8)
    ax_bar.text(x_idx[i] + width/2, c_deltas[i] + 0.25, f"{c_deltas[i]:.2f}\n({c_thetas[i]:.1f}°)",
                ha='center', va='bottom', color='#fecdd3', fontsize=7.8)

ax_bar.set_xticks(x_idx)
ax_bar.set_xticklabels([f"{c[0].replace('_', ' ')}\n{c[1]}" for c in cases_info], color='#e2e8f0', fontsize=8.5)
ax_bar.set_ylabel("Tip Vertical Deflection δ (mm)", color='#e2e8f0', fontsize=10)
ax_bar.set_ylim(0, max(max(py_deltas), max(c_deltas)) * 1.25)
ax_bar.set_title("Tip Deflection δ & Bending Angle θ by Chamber Pattern (Leverage Effect: Base > Mid > Tip)",
                 color='#f8fafc', fontsize=11.5, fontweight='bold')
ax_bar.grid(True, axis='y', linestyle=':', alpha=0.4, color='#64748b')
ax_bar.legend(facecolor='#1e293b', edgecolor='#334155', labelcolor='#f8fafc', fontsize=9, loc='upper right')

# Row 3 Right: Quantitative Verification Table
ax_tab = fig.add_subplot(gs[2, 2])
ax_tab.set_facecolor('#1e293b')
ax_tab.axis('off')
ax_tab.set_title("4-Cavity Coded vs. COMSOL 6.4 Summary", color='#f8fafc', fontsize=11.5, fontweight='bold', pad=10)

table_rows = []
for cname, w, label, col in cases_info:
    crow = df_c[df_c["case_name"] == cname].iloc[0]
    cr = coded_results[cname]
    w_str = "".join(str(x) for x in w)
    table_rows.append([
        f"[{w_str}]",
        f"{cr['delta_mm']:.2f} / {crow['delta_mm']:.2f}",
        f"{cr['theta_deg']:.2f}° / {crow['theta_deg']:.2f}°",
        f"{crow['max_mises_MPa']:.3f} MPa"
    ])

col_labels = ["Pattern", "δ (Py / COM) mm", "θ (Py / COM)", "Max σ_vM"]
tab = ax_tab.table(cellText=table_rows, colLabels=col_labels, loc='center', cellLoc='center')
tab.auto_set_font_size(False)
tab.set_fontsize(8.3)
tab.scale(1.0, 1.65)
for (r, c), cell in tab.get_celld().items():
    cell.set_edgecolor('#334155')
    if r == 0:
        cell.set_facecolor('#0f172a')
        cell.set_text_props(color='#38bdf8', fontweight='bold')
    else:
        cell.set_facecolor('#1e293b' if r % 2 == 0 else '#243247')
        cell.set_text_props(color='#f8fafc')

out_dash = OUT_DIR / "comsol_vs_coded_4cavity.png"
plt.savefig(out_dash, bbox_inches='tight', dpi=200, facecolor=fig.get_facecolor())
print("Dashboard saved to:", out_dash)
