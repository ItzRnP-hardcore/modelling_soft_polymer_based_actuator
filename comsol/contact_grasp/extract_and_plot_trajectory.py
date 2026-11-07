import os
import subprocess
import re
import shutil
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
class_file = os.path.join(base_dir, "ExportTrajectoryFromMPH.class")
out_dir = os.path.join(base_dir, "loop_caging_optimization")
img_dir = os.path.join(out_dir, "images")
csv_path = os.path.join(out_dir, "actuator_free_curling_trajectory.csv")
brain_dir = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562"

os.makedirs(img_dir, exist_ok=True)
comsol_batch = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"

print("Executing ExportTrajectoryFromMPH...")
cmd = [comsol_batch, "-inputfile", class_file]
proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)

rows = []
pattern = re.compile(r"TRAJ_DATA:\s+P=(?P<P>[^,]+),\s+pt=(?P<pt>[^,]+),\s+s=(?P<s>[^,]+),\s+X=(?P<X>[^,]+),\s+Y=(?P<Y>[^\s\r\n]+)")

for line in iter(proc.stdout.readline, ''):
    line_str = line.strip()
    if "TRAJ_DATA" in line_str:
        m = pattern.search(line_str)
        if m:
            d = m.groupdict()
            rows.append({
                'P_kPa': float(d['P']),
                'pt': int(d['pt']),
                's_mm': float(d['s']),
                'X_mm': float(d['X']),
                'Y_mm': float(d['Y'])
            })
    elif any(k in line_str for k in ["Exported", "Loading", "Extracted", "complete"]):
        print(line_str)

proc.stdout.close()
proc.wait()

if rows:
    df = pd.DataFrame(rows)
    df.to_csv(csv_path, index=False)
    shutil.copy2(csv_path, os.path.join(brain_dir, "actuator_free_curling_trajectory.csv"))
    print(f"Saved {len(df)} points to {csv_path}")

# Copy newly exported renders
for pVal in [40, 60, 80]:
    src = os.path.join(base_dir, "images", f"actuator_free_curling_220mm_disp_P{pVal}.png")
    if os.path.exists(src):
        shutil.copy2(src, os.path.join(img_dir, os.path.basename(src)))
        shutil.copy2(src, os.path.join(brain_dir, os.path.basename(src)))
        print(f"Copied render P{pVal} to brain directory.")

# Plot the 2D trajectories of the centerline at each pressure step
plt.figure(figsize=(10, 8), dpi=200)
colors = plt.cm.viridis(np.linspace(0.1, 0.95, 6))
p_list = [0, 20, 40, 60, 80, 100]

for idx, p in enumerate(p_list):
    sub = df[df['P_kPa'] == p]
    if not sub.empty:
        plt.plot(sub['X_mm'], sub['Y_mm'], marker='o', markersize=3, label=f"P = {p} kPa", color=colors[idx], linewidth=2.0)
        # Highlight tip
        tip = sub.iloc[-1]
        plt.plot(tip['X_mm'], tip['Y_mm'], marker='s', markersize=7, color=colors[idx])
        plt.text(tip['X_mm'] + 1.5, tip['Y_mm'], f"Tip ({tip['X_mm']:.1f}, {tip['Y_mm']:.1f})", fontsize=8, color=colors[idx], weight='bold')

plt.title("Free Curling Centerline Trajectory of Tapered 4-Cavity Actuator (L = 220 mm)", fontsize=13, weight='bold')
plt.xlabel("X (Deformed Axial Position) [mm]", fontsize=11)
plt.ylabel("Y (Deformed Transverse Deflection) [mm]", fontsize=11)
plt.grid(True, linestyle="--", alpha=0.6)
plt.axis("equal")
plt.legend(frameon=True, fontsize=10, loc="lower left")

plot_out = os.path.join(img_dir, "actuator_free_curling_centerlines.png")
plt.savefig(plot_out, bbox_inches='tight')
shutil.copy2(plot_out, os.path.join(brain_dir, "actuator_free_curling_centerlines.png"))
print(f"Saved trajectory comparison plot to: {plot_out}")

