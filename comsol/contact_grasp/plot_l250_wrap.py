import re
import numpy as np
import matplotlib.pyplot as plt
import os

log_path = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562\.system_generated\tasks\task-5239.log"
with open(log_path, "r", encoding="utf-8") as f:
    text = f.read()

pat = re.compile(r">> L250_PT: s=([0-9.]+), X=([0-9.-]+), Y=([0-9.-]+)")
s_list, x_list, y_list = [], [], []

for m in pat.finditer(text):
    s_list.append(float(m.group(1)))
    x_list.append(float(m.group(2)))
    y_list.append(float(m.group(3)))

s = np.array(s_list)
x = np.array(x_list)
y = np.array(y_list)

# Initial and deformed cylinder parameters
R_cyl = 8.0
Xc_init, Yc_init = 32.0, -52.0
ux, uy = 1.065, 3.329
Xc_def, Yc_def = Xc_init + ux, Yc_init + uy

# Angular sweep
dx = x - Xc_def
dy = y - Yc_def
angles_deg = np.rad2deg(np.arctan2(dy, dx))
unwrapped = np.unwrap(np.arctan2(dy, dx))
angular_span = np.rad2deg(unwrapped.max() - unwrapped.min())
tip_overlap = np.rad2deg(unwrapped[-1] - unwrapped.min())
print(f"Angular circumferential span: {angular_span:.1f} degrees (> 270 deg / 3/4 circumference!)")
print(f"Tip hook-back cinch overlap: {tip_overlap:.1f} degrees")

# Actuator radius taper profile: R_base = 5.5, R_tip = 3.0 mm
L_act = 250.0
r_act = 5.5 - (5.5 - 3.0) * (s / L_act)

# Create high-resolution plot
fig, ax = plt.subplots(figsize=(11, 9.5), dpi=300)

# Draw initial cylinder
theta = np.linspace(0, 2*np.pi, 200)
ax.plot(Xc_init + R_cyl*np.cos(theta), Yc_init + R_cyl*np.sin(theta), 
        '--', color='gray', linewidth=1.5, label=f'Initial Cylinder ($R=8$ mm, $X_c=32, Y_c=-52$)')

# Draw displaced cylinder
cyl_circle = plt.Circle((Xc_def, Yc_def), R_cyl, color='#3b82f6', alpha=0.35, zorder=3,
                        label=f'Displaced Cylinder ($u_x=+1.07$, $u_y=+3.33$ mm, $F_n=69.8$ mN)')
ax.add_patch(cyl_circle)
ax.plot(Xc_def + R_cyl*np.cos(theta), Yc_def + R_cyl*np.sin(theta), 
        '-', color='#1d4ed8', linewidth=2.5, zorder=4)
ax.plot(Xc_def, Yc_def, 'kx', markersize=10, markeredgewidth=2, zorder=5)

# Plot centerline with color gradient for s
scatter = ax.scatter(x, y, c=s, cmap='plasma', s=35, zorder=6, label='Actuator Centerline ($s=0 \\rightarrow 250$ mm)')
cbar = plt.colorbar(scatter, ax=ax, fraction=0.046, pad=0.04)
cbar.set_label('Actuator Arc Length $s$ [mm]', fontsize=11, fontweight='bold')

# Plot tube envelope by offsetting normal
dx_dt = np.gradient(x)
dy_dt = np.gradient(y)
norm = np.hypot(dx_dt, dy_dt)
nx = -dy_dt / norm
ny = dx_dt / norm

x_upper = x + nx * r_act
y_upper = y + ny * r_act
x_lower = x - nx * r_act
y_lower = y - ny * r_act

ax.plot(x_upper, y_upper, color='#059669', linewidth=1.8, linestyle='-', label='Actuator Outer Enclosure Envelope')
ax.plot(x_lower, y_lower, color='#059669', linewidth=1.8, linestyle='-')
ax.fill_between(x, y_lower, y_upper, color='#10b981', alpha=0.15)

# Annotations
ax.annotate('Base Anchor ($s=0$ mm)', xy=(0, 0), xytext=(-12, 12),
            arrowprops=dict(facecolor='black', shrink=0.08, width=1.5, headwidth=6),
            fontsize=10, fontweight='bold')

ax.annotate(f'Distal Hook Tip ($s=250$ mm)\nSpan = {angular_span:.1f}° (> 3/4 circumference!)\nHook Overlap = {tip_overlap:.1f}°', 
            xy=(x[-1], y[-1]), xytext=(x[-1]-20, y[-1]-22),
            arrowprops=dict(facecolor='#dc2626', shrink=0.08, width=2, headwidth=8),
            fontsize=10, fontweight='bold', color='#dc2626',
            bbox=dict(boxstyle="round,pad=0.3", fc="#fee2e2", ec="#ef4444", lw=1.5))

ax.annotate(f'Active Contact Cinch Zone\n$F_n = 69.8$ mN, $F_{{fric}} = 34.9$ mN\nPeak $T_n = 2.00$ kPa',
            xy=(x[46], y[46]), xytext=(x[46]-45, y[46]-12),
            arrowprops=dict(facecolor='#1e40af', shrink=0.08, width=1.5, headwidth=6),
            fontsize=9, fontweight='bold', color='#1e40af',
            bbox=dict(boxstyle="round,pad=0.3", fc="#dbeafe", ec="#3b82f6", lw=1.2))

# Draw angle sweep arc indicator around cylinder
arc_theta = np.linspace(np.radians(124.2), np.radians(124.2 - 272.8), 100)
arc_r = 13.0
ax.plot(Xc_def + arc_r*np.cos(arc_theta), Yc_def + arc_r*np.sin(arc_theta),
        ':', color='#7c3aed', linewidth=2.5, label=f'Circumferential Wrap Span ({angular_span:.1f}°)')

ax.set_aspect('equal', 'box')
ax.set_xlabel('Global X Coordinate [mm]', fontsize=12, fontweight='bold')
ax.set_ylabel('Global Y Coordinate [mm]', fontsize=12, fontweight='bold')
ax.set_title('Form-Closure Loop Caging Analysis: $L=250$ mm Actuator ($P=71.1$ kPa)\nCircumferential Enclosure Span = 273° with 98° Distal Hook Cinch around $R=8.0$ mm Cylinder',
             fontsize=12, fontweight='bold', pad=15)
ax.grid(True, linestyle=':', alpha=0.6)
ax.legend(loc='upper right', framealpha=0.95, fontsize=8.5)

out_fig = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562\loop_caging_2d_trajectory_wrap.png"
plt.tight_layout()
plt.savefig(out_fig)
plt.savefig(r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\loop_caging_optimization\loop_caging_2d_trajectory_wrap.png")
print("Saved figure successfully!")