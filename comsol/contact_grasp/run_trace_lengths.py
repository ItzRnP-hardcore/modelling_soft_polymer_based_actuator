import os
import subprocess
import re
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
class_file = os.path.join(base_dir, "TraceLengthsFreeCurling.class")
out_dir = os.path.join(base_dir, "loop_caging_optimization")
os.makedirs(out_dir, exist_ok=True)
csv_out = os.path.join(out_dir, "free_curling_lengths_trajectory.csv")

comsol_compile = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolcompile.exe"
comsol_batch = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"

print("Compiling TraceLengthsFreeCurling.java...")
res = subprocess.run([comsol_compile, os.path.join(base_dir, "TraceLengthsFreeCurling.java")], capture_output=True, text=True)
if res.returncode != 0:
    print("Compilation error:", res.stderr)
    exit(1)
print("Compiled successfully!")

cmd = [comsol_batch, "-inputfile", class_file]
proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1, universal_newlines=True)

rows = []
traj_pat = re.compile(r">> TRAJ_ROW:\s+L=(?P<L>[^,]+),\s+P=(?P<P>[^,]+),\s+s=(?P<s>[^,]+),\s+X=(?P<X>[^,]+),\s+Y=(?P<Y>[^\s]+)")

for line in iter(proc.stdout.readline, ''):
    l = line.strip()
    if any(k in l for k in ["SOLVING", "FREE_CURL", "Solve completed", "Solved steps", "Exported render", "Notice"]):
        print(l)
    m = traj_pat.search(l)
    if m:
        d = m.groupdict()
        rows.append({
            'L_act_mm': float(d['L']),
            'P_kPa': float(d['P']),
            's_mm': float(d['s']),
            'X_mm': float(d['X']),
            'Y_mm': float(d['Y'])
        })

proc.stdout.close()
proc.wait()

if rows:
    df = pd.DataFrame(rows)
    df.to_csv(csv_out, index=False)
    print(f"Saved trajectory data to {csv_out}")
    print(df.groupby('L_act_mm').agg({'X_mm': ['min', 'max'], 'Y_mm': ['min', 'max']}))

