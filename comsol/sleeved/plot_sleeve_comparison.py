"""
plot_sleeve_comparison.py
Plots COMSOL 6.4 vs Python FEA results for the Sleeved Actuator,
combining both 3D Total Displacement and 3D von Mises Stress renders (true 1:1 scale)
with quantitative delta(P), theta(P), and axial elongation comparisons.
"""
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from PIL import Image
import os

comsol_csv = "c:/Users/rudra/Documents/BTP/comsol/sleeved/comsol_sleeve_summary.csv"
py_csv = "c:/Users/rudra/Documents/BTP/results/sleeve/summary.csv"

df_c = pd.read_csv(comsol_csv)
df_py = pd.read_csv(py_csv)

fig = plt.figure(figsize=(18, 10.5), dpi=200, facecolor='#0f172a')
gs = fig.add_gridspec(2, 3, width_ratios=[1.15, 1.15, 1.0], hspace=0.32, wspace=0.25)

# 1. 3D Total Displacement Render (Top Left)
ax_disp = fig.add_subplot(gs[0, 0])
disp_path = "c:/Users/rudra/Documents/BTP/comsol/sleeved/comsol_sleeve_disp_P110.png"
if os.path.exists(disp_path):
    img_d = Image.open(disp_path)
    ax_disp.imshow(img_d)
    ax_disp.axis('off')
    ax_disp.set_title("COMSOL 6.4: 3D Total Displacement (P = 110 kPa, True 1:1 Scale)\nTip deflection δ = 39.47 mm, Tip angle θ = 43.02°",
                      color='#f8fafc', fontsize=11, fontweight='bold', pad=10)

# 2. 3D von Mises Stress Render (Bottom Left)
ax_stress = fig.add_subplot(gs[1, 0])
stress_path = "c:/Users/rudra/Documents/BTP/comsol/sleeved/comsol_sleeve_stress_P110.png"
if os.path.exists(stress_path):
    img_s = Image.open(stress_path)
    ax_stress.imshow(img_s)
    ax_stress.axis('off')
    ax_stress.set_title("COMSOL 6.4: 3D von Mises Stress (P = 110 kPa, True 1:1 Scale)\nPeak stress localized at inner eccentric cavity wall",
                        color='#f8fafc', fontsize=11, fontweight='bold', pad=10)

# 3. Tip Deflection δ(P): Python FEA vs COMSOL 6.4 (Top Middle)
ax_d = fig.add_subplot(gs[0, 1])
ax_d.set_facecolor('#1e293b')
ax_d.tick_params(colors='#94a3b8')
for spine in ax_d.spines.values():
    spine.set_color('#334155')

ax_d.plot(df_py['P_kPa'], df_py['delta_mm'], '-', color='#38bdf8', lw=2.8, label='Python FEA (Q2/P1 3D, δ=39.20 mm)')
ax_d.plot(df_c['P_kPa'], df_c['delta_mm'], 'o--', color='#f43f5e', lw=2.2, markersize=6, label='COMSOL 6.4 (3D Solid, δ=39.47 mm)')
ax_d.set_xlabel('Cavity Pressure P (kPa)', color='#e2e8f0', fontsize=10)
ax_d.set_ylabel('Tip Vertical Deflection δ (mm)', color='#e2e8f0', fontsize=10)
ax_d.set_title('Tip Deflection δ vs. Pressure (0.7% Agreement)', color='#f8fafc', fontsize=11.5, fontweight='bold')
ax_d.grid(True, linestyle=':', alpha=0.4, color='#64748b')
ax_d.legend(facecolor='#1e293b', edgecolor='#334155', labelcolor='#f8fafc', fontsize=9)

# 4. Tip Bending Angle θ(P): Python FEA vs COMSOL 6.4 (Bottom Middle)
ax_th = fig.add_subplot(gs[1, 1])
ax_th.set_facecolor('#1e293b')
ax_th.tick_params(colors='#94a3b8')
for spine in ax_th.spines.values():
    spine.set_color('#334155')

ax_th.plot(df_py['P_kPa'], df_py['theta_deg'], '-', color='#10b981', lw=2.8, label='Python FEA (θ=42.72° at 110 kPa)')
ax_th.plot(df_c['P_kPa'], df_c['theta_deg'], 's--', color='#a855f7', lw=2.2, markersize=6, label='COMSOL 6.4 (θ=43.02° at 110 kPa)')
ax_th.set_xlabel('Cavity Pressure P (kPa)', color='#e2e8f0', fontsize=10)
ax_th.set_ylabel('Tip Rotation Angle θ (deg)', color='#e2e8f0', fontsize=10)
ax_th.set_title('Tip Bending Angle θ vs. Pressure (0.7% Agreement)', color='#f8fafc', fontsize=11.5, fontweight='bold')
ax_th.grid(True, linestyle=':', alpha=0.4, color='#64748b')
ax_th.legend(facecolor='#1e293b', edgecolor='#334155', labelcolor='#f8fafc', fontsize=9)

# 5. Tip Horizontal Coordinate x_tip(P) (Top Right)
ax_x = fig.add_subplot(gs[0, 2])
ax_x.set_facecolor('#1e293b')
ax_x.tick_params(colors='#94a3b8')
for spine in ax_x.spines.values():
    spine.set_color('#334155')

ax_x.plot(df_py['P_kPa'], df_py['x_tip_mm'], '-', color='#38bdf8', lw=2.8, label='Python FEA x_tip(P)')
ax_x.plot(df_c['P_kPa'], df_c['x_tip_mm'], '^--', color='#fbbf24', lw=2.2, markersize=6, label='COMSOL 6.4 x_tip(P)')
ax_x.set_xlabel('Cavity Pressure P (kPa)', color='#e2e8f0', fontsize=10)
ax_x.set_ylabel('Deformed Tip Position x_tip (mm)', color='#e2e8f0', fontsize=10)
ax_x.set_title('Axial Extension & Large-Deflection Pullback', color='#f8fafc', fontsize=11, fontweight='bold')
ax_x.grid(True, linestyle=':', alpha=0.4, color='#64748b')
ax_x.legend(facecolor='#1e293b', edgecolor='#334155', labelcolor='#f8fafc', fontsize=9)

# 6. Quantitative Verification Table (Bottom Right)
ax_tab = fig.add_subplot(gs[1, 2])
ax_tab.set_facecolor('#1e293b')
ax_tab.axis('off')
ax_tab.set_title('Python FEA vs. COMSOL 6.4 (Sleeved)', color='#f8fafc', fontsize=11, fontweight='bold', pad=10)

rows = []
for p_val in [20, 50, 70, 90, 110]:
    r_py = df_py[np.isclose(df_py['P_kPa'], p_val)].iloc[0]
    r_c = df_c[np.isclose(df_c['P_kPa'], p_val)].iloc[0]
    err_d = abs(r_c['delta_mm'] - r_py['delta_mm']) / r_py['delta_mm'] * 100.0
    rows.append([
        f"{p_val} kPa",
        f"{r_py['delta_mm']:.2f} mm",
        f"{r_c['delta_mm']:.2f} mm",
        f"{r_py['theta_deg']:.1f}° / {r_c['theta_deg']:.1f}°",
        f"{err_d:.2f}%"
    ])

col_labels = ["P", "Py δ", "COMSOL δ", "θ (Py/COM)", "Diff"]
tab = ax_tab.table(cellText=rows, colLabels=col_labels, loc='center', cellLoc='center')
tab.auto_set_font_size(False)
tab.set_fontsize(8.5)
tab.scale(1.0, 1.85)
for (r, c), cell in tab.get_celld().items():
    cell.set_edgecolor('#334155')
    if r == 0:
        cell.set_facecolor('#0f172a')
        cell.set_text_props(color='#38bdf8', fontweight='bold')
    else:
        cell.set_facecolor('#1e293b' if r % 2 == 0 else '#243247')
        cell.set_text_props(color='#f8fafc')

out_fig = "c:/Users/rudra/Documents/BTP/comsol/sleeved/sleeve_dashboard.png"
plt.savefig(out_fig, bbox_inches='tight', dpi=200, facecolor=fig.get_facecolor())
print("Saved sleeve dashboard:", out_fig)
