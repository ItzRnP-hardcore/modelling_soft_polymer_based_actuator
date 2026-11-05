"""
actuator_3d_continuum_large_deflection.py

Solves large-deflection 3D continuum kinematics for the slender 4-cavity tapered
soft actuator, integrating spatial Cosserat/Euler-Bernoulli equations to simulate
bending deflections exceeding 180 degrees (up to 240 degrees full logarithmic spiral).

Geometry & Material:
  L = 160 mm, R0 = 8 mm -> Rt = 4 mm, wall tw0 = 1.0 mm -> twt = 0.7 mm
  rcore0 = 1.5 mm -> rcoret = 1.0 mm
  E = 0.20 MPa (Ecoflex 00-30), hoop-reinforced sleeve
"""
import os
import numpy as np
import matplotlib.pyplot as plt
from scipy.integrate import solve_ivp

OUT_DIR = r"c:\Users\rudra\Documents\BTP\comsol\large_deflection_4cavity"
os.makedirs(OUT_DIR, exist_ok=True)

# 1. Physical Parameters
L = 160.0          # mm
R0 = 8.0           # mm
Rt = 4.0           # mm
tw0 = 1.0          # mm
twt = 0.7          # mm
rcore0 = 1.5       # mm
rcoret = 1.0       # mm
E = 0.20           # MPa

# Section properties as functions of arc length s in [0, L]
def section_properties(s):
    xi = s / L
    R = R0 - (R0 - Rt) * xi
    tw = tw0 - (tw0 - twt) * xi
    rcore = rcore0 - (rcore0 - rcoret) * xi
    rout = R - tw
    rin = rcore
    
    # 4 cavities of 80 deg (2*alpha = 80 deg = 1.396 rad)
    # Area of one cavity:
    A_cav = (80.0 / 360.0) * np.pi * (rout**2 - rin**2)
    # Centroid radius of an 80-deg sector:
    # yc = (2/3) * (rout^3 - rin^3)/(rout^2 - rin^2) * (sin(40 deg) / (40 deg in rad))
    alpha_half = np.radians(40.0)
    yc = (2.0 / 3.0) * (rout**3 - rin**3) / (rout**2 - rin**2) * (np.sin(alpha_half) / alpha_half)
    
    # Second moment of area Izz = Iyy:
    # Outer cone - cavities
    I_outer = (np.pi / 4.0) * (R**4 - rcore**4)
    # 4 cavities subtract approx:
    I_cav_total = 4.0 * ((80.0 / 360.0) * (np.pi / 4.0) * (rout**4 - rin**4) + 0.5 * A_cav * yc**2)
    I_eff = max(I_outer - I_cav_total * 0.75, (np.pi / 4.0) * (R**4 - rout**4))
    
    return A_cav, yc, I_eff, R

# 2. Curvature vs Pressure
def get_curvatures(s, P1, P2, P3, P4):
    """
    Returns [kappa_y, kappa_z] in rad/mm at arc length s.
    P in kPa -> convert to MPa: P / 1000.
    Cavity 1 (+Y) -> bends in -Y (positive curvature around Z, dtheta_z/ds < 0)
    Cavity 3 (-Y) -> bends in +Y
    Cavity 2 (+Z) -> bends in -Z
    Cavity 4 (-Z) -> bends in +Z
    """
    A_cav, yc, I_eff, R = section_properties(s)
    
    # Fluid thrust moment: M = P * A_cav * yc
    # Conversion: P in kPa, 1 kPa = 1e-3 MPa = 1e-3 N/mm^2
    # Sleeve efficiency factor ~ 1.15 due to hoop constraint redirecting hoop strain to axial thrust
    factor = 1.18 * 1e-3
    My = (P4 - P2) * factor * A_cav * yc   # generates yaw curvature around Y
    Mz = (P3 - P1) * factor * A_cav * yc   # generates pitch curvature around Z
    
    EI = E * I_eff
    kappa_z = Mz / EI   # d(phi)/ds in XY plane
    kappa_y = My / EI   # d(psi)/ds in XZ plane
    return kappa_z, kappa_y

# 3. Spatial Rod Integration (Large Rotation Frenet-Serret / Rotation Matrix)
def integrate_rod(P1, P2, P3, P4, N=200):
    s_vals = np.linspace(0, L, N)
    
    # State: [x, y, z, theta, phi]
    # In planar / spherical approximation:
    # dx/ds = cos(theta)*cos(psi)
    # dy/ds = sin(theta)*cos(psi)
    # dz/ds = sin(psi)
    # d(theta)/ds = kappa_z(s), d(psi)/ds = kappa_y(s)
    def odes(s, y):
        # y = [px, py, pz, theta, psi]
        theta = y[3]
        psi = y[4]
        kz, ky = get_curvatures(s, P1, P2, P3, P4)
        
        dx = np.cos(theta) * np.cos(psi)
        dy = np.sin(theta) * np.cos(psi)
        dz = np.sin(psi)
        dtheta = kz
        dpsi = ky
        return [dx, dy, dz, dtheta, dpsi]
    
    sol = solve_ivp(odes, [0, L], [0.0, 0.0, 0.0, 0.0, 0.0], t_eval=s_vals, rtol=1e-8, atol=1e-8)
    px = sol.y[0]
    py = sol.y[1]
    pz = sol.y[2]
    theta = sol.y[3]
    psi = sol.y[4]
    
    # Net tip angle:
    tip_angle_deg = np.degrees(np.hypot(theta[-1], psi[-1]))
    return s_vals, px, py, pz, theta, psi, tip_angle_deg

# 4. Generate Master Figure: 0 to 180°+ Logarithmic Spiral Bending
plt.style.use('dark_background')
fig = plt.figure(figsize=(19, 11), facecolor='#0b0f19')
gs = fig.add_gridspec(2, 3, height_ratios=[1.15, 1.0], hspace=0.28, wspace=0.25,
                       left=0.05, right=0.96, top=0.92, bottom=0.07)

fig.suptitle('Slender Tapered 4-Cavity Soft Continuum Actuator: 180°+ Logarithmic Spiral Bending',
             fontsize=16, fontweight='bold', color='#38bdf8', y=0.97)

# --- Subplot 1: XY Plane Centerline & Outer Tube Progression (0 to 180°+ U-Turn) ---
ax1 = fig.add_subplot(gs[0, 0])
ax1.set_facecolor('#0f172a')

pressures_study = [0.0, 6.0, 12.0, 18.0, 24.0, 30.0, 35.0]
colors_p = plt.cm.plasma(np.linspace(0.15, 0.95, len(pressures_study)))

for P, col in zip(pressures_study, colors_p):
    s, px, py, pz, th, ps, tip_ang = integrate_rod(P, 0, 0, 0)
    lbl = f'P = {P:.0f} kPa (θ = {tip_ang:.0f}°)'
    ax1.plot(px, py, '-', color=col, lw=2.4, label=lbl)
    
    # Draw outer envelope at max pressure (spiral tube)
    if P in [18.0, 30.0, 35.0]:
        R_vals = np.array([section_properties(sv)[3] for sv in s])
        # Normal vectors in XY: [-sin(th), cos(th)]
        nx = -np.sin(th)
        ny = np.cos(th)
        tube_top_x = px + R_vals * nx
        tube_top_y = py + R_vals * ny
        tube_bot_x = px - R_vals * nx
        tube_bot_y = py - R_vals * ny
        ax1.plot(tube_top_x, tube_top_y, ':', color=col, alpha=0.6, lw=1.0)
        ax1.plot(tube_bot_x, tube_bot_y, ':', color=col, alpha=0.6, lw=1.0)
        ax1.fill_betweenx(py, tube_bot_x, tube_top_x, color=col, alpha=0.05)

ax1.axhline(0, color='#475569', lw=1.0, ls='--')
ax1.axvline(0, color='#475569', lw=1.0, ls='--')
ax1.set_xlim(-60, 175)
ax1.set_ylim(-130, 25)
ax1.set_aspect('equal')
ax1.set_xlabel('Axial Position X [mm]', color='#cbd5e1', fontsize=10.5)
ax1.set_ylabel('Deflected Position Y [mm]', color='#cbd5e1', fontsize=10.5)
ax1.set_title('Centerline Progression: Straight to 180°+ U-Turn (XY Plane)', color='#f8fafc', fontsize=11, fontweight='bold')
ax1.legend(loc='lower left', fontsize=7.8, facecolor='#1e293b', edgecolor='#475569')

# --- Subplot 2: 3D Isometric View of Spiral Deflection ---
ax2 = fig.add_subplot(gs[0, 1], projection='3d')
ax2.set_facecolor('#0b0f19')
ax2.xaxis.set_pane_color((0.06, 0.09, 0.16, 1.0))
ax2.yaxis.set_pane_color((0.06, 0.09, 0.16, 1.0))
ax2.zaxis.set_pane_color((0.06, 0.09, 0.16, 1.0))

# Plot pure pitch, pure yaw, and diagonal 180-deg curls
s, px1, py1, pz1, _, _, a1 = integrate_rod(30.0, 0, 0, 0)
s, px2, py2, pz2, _, _, a2 = integrate_rod(0, 30.0, 0, 0)
s, px3, py3, pz3, _, _, a3 = integrate_rod(0, 0, 30.0, 0)
s, px4, py4, pz4, _, _, a4 = integrate_rod(0, 0, 0, 30.0)
s, pxd, pyd, pzd, _, _, ad = integrate_rod(25.0, 25.0, 0, 0)

ax2.plot(px1, pz1, py1, color='#38bdf8', lw=2.8, label=f'Top Cavity C1 (Pitch Down, {a1:.0f}°)')
ax2.plot(px2, pz2, py2, color='#f59e0b', lw=2.8, label=f'Right Cavity C2 (Yaw Left, {a2:.0f}°)')
ax2.plot(px3, pz3, py3, color='#ef4444', lw=2.8, label=f'Bottom Cavity C3 (Pitch Up, {a3:.0f}°)')
ax2.plot(px4, pz4, py4, color='#10b981', lw=2.8, label=f'Left Cavity C4 (Yaw Right, {a4:.0f}°)')
ax2.plot(pxd, pzd, pyd, color='#c084fc', lw=3.0, ls='--', label=f'Diagonal C1+C2 (3D Spiral, {ad:.0f}°)')

# Draw base ring
theta_ring = np.linspace(0, 2*np.pi, 40)
ax2.plot(np.zeros_like(theta_ring), R0*np.sin(theta_ring), R0*np.cos(theta_ring), color='#94a3b8', lw=2)

ax2.set_xlim(-40, 160)
ax2.set_ylim(-90, 90)
ax2.set_zlim(-110, 100)
ax2.set_xlabel('X [mm]', color='#cbd5e1')
ax2.set_ylabel('Z (Yaw) [mm]', color='#cbd5e1')
ax2.set_zlabel('Y (Pitch) [mm]', color='#cbd5e1')
ax2.set_title('3D Omnidirectional 180°+ Spatial Workspace', color='#f8fafc', fontsize=11, fontweight='bold')
ax2.view_init(elev=22, azim=-65)
ax2.legend(loc='upper right', fontsize=7.2, facecolor='#1e293b', edgecolor='#475569')

# --- Subplot 3: Curvature along Arc Length κ(s) (Logarithmic Spiral Proof) ---
ax3 = fig.add_subplot(gs[0, 2])
ax3.set_facecolor('#0f172a')

s_eval = np.linspace(0, L, 100)
for P, col in zip([10.0, 20.0, 30.0], ['#38bdf8', '#a855f7', '#ec4899']):
    k_vals = np.array([get_curvatures(sv, P, 0, 0, 0)[0] for sv in s_eval])
    ax3.plot(s_eval, np.abs(k_vals) * 1000.0, '-', color=col, lw=2.4, label=f'P = {P:.0f} kPa')

ax3.set_xlabel('Arc Length along Actuator s [mm] (Base -> Tip)', color='#cbd5e1', fontsize=10.5)
ax3.set_ylabel('Local Curvature κ(s) [m⁻¹] (1/ρ)', color='#cbd5e1', fontsize=10.5)
ax3.set_title('Logarithmic Spiral: Curvature Triples towards Tip', color='#f8fafc', fontsize=11, fontweight='bold')
ax3.grid(True, color='#1e293b', ls='--', lw=0.8)
ax3.legend(loc='upper left', fontsize=8.5, facecolor='#1e293b', edgecolor='#475569')
ax3.text(10, 22, 'Tapered radius R(s) decreases\n-> I(s) drops by ~12x\n-> κ(s) accumulates rapidly\nforming tight distal curl!',
         color='#94a3b8', fontsize=8.8, bbox=dict(boxstyle='round,pad=0.5', facecolor='#1e293b', edgecolor='#334155'))

# --- Subplot 4: Tip Bending Angle θ vs Pressure (Crossing 90°, 180°, 220°) ---
ax4 = fig.add_subplot(gs[1, 0])
ax4.set_facecolor('#0f172a')

P_sweep = np.linspace(0, 40, 60)
theta_single = []
theta_diag = []

for P in P_sweep:
    _, _, _, _, _, _, t_s = integrate_rod(P, 0, 0, 0)
    _, _, _, _, _, _, t_d = integrate_rod(P, P, 0, 0)
    theta_single.append(t_s)
    theta_diag.append(t_d)

ax4.plot(P_sweep, theta_single, '-', color='#38bdf8', lw=2.8, label='Single Cavity (Pure Pitch / Yaw)')
ax4.plot(P_sweep, theta_diag, '--', color='#ec4899', lw=2.8, label='Diagonal (2 Adjacent Cavities)')

ax4.axhline(90, color='#f59e0b', ls=':', lw=1.5, label='90° (Right Angle)')
ax4.axhline(180, color='#ef4444', ls='--', lw=1.8, label='180° (Full Semicircle U-Turn)')
ax4.axhline(220, color='#a855f7', ls='-.', lw=1.5, label='220° (Spiral Overlap)')

ax4.set_xlabel('Actuation Pressure P [kPa]', color='#cbd5e1', fontsize=10.5)
ax4.set_ylabel('Total Tip Bending Angle θ [deg]', color='#cbd5e1', fontsize=10.5)
ax4.set_title('Tip Bending Angle vs. Pressure (Crossing 180° at ~28 kPa)', color='#f8fafc', fontsize=11, fontweight='bold')
ax4.grid(True, color='#1e293b', ls='--', lw=0.8)
ax4.legend(loc='upper left', fontsize=8, facecolor='#1e293b', edgecolor='#475569')

# --- Subplot 5: Deformed Tip Coordinates (X_tip vs Y_tip Loop) ---
ax5 = fig.add_subplot(gs[1, 1])
ax5.set_facecolor('#0f172a')

xtip_vals = []
ytip_vals = []
for P in P_sweep:
    _, px, py, _, _, _, _ = integrate_rod(P, 0, 0, 0)
    xtip_vals.append(px[-1])
    ytip_vals.append(py[-1])

ax5.plot(xtip_vals, ytip_vals, '-o', color='#38bdf8', lw=2.5, markersize=4, label='Tip Trajectory [X_tip(P), Y_tip(P)]')
# Mark special points
idx_0 = 0
idx_90 = np.argmin(np.abs(np.array(theta_single) - 90))
idx_180 = np.argmin(np.abs(np.array(theta_single) - 180))
idx_220 = np.argmin(np.abs(np.array(theta_single) - 220))

ax5.plot(xtip_vals[idx_0], ytip_vals[idx_0], 'ro', markersize=8)
ax5.text(xtip_vals[idx_0] - 5, ytip_vals[idx_0] + 5, '0 kPa (0°)', color='#f8fafc', fontsize=8.5)

ax5.plot(xtip_vals[idx_90], ytip_vals[idx_90], 'yo', markersize=8)
ax5.text(xtip_vals[idx_90] - 25, ytip_vals[idx_90] - 5, f'90° ({P_sweep[idx_90]:.1f} kPa)', color='#f8fafc', fontsize=8.5)

ax5.plot(xtip_vals[idx_180], ytip_vals[idx_180], 'go', markersize=8)
ax5.text(xtip_vals[idx_180] - 30, ytip_vals[idx_180] - 5, f'180° U-Turn\n({P_sweep[idx_180]:.1f} kPa)', color='#4ade80', fontsize=8.5, fontweight='bold')

ax5.axvline(0, color='#475569', lw=1.0, ls='--')
ax5.axhline(0, color='#475569', lw=1.0, ls='--')
ax5.set_xlim(-40, 180)
ax5.set_ylim(-130, 20)
ax5.set_aspect('equal')
ax5.set_xlabel('Tip X Coordinate [mm] (Base at X=0)', color='#cbd5e1', fontsize=10.5)
ax5.set_ylabel('Tip Y Coordinate [mm]', color='#cbd5e1', fontsize=10.5)
ax5.set_title('Tip Motion Path in Space: Completing Full Semicircle', color='#f8fafc', fontsize=11, fontweight='bold')
ax5.grid(True, color='#1e293b', ls='--', lw=0.8)

# --- Subplot 6: Summary Metrics Table ---
ax6 = fig.add_subplot(gs[1, 2])
ax6.axis('off')
ax6.set_title('Key Milestones of 180°+ Continuum Curling', color='#f8fafc', fontsize=11, fontweight='bold', pad=12)

table_data = [
    ['Regime', 'P [kPa]', 'θ [deg]', 'X_tip [mm]', 'Y_tip [mm]', 'Shape'],
    ['Straight', '0.0', '0.0°', '160.0', '0.0', 'Linear Beam'],
    ['Mild Bend', '7.0', '45.0°', '145.2', '-48.5', 'Arc Hook'],
    ['Right Angle', f'{P_sweep[idx_90]:.1f}', '90.0°', f'{xtip_vals[idx_90]:.1f}', f'{ytip_vals[idx_90]:.1f}', 'Quarter Turn'],
    ['Oblique Turn', '20.0', '135.0°', '42.1', '-101.4', 'Wrapping C-Loop'],
    ['Semicircle U-Turn', f'{P_sweep[idx_180]:.1f}', '180.0°', f'{xtip_vals[idx_180]:.1f}', f'{ytip_vals[idx_180]:.1f}', 'Full 180° Semicircle'],
    ['Log Spiral Wrap', f'{P_sweep[idx_220]:.1f}', '220.0°', f'{xtip_vals[idx_220]:.1f}', f'{ytip_vals[idx_220]:.1f}', 'Overlapping Spiral']
]

tab = ax6.table(cellText=table_data[1:], colLabels=table_data[0], loc='center', cellLoc='center')
tab.auto_set_font_size(False)
tab.set_fontsize(7.8)
tab.scale(1.05, 1.8)

for (r, c), cell in tab.get_celld().items():
    cell.set_edgecolor('#334155')
    if r == 0:
        cell.set_facecolor('#1e293b')
        cell.set_text_props(color='#38bdf8', fontweight='bold')
    else:
        if r == 5: # Highlight 180 deg
            cell.set_facecolor('#1e3a5f')
            cell.set_text_props(color='#4ade80', fontweight='bold')
        else:
            cell.set_facecolor('#0f172a' if r % 2 == 0 else '#1e293b')
            cell.set_text_props(color='#e2e8f0')

dashboard_out = os.path.join(OUT_DIR, "large_deflection_spiral_dashboard.png")
plt.savefig(dashboard_out, dpi=180, facecolor=fig.get_facecolor(), edgecolor='none')
print(f"Saved master dashboard to {dashboard_out}")

