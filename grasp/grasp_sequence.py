"""
Does the order in which a segmented finger's chambers are inflated change the grip?

    python grasp_sequence.py      -> results/grasp/sequence.csv

Same 4-chamber finger as tentacle_animation.py, same object as grasp_study.py
(radius 15 mm), mu = 0.6, 50 kPa per chamber.  Three inflation orders:
all chambers together, base to tip, tip to base.  Reports the upward force
within 10 mm of sag from the grasped position, and the ultimate pull-out force.
"""
from __future__ import annotations
import csv
import numpy as np
import sys
from pathlib import Path
HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))
from grasp_model import Finger, Grasp, GraspSim
from grasp_study import OUT, A_OBJ, PULL, N_PULL

FINGER = Finger(R0=10, Rt=10, t0=1.5, tt=1.5, n_seg=4, E=0.15)
PMAX, MU, RAMP = 0.050, 0.6, 12
ORDERS = {"together": [[0, 1, 2, 3]], "base-to-tip": [[0], [1], [2], [3]], "tip-to-base": [[3], [2], [1], [0]]}


def run(order, depth):
    sim = GraspSim(Grasp(FINGER, a=A_OBJ, depth=depth), MU)
    P = np.zeros(4)
    for group in ORDERS[order]:
        for w in np.linspace(0, 1, RAMP + 1)[1:]:
            P[group] = w * PMAX
            sim.step(P=P.copy())
    c0, F = sim.c.copy(), []
    for d in np.linspace(0, PULL, N_PULL + 1)[1:]:
        F.append((d, sim.step(c=c0 + np.array([0.0, -d]))["Fy"]))
    F = np.array(F)
    return F[F[:, 0] <= 10.0 + 1e-9, 1].max(), F[:, 1].max(), F[np.argmax(F[:, 1]), 0]


if __name__ == "__main__":
    with open(OUT / "sequence.csv", "w", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(["order", "depth_mm", "F_sag10_N", "F_ultimate_N", "pull_at_ultimate_mm"])
        for depth in (40, 60, 70, 80):
            for order in ORDERS:
                f10, fu, du = run(order, depth)
                w.writerow([order, depth, f"{f10:.4f}", f"{fu:.4f}", f"{du:.2f}"])
                print(f"depth {depth}  {order:12s}  within 10 mm sag {f10:6.3f} N   ultimate {fu:6.3f} N at {du:4.1f} mm")
