import pandas as pd
import numpy as np

csv_path = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\loop_caging_optimization\actuator_free_curling_trajectory.csv"
df = pd.read_csv(csv_path)

sub100 = df[df['P_kPa'] == 100].reset_index(drop=True)

xc, yc = 46.0, -46.0
dists = np.sqrt((sub100['X_mm'] - xc)**2 + (sub100['Y_mm'] - yc)**2)

print("=== DISTANCE FROM CYLINDER CENTER (46, -46) TO ACTUATOR CENTERLINE (P = 100 kPa) ===")
for idx in range(0, len(dists), 3):
    s = sub100.loc[idx, 's_mm']
    x = sub100.loc[idx, 'X_mm']
    y = sub100.loc[idx, 'Y_mm']
    d = dists[idx]
    # Local actuator outer radius roughly Rt + (R0-Rt)*(1 - s/L)
    r_act = 3.2 + (11.5 - 3.2)*(1.0 - s/220.0)
    gap = d - (6.0 + r_act)
    print(f"s = {s:5.1f} mm | Pos = ({x:6.2f}, {y:6.2f}) | Dist = {d:5.2f} mm | Act_Rad = {r_act:4.2f} mm | Gap = {gap:5.2f} mm")

min_idx = np.argmin(dists)
print(f"\nMinimum Distance = {dists[min_idx]:.2f} mm at s = {sub100.loc[min_idx, 's_mm']:.1f} mm")

