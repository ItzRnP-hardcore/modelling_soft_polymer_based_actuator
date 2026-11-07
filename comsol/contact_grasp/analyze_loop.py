import pandas as pd
import numpy as np

csv_path = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\loop_caging_optimization\actuator_free_curling_trajectory.csv"
df = pd.read_csv(csv_path)

print("=== CENTERLINE SPAN AND TIP POSITIONS ===")
for p in [0, 20, 40, 60, 80, 100]:
    sub = df[df['P_kPa'] == p]
    tip = sub.iloc[-1]
    x_min, x_max = sub['X_mm'].min(), sub['X_mm'].max()
    y_min, y_max = sub['Y_mm'].min(), sub['Y_mm'].max()
    print(f"P = {p:3.0f} kPa | X_span: [{x_min:6.1f}, {x_max:6.1f}] | Y_span: [{y_min:6.1f}, {y_max:6.1f}] | Tip: ({tip['X_mm']:6.1f}, {tip['Y_mm']:6.1f})")

print("\n=== LOOP CENTER & RADIUS FIT ===")
def fit_circle(x, y):
    x_m = np.mean(x)
    y_m = np.mean(y)
    u = x - x_m
    v = y - y_m
    Suv = np.sum(u*v)
    Suu = np.sum(u**2)
    Svv = np.sum(v**2)
    Suuv = np.sum(u**2 * v)
    Suvv = np.sum(u * v**2)
    Suuu = np.sum(u**3)
    Svvv = np.sum(v**3)
    A = np.array([[Suu, Suv], [Suv, Svv]])
    B = np.array([0.5*(Suuu + Suvv), 0.5*(Svvv + Suuv)])
    try:
        uc, vc = np.linalg.solve(A, B)
        xc = x_m + uc
        yc = y_m + vc
        R = np.mean(np.sqrt((x - xc)**2 + (y - yc)**2))
        return xc, yc, R
    except:
        return np.nan, np.nan, np.nan

for p in [20, 40, 60, 80, 100]:
    sub = df[df['P_kPa'] == p]
    distal = sub[sub['s_mm'] >= 80.0]
    xc, yc, R = fit_circle(distal['X_mm'].values, distal['Y_mm'].values)
    print(f"P = {p:3.0f} kPa | Distal Spine (s>=80mm): Center = ({xc:6.2f}, {yc:6.2f}) mm, Curvature Radius R = {R:5.2f} mm")

print("\n=== DETAILED TRAJECTORY POINTS AT P = 60 kPa ===")
sub60 = df[df['P_kPa'] == 60]
for idx, r in sub60.iterrows():
    if r['pt'] % 4 == 0:
        print(f"s = {r['s_mm']:5.1f} mm -> (X = {r['X_mm']:6.2f}, Y = {r['Y_mm']:6.2f}) mm")

print("\n=== DETAILED TRAJECTORY POINTS AT P = 100 kPa ===")
sub100 = df[df['P_kPa'] == 100]
for idx, r in sub100.iterrows():
    if r['pt'] % 4 == 0:
        print(f"s = {r['s_mm']:5.1f} mm -> (X = {r['X_mm']:6.2f}, Y = {r['Y_mm']:6.2f}) mm")

