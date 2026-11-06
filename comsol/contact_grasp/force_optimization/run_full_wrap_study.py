#!/usr/bin/env python3
"""
run_full_wrap_study.py

Orchestrates Phase 2: Full-Wrap (> 270 deg) Continuum Grasping simulations.
Tests R_cyl = 15 mm and R_cyl = 20 mm under continuation pressure up to 100 kPa.
"""

import os
import sys
import re
import subprocess
import time
import pandas as pd

BASE_DIR = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\force_optimization"
CSV_PATH = os.path.join(BASE_DIR, "full_wrap_grasp_summary.csv")
COMSOL_BATCH = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"
CLASS_FILE = os.path.join(BASE_DIR, "BuildFullWrapGraspModel.class")

ROW_REGEX = re.compile(
    r"FULL_WRAP_ROW:\s+R=([\d\.]+),\s+Xc=([-\d\.]+),\s+Yc=([-\d\.]+),\s+P=([\d\.]+),\s+Fn=([-\d\.]+),\s+Ffric=([-\d\.]+),\s+MaxTn=([-\d\.]+),\s+VmMax=([-\d\.]+),\s+ux=([-\d\.]+),\s+uy=([-\d\.]+),\s+uMag=([-\d\.]+),\s+xTip=([-\d\.]+),\s+yTip=([-\d\.]+),\s+TipAngle=([-\d\.]+),\s+WrapAngle=([-\d\.]+)"
)

# Jobs: (rCyl, Xc, Yc, pMax)
JOBS = [
    ("15.0", "38.0", "-32.0", "100"),  # Full wrap around R=15 mm
    ("20.0", "44.0", "-38.0", "100"),  # Full wrap around R=20 mm
]

def load_existing():
    if os.path.exists(CSV_PATH):
        try:
            return pd.read_csv(CSV_PATH)
        except Exception:
            pass
    return pd.DataFrame(columns=[
        "R_mm", "Xc_mm", "Yc_mm", "P_kPa", "Fn_N", "Ffric_N",
        "MaxTn_kPa", "VmMax_kPa", "ux_mm", "uy_mm", "uMag_mm",
        "xTip_mm", "yTip_mm", "TipAngle_deg", "WrapAngle_deg"
    ])

def save_records(records):
    df_existing = load_existing()
    df_new = pd.DataFrame(records)
    df_combined = pd.concat([df_existing, df_new], ignore_index=True)
    df_combined = df_combined.drop_duplicates(subset=["R_mm", "P_kPa"]).sort_values(by=["R_mm", "P_kPa"])
    df_combined.to_csv(CSV_PATH, index=False)
    print(f"[CSV UPDATED] {len(df_combined)} rows now in {CSV_PATH}", flush=True)

def run_job(r_cyl, xc, yc, p_max):
    print(f"\n=======================================================", flush=True)
    print(f"  LAUNCHING FULL-WRAP: R_cyl = {r_cyl} mm | (Xc, Yc) = ({xc}, {yc}) | P_max = {p_max} kPa", flush=True)
    print(f"=======================================================", flush=True)
    t0 = time.time()
    cmd = [COMSOL_BATCH, "-inputfile", CLASS_FILE, r_cyl, xc, yc, p_max]

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
        if "FULL_WRAP_ROW:" in line_str:
            print("  >> " + line_str, flush=True)
            m = ROW_REGEX.search(line_str)
            if m:
                records.append({
                    "R_mm": float(m.group(1)),
                    "Xc_mm": float(m.group(2)),
                    "Yc_mm": float(m.group(3)),
                    "P_kPa": float(m.group(4)),
                    "Fn_N": float(m.group(5)),
                    "Ffric_N": float(m.group(6)),
                    "MaxTn_kPa": float(m.group(7)),
                    "VmMax_kPa": float(m.group(8)),
                    "ux_mm": float(m.group(9)),
                    "uy_mm": float(m.group(10)),
                    "uMag_mm": float(m.group(11)),
                    "xTip_mm": float(m.group(12)),
                    "yTip_mm": float(m.group(13)),
                    "TipAngle_deg": float(m.group(14)),
                    "WrapAngle_deg": float(m.group(15))
                })
        elif "Solve completed in" in line_str or "Saving model to" in line_str or "Saved successfully" in line_str:
            print("  [INFO] " + line_str, flush=True)

    proc.stdout.close()
    proc.wait()
    dt = time.time() - t0
    print(f"  Full-wrap job R={r_cyl}mm finished with exit code {proc.returncode} in {dt:.1f} s", flush=True)

    if records:
        save_records(records)
    else:
        print(f"  [WARN] No records parsed for R={r_cyl}mm", flush=True)

def main():
    print("Starting Full-Wrap Grasp Runner...", flush=True)
    for r_cyl, xc, yc, p_max in JOBS:
        df = load_existing()
        already_done = not df[(df["R_mm"] == float(r_cyl)) & (df["P_kPa"] == float(p_max))].empty
        if already_done:
            print(f"[SKIP] Full-wrap R={r_cyl}mm already complete.", flush=True)
            continue
        run_job(r_cyl, xc, yc, p_max)

    print("\nALL FULL-WRAP JOBS FINISHED!", flush=True)

if __name__ == "__main__":
    main()

