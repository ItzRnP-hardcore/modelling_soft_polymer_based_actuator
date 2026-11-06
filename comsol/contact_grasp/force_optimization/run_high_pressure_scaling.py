#!/usr/bin/env python3
"""
run_high_pressure_scaling.py

Orchestrates the Phase 1 High-Pressure (up to 100 kPa) and Geometric Scaling (1.0x, 1.5x, 2.0x)
simulations for soft continuum grasp optimization.
"""

import os
import sys
import re
import subprocess
import time
import pandas as pd

BASE_DIR = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\force_optimization"
CSV_PATH = os.path.join(BASE_DIR, "scale_high_pressure_sweep.csv")
COMSOL_BATCH = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"
CLASS_FILE = os.path.join(BASE_DIR, "BuildHighPressureScaleSweep.class")

ROW_REGEX = re.compile(
    r"SCALE_SWEEP_ROW:\s+Scale=([\d\.]+),\s+R=([\d\.]+),\s+Xc=([-\d\.]+),\s+Yc=([-\d\.]+),\s+P=([\d\.]+),\s+Fn=([-\d\.]+),\s+Ffric=([-\d\.]+),\s+MaxTn=([-\d\.]+),\s+VmMax=([-\d\.]+),\s+ux=([-\d\.]+),\s+uy=([-\d\.]+),\s+uMag=([-\d\.]+),\s+xTip=([-\d\.]+),\s+yTip=([-\d\.]+),\s+TipAngle=([-\d\.]+)"
)

JOBS = [
    ("1.0", "22.0", "100"),  # Scale 1.0x, R=22 mm, 100 kPa
    ("1.5", "22.0", "100"),  # Scale 1.5x, R=22 mm, 100 kPa
    ("2.0", "22.0", "100"),  # Scale 2.0x, R=22 mm, 100 kPa
    ("1.0", "15.0", "100"),  # Scale 1.0x, R=15 mm, 100 kPa
    ("1.5", "15.0", "100"),  # Scale 1.5x, R=15 mm, 100 kPa
]

def load_existing():
    if os.path.exists(CSV_PATH):
        try:
            return pd.read_csv(CSV_PATH)
        except Exception:
            pass
    return pd.DataFrame(columns=[
        "Scale", "R_mm", "Xc_mm", "Yc_mm", "P_kPa", "Fn_N", "Ffric_N",
        "MaxTn_kPa", "VmMax_kPa", "ux_mm", "uy_mm", "uMag_mm", "xTip_mm", "yTip_mm", "TipAngle_deg"
    ])

def save_records(records):
    df_existing = load_existing()
    df_new = pd.DataFrame(records)
    df_combined = pd.concat([df_existing, df_new], ignore_index=True)
    df_combined = df_combined.drop_duplicates(subset=["Scale", "R_mm", "P_kPa"]).sort_values(
        by=["Scale", "R_mm", "P_kPa"]
    )
    df_combined.to_csv(CSV_PATH, index=False)
    print(f"[CSV UPDATED] {len(df_combined)} records now in {CSV_PATH}")

def run_job(scale, r_cyl, p_max):
    print(f"\n=======================================================")
    print(f"  LAUNCHING: Scale = {scale}x | R_cyl = {r_cyl} mm | P_max = {p_max} kPa")
    print(f"=======================================================")
    t0 = time.time()
    cmd = [COMSOL_BATCH, "-inputfile", CLASS_FILE, scale, r_cyl, p_max]

    proc = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )

    records = []
    for line in iter(proc.stdout.readline, ''):
        line_str = line.strip()
        if "SCALE_SWEEP_ROW:" in line_str:
            print("  >> " + line_str)
            m = ROW_REGEX.search(line_str)
            if m:
                records.append({
                    "Scale": float(m.group(1)),
                    "R_mm": float(m.group(2)),
                    "Xc_mm": float(m.group(3)),
                    "Yc_mm": float(m.group(4)),
                    "P_kPa": float(m.group(5)),
                    "Fn_N": float(m.group(6)),
                    "Ffric_N": float(m.group(7)),
                    "MaxTn_kPa": float(m.group(8)),
                    "VmMax_kPa": float(m.group(9)),
                    "ux_mm": float(m.group(10)),
                    "uy_mm": float(m.group(11)),
                    "uMag_mm": float(m.group(12)),
                    "xTip_mm": float(m.group(13)),
                    "yTip_mm": float(m.group(14)),
                    "TipAngle_deg": float(m.group(15))
                })
        elif "Solve completed in" in line_str or "Saving model to" in line_str or "Saved successfully" in line_str:
            print("  [INFO] " + line_str)

    proc.stdout.close()
    proc.wait()
    dt = time.time() - t0
    print(f"  Job Scale={scale}x R={r_cyl}mm finished with exit code {proc.returncode} in {dt:.1f} s")

    if records:
        save_records(records)
    else:
        print(f"  [WARN] No records parsed for Scale={scale}x R={r_cyl}mm")

def main():
    print("Starting High-Pressure Scaling Study Orchestrator...")
    for scale, r_cyl, p_max in JOBS:
        df = load_existing()
        already_done = not df[(df["Scale"] == float(scale)) & (df["R_mm"] == float(r_cyl)) & (df["P_kPa"] == float(p_max))].empty
        if already_done:
            print(f"[SKIP] Scale={scale}x R={r_cyl}mm already complete.")
            continue
        run_job(scale, r_cyl, p_max)

    print("\nALL HIGH-PRESSURE SCALING JOBS FINISHED!")

if __name__ == "__main__":
    main()

