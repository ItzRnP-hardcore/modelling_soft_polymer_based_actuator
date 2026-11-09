"""
run_truepressure.py

Compiles and runs BuildActuatorTapered4CavityTruePressure.java (tapered 4-cavity actuator with
REAL cavity pressure on every wetted face), writes truepressure_summary.csv from the DOE_ROW lines,
removes COMSOL's autosaved *_Model.mph, and plots the comparison against the original tuned-load
model (../tapered_4cavity/comsol_multiaxial_doe_summary.csv, pitch_down).

Usage:  python run_truepressure.py            (compile + solve + plot, about 2 minutes)
        python run_truepressure.py --plot     (plot only, from existing CSVs)
"""
import csv
import os
import re
import subprocess
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

HERE = os.path.dirname(os.path.abspath(__file__))
BIN = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64"
JAVA = os.path.join(HERE, "BuildActuatorTapered4CavityTruePressure.java")
CLASS = os.path.join(HERE, "BuildActuatorTapered4CavityTruePressure.class")
LOG = os.path.join(HERE, "run_truepressure.txt")
CSV_TRUE = os.path.join(HERE, "truepressure_summary.csv")
CSV_TUNED = os.path.join(HERE, "..", "tapered_4cavity", "comsol_multiaxial_doe_summary.csv")
HEADER = ("mode,w1,w2,w3,w4,P_step_kPa,u_tip_mm,v_tip_mm,w_tip_mm,disp_tip_mm,delta_trans_mm,"
          "theta_tip_deg,phi_plane_deg,vm_max_kPa,sp1_max_kPa,min_J,D_ratio_x70,D_ratio_x120")


def solve():
    r = subprocess.run([os.path.join(BIN, "comsolcompile.exe"), JAVA],
                       capture_output=True, text=True, encoding="latin-1", errors="replace")
    if r.returncode != 0:
        sys.exit("Compilation failed:\n" + r.stdout + r.stderr)
    with open(LOG, "w", encoding="latin-1", errors="replace") as f:
        subprocess.run([os.path.join(BIN, "comsolbatch.exe"), "-inputfile", CLASS],
                       stdout=f, stderr=subprocess.STDOUT)
    rows = [m.group(1) for m in re.finditer(r"DOE_ROW: (.+)", open(LOG, encoding="latin-1").read())]
    if not rows:
        sys.exit("No DOE_ROW lines in " + LOG)
    with open(CSV_TRUE, "w", newline="") as f:
        f.write(HEADER + "\n" + "\n".join(r.strip() for r in rows) + "\n")
    tmp = os.path.join(HERE, "BuildActuatorTapered4CavityTruePressure_Model.mph")
    if os.path.exists(tmp):
        os.remove(tmp)
    print(f"{len(rows)} rows -> {CSV_TRUE}")


def load(path, mode="pitch_down"):
    with open(path, encoding="utf-8-sig") as f:
        return [r for r in csv.DictReader(f) if r["mode"] == mode]


def plot():
    true, tuned = load(CSV_TRUE), load(CSV_TUNED)
    col = lambda rows, k: [float(r[k]) for r in rows]
    fig, ax = plt.subplots(1, 3, figsize=(15, 4.4))

    ax[0].plot(col(tuned, "P_step_kPa"), col(tuned, "theta_tip_deg"), "o--", color="#c0392b",
               label="original: tuned load (not real pressure)")
    ax[0].plot(col(true, "P_step_kPa"), col(true, "theta_tip_deg"), "o-", color="#1f6fb4",
               label="real cavity pressure")
    ax[0].set(xlabel="P [kPa]", ylabel="tip angle θ [deg]", title="Tip angle, pitch down")

    ax[1].plot(col(tuned, "P_step_kPa"), col(tuned, "delta_trans_mm"), "o--", color="#c0392b")
    ax[1].plot(col(true, "P_step_kPa"), col(true, "delta_trans_mm"), "o-", color="#1f6fb4")
    ax[1].set(xlabel="P [kPa]", ylabel="transverse tip deflection δ [mm]",
              title="Tip deflection (tuned curve curls back past 90°)")

    ax[2].plot(col(true, "P_step_kPa"), col(true, "D_ratio_x70"), "o-", color="#1f6fb4", label="x = 70 mm")
    ax[2].plot(col(true, "P_step_kPa"), col(true, "D_ratio_x120"), "s-", color="#5aa0d8", label="x = 120 mm")
    ax[2].set(xlabel="P [kPa]", ylabel="deformed / original section diameter",
              title="Ballooning gauge (real pressure)")

    for a in ax:
        a.grid(alpha=0.3)
    ax[0].legend(fontsize=8)
    ax[2].legend(fontsize=8)
    fig.suptitle("Tapered 4-cavity actuator, same geometry/material/mesh: tuned load vs real cavity pressure "
                 "(E = 0.40 MPa, ν = 0.45)", fontsize=11)
    fig.tight_layout()
    out = os.path.join(HERE, "images", "tuned_vs_true_pressure.png")
    fig.savefig(out, dpi=150)
    print("figure ->", out)


if __name__ == "__main__":
    if "--plot" not in sys.argv:
        solve()
    plot()
