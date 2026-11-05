"""
Figures for the grasp study (run grasp_study.py and grasp_sequence.py first):

    python grasp_figures.py      -> report/figures/grasp_*.png

Holding force is reported two ways:
  working   the largest upward force reached within SAG mm of the grasped
            position, i.e. the weight the grasp carries without the object
            dropping more than SAG mm
  ultimate  the peak over the whole pull (fingers dragged straight, tips hooked
            under the object); an upper bound that relies on the layer being
            inextensible
"""
from __future__ import annotations
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.colors import LinearSegmentedColormap
import sys
HERE = Path(__file__).resolve().parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))
from grasp_model import Grasp, GraspSim, free_curl
from grasp_study import DESIGNS, DEPTHS, PRESSURES, MUS, A_OBJ, PULL, N_INFLATE, N_PULL

ROOT = Path(__file__).resolve().parent.parent
RES, OUT = ROOT / "results" / "grasp", ROOT / "report" / "figures"
SAG = 10.0
INK, MUTED, GRID, SURF = "#0b0b0b", "#52514e", "#e1e0d9", "#fcfcfb"
DESIGN_C = {"uniform": "#2a78d6", "tapered": "#eb6834", "tip-hook": "#1baf7a"}   # categorical slots 1-3
MU_C = dict(zip(MUS, ["#86b6ef", "#3987e5", "#1c5cab", "#0d366b"]))             # blue ramp, ordered by mu
P_C = ["#86b6ef", "#2a78d6", "#104281"]
BLUES = LinearSegmentedColormap.from_list("blues", ["#cde2fb", "#6da7ec", "#2a78d6", "#184f95", "#0d366b"])
plt.rcParams.update({"font.family": "sans-serif", "font.size": 10, "axes.edgecolor": "#c3c2b7",
                     "axes.labelcolor": MUTED, "xtick.color": MUTED, "ytick.color": MUTED, "text.color": INK,
                     "axes.spines.top": False, "axes.spines.right": False, "figure.facecolor": "white"})


def load():
    s = np.genfromtxt(RES / "sweep.csv", delimiter=",", names=True, dtype=None, encoding="utf-8")
    c = np.genfromtxt(RES / "curves.csv", delimiter=",", names=True, dtype=None, encoding="utf-8")
    return s, c


def curve(c, design, depth, P_kPa, mu):
    m = (c["design"] == design) & (c["depth_mm"] == depth) & (c["P_kPa"] == P_kPa) & np.isclose(c["mu"], mu)
    return c[m]


def working(c, design, depth, P_kPa, mu, sag=SAG):
    """(force, normal part, friction part, pull) at the largest upward force within `sag` mm."""
    k = curve(c, design, depth, P_kPa, mu)
    k = k[k["pull_mm"] <= sag + 1e-9]
    i = int(np.argmax(k["Fy_N"]))
    return k["Fy_N"][i], k["Fy_normal_N"][i], k["Fy_friction_N"][i], k["pull_mm"][i]


def best_working(s, c, design, mu):
    rows = [r for r in s if r["design"] == design and np.isclose(r["mu"], mu)]
    w = [working(c, design, r["depth_mm"], r["P_kPa"], mu) for r in rows]
    i = int(np.argmax([x[0] for x in w]))
    return rows[i], w[i]


def best_ultimate(s, design, mu):
    m = (s["design"] == design) & np.isclose(s["mu"], mu)
    return s[m][np.argmax(s[m]["peak_N"])]


def fig_shapes():
    fig, axes = plt.subplots(1, 3, figsize=(11, 4.6), sharey=True)
    for ax, (name, f) in zip(axes, DESIGNS.items()):
        for P, col in zip([0.02, 0.035, 0.05], P_C):
            p, th = free_curl(f, P)
            ax.plot(p[:, 0], p[:, 1], color=col, lw=2, label=f"{P*1e3:.0f} kPa  (tip turns {np.degrees(th):.0f}°)")
        ax.axhline(0, color=MUTED, lw=1)
        ax.set_title(name, color=INK, loc="left", fontsize=11)
        ax.set_aspect("equal")
        ax.set_xlim(-5, 72)
        ax.set_xlabel("x [mm]")
        ax.grid(color=GRID, lw=0.6)
        ax.legend(frameon=False, fontsize=8, loc="lower right" if name == "tip-hook" else "lower left")
    axes[0].set_ylabel("y [mm]  (palm at y = 0)")
    fig.suptitle("Free curl under pressure (no object), E = 0.15 MPa", color=INK, x=0.01, ha="left")
    fig.tight_layout()
    fig.savefig(OUT / "grasp_shapes.png", dpi=200)


def fig_maps(s, c, mu=0.6):
    fig, axes = plt.subplots(1, 3, figsize=(12, 4.2), sharey=True)
    Zs = {}
    for name in DESIGNS:
        Zs[name] = np.array([[working(c, name, d, round(P * 1e3), mu)[0] for P in PRESSURES] for d in DEPTHS])
    vmax = max(Z.max() for Z in Zs.values())
    for ax, name in zip(axes, DESIGNS):
        Z = Zs[name]
        im = ax.imshow(Z, origin="upper", cmap=BLUES, vmin=0, vmax=vmax, aspect="auto")
        for i in range(len(DEPTHS)):
            for j in range(len(PRESSURES)):
                ax.text(j, i, f"{Z[i, j]:.1f}", ha="center", va="center", fontsize=8,
                        color="white" if Z[i, j] > 0.55 * vmax else INK)
        ax.set_xticks(range(len(PRESSURES)), [f"{P*1e3:.0f}" for P in PRESSURES])
        ax.set_yticks(range(len(DEPTHS)), DEPTHS)
        ax.set_xlabel("pressure [kPa]")
        ax.set_title(name, color=INK, loc="left", fontsize=11)
        for sp in ax.spines.values(): sp.set_visible(False)
    axes[0].set_ylabel("object centre below palm [mm]\n(finger length 100 mm)")
    cb = fig.colorbar(im, ax=axes, shrink=0.85, pad=0.01)
    cb.set_label(f"upward force within {SAG:.0f} mm sag [N]")
    cb.outline.set_visible(False)
    fig.suptitle(f"Holding force: object radius {A_OBJ:.0f} mm, friction μ = {mu}", color=INK, x=0.01, ha="left")
    fig.savefig(OUT / "grasp_maps.png", dpi=200, bbox_inches="tight")


def fig_capacity(s, c):
    fig, (a1, a2) = plt.subplots(1, 2, figsize=(11, 4.2))
    for name, col in DESIGN_C.items():
        F = [best_working(s, c, name, mu)[1][0] for mu in MUS]
        a1.plot(MUS, F, color=col, lw=2, marker="o", ms=8, label=name)
        a1.annotate(f"{F[-1]:.1f} N", (MUS[-1], F[-1]), xytext=(6, 0), textcoords="offset points",
                    va="center", color=INK, fontsize=9)
    a1.set_xlabel("friction coefficient μ")
    a1.set_ylabel(f"upward force within {SAG:.0f} mm sag [N]")
    a1.set_title("Holding capacity (best depth and pressure)", loc="left", color=INK, fontsize=11)
    a1.set_xlim(-0.05, 1.2)
    a1.set_ylim(bottom=0)
    a1.grid(color=GRID, lw=0.6)
    a1.legend(frameon=False, fontsize=9, loc="upper left")
    names, w = list(DESIGNS), 0.6
    best = [best_working(s, c, n, 0.6) for n in names]
    nrm = np.array([b[1][1] for b in best])
    fri = np.array([b[1][2] for b in best])
    x = np.arange(len(names))
    a2.bar(x, nrm, w, color="#2a78d6", label="normal forces (geometry)", edgecolor=SURF, linewidth=2)
    a2.bar(x, fri, w, bottom=nrm, color="#eb6834", label="friction forces", edgecolor=SURF, linewidth=2)
    for i, (r, wk) in enumerate(best):
        a2.text(i, nrm[i] + fri[i] + 0.03 * max(nrm + fri), f"{wk[0]:.2f} N\n"
                f"depth {r['depth_mm']} mm, {r['P_kPa']} kPa", ha="center", va="bottom", fontsize=8, color=INK)
    a2.set_xticks(x, names)
    a2.set_ylabel("upward force [N]")
    a2.set_ylim(0, 1.3 * max(nrm + fri))
    a2.set_title("Where the upward force comes from (μ = 0.6)", loc="left", color=INK, fontsize=11)
    a2.legend(frameon=False, fontsize=9, loc="upper right")
    a2.grid(color=GRID, lw=0.6, axis="y")
    fig.tight_layout()
    fig.savefig(OUT / "grasp_capacity.png", dpi=200)


def fig_curves(s, c):
    """Force against pull for the same design at two object depths: a fingertip grasp and a palm grasp."""
    fig, axes = plt.subplots(1, 2, figsize=(11, 4), sharey=True)
    for ax, depth, label in ((axes[0], 70, "fingertip grasp: object 70 mm down"),
                             (axes[1], 30, "palm grasp: object 30 mm down")):
        for mu in MUS:
            k = curve(c, "uniform", depth, 50, mu)
            ax.plot(k["pull_mm"], k["Fy_N"], color=MU_C[mu], lw=2, label=f"μ = {mu}")
        ax.axvspan(0, SAG, color="#f1f0ec", zorder=0)
        ax.text(SAG / 2, 0.97, f"≤ {SAG:.0f} mm\nsag", transform=ax.get_xaxis_transform(), ha="center", va="top",
                fontsize=8, color=MUTED)
        ax.set_title(f"uniform finger, 50 kPa, {label}", loc="left", color=INK, fontsize=10)
        ax.set_xlabel("object pulled down from the grasped position [mm]")
        ax.grid(color=GRID, lw=0.6)
    axes[0].set_ylabel("upward force on object [N]")
    axes[0].legend(frameon=False, fontsize=9, loc="center right")
    fig.tight_layout()
    fig.savefig(OUT / "grasp_curves.png", dpi=200)


def snapshot_at(f, depth, P, mu, target_pull):
    sim = GraspSim(Grasp(f, a=A_OBJ, depth=depth), mu)
    for Pk in np.linspace(0, P, N_INFLATE + 1)[1:]:
        sim.step(P=Pk)
    c0 = sim.c.copy()
    for d in np.linspace(0, PULL, N_PULL + 1)[1:]:
        rec = sim.step(c=c0 + np.array([0.0, -d]))
        if d >= target_pull - 1e-9:
            return sim.snapshot(), rec


def fig_snapshots(s, c, mu=0.6):
    fig, axes = plt.subplots(1, 3, figsize=(12, 5.2))
    for ax, (name, f) in zip(axes, DESIGNS.items()):
        r, wk = best_working(s, c, name, mu)
        sn, rec = snapshot_at(f, float(r["depth_mm"]), r["P_kPa"] / 1e3, mu, wk[3])
        p, fn, ff = sn["nodes"], sn["normal"], sn["friction"]
        scale = 12.0 / max(1e-9, np.max(np.linalg.norm(fn + ff, axis=1)))
        for sgn in (1, -1):
            ax.plot(sgn * p[:, 0], p[:, 1], color=DESIGN_C[name], lw=3, solid_capstyle="round")
        ax.add_patch(plt.Circle(sn["c"], sn["a"], fc="#f1f0ec", ec=MUTED, lw=1.2))
        on = np.linalg.norm(fn, axis=1) > 1e-6
        q = p[1:][on]
        for vec, col, lab in ((fn[on], "#2a78d6", "normal"), (ff[on], "#eb6834", "friction")):
            for sgn in (1, -1):
                ax.quiver(sgn * q[:, 0], q[:, 1], -sgn * vec[:, 0] * scale, -vec[:, 1] * scale, color=col,
                          angles="xy", scale_units="xy", scale=1, width=0.006, label=lab if sgn == 1 else None)
        ax.axhline(0, color=MUTED, lw=1)
        ax.set_aspect("equal")
        ax.set_xlim(-45, 45)
        ax.set_title(f"{name}: {rec['Fy']:.2f} N up after {wk[3]:.1f} mm sag", loc="left", color=INK, fontsize=10)
        ax.set_xlabel("x [mm]")
        ax.grid(color=GRID, lw=0.6)
    axes[0].set_ylabel("y [mm]")
    axes[0].legend(frameon=False, fontsize=8, loc="center left", title="force on the object", title_fontsize=8)
    fig.suptitle(f"Best grasp of each design within {SAG:.0f} mm sag, μ = {mu}, 50 kPa (arrows: contact forces on the object)",
                 color=INK, x=0.01, ha="left")
    fig.tight_layout()
    fig.savefig(OUT / "grasp_snapshots.png", dpi=200)


if __name__ == "__main__":
    s, c = load()
    fig_shapes(); fig_maps(s, c); fig_capacity(s, c); fig_curves(s, c); fig_snapshots(s, c)
    for name in DESIGNS:
        for mu in MUS:
            r, wk = best_working(s, c, name, mu)
            u = best_ultimate(s, name, mu)
            print(f"{name:8s} mu {mu:.1f}: within {SAG:.0f} mm sag {wk[0]:6.2f} N (normal {wk[1]:.2f}, friction {wk[2]:.2f})"
                  f" at depth {r['depth_mm']} mm, {r['P_kPa']} kPa | ultimate {u['peak_N']:6.2f} N")
