#!/usr/bin/env python3
"""
run_and_collect_floating_sweep.py

Automated orchestrator and data aggregator for floating cylinder grasp location sweep.
Reads existing log files or runs COMSOL batch cases, extracts GRASP_SWEEP_ROW records,
and writes floating_contact_location_sweep.csv.
"""

import sys
import re
import os
import subprocess
import pandas as pd

BASE_DIR = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
CSV_OUT = os.path.join(BASE_DIR, "floating_contact_location_sweep.csv")
COMSOL_BATCH = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"
CLASS_FILE = os.path.join(BASE_DIR, "BuildFloatingCylinderLocationSweep.class")

ROW_REGEX = re.compile(
    r"GRASP_SWEEP_ROW:\s+R=([\d\.]+),\s+Loc=(\w+),\s+Xc=([-\d\.]+),\s+Yc=([-\d\.]+),\s+P=([\d\.]+),\s+ux=([-\d\.]+),\s+uy=([-\d\.]+),\s+uMag=([-\d\.]+),\s+Fn=([-\d\.]+),\s+MaxTn=([-\d\.]+),\s+Ffric=([-\d\.]+)"
)

def parse_log_text(text):
    records = []
    for line in text.splitlines():
        m = ROW_REGEX.search(line)
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
    return records

def scan_all_logs(log_dir):
    all_records = []
    if not os.path.exists(log_dir):
        return all_records
    for fname in os.listdir(log_dir):
        if fname.endswith(".log") or fname.endswith(".txt"):
            fpath = os.path.join(log_dir, fname)
            try:
                with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                    recs = parse_log_text(f.read())
                    if recs:
                        all_records.extend(recs)
            except Exception as e:
                pass
    return all_records

def main():
    print("Aggregating floating cylinder sweep data...")
    # Check task logs directory
    tasks_dir = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562\.system_generated\tasks"
    steps_dir = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562\.system_generated\steps"

    recs = []
    recs.extend(scan_all_logs(tasks_dir))
    recs.extend(scan_all_logs(steps_dir))
    recs.extend(scan_all_logs(BASE_DIR))

    if not recs:
        print("No GRASP_SWEEP_ROW records found yet.")
        return

    df = pd.DataFrame(recs)
    df = df.drop_duplicates(subset=["Radius_mm", "Loc", "P_kPa"]).sort_values(by=["Radius_mm", "Loc", "P_kPa"])
    df.to_csv(CSV_OUT, index=False)
    print(f"Saved {len(df)} records to {CSV_OUT}")
    print(df.to_string())

if __name__ == "__main__":
    main()

