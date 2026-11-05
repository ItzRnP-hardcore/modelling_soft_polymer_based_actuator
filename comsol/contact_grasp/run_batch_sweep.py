#!/usr/bin/env python3
"""
run_batch_sweep.py

Executes floating cylinder location sweep across radii and locations sequentially.
Extracts results immediately upon each completion and writes them to:
  floating_contact_location_sweep.csv
"""

import os
import sys
import re
import subprocess
import time
import pandas as pd

BASE_DIR = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
CSV_PATH = os.path.join(BASE_DIR, "floating_contact_location_sweep.csv")
COMSOL_BATCH = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"
CLASS_FILE = os.path.join(BASE_DIR, "BuildFloatingCylinderLocationSweep.class")

ROW_REGEX = re.compile(
    r"GRASP_SWEEP_ROW:\s+R=([\d\.]+),\s+Loc=(\w+),\s+Xc=([-\d\.]+),\s+Yc=([-\d\.]+),\s+P=([\d\.]+),\s+ux=([-\d\.]+),\s+uy=([-\d\.]+),\s+uMag=([-\d\.]+),\s+Fn=([-\d\.]+),\s+MaxTn=([-\d\.]+),\s+Ffric=([-\d\.]+)"
)

# Queue of (radius, loc_index, loc_name)
QUEUE = [
    ("12", "1", "Central"),
    ("12", "2", "Distal"),
    ("15", "0", "Proximal"),
    ("15", "1", "Central"),
    ("15", "2", "Distal"),
    ("18", "0", "Proximal"),
    ("18", "1", "Central"),
    ("18", "2", "Distal"),
    ("22", "0", "Proximal"),
    ("22", "1", "Central"),
    ("22", "2", "Distal"),
]

def load_existing():
    if os.path.exists(CSV_PATH):
        try:
            return pd.read_csv(CSV_PATH)
        except Exception:
            pass
    return pd.DataFrame(columns=[
        "Radius_mm", "Loc", "Xc_mm", "Yc_mm", "P_kPa",
        "ux_mm", "uy_mm", "uMag_mm", "Fn_N", "MaxTn_kPa", "Ffric_N"
    ])

def save_records(records):
    df_existing = load_existing()
    df_new = pd.DataFrame(records)
    df_combined = pd.concat([df_existing, df_new], ignore_index=True)
    df_combined = df_combined.drop_duplicates(subset=["Radius_mm", "Loc", "P_kPa"]).sort_values(
        by=["Radius_mm", "Loc", "P_kPa"]
    )
    df_combined.to_csv(CSV_PATH, index=False)
    print(f"[CSV UPDATED] {len(df_combined)} rows now in {CSV_PATH}")

def run_job(radius, loc_idx, loc_name):
    print(f"\n=======================================================")
    print(f"  LAUNCHING: R = {radius} mm | Location = {loc_name} (idx {loc_idx})")
    print(f"=======================================================")
    t0 = time.time()
    cmd = [COMSOL_BATCH, "-inputfile", CLASS_FILE, radius, loc_idx]
    
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
        if "GRASP_SWEEP_ROW:" in line_str:
            print("  >> " + line_str)
            m = ROW_REGEX.search(line_str)
            if m:
                records.append({
                    "Radius_mm": float(m.group(1)),
                    "Loc": m.group(2),
                    "Xc_mm": float(m.group(3)),
                    "Yc_mm": float(m.group(4)),
                    "P_kPa": float(m.group(5)),
                    "ux_mm": float(m.group(6)),
                    "uy_mm": float(m.group(7)),
                    "uMag_mm": float(m.group(8)),
                    "Fn_N": float(m.group(9)),
                    "MaxTn_kPa": float(m.group(10)),
                    "Ffric_N": float(m.group(11))
                })
        elif "Solve completed in" in line_str or "Saving model to" in line_str or "Saved successfully" in line_str:
            print("  [INFO] " + line_str)

    proc.stdout.close()
    proc.wait()
    dt = time.time() - t0
    print(f"  Job R={radius} mm {loc_name} finished with exit code {proc.returncode} in {dt:.1f} s")

    if records:
        save_records(records)
    else:
        print(f"  [WARN] No records parsed for R={radius} mm {loc_name}")

def main():
    print("Starting Floating Cylinder Location Sweep Runner...")
    
    # First, ensure R=12 Proximal (from task-3455) is already recorded
    from run_and_collect_floating_sweep import scan_all_logs, parse_log_text
    tasks_dir = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562\.system_generated\tasks"
    steps_dir = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562\.system_generated\steps"
    existing_recs = scan_all_logs(tasks_dir) + scan_all_logs(steps_dir)
    if existing_recs:
        save_records(existing_recs)

    # Process queue
    for radius, loc_idx, loc_name in QUEUE:
        # Check if already present in CSV
        df = load_existing()
        already_done = not df[(df["Radius_mm"] == float(radius)) & (df["Loc"] == loc_name)].empty
        if already_done:
            print(f"[SKIP] R={radius} mm {loc_name} already in CSV.")
            continue
        run_job(radius, loc_idx, loc_name)

    print("\nALL SWEEPS COMPLETED SUCCESSFULLY!")

if __name__ == "__main__":
    main()

