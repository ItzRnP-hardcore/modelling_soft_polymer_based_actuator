import pandas as pd
import numpy as np

csv_path = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\loop_caging_optimization\actuator_free_curling_trajectory.csv"
df = pd.read_csv(csv_path)

sub100 = df[df['P_kPa'] == 100].reset_index(drop=True)

test_positions = [
    {"name": "Pos1: (48, -42)", "xc": 48.0, "yc": -42.0, "rc": 8.0},
    {"name": "Pos2: (48, -44)", "xc": 48.0, "yc": -44.0, "rc": 8.0},
    {"name": "Pos3: (50, -44)", "xc": 50.0, "yc": -44.0, "rc": 8.0},
]

for tp in test_positions:
    xc, yc, rc = tp["xc"], tp["yc"], tp["rc"]
    dists = np.sqrt((sub100['X_mm'] - xc)**2 + (sub100['Y_mm'] - yc)**2)
    # Check minimum distance
    min_d = dists.min()
    min_idx = np.argmin(dists)
    s_min = sub100.loc[min_idx, 's_mm']
    r_act = 3.2 + (11.5 - 3.2)*(1.0 - s_min/220.0)
    overlap = (rc + r_act) - min_d
    print(f"{tp['name']} | Min Dist = {min_d:.2f} mm at s = {s_min:.1f} mm | Act_Rad = {r_act:.2f} mm | Interference/Overlap = {overlap:+.2f} mm")

