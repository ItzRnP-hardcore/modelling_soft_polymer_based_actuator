"""
Animations of the segmented (multi-chamber) finger, built on grasp_model.py.

    python tentacle_animation.py            -> report/animations/*.mp4 and *.gif

1. tentacle_segments: one finger with 4 sealed chambers.  Inflating a chamber
   bends the finger only there, so the bend can be placed anywhere, moved along
   the arm, or combined into a hook or a full spiral.
2. tentacle_grasp: two mirrored 4-chamber fingers.  The chambers inflate one
   after another from base to tip and wrap the object; the object is then
   pulled down and the contact forces it feels (normal and friction) are drawn,
   with the upward force on the object plotted as it is pulled.
"""
from __future__ import annotations
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib import animation
from matplotlib.collections import LineCollection
from matplotlib.colors import LinearSegmentedColormap
import sys
HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))
from grasp_model import Finger, Grasp, GraspSim, free_curl

OUT = ROOT / "report" / "animations"
INK, MUTED, GRID = "#0b0b0b", "#52514e", "#e1e0d9"
NORMAL_C, FRICTION_C = "#2a78d6", "#eb6834"
# chamber colour: pale (no pressure) -> deep blue (full pressure)
PCMAP = LinearSegmentedColormap.from_list("p", ["#cde2fb", "#6da7ec", "#2a78d6", "#104281"])
plt.rcParams.update({"font.family": "sans-serif", "font.size": 10, "axes.edgecolor": "#c3c2b7",
                     "axes.labelcolor": MUTED, "xtick.color": MUTED, "ytick.color": MUTED, "text.color": INK,
                     "axes.spines.top": False, "axes.spines.right": False})

FINGER = Finger(R0=10, Rt=10, t0=1.5, tt=1.5, n_seg=4, E=0.15)
PMAX = 0.050   # [MPa]


def finger_lines(p, f, P_joint, pmax):
    """Line segments of the finger centreline, coloured by the local chamber pressure."""
    seg = np.stack([p[:-1], p[1:]], axis=1)
    return seg, P_joint / pmax


def save(anim, name, fps):
    OUT.mkdir(parents=True, exist_ok=True)
    anim.save(OUT / f"{name}.mp4", writer=animation.FFMpegWriter(fps=fps, bitrate=2400), dpi=130)
    anim.save(OUT / f"{name}.gif", writer=animation.PillowWriter(fps=fps), dpi=80)
    print("saved", name)


# ----------------------------------------------------------------------------
# 1. One finger, chamber patterns
# ----------------------------------------------------------------------------
KEYS = [  # (chamber pressures in kPa, caption)
    ([0, 0, 0, 0], "All chambers vented: the finger hangs straight"),
    ([50, 0, 0, 0], "Chamber 1 only: bends at the base"),
    ([0, 50, 0, 0], "Chamber 2 only: the bend moves along the arm"),
    ([0, 0, 50, 0], "Chamber 3 only"),
    ([0, 0, 0, 50], "Chamber 4 only: bends at the tip"),
    ([0, 0, 50, 50], "Chambers 3 + 4: a hook at the end"),
    ([50, 50, 0, 0], "Chambers 1 + 2: bend near the base, straight tip"),
    ([50, 50, 50, 50], "All chambers: full spiral"),
    ([0, 0, 0, 0], "Vent"),
    ([50, 0, 0, 0], "Reach and wrap: inflate base to tip, one chamber at a time"),
    ([50, 50, 0, 0], "Reach and wrap: inflate base to tip, one chamber at a time"),
    ([50, 50, 50, 0], "Reach and wrap: inflate base to tip, one chamber at a time"),
    ([50, 50, 50, 50], "Reach and wrap: inflate base to tip, one chamber at a time"),
    ([0, 0, 0, 0], "All chambers vented: the finger hangs straight"),
]


def anim_segments(n_move=14, n_hold=10, fps=20):
    frames = []
    for (pa, cap_a), (pb, cap_b) in zip(KEYS[:-1], KEYS[1:]):
        pa, pb = np.array(pa, float), np.array(pb, float)
        for t in np.linspace(0, 1, n_move, endpoint=False):
            w = 0.5 - 0.5 * np.cos(np.pi * t)     # ease in and out
            frames.append(((1 - w) * pa + w * pb, cap_b))
        frames += [(pb, cap_b)] * n_hold

    fig = plt.figure(figsize=(9.6, 5.6))
    ax = fig.add_axes([0.05, 0.08, 0.55, 0.80])
    axb = fig.add_axes([0.70, 0.18, 0.27, 0.55])
    ax.set_xlim(-40, 80); ax.set_ylim(-108, 30); ax.set_aspect("equal")
    ax.axhline(0, color=MUTED, lw=2); ax.grid(color=GRID, lw=0.6)
    ax.set_xlabel("x [mm]"); ax.set_ylabel("y [mm]")
    lc = LineCollection([], linewidths=14, cmap=PCMAP, capstyle="round")
    lc.set_clim(0, 1); ax.add_collection(lc)
    divs, = ax.plot([], [], "o", ms=5, color="white", mec=MUTED, mew=1, zorder=5)
    tip_txt = ax.text(-36, 22, "", fontsize=9, color=MUTED)
    title = fig.text(0.05, 0.94, "", fontsize=12, color=INK)
    bars = axb.bar(range(1, 5), [0] * 4, color="#2a78d6", width=0.6)
    axb.set_ylim(0, PMAX * 1e3 * 1.15); axb.set_xticks(range(1, 5), ["1\nbase", "2", "3", "4\ntip"])
    axb.set_ylabel("chamber pressure [kPa]"); axb.set_title("chambers", loc="left", fontsize=10, color=INK)
    fig.text(0.70, 0.08, f"4 chambers x {FINGER.L/4:.0f} mm, R = {FINGER.R0:.0f} mm,\n"
             f"wall {FINGER.t0} mm, E = {FINGER.E} MPa", fontsize=8, color=MUTED)
    idx = (np.arange(1, 4) * FINGER.N) // 4

    def draw(i):
        pk, cap = frames[i]
        p, th = free_curl(FINGER, pk / 1e3)
        Pj = FINGER.pressure_field(pk / 1e3)
        seg, val = finger_lines(p, FINGER, Pj, PMAX)
        lc.set_segments(seg); lc.set_array(val)
        divs.set_data(p[idx, 0], p[idx, 1])
        for b, v in zip(bars, pk):
            b.set_height(v); b.set_color(PCMAP(v / (PMAX * 1e3)))
        title.set_text(cap)
        tip_txt.set_text(f"tip has turned {np.degrees(th):.0f}°")
        return lc, divs, title, tip_txt, *bars

    anim = animation.FuncAnimation(fig, draw, frames=len(frames), interval=1000 / fps, blit=False)
    save(anim, "tentacle_segments", fps)
    plt.close(fig)


# ----------------------------------------------------------------------------
# 2. Two fingers grasp and hold an object
# ----------------------------------------------------------------------------
def simulate_grasp(mu=0.6, a=12.0, depth=28.0, n_ramp=12, pull=60.0, n_pull=90):
    g = Grasp(FINGER, a=a, depth=depth)
    sim = GraspSim(g, mu)
    states, Pk = [], np.zeros(4)

    def grab(phase, pull_d=0.0, Fy=np.nan):
        sn = sim.snapshot()
        states.append(dict(nodes=sn["nodes"], normal=sn["normal"], friction=sn["friction"], c=sn["c"],
                           P=Pk.copy(), phase=phase, pull=pull_d, Fy=Fy))

    grab("Fingers open around the object")
    for k in range(4):                                   # inflate base to tip
        for w in np.linspace(0, 1, n_ramp + 1)[1:]:
            Pk[k] = w * PMAX
            sim.step(P=Pk.copy())
            grab(f"Inflate chamber {k+1} (base to tip): the fingers wrap the object")
    c0 = sim.c.copy()
    for d in np.linspace(0, pull, n_pull + 1)[1:]:
        rec = sim.step(c=c0 + np.array([0.0, -d]))
        grab("Pull the object down: the contact forces resist", d, rec["Fy"])
    return g, states


def anim_grasp(mu=0.6, fps=20):
    g, states = simulate_grasp(mu)
    peak = np.nanmax([s["Fy"] for s in states])
    fig = plt.figure(figsize=(10.5, 5.8))
    ax = fig.add_axes([0.04, 0.08, 0.50, 0.80])
    axf = fig.add_axes([0.63, 0.50, 0.34, 0.36])
    axb = fig.add_axes([0.63, 0.10, 0.34, 0.24])
    ax.set_xlim(-48, 48); ax.set_ylim(-118, 12); ax.set_aspect("equal")
    ax.axhline(0, color=MUTED, lw=3); ax.grid(color=GRID, lw=0.6)
    ax.set_xlabel("x [mm]"); ax.set_ylabel("y [mm]")
    lcs = []
    for _ in range(2):
        lc = LineCollection([], linewidths=10, cmap=PCMAP, capstyle="round"); lc.set_clim(0, 1)
        ax.add_collection(lc); lcs.append(lc)
    obj = plt.Circle((0, -g.depth), g.a, fc="#f1f0ec", ec=MUTED, lw=1.5, zorder=3); ax.add_patch(obj)
    z = np.zeros(2 * FINGER.N)   # one arrow per node on both fingers; zero length when not in contact
    qn = ax.quiver(z, z, z, z, color=NORMAL_C, angles="xy", scale_units="xy", scale=1, width=0.006, zorder=6)
    qf = ax.quiver(z, z, z, z, color=FRICTION_C, angles="xy", scale_units="xy", scale=1, width=0.006, zorder=6)
    ax.plot([], [], color=NORMAL_C, lw=3, label="normal force on object")
    ax.plot([], [], color=FRICTION_C, lw=3, label="friction force on object")
    ax.legend(frameon=False, fontsize=8, loc="lower left")
    title = fig.text(0.04, 0.94, "", fontsize=12, color=INK)
    fig.text(0.04, 0.905, f"two 4-chamber fingers, object radius {g.a:.0f} mm, friction μ = {mu}, "
             f"{PMAX*1e3:.0f} kPa", fontsize=9, color=MUTED)
    pulls = [s["pull"] for s in states if s["pull"] > 0]
    axf.set_xlim(0, max(pulls)); axf.set_ylim(0, 1.15 * peak)
    axf.set_xlabel("object pulled down [mm]"); axf.set_ylabel("upward force on object [N]")
    axf.grid(color=GRID, lw=0.6)
    line, = axf.plot([], [], color=INK, lw=2)
    dot, = axf.plot([], [], "o", color=INK, ms=6)
    ftxt = axf.text(0.02, 0.92, "", transform=axf.transAxes, fontsize=9, va="top")
    bars = axb.bar(range(1, 5), [0] * 4, width=0.6)
    axb.set_ylim(0, PMAX * 1e3 * 1.15); axb.set_xticks(range(1, 5), ["1 base", "2", "3", "4 tip"])
    axb.set_ylabel("kPa"); axb.set_title("chamber pressures", loc="left", fontsize=9, color=INK)
    fmax = max(np.max(np.linalg.norm(s["normal"] + s["friction"], axis=1)) for s in states)
    scale = 14.0 / max(fmax, 1e-9)
    hist_x, hist_y = [], []

    def draw(i):
        s = states[i]
        p, Pj = s["nodes"], FINGER.pressure_field(s["P"])
        for lc, sgn in zip(lcs, (1, -1)):
            q = p * np.array([sgn, 1.0])
            lc.set_segments(np.stack([q[:-1], q[1:]], axis=1)); lc.set_array(Pj / PMAX)
        obj.center = s["c"]
        q = p[1:]
        for quiv, vec in ((qn, s["normal"]), (qf, s["friction"])):
            X = np.concatenate([q[:, 0], -q[:, 0]]); Y = np.concatenate([q[:, 1], q[:, 1]])
            U = np.concatenate([-vec[:, 0], vec[:, 0]]) * scale; V = np.concatenate([-vec[:, 1], -vec[:, 1]]) * scale
            off = np.hypot(U, V) < 0.05                         # hide arrows of nodes out of contact
            quiv.set_offsets(np.c_[X, Y])
            quiv.set_UVC(np.ma.masked_where(off, U), np.ma.masked_where(off, V))
        if i == 0:
            hist_x.clear(); hist_y.clear()
        if s["pull"] > 0:
            hist_x.append(s["pull"]); hist_y.append(s["Fy"])
            line.set_data(hist_x, hist_y); dot.set_data([hist_x[-1]], [hist_y[-1]])
            ftxt.set_text(f"now {s['Fy']:.2f} N   (peak {max(hist_y):.2f} N)")
        else:
            line.set_data([], []); dot.set_data([], []); ftxt.set_text("")
        for b, v in zip(bars, s["P"] * 1e3):
            b.set_height(v); b.set_color(PCMAP(v / (PMAX * 1e3)))
        title.set_text(s["phase"])
        return (*lcs, obj, qn, qf, line, dot, ftxt, title, *bars)

    frames = list(range(len(states))) + [len(states) - 1] * fps   # hold the last frame 1 s
    anim = animation.FuncAnimation(fig, draw, frames=frames, interval=1000 / fps, blit=False)
    save(anim, "tentacle_grasp", fps)
    plt.close(fig)
    return peak


if __name__ == "__main__":
    anim_segments()
    print("grasp peak upward force %.2f N" % anim_grasp())
