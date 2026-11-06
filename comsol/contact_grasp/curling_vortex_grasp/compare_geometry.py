import numpy as np
import matplotlib.pyplot as plt

# Centerline points of tapered 4-cavity at P = 40 kPa (approximate polynomial fit from FEA tip & curvature)
# The actuator length is 140 mm.
s = np.linspace(0, 140, 100)
# Curvature kappa(s) increases from base to tip: kappa(s) = k0 + k1*(s/L)^2
# At s=140, total angle is 186 deg = 3.25 rad.
# Let's integrate theta(s) = integral kappa(s') ds'
# kappa(s) ~ 0.01 + 0.035*(s/140)**2
# integral kappa ds = 0.01*s + 0.035/3 * s**3 / 140**2
# At s=140: 0.01*140 + 0.035/3 * 140 = 1.4 + 1.63 = 3.03 rad ~ 174 deg.
kappa = 0.011 + 0.036 * (s / 140.0)**2
theta = np.zeros_like(s)
for i in range(1, len(s)):
    theta[i] = theta[i-1] + 0.5 * (kappa[i] + kappa[i-1]) * (s[i] - s[i-1])

x = np.zeros_like(s)
y = np.zeros_like(s)
for i in range(1, len(s)):
    ds = s[i] - s[i-1]
    th_mid = 0.5 * (theta[i] + theta[i-1])
    x[i] = x[i-1] + ds * np.cos(th_mid)
    y[i] = y[i-1] - ds * np.sin(th_mid)

print(f"Computed Free Bending Tip: x={x[-1]:.1f} mm, y={y[-1]:.1f} mm, angle={np.degrees(theta[-1]):.1f} deg")

# Compare with cylinder positions:
# 1. Previous "Distal" position: Xc = 42, Yc = -25 (R=12)
# 2. Optimal Curling Vortex position: Xc = 48, Yc = -55 (R=10)

fig, ax = plt.subplots(figsize=(8, 7), dpi=150)
ax.plot(x, y, 'b-', lw=3, label='Actuator Centerline (Free Curl, P=40 kPa)')

# Actuator thickness envelope
r_act = 9.5 - (9.5 - 3.8) * (s / 140.0)
# Draw normal offset
# normal vector: (-sin(th), -cos(th))
nx = -np.sin(theta)
ny = -np.cos(theta)
ax.plot(x + r_act*nx, y - r_act*ny, 'c--', alpha=0.6, label='Outer Envelope')
ax.plot(x - r_act*nx, y + r_act*ny, 'c--', alpha=0.6)

# 1. Previous position (Xc=42, Yc=-25, R=12)
c1 = plt.Circle((42, -25), 12, color='red', fill=False, lw=2.5, ls='--', label='Previous Setup: (Xc=42, Yc=-25, R=12)')
ax.add_patch(c1)
ax.plot(42, -25, 'r^', ms=8)

# 2. Optimal Curling Vortex position: (Xc=48, Yc=-55, R=10)
c2 = plt.Circle((48, -55), 10, color='green', fill=True, alpha=0.35, lw=2.5, ec='green', label='Proposed Curling Vortex: (Xc=48, Yc=-55, R=10)')
ax.add_patch(c2)
ax.plot(48, -55, 'go', ms=8)

# 3. Smaller R=8 at Curling Vortex: (Xc=50, Yc=-58, R=8)
c3 = plt.Circle((50, -58), 8, color='darkorange', fill=False, lw=2.5, ls='-', label='Proposed Small Vortex: (Xc=50, Yc=-58, R=8)')
ax.add_patch(c3)

ax.set_xlim(-15, 110)
ax.set_ylim(-110, 20)
ax.set_aspect('equal')
ax.grid(True, alpha=0.3)
ax.set_xlabel('X [mm]', fontsize=11, fontweight='bold')
ax.set_ylabel('Y [mm]', fontsize=11, fontweight='bold')
ax.set_title('Geometric Comparison: Previous Contact vs. Optimal Curling Vortex Grasp', fontsize=12, fontweight='bold')
ax.legend(loc='upper right', fontsize=8.5)

out_fig = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\curling_vortex_grasp\images\geometric_curl_comparison.png"
fig.savefig(out_fig, bbox_inches='tight')
print(f"Saved: {out_fig}")

