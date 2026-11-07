import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import shutil, os

csv_path = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\loop_caging_optimization\actuator_free_curling_trajectory.csv"
df = pd.read_csv(csv_path)

fig, ax = plt.subplots(figsize=(10, 8), dpi=200)

# Plot free actuator spines
colors = {20: 'lightblue', 40: 'skyblue', 60: 'orange', 80: 'coral', 100: 'crimson'}
for p in [20, 40, 60, 80, 100]:
    sub = df[df['P_kPa'] == p]
    ax.plot(sub['X_mm'], sub['Y_mm'], label=f"Actuator Spine P={p} kPa", color=colors[p], linewidth=2.5)

# Test cylinder placements: R = 6 mm
cyl_tests = [
    {"name": "Previous Run (R=12mm at Xc=48, Yc=-42)", "xc": 48.0, "yc": -42.0, "r": 12.0, "col": "gray", "ls": "--"},
    {"name": "Candidate 1: R=6mm at (Xc=44, Yc=-48)", "xc": 44.0, "yc": -48.0, "r": 6.0, "col": "green", "ls": "-"},
    {"name": "Candidate 2: R=6mm at (Xc=42, Yc=-52)", "xc": 42.0, "yc": -52.0, "r": 6.0, "col": "blue", "ls": "-"},
    {"name": "Candidate 3: R=6mm at (Xc=46, Yc=-46)", "xc": 46.0, "yc": -46.0, "r": 6.0, "col": "purple", "ls": "-"}
]

for c in cyl_tests:
    circle = plt.Circle((c["xc"], c["yc"]), c["r"], color=c["col"], fill=False, linewidth=2.5, linestyle=c["ls"], label=c["name"])
    ax.add_patch(circle)
    ax.plot(c["xc"], c["yc"], marker="+", color=c["col"], markersize=8)

ax.set_title("Geometric Overlay: Actuator Curling Envelope vs Cylinder Placement & Radius", fontsize=12, weight='bold')
ax.set_xlabel("X [mm]", fontsize=11)
ax.set_ylabel("Y [mm]", fontsize=11)
ax.axis("equal")
ax.grid(True, linestyle=":", alpha=0.6)
ax.legend(fontsize=9, loc="lower left")

out_png = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\loop_caging_optimization\images\geometric_envelope_overlay.png"
plt.savefig(out_png, bbox_inches='tight')
shutil.copy2(out_png, r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562\geometric_envelope_overlay.png")
print("Saved overlay plot to:", out_png)

