"""
run_sleeved_study.py

Runs BuildActuator4CavitySleeved.java (tapered 4-cavity actuator, REAL cavity pressure, real
silicone nu = 0.4995, optional hoop sleeve) for a set of cases, writes
    sleeved_study_summary.csv     one row per converged pressure step
    sleeved_study_centreline.csv  deformed centreline (29 points) per step
and plots the results together with the first-order (Tier 1, hoop-reinforced beam) estimate.

Usage:
    python run_sleeved_study.py                 all cases
    python run_sleeved_study.py sleeved_base    only cases whose name contains this text
    python run_sleeved_study.py --plot          plot only, from existing CSVs
"""
import csv
import os
import re
import subprocess
import sys

import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

HERE = os.path.dirname(os.path.abspath(__file__))
BIN = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64"
JAVA = os.path.join(HERE, "BuildActuator4CavitySleeved.java")
CLASS = os.path.join(HERE, "BuildActuator4CavitySleeved.class")
SUMMARY = os.path.join(HERE, "sleeved_study_summary.csv")
CENTRE = os.path.join(HERE, "sleeved_study_centreline.csv")
SUM_HDR = ["case", "sleeve", "E_MPa", "cav_deg", "depth0_mm", "P_kPa", "x_tip_mm", "y_tip_mm",
           "z_tip_mm", "theta_tip_deg", "vm_max_kPa", "min_J", "D_ratio_x70"]
CL_HDR = ["case", "P_kPa", "X0_mm", "x_mm", "y_mm", "z_mm"]

UTS_KPA = 1400.0   # Ecoflex 00-30 tensile strength ~1.4 MPa (datasheet value; check for the silicone used)
PL_FULL ="0 2 4 6 8 10 12.5 15 17.5 20 22.5 25 27.5 30 35 40 45 50 55 60 65 70 75 80 90 100"
# name: (SLEEVE, E_MPA, DEPTH0, DEPTHT, CAV_DEG[, FILLET radius at base in mm])
CASES = {
    "bare_base_E0.07":    ("0", "0.07", "2.5", "1.0", "56"),   # real silicone, no sleeve
    "sleeved_base_E0.07": ("1", "0.07", "2.5", "1.0", "56"),   # + hoop sleeve, current chambers
    "sleeved_deep_E0.07": ("1", "0.07", "5.0", "1.5", "70"),   # + deeper/wider chambers
    "sleeved_fillet_E0.07": ("1", "0.07", "2.5", "1.0", "56", "0.6"),  # + 0.6 mm corner fillets (0.24 at tip)
}


CASE_JAVA = os.path.join(HERE, "SleevedCase.java")
CASE_CLASS = os.path.join(HERE, "SleevedCase.class")


def compile_case(cfg):
    """COMSOL blocks System.getenv, so write a copy of the builder with the case values inlined."""
    src = open(JAVA, encoding="utf-8").read()
    entries = ", ".join('{"%s", "%s"}' % kv for kv in cfg.items())
    src = src.replace("/*CFG*/", entries).replace("class BuildActuator4CavitySleeved", "class SleevedCase")
    open(CASE_JAVA, "w", encoding="utf-8").write(src)
    r = subprocess.run([os.path.join(BIN, "comsolcompile.exe"), CASE_JAVA],
                       capture_output=True, text=True, encoding="latin-1", errors="replace")
    if r.returncode != 0 or not os.path.exists(CASE_CLASS):
        sys.exit("Compilation failed:\n" + r.stdout + r.stderr)


def compile_java():
    pass  # each case is compiled in run_case


def run_case(name, spec, plist=PL_FULL):
    sleeve, E, d0, dt, cav = spec[:5]
    fil = spec[5] if len(spec) > 5 else "0"
    compile_case(dict(CASE_NAME=name, SLEEVE=sleeve, E_MPA=E, DEPTH0=d0, DEPTHT=dt, CAV_DEG=cav,
                      FILLET=fil, PLIST=plist))
    log = os.path.join(HERE, f"log_{name}.txt")
    with open(log, "w", encoding="latin-1", errors="replace") as f:
        subprocess.run([os.path.join(BIN, "comsolbatch.exe"), "-inputfile", CASE_CLASS],
                       stdout=f, stderr=subprocess.STDOUT)
    txt = open(log, encoding="latin-1").read()
    for tag in ("CASE", "CLASSIFY", "CONVERGED_STEPS", "SOLVER_STOPPED", "FAILED"):
        for line in re.findall(rf"^.*{tag}.*$", txt, re.M):
            print("  ", line.strip()[:200])
    doe = [l.split(",") for l in re.findall(r"DOE_ROW: (.+)", txt)]
    cl = [l.split(",") for l in re.findall(r"CL_ROW: (.+)", txt)]
    tmp = os.path.join(HERE, "SleevedCase_Model.mph")
    if os.path.exists(tmp):
        os.remove(tmp)
    return doe, cl


def merge(path, hdr, rows, names):
    old = []
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            old = [r for r in csv.reader(f)][1:]
    old = [r for r in old if r[0] not in names]
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(hdr)
        w.writerows(old + [[c.strip() for c in r] for r in rows])


def tier1(P_kPa, E, d0, dt, cav_deg, L=140, R0=9.5, Rt=3.8, tw0=2.0, twt=0.8, xs=4, xe=136):
    """Hoop-reinforced beam estimate: theta = int P*A_cav*e/(E*I) dx (notes eq. 7, tapered)."""
    al = np.radians(cav_deg / 2)
    x = np.linspace(xs, xe, 400)
    xi = (x - xs) / (xe - xs)
    R = R0 - (R0 - Rt) * x / L
    b = (R0 - tw0) - ((R0 - tw0) - (Rt - twt)) * xi
    a = b - (d0 - (d0 - dt) * xi)
    Ac = al * (b ** 2 - a ** 2)
    e = 2 * np.sin(al) / (3 * al) * (b ** 3 - a ** 3) / (b ** 2 - a ** 2)
    I = np.pi * R ** 4 / 4 - 4 * al * (b ** 4 - a ** 4) / 4
    k = np.trapezoid(Ac * e / I, x)
    return np.degrees(np.asarray(P_kPa) / 1000.0 / E * k)


def plot():
    with open(SUMMARY, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    with open(CENTRE, encoding="utf-8") as f:
        cl = list(csv.DictReader(f))
    names = list(dict.fromkeys(r["case"] for r in rows))
    colors = dict(zip(names, ["#7f8c8d", "#1f6fb4", "#c0392b", "#27ae60"]))

    fig, ax = plt.subplots(1, 3, figsize=(16, 4.8))
    for n in names:
        rr = [r for r in rows if r["case"] == n]
        P = np.array([float(r["P_kPa"]) for r in rr])
        th = np.array([float(r["theta_tip_deg"]) for r in rr])
        vm = np.array([float(r["vm_max_kPa"]) for r in rr])
        bad = vm > UTS_KPA   # local peak stress beyond silicone tensile strength: not physically reliable
        ax[0].plot(P, th, "-", color=colors[n])
        ax[0].plot(P[~bad], th[~bad], "o", ms=4, color=colors[n], label=f"{n} (FE)")
        if bad.any():
            ax[0].plot(P[bad], th[bad], "o", ms=6, mfc="white", color=colors[n],
                       label=f"{n}: peak stress > {UTS_KPA/1000:g} MPa")
        r0 = rr[0]
        if r0["sleeve"] == "1":
            Pf = np.linspace(0, P.max(), 50)
            ax[0].plot(Pf, tier1(Pf, float(r0["E_MPa"]), float(r0["depth0_mm"]),
                                 1.5 if float(r0["depth0_mm"]) > 3 else 1.0, float(r0["cav_deg"])),
                       ":", color=colors[n], label=f"{n} (Tier-1 beam estimate)")
        ax[1].plot(P, [float(r["D_ratio_x70"]) for r in rr], "o-", ms=3.5, color=colors[n], label=n)
    for v in (180, 360):
        ax[0].axhline(v, color="k", lw=0.6, ls="--")
    ax[0].set(xlabel="cavity pressure P [kPa]", ylabel="tip rotation [deg]", title="Tip rotation (pitch down)")
    ax[0].legend(fontsize=7)
    ax[1].set(xlabel="P [kPa]", ylabel="section diameter / original (x = 70 mm)", title="Ballooning gauge")
    ax[1].legend(fontsize=7)

    for n in names:
        Pmax = max(float(r["P_kPa"]) for r in cl if r["case"] == n)
        for frac, ls in ((1.0, "-"), (0.5, "--")):
            target = min({float(r["P_kPa"]) for r in cl if r["case"] == n}, key=lambda p: abs(p - frac * Pmax))
            pts = [r for r in cl if r["case"] == n and float(r["P_kPa"]) == target]
            ax[2].plot([float(r["x_mm"]) for r in pts], [float(r["y_mm"]) for r in pts], ls, color=colors[n],
                       label=f"{n}, {target:g} kPa")
    ax[2].set(xlabel="x [mm]", ylabel="y [mm]", title="Deformed centreline (true scale)", aspect="equal")
    ax[2].legend(fontsize=7)
    for a in ax:
        a.grid(alpha=0.3)
    fig.suptitle("Tapered 4-cavity actuator, real cavity pressure, real silicone (ν = 0.4995): bare vs hoop sleeve",
                 fontsize=11)
    fig.tight_layout()
    out = os.path.join(HERE, "images", "sleeved_study.png")
    fig.savefig(out, dpi=150)
    print("figure ->", out)


if __name__ == "__main__":
    if "--plot" in sys.argv:
        plot()
        sys.exit()
    sel = [a for a in sys.argv[1:] if not a.startswith("--")]
    todo = {k: v for k, v in CASES.items() if not sel or any(s in k for s in sel)}
    plist = PL_FULL if "--quick" not in sys.argv else "0 5 10"
    compile_java()
    allD, allC = [], []
    for name, spec in todo.items():
        print(f"== {name}")
        d, c = run_case(name, spec, plist)
        allD += d
        allC += c
    merge(SUMMARY, SUM_HDR, allD, set(todo))
    merge(CENTRE, CL_HDR, allC, set(todo))
    plot()
