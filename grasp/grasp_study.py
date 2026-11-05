"""
Parameter study with grasp_model.py: upward holding force on a rigid cylinder
for three finger designs, swept over object depth, pressure and friction.

    python -u grasp_study.py [design ...] > results/grasp.log

With design names, only those designs are run and the files get the names as a
suffix (run the three designs in parallel, then merge with --merge).

Writes results/grasp/sweep.csv (one row per run) and results/grasp/curves.csv
(force against pull for every run).

Designs (all L = 100 mm, E = 0.15 MPa, hoop-reinforced, concentric cavity):
  uniform  R = 10 mm, wall 2 mm, strain-limiting layer over the full length
  tapered  R 10 -> 5 mm and wall 2 -> 1 mm (same shape scaled down), full layer;
           the tip curls twice as tightly as the base
  tip-hook R = 10 mm, wall 2 mm, layer only on the last 40 %: the base only
           lengthens, the tip curls into a hook
Object: rigid cylinder, radius 15 mm, centre on the axis between two mirrored
fingers whose layers start 1 mm from it.
"""
from __future__ import annotations
import csv
import time
from pathlib import Path
import numpy as np
import sys
HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))
from grasp_model import Finger, Grasp, GraspSim

DESIGNS = {
    "uniform": Finger(R0=10, Rt=10, t0=2.0, tt=2.0),
    "tapered": Finger(R0=10, Rt=5, t0=2.0, tt=1.0),
    "tip-hook": Finger(R0=10, Rt=10, t0=2.0, tt=2.0, layer_from=0.6),
}
DEPTHS = [20, 30, 40, 50, 60, 70, 80]          # object centre below the palm [mm]
PRESSURES = [0.010, 0.020, 0.030, 0.040, 0.050]  # [MPa]
MUS = [0.0, 0.3, 0.6, 1.0]
A_OBJ, PULL, N_INFLATE, N_PULL = 15.0, 70.0, 20, 112
OUT = ROOT / "results" / "grasp"


def one_run(f, depth, P, mu):
    sim = GraspSim(Grasp(f, a=A_OBJ, depth=depth), mu)
    for Pk in np.linspace(0, P, N_INFLATE + 1)[1:]:
        sim.step(P=Pk)
    tips_meet = bool(sim.nodes()[:, 0].max() > -0.05)
    contact0 = sim.history[-1]["n_contact"]
    c0, rows = sim.c.copy(), []
    for d in np.linspace(0, PULL, N_PULL + 1)[1:]:
        rec = sim.step(c=c0 + np.array([0.0, -d]))
        rows.append((d, rec["Fy"], rec["Fy_normal"], rec["Fy_friction"], rec["n_contact"]))
    rows = np.array(rows)
    i = int(np.argmax(rows[:, 1]))
    ok = all(h["converged"] for h in sim.history)
    resid = max(h["residual_N"] for h in sim.history)
    summary = dict(peak=rows[i, 1], peak_normal=rows[i, 2], peak_friction=rows[i, 3], pull_at_peak=rows[i, 0],
                   contacts_at_peak=int(rows[i, 4]), final=rows[-1, 1], contacts_after_inflate=contact0,
                   tips_meet=tips_meet, converged=ok, max_residual=resid)
    return summary, rows


def merge():
    for stem in ("sweep", "curves"):
        parts = [OUT / f"{stem}_{name}.csv" for name in DESIGNS]
        with open(OUT / f"{stem}.csv", "w", newline="") as out:
            for i, p in enumerate(parts):
                lines = p.read_text().splitlines(keepends=True)
                out.writelines(lines if i == 0 else lines[1:])


if __name__ == "__main__":
    import sys
    if sys.argv[1:] == ["--merge"]:
        merge()
        sys.exit()
    names = sys.argv[1:] or list(DESIGNS)
    suffix = "" if len(names) == len(DESIGNS) else "_" + "_".join(names)
    OUT.mkdir(parents=True, exist_ok=True)
    fs = open(OUT / f"sweep{suffix}.csv", "w", newline="")
    fc = open(OUT / f"curves{suffix}.csv", "w", newline="")
    ws, wc = csv.writer(fs), csv.writer(fc)
    head = ["design", "depth_mm", "P_kPa", "mu"]
    ws.writerow(head + ["peak_N", "peak_normal_N", "peak_friction_N", "pull_at_peak_mm", "contacts_at_peak",
                        "final_N", "contacts_after_inflate", "tips_meet", "converged", "max_residual_N"])
    wc.writerow(head + ["pull_mm", "Fy_N", "Fy_normal_N", "Fy_friction_N", "n_contact"])
    t0, n, total = time.time(), 0, len(names) * len(DEPTHS) * len(PRESSURES) * len(MUS)
    for name in names:
        f = DESIGNS[name]
        for depth in DEPTHS:
            for P in PRESSURES:
                for mu in MUS:
                    s, rows = one_run(f, depth, P, mu)
                    key = [name, depth, round(P * 1e3), mu]
                    ws.writerow(key + [f"{s['peak']:.4f}", f"{s['peak_normal']:.4f}", f"{s['peak_friction']:.4f}",
                                       f"{s['pull_at_peak']:.3f}", s["contacts_at_peak"], f"{s['final']:.4f}",
                                       s["contacts_after_inflate"], int(s["tips_meet"]), int(s["converged"]),
                                       f"{s['max_residual']:.2e}"])
                    for r in rows:
                        wc.writerow(key + [f"{r[0]:.3f}", f"{r[1]:.4f}", f"{r[2]:.4f}", f"{r[3]:.4f}", int(r[4])])
                    fs.flush(); fc.flush()
                    n += 1
                    print(f"[{n:3d}/{total}] {name:8s} depth {depth:2d}  P {P*1e3:3.0f} kPa  mu {mu:.1f}  "
                          f"peak {s['peak']:6.3f} N (normal {s['peak_normal']:6.3f}, friction {s['peak_friction']:6.3f})"
                          f"  tips_meet {int(s['tips_meet'])}  resid {s['max_residual']:.0e}  {time.time()-t0:6.0f} s")
    fs.close(); fc.close()
