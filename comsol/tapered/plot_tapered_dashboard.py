"""
plot_tapered_dashboard.py
Creates a comprehensive comparison dashboard for the Tapered Actuator (R0=10mm -> Rt=6mm, t0=2mm -> tt=1mm)
combining 3D Total Displacement, 3D von Mises Stress renders from COMSOL 6.4, and quantitative comparison
with the Python Tapered Actuator model (grasp_model.py).
"""
import sys
sys.path.insert(0, "c:/Users/rudra/Documents/BTP")
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from PIL import Image
import os
from grasp_model import Finger, free_curl

OUT_DIR = "c:/Users/rudra/Documents/BTP/comsol/tapered"
df_c = pd.read_csv(f"{OUT_DIR}/comsol_tapered_summary.csv")

# Coded tapered finger vs uniform finger (grasp_model.py)
f_tap = Finger(R0=10.0, Rt=6.0, t0=2.0, tt=1.0, n_seg=4, E=0.15)
f_uni = Finger(R0=10.0, Rt=10.0, t0=1.5, tt=1.5, n_seg=4, E=0.15)

p_vals_kpa = np.linspace(0, 30, 31)
th_tap, d_tap, x_tap_list, y_tap_list = [], [], [], []
for pk in p_vals_kpa:
    pts, th = free_curl(f_tap, [pk/1000.0]*4)
    x_c = -pts[:, 1]
    y_c = -pts[:, 0]
    th_tap.append(np.degrees(th))
    d_tap.append(-y_c[-1])
    if pk in [0, 10, 20, 30]:
        x_tap_list.append((pk, x_c, y_c, np.degrees(th)))

fig = plt.figure(figsize=(18, 10.5), dpi=200, facecolor='#0f172a')
gs = fig.add_gridspec(2, 3, width_ratios=[1.15, 1.15, 1.0], hspace=0.32, wspace=0.25)

# 1. 3D Total Displacement Render (Top Left)
ax_disp = fig.add_subplot(gs[0, 0])
disp_path = f"{OUT_DIR}/comsol_tapered_disp.png"
if os.path.exists(disp_path):
    ax_disp.imshow(Image.open(disp_path))
    ax_disp.axis('off')
    ax_disp.set_title("COMSOL 6.4: Tapered Actuator 3D Total Displacement (True 1:1 Scale)\nHigh-curvature distal curl (x_tip = -0.27 mm, δ = 22.30 mm, |θ| = 215.5°)",
                      color='#f8fafc', fontsize=11, fontweight='bold', pad=10)

# 2. 3D von Mises Stress Render (Bottom Left)
ax_stress = fig.add_subplot(gs[1, 0])
stress_path = f"{OUT_DIR}/comsol_tapered_stress.png"
if os.path.exists(stress_path):
    ax_stress.imshow(Image.open(stress_path))
    ax_stress.axis('off')
    ax_stress.set_title("COMSOL 6.4: Tapered Actuator 3D von Mises Stress (True 1:1 Scale)\nDistal wall thinning (2 mm -> 1 mm) amplifies tip strain and curvature",
                        color='#f8fafc', fontsize=11, fontweight='bold', pad=10)

# 3. Tapered Centerline Spiral Profiles (Top Middle)
ax_prof = fig.add_subplot(gs[0, 1])
ax_prof.set_facecolor('#1e293b')
ax_prof.tick_params(colors='#94a3b8')
for spine in ax_prof.spines.values():
    spine.set_color('#334155')

cols = ['#64748b', '#38bdf8', '#a855f7', '#f43f5e']
for (pk, xc, yc, th_d), col in zip(x_tap_list, cols):
    ax_prof.plot(xc, yc, lw=2.6, color=col, label=f"Coded Tapered P={pk:.0f} kPa (θ={th_d:.1f}°)")

# Plot COMSOL tip positions at step 1 (15 kPa) and step 2 (30 kPa)
ax_prof.scatter([100.0, df_c.loc[1, 'x_tip_mm'], df_c.loc[2, 'x_tip_mm']],
                [0.0, -df_c.loc[1, 'delta_mm'], -df_c.loc[2, 'delta_mm']],
                s=90, color=['#64748b', '#10b981', '#fbbf24'], zorder=6, edgecolor='#f8fafc', lw=1.5,
                label='COMSOL 3D Tip Positions (0, 15, 30 kPa)')

ax_prof.set_aspect('equal')
ax_prof.set_xlabel('Horizontal Coordinate x (mm)', color='#e2e8f0', fontsize=10)
ax_prof.set_ylabel('Vertical Coordinate y (mm)', color='#e2e8f0', fontsize=10)
ax_prof.set_title('Tapered Logarithmic Spiral Curl Trajectory', color='#f8fafc', fontsize=11.5, fontweight='bold')
ax_prof.grid(True, linestyle=':', alpha=0.4, color='#64748b')
ax_prof.legend(facecolor='#1e293b', edgecolor='#334155', labelcolor='#f8fafc', fontsize=8.5)

# 4. Curvature Amplification Along Tapered Length s/L (Bottom Middle)
ax_k = fig.add_subplot(gs[1, 1])
ax_k.set_facecolor('#1e293b')
ax_k.tick_params(colors='#94a3b8')
for spine in ax_k.spines.values():
    spine.set_color('#334155')

s_norm = np.linspace(0, 1, 100)
R_s = 10.0 + (6.0 - 10.0) * s_norm
t_s = 2.0 + (1.0 - 2.0) * s_norm
# EI(s) / EI(0) and local curvature ratio kappa(s) / kappa(0)
ei_ratio = (R_s**3 * t_s) / (10.0**3 * 2.0)
kappa_ratio = ((R_s - t_s)**2 * R_s / (R_s**3 * t_s)) / ((10.0 - 2.0)**2 * 10.0 / (10.0**3 * 2.0))

ax_k.plot(s_norm * 100, ei_ratio, '-', color='#38bdf8', lw=2.6, label='Normalized Flexural Rigidity $EI(s)/EI(0)$')
ax_k.plot(s_norm * 100, kappa_ratio, '--', color='#f43f5e', lw=2.6, label='Local Curvature Amplification $\\kappa(s)/\\kappa(0)$')
ax_k.set_xlabel('Axial Position s along Actuator (mm)', color='#e2e8f0', fontsize=10)
ax_k.set_ylabel('Normalized Ratio', color='#e2e8f0', fontsize=10)
ax_k.set_title('Distal Compliance & Curvature Amplification ($R: 10\\to 6$ mm, $t: 2\\to 1$ mm)', color='#f8fafc', fontsize=11, fontweight='bold')
ax_k.grid(True, linestyle=':', alpha=0.4, color='#64748b')
ax_k.legend(facecolor='#1e293b', edgecolor='#334155', labelcolor='#f8fafc', fontsize=9)

# 5. Quantitative Comparison Table (Right Column)
ax_tab = fig.add_subplot(gs[:, 2])
ax_tab.set_facecolor('#1e293b')
ax_tab.axis('off')
ax_tab.set_title('Tapered Actuator: Coded vs. COMSOL 6.4', color='#f8fafc', fontsize=12, fontweight='bold', pad=12)

# Unwrap COMSOL angle (-144.47 deg is 360 - 144.47 = 215.53 deg cumulative curl)
th_c_step1 = df_c.loc[1, 'theta_deg']
th_c_step2 = 360.0 + df_c.loc[2, 'theta_deg'] if df_c.loc[2, 'theta_deg'] < 0 else df_c.loc[2, 'theta_deg']

pts_15, th_15 = free_curl(f_tap, [0.015]*4)
pts_30, th_30 = free_curl(f_tap, [0.030]*4)

rows = [
    ["Outer Radius R(s)", "10.0 → 6.0 mm", "10.0 → 6.0 mm"],
    ["Wall Thickness t(s)", "2.0 → 1.0 mm", "2.0 → 1.0 mm"],
    ["Tip Angle θ (15 kPa)", f"{np.degrees(th_15):.1f}°", f"{th_c_step1:.1f}°"],
    ["Tip Deflection δ (15 kPa)", f"{pts_15[-1,0]:.2f} mm", f"{df_c.loc[1, 'delta_mm']:.2f} mm"],
    ["Tip Angle θ (30 kPa)", f"{np.degrees(th_30):.1f}°", f"{th_c_step2:.1f}°"],
    ["Tip x_tip (30 kPa)", f"{-pts_30[-1,1]:.2f} mm", f"{df_c.loc[2, 'x_tip_mm']:.2f} mm"],
    ["Tip y_tip (30 kPa)", f"{-pts_30[-1,0]:.2f} mm", f"{-df_c.loc[2, 'delta_mm']:.2f} mm"],
]

tab = ax_tab.table(cellText=rows, colLabels=["Metric", "Coded Model", "COMSOL 6.4"], loc='upper center', cellLoc='center',
                   bbox=[0.02, 0.42, 0.96, 0.52])
tab.auto_set_font_size(False)
tab.set_fontsize(9)
for (r, c), cell in tab.get_celld().items():
    cell.set_edgecolor('#334155')
    if r == 0:
        cell.set_facecolor('#0f172a')
        cell.set_text_props(color='#38bdf8', fontweight='bold')
    else:
        cell.set_facecolor('#1e293b' if r % 2 == 0 else '#243247')
        cell.set_text_props(color='#f8fafc')

summary_txt = (
    "Physical Insights (Tapered Actuator):\n"
    "• Tapering R from 10→6 mm and t from 2→1 mm\n"
    "  reduces distal flexural rigidity EI(s) by 9.3×\n"
    "  at the tip while increasing local curvature.\n"
    "• Instead of a uniform circular arc, the actuator\n"
    "  curls into a tight logarithmic spiral:\n"
    "  - At P = 15 kPa: COMSOL θ = 169.1°, δ = 53.6 mm\n"
    "  - At P = 30 kPa: COMSOL curls past 180° to\n"
    "    θ = 215.5° (-144.5° wrapped), bringing the\n"
    "    tip all the way back to x_tip = -0.27 mm!"
)
ax_tab.text(0.04, 0.36, summary_txt, transform=ax_tab.transAxes, color='#e2e8f0', fontsize=9.2, va='top', family='monospace')

out_fig = f"{OUT_DIR}/tapered_dashboard.png"
plt.savefig(out_fig, bbox_inches='tight', dpi=200, facecolor=fig.get_facecolor())
print("Saved tapered dashboard:", out_fig)

