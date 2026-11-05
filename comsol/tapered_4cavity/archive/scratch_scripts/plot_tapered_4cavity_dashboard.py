import os
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import numpy as np
import pandas as pd

plt.style.use('dark_background')
OUT_DIR = r"c:\Users\rudra\Documents\BTP\comsol\tapered_4cavity"

steps_csv = os.path.join(OUT_DIR, "comsol_tapered_4cavity_steps.csv")
summary_csv = os.path.join(OUT_DIR, "comsol_tapered_4cavity_summary.csv")

df_steps = pd.read_csv(steps_csv)
df_sum = pd.read_csv(summary_csv)

fig = plt.figure(figsize=(19, 11.5), facecolor='#0b0f19')
gs = fig.add_gridspec(2, 3, height_ratios=[1.15, 1.0], hspace=0.28, wspace=0.22,
                       left=0.05, right=0.96, top=0.91, bottom=0.06)

fig.suptitle('Full 3D Tapered Continuum Actuator: 4 Arc-Shaped (80°) Cavities & 4 Radial Rib Separations (COMSOL 6.4)',
             fontsize=16, fontweight='bold', color='#38bdf8', y=0.965)

c1_row = df_sum[df_sum['case_name'] == 'cavity_1_top_pitch_down'].iloc[0]
p_max = float(c1_row['P_max_kPa'])
d_c1 = float(c1_row['delta_3d_mm'])
phi_c1 = float(c1_row['phi_bend_deg'])

# --- 1. Top Left: Clean 3D Deformed Continuum (Mode 1 Pitch) ---
ax_img1 = fig.add_subplot(gs[0, 0])
ax_img1.set_facecolor('#0f172a')
img_path1 = os.path.join(OUT_DIR, "comsol_cavity_1_top_pitch_down_disp.png")
if os.path.exists(img_path1):
    img1 = mpimg.imread(img_path1)
    ax_img1.imshow(img1)
    ax_img1.axis('off')
    ax_img1.set_title(f'COMSOL 3D: Mode 1 (Top Cavity +Y, P={p_max:.0f} kPa)\nClean Continuum Pitch Bending (δ = {d_c1:.2f} mm, φ = {phi_c1:.1f}°)',
                      color='#38bdf8', fontsize=10.5, fontweight='bold')

# --- 2. Top Middle: 3D Cross-Sectional Slices showing Cavity & Rib Separation Stress ---
ax_img2 = fig.add_subplot(gs[0, 1])
ax_img2.set_facecolor('#0f172a')
img_path2 = os.path.join(OUT_DIR, "comsol_cavities_1_2_diagonal_45deg_slice3d_stress.png")
if os.path.exists(img_path2):
    img2 = mpimg.imread(img_path2)
    ax_img2.imshow(img2)
    ax_img2.axis('off')
    ax_img2.set_title('COMSOL 3D Cross-Sectional Slices (Mode 5 Diagonal 1+2)\nvon Mises Stress Across Arc Cavities & Radial Ribs',
                      color='#a855f7', fontsize=10.5, fontweight='bold')

# --- 3. Top Right: 2D Mid-Cross-Section (X=45 mm) showing 4 Arc Cavities & Rib Stress ---
ax_img3 = fig.add_subplot(gs[0, 2])
ax_img3.set_facecolor('#0f172a')
img_path3 = os.path.join(OUT_DIR, "comsol_cavities_1_2_diagonal_45deg_sec2d_stress.png")
if os.path.exists(img_path3):
    img3 = mpimg.imread(img_path3)
    ax_img3.imshow(img3)
    ax_img3.axis('off')
    ax_img3.set_title('Cross-Section at X = 45 mm (4 × 80° Arc Cavities)\nvon Mises Stress on Radial Rib Separations',
                      color='#34d399', fontsize=10.5, fontweight='bold')

# --- 4. Bottom Left: 3D Omnidirectional Polar Steering Workspace (Y-Z Tip Orbit) ---
ax_polar = fig.add_subplot(gs[1, 0])
ax_polar.set_facecolor('#0f172a')

colors = {
    'cavity_1_top_pitch_down': '#38bdf8',
    'cavity_2_right_yaw_left': '#f59e0b',
    'cavity_3_bottom_pitch_up': '#ef4444',
    'cavity_4_left_yaw_right': '#10b981',
    'cavities_1_2_diagonal_45deg': '#a855f7',
    'cavities_1_4_diagonal_135deg': '#ec4899',
    'all_4_cavities_axial_stiffening': '#94a3b8'
}

labels = {
    'cavity_1_top_pitch_down': 'Cavity 1 (+Y Top -> -Y Pitch)',
    'cavity_2_right_yaw_left': 'Cavity 2 (+Z Right -> -Z Yaw)',
    'cavity_3_bottom_pitch_up': 'Cavity 3 (-Y Bot -> +Y Pitch)',
    'cavity_4_left_yaw_right': 'Cavity 4 (-Z Left -> +Z Yaw)',
    'cavities_1_2_diagonal_45deg': 'Diag 1+2 (-135° Steering)',
    'cavities_1_4_diagonal_135deg': 'Diag 1+4 (+135° Steering)',
    'all_4_cavities_axial_stiffening': 'All 4 Cavities (Pure Axial)'
}

for cname in colors.keys():
    grp = df_steps[df_steps['case_name'] == cname]
    if len(grp) > 0:
        col = colors[cname]
        lbl = labels[cname]
        ax_polar.plot(grp['w_tip_mm'].astype(float), grp['v_tip_mm'].astype(float), '-o', color=col, lw=2.2, ms=5, label=lbl)

for r in [10, 20, 30]:
    circle = plt.Circle((0, 0), r, color='#334155', fill=False, ls='--', lw=0.8)
    ax_polar.add_patch(circle)
    ax_polar.text(r * 0.707, r * 0.707, f'{r} mm', color='#64748b', fontsize=8)

ax_polar.axhline(0, color='#475569', lw=1.0, ls=':')
ax_polar.axvline(0, color='#475569', lw=1.0, ls=':')
ax_polar.set_xlim(-32, 32)
ax_polar.set_ylim(-32, 32)
ax_polar.set_aspect('equal')
ax_polar.set_xlabel('Lateral Tip Deflection w [mm] (Yaw Axis, Z)', color='#cbd5e1', fontsize=10)
ax_polar.set_ylabel('Vertical Tip Deflection v [mm] (Pitch Axis, Y)', color='#cbd5e1', fontsize=10)
ax_polar.set_title('100% Symmetric Omnidirectional Polar Workspace (Y-Z)', color='#f8fafc', fontsize=11, fontweight='bold')
ax_polar.legend(loc='upper right', fontsize=7.2, facecolor='#1e293b', edgecolor='#475569')

# --- 5. Bottom Middle: Net 3D Deflection delta_3D vs Pressure ---
ax_delta = fig.add_subplot(gs[1, 1])
ax_delta.set_facecolor('#0f172a')

for cname in colors.keys():
    grp = df_steps[df_steps['case_name'] == cname]
    if len(grp) > 0:
        col = colors[cname]
        lbl = labels[cname]
        p_col = 'P_step_kPa' if 'P_step_kPa' in grp.columns else 'P_kPa'
        ax_delta.plot(grp[p_col].astype(float), grp['delta_3d_mm'].astype(float), '-o', color=col, lw=2.2, ms=5, label=lbl)

ax_delta.set_xlabel('Chamber Pressure P [kPa]', color='#cbd5e1', fontsize=10)
ax_delta.set_ylabel('Net 3D Tip Deflection $\\delta_{3D}$ [mm]', color='#cbd5e1', fontsize=10)
ax_delta.set_title(f'Nonlinear Pressure vs. Tip Deflection (0 to {p_max:.0f} kPa)', color='#f8fafc', fontsize=11, fontweight='bold')
ax_delta.grid(True, color='#1e293b', ls='--', lw=0.8)
ax_delta.legend(loc='upper left', fontsize=7.5, facecolor='#1e293b', edgecolor='#475569')

# --- 6. Bottom Right: Summary Data Table at P_max ---
ax_tab = fig.add_subplot(gs[1, 2])
ax_tab.axis('off')
ax_tab.set_title(f'Quantitative 7-Mode Comparison at P = {p_max:.1f} kPa', color='#f8fafc', fontsize=11, fontweight='bold', pad=12)

table_data = []
for idx, row in df_sum.iterrows():
    c_short = row['case_name'].replace('cavity_', 'C').replace('cavities_', 'C').replace('all_4_cavities_', 'All-4')
    c_short = c_short[:15]
    table_data.append([
        c_short,
        f"{float(row['u_tip_mm']):+.2f}",
        f"{float(row['v_tip_mm']):+.2f}",
        f"{float(row['w_tip_mm']):+.2f}",
        f"{float(row['delta_3d_mm']):.2f}",
        f"{float(row['phi_bend_deg']):+.1f}°",
        f"{float(row['theta_deg']):.2f}°"
    ])

col_labels = ['Mode', 'u [mm]', 'v [mm]', 'w [mm]', 'δ [mm]', 'φ [deg]', 'θ [deg]']
tab = ax_tab.table(cellText=table_data, colLabels=col_labels, loc='center', cellLoc='center')
tab.auto_set_font_size(False)
tab.set_fontsize(8.0)
tab.scale(1.04, 1.65)

for (r, c), cell in tab.get_celld().items():
    cell.set_edgecolor('#334155')
    if r == 0:
        cell.set_facecolor('#1e293b')
        cell.set_text_props(color='#38bdf8', fontweight='bold')
    else:
        cell.set_facecolor('#0f172a' if r % 2 == 0 else '#1e293b')
        cell.set_text_props(color='#e2e8f0')

dashboard_out = os.path.join(OUT_DIR, "comsol_tapered_4cavity_dashboard.png")
plt.savefig(dashboard_out, dpi=180, facecolor=fig.get_facecolor(), edgecolor='none')
print(f"Saved updated dashboard to {dashboard_out}")
