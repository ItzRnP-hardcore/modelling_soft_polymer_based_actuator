import matplotlib.pyplot as plt
import numpy as np
import os
import pandas as pd

# Load trajectory data from centerline inspector
# Data from InspectDeformedCenterline:
# Pos 1: Xc = 36.0, Yc = -46.0, P = 100 kPa
pos1_pts = [
    (0.0, 0.0, 0.0), (15.0, 14.86, -0.23), (30.0, 29.64, -1.54), (45.0, 44.14, -4.62),
    (60.0, 57.80, -10.40), (75.0, 69.24, -19.85), (90.0, 75.87, -33.14), (105.0, 74.70, -47.94),
    (120.0, 65.83, -59.84), (135.0, 52.66, -66.65), (150.0, 38.14, -69.53), (165.0, 23.37, -69.72),
    (180.0, 10.10, -63.71), (195.0, 16.67, -53.12), (200.0, 21.72, -53.47), (205.0, 26.54, -54.94),
    (210.0, 31.19, -56.87), (215.0, 35.83, -58.78), (220.0, 40.46, -60.67)
]

# Pos 2: Xc = 34.0, Yc = -48.0, P = 98.6 kPa
pos2_pts = [
    (0.0, 0.0, 0.0), (15.0, 14.86, -0.25), (30.0, 29.63, -1.65), (45.0, 44.10, -4.88),
    (60.0, 57.68, -10.87), (75.0, 68.96, -20.51), (90.0, 75.39, -33.89), (105.0, 74.16, -48.67),
    (120.0, 65.43, -60.66), (135.0, 52.42, -67.79), (150.0, 37.99, -71.08), (165.0, 23.23, -71.61),
    (180.0, 9.91, -65.73), (195.0, 16.00, -54.73), (200.0, 21.06, -54.79), (205.0, 25.95, -56.02),
    (210.0, 30.68, -57.70), (215.0, 35.42, -59.37), (220.0, 40.14, -61.01)
]

# Pos 3: Xc = 32.0, Yc = -52.0, P = 94.3 kPa
pos3_pts = [
    (0.0, 0.0, 0.0), (15.0, 14.87, -0.17), (30.0, 29.66, -1.36), (45.0, 44.19, -4.33),
    (60.0, 57.87, -10.11), (75.0, 69.17, -19.76), (90.0, 75.28, -33.31), (105.0, 73.27, -48.04),
    (120.0, 63.62, -59.36), (135.0, 50.05, -65.44), (145.0, 40.33, -67.37), (150.0, 35.41, -67.96),
    (155.0, 30.49, -68.38), (160.0, 25.55, -68.62), (165.0, 20.61, -68.59), (180.0, 6.64, -64.15),
    (195.0, 11.79, -53.11), (200.0, 16.87, -53.37), (205.0, 21.66, -55.00), (210.0, 26.19, -57.22),
    (215.0, 30.69, -59.47), (220.0, 35.19, -61.65)
]

fig, axes = plt.subplots(1, 3, figsize=(18, 6), sharex=True, sharey=True)

cases_data = [
    ("Pos 1: Xc=36, Yc=-46\nP=100 kPa | Fn=0.000 N", 36.0, -46.0, 0.0, 0.0, pos1_pts, "tab:blue"),
    ("Pos 2: Xc=34, Yc=-48\nP=98.6 kPa | Fn=0.013 N", 34.0, -48.0, 0.383, 0.323, pos2_pts, "tab:orange"),
    ("Pos 3: Xc=32, Yc=-52\nP=94.3 kPa | Fn=0.057 N (ACTIVE CONTACT!)", 32.0, -52.0, 0.270, 2.585, pos3_pts, "tab:green")
]

theta = np.linspace(0, 2*np.pi, 200)
R_cyl = 8.0

for ax, (title, xc, yc, ux, uy, pts, col) in zip(axes, cases_data):
    # Actuator spine
    s = [p[0] for p in pts]
    x = [p[1] for p in pts]
    y = [p[2] for p in pts]
    ax.plot(x, y, '-o', color=col, linewidth=2.5, markersize=4, label='Actuator Centerline')
    
    # Outer radius envelope
    # R(s) = 11.5 - (11.5 - 3.2)*(s/220)
    # Approximate normal envelope
    for i in range(len(pts)-1):
        s_mid = 0.5*(s[i] + s[i+1])
        r_mid = 11.5 - (11.5 - 3.2)*(s_mid/220.0)
        dx = x[i+1] - x[i]
        dy = y[i+1] - y[i]
        L = np.hypot(dx, dy)
        if L > 1e-4:
            nx = -dy / L
            ny = dx / L
            xm = 0.5*(x[i] + x[i+1])
            ym = 0.5*(y[i] + y[i+1])
            # Draw envelope line
            ax.plot([xm - nx*r_mid, xm + nx*r_mid], [ym - ny*r_mid, ym + ny*r_mid], color=col, alpha=0.15, linewidth=3)

    # Initial Cylinder circle
    xc_init = xc + R_cyl*np.cos(theta)
    yc_init = yc + R_cyl*np.sin(theta)
    ax.plot(xc_init, yc_init, 'k--', linewidth=1.5, alpha=0.6, label='Initial Cyl (R=8mm)')
    
    # Deformed / Displaced Cylinder circle
    xc_def = (xc + ux) + R_cyl*np.cos(theta)
    yc_def = (yc + uy) + R_cyl*np.sin(theta)
    ax.fill(xc_def, yc_def, color='royalblue', alpha=0.35, label=f'Displaced Cyl (u={np.hypot(ux,uy):.2f}mm)')
    ax.plot(xc_def, yc_def, 'b-', linewidth=2)
    ax.plot(xc + ux, yc + uy, 'bx', markersize=8)

    # Base clamp
    ax.plot([0], [0], 'ks', markersize=8, label='Actuator Base (0,0)')

    ax.set_title(title, fontsize=12, fontweight='bold')
    ax.set_xlabel('X [mm]', fontsize=11)
    ax.set_ylabel('Y [mm]', fontsize=11)
    ax.grid(True, linestyle=':', alpha=0.6)
    ax.set_aspect('equal')
    ax.legend(loc='upper right', fontsize=8)

plt.suptitle('COMSOL 6.4: Loop Caging Deformed Geometry & Contact Engagement Analysis', fontsize=14, fontweight='bold')
plt.tight_layout()

out_path = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\loop_caging_optimization\loop_caging_centerline_analysis.png"
plt.savefig(out_path, dpi=200)
print(f"Saved analysis plot to {out_path}")

brain_path = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562\loop_caging_centerline_analysis.png"
import shutil
shutil.copy2(out_path, brain_path)
print(f"Copied to brain: {brain_path}")

