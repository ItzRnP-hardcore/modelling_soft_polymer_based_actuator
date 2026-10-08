"""
Figures for the derivation report and the progress slides, built from the saved
FE runs in results/.  Run from the project root:

    python report/make_figures.py
"""
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.ticker
from matplotlib.collections import PolyCollection
from scipy.spatial import ConvexHull

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "report" / "figures"
OUT.mkdir(parents=True, exist_ok=True)

plt.rcParams.update({"font.size": 10, "axes.spines.top": False, "axes.spines.right": False})
CMAP = "viridis"


def plane_polygons(X, conn, axis, value=0.0, tol=1e-9):
    """Element faces lying on the plane X[:, axis] == value, as node-index lists."""
    on = np.abs(X[:, axis] - value) < tol
    faces, elems = [], []
    for e, c in enumerate(conn):
        nodes = c[on[c]]
        if len(nodes) == 9:
            faces.append(nodes)
            elems.append(e)
    return faces, np.array(elems)


def hull_poly(pts):
    return pts[ConvexHull(pts).vertices]


def load(run):
    d = np.load(ROOT / "results" / run / "results.npz")
    return {k: d[k] for k in d.files}


def deformed_sections():
    """Longitudinal section (symmetry plane z = 0) at several pressures, coloured by stretch."""
    runs = {"bare": load("bare"), "sleeve": load("sleeve")}
    picks = {"bare": [0.04, 0.08, 0.10, None], "sleeve": [0.04, 0.08, 0.10, 0.11]}
    fig, axes = plt.subplots(2, 4, figsize=(13, 5.6), sharex=True, sharey=True)
    vmax = 2.6
    for row, (name, d) in enumerate(runs.items()):
        X, conn = d["X"], d["conn"]
        faces, elems = plane_polygons(X, conn, axis=2)
        for col, P in enumerate(picks[name]):
            i = int(np.argmax(d["P"])) if P is None else int(np.argmin(np.abs(d["P"] - P)))
            x = X + d["u"][i]
            polys = [hull_poly(x[f][:, :2]) for f in faces]
            ax = axes[row, col]
            pc = PolyCollection(polys, array=d["lam_elem"][i][elems], cmap=CMAP,
                                clim=(1, vmax), edgecolors="k", linewidths=0.15)
            ax.add_collection(pc)
            ref = [hull_poly(X[f][:, :2]) for f in faces]
            ax.add_collection(PolyCollection(ref, facecolors="none", edgecolors="0.75",
                                             linewidths=0.3, linestyles=":"))
            label = "limit point" if P is None else ""
            ax.set_title(f"{name}: P = {1000 * d['P'][i]:.1f} kPa {label}", fontsize=9.5)
            ax.set_aspect("equal")
            ax.set_xlim(-5, 112)
            ax.set_ylim(-75, 22)
            if col == 0:
                ax.set_ylabel("y [mm]")
            if row == 1:
                ax.set_xlabel("x [mm]")
    cb = fig.colorbar(pc, ax=axes, shrink=0.85, pad=0.015)
    cb.set_label("largest principal stretch in element")
    fig.suptitle("FE deformed shape, section in the bending plane (dotted: undeformed)", fontsize=11)
    fig.savefig(OUT / "fe_deformed_sections.png", dpi=220, bbox_inches="tight")
    plt.close(fig)


def mesh_cross_section():
    """Base cross-section of the FE mesh (x = 0), mirrored about z = 0 to show the full section."""
    d = load("bare")
    X, conn = d["X"], d["conn"]
    faces, _ = plane_polygons(X, conn, axis=0)
    fig, ax = plt.subplots(figsize=(4.2, 4.2))
    for f in faces:
        p = hull_poly(X[f][:, [2, 1]])                     # (z, y)
        for s in (1, -1):
            q = p.copy()
            q[:, 0] *= s
            ax.fill(q[:, 0], q[:, 1], fc="#cfe0f3" if s > 0 else "#eef3f9", ec="#1f4e79", lw=0.6)
    ax.plot(0, 0, "k+", ms=8)
    ax.plot(0, 3, "r+", ms=8)
    ax.axvline(0, color="0.4", lw=0.8, ls="--")
    ax.text(0.4, 6.1, "symmetry\nplane z = 0", fontsize=7.5, color="0.3")
    ax.text(-11, 10.6, "mirrored half", fontsize=8, color="0.45")
    ax.text(4.5, 10.6, "meshed half", fontsize=8, color="#1f4e79")
    ax.annotate("thin wall\nt_min = 3 mm", xy=(0.3, 9.2), xytext=(11, 10), fontsize=8,
                va="center", annotation_clip=False, arrowprops=dict(arrowstyle="-", lw=0.6))
    ax.annotate("thick wall\nt_max = 9 mm", xy=(0.3, -7.5), xytext=(11, -8), fontsize=8,
                va="center", annotation_clip=False, arrowprops=dict(arrowstyle="-", lw=0.6))
    ax.set_aspect("equal")
    ax.set_xlabel("z [mm]")
    ax.set_ylabel("y [mm]")
    ax.set_title("FE mesh, cross-section (27-node hexahedra)", fontsize=9.5, pad=16)
    fig.savefig(OUT / "fe_mesh_section.png", dpi=220, bbox_inches="tight")
    plt.close(fig)



def response_summary():
    """Clean two-panel figure: tip angle and deflection vs pressure, FE and Tier 1."""
    import csv
    def read(run):
        with open(ROOT / "results" / run / "summary.csv") as f:
            rows = list(csv.DictReader(f))
        return {k: np.array([float(r[k]) for r in rows]) for k in ("P_kPa", "theta_deg", "delta_mm")}
    b, s = read("bare"), read("sleeve")
    R, r, rp, L, E = 10, 4, 3, 100, 0.6
    I = np.pi * (R**4 - r**4) / 4 - np.pi * R**2 * r**2 * rp**2 / (R**2 - r**2)
    P = np.linspace(0, 110, 200)
    kap = np.pi * (P / 1000) * R**2 * r**2 * rp / (E * I * (R**2 - r**2))
    th = kap * L
    dl = (1 - np.cos(th)) / np.where(kap > 0, kap, 1)
    fig, axes = plt.subplots(1, 2, figsize=(10, 3.8))
    for ax, key, beam, lab in [(axes[0], "theta_deg", np.degrees(th), "tip angle θ [deg]"),
                               (axes[1], "delta_mm", dl, "tip deflection δ [mm]")]:
        ax.plot(P, beam, color="#e4572e", lw=2, label="beam theory (Euler–Bernoulli)")
        ax.plot(s["P_kPa"], s[key], "o-", ms=3.5, color="#f0a202", lw=2, label="FE, hoop-fibre sleeve")
        ax.plot(b["P_kPa"], b[key], "o-", ms=3.5, color="#2274a5", lw=2, label="FE, bare actuator")
        ax.axvline(104.65, color="#c0392b", lw=1, ls="--")
        ax.text(103, ax.get_ylim()[1] * 0.05 if False else 2, "ballooning\nlimit 104.7 kPa",
                ha="right", fontsize=8, color="#c0392b")
        ax.set_xlabel("cavity pressure P [kPa]")
        ax.set_ylabel(lab)
        ax.grid(alpha=0.3)
    axes[0].legend(fontsize=8, loc="upper left", frameon=False)
    fig.suptitle("R = 10, r = 4, r_p = 3, L = 100 mm, neo-Hookean E = 0.6 MPa", fontsize=10)
    fig.tight_layout()
    fig.savefig(OUT / "fe_response.png", dpi=220, bbox_inches="tight")
    plt.close(fig)


def read_summary(run):
    import csv
    with open(ROOT / "results" / run / "summary.csv") as f:
        rows = list(csv.DictReader(f))
    out = {k: np.array([float(r[k]) for r in rows]) for k in rows[0] if k != "mode"}
    out["mode"] = np.array([r["mode"] for r in rows])
    return out


def mesh_3d():
    """Half model in 3-D: undeformed, bare near its limit, sleeved at 110 kPa."""
    import sys
    sys.path.insert(0, str(ROOT))
    from mpl_toolkits.mplot3d.art3d import Poly3DCollection
    from actuator_model import Geometry
    import actuator_fea as fea
    m = fea.build_mesh(Geometry(R=10, r=4, rp=3, L=100), **fea.MESH_PRESETS["medium"])
    corners = [0, 2, 8, 6]
    runs = {"bare": load("bare"), "sleeve": load("sleeve")}
    assert np.allclose(m["X"], runs["bare"]["X"])
    X = m["X"]
    sym, sym_el = plane_polygons(X, m["conn"], axis=2)
    sym_order = [f[ConvexHull(X[f][:, :2]).vertices] for f in sym]
    cases = [("Undeformed half model (588 hex27)", "bare", 0),
             ("Bare, P = 100 kPa", "bare", int(np.argmin(np.abs(runs["bare"]["P"] - 0.1)))),
             ("Sleeved, P = 110 kPa", "sleeve", -1)]
    fig = plt.figure(figsize=(15, 4.6))
    for k, (title, run, i) in enumerate(cases):
        d = runs[run]
        x = X + d["u"][i]
        lam = d["lam_elem"][i]
        ax = fig.add_subplot(1, 3, k + 1, projection="3d")
        outer = Poly3DCollection([x[f[corners]][:, [0, 2, 1]] for f in m["outer"]],
                                 facecolor="#cfe0f3", edgecolor="#1f4e79", linewidths=0.25, alpha=0.55)
        cmap = plt.get_cmap(CMAP)
        cols = cmap(np.clip((lam[sym_el] - 1) / 1.6, 0, 1))
        cut = Poly3DCollection([x[f][:, [0, 2, 1]] for f in sym_order], facecolors=cols,
                               edgecolor="k", linewidths=0.15)
        ax.add_collection3d(outer)
        ax.add_collection3d(cut)
        ax.set_xlim(0, 105); ax.set_ylim(-5, 40); ax.set_zlim(-55, 15)
        ax.set_box_aspect((105, 45, 70))
        ax.view_init(elev=14, azim=-72)
        ax.set_xlabel("x [mm]", labelpad=6); ax.set_zlabel("y [mm]"); ax.set_yticks([])
        ax.set_title(title, fontsize=11)
    sm = plt.cm.ScalarMappable(cmap=CMAP, norm=plt.Normalize(1, 2.6))
    fig.subplots_adjust(right=0.9, wspace=0.08)
    cb = fig.colorbar(sm, cax=fig.add_axes([0.93, 0.22, 0.01, 0.56]))
    cb.set_label("largest principal stretch (cut face)")
    fig.savefig(OUT / "fe_mesh_3d.png", dpi=200, bbox_inches="tight")
    plt.close(fig)


def verification():
    """The four checks of fea_verify.py, read from results/verification.log."""
    import re
    import sys
    sys.path.insert(0, str(ROOT))
    from scipy.integrate import quad
    from actuator_model import Material
    log = (ROOT / "results" / "verification.log").read_text()
    uni = re.findall(r"(neo-Hookean|Yeoh)\s+lambda = ([\d.]+)\s+FE\s+([\d.]+)", log)
    tube = re.findall(r"P/mu = ([\d.]+)\s+a/A = ([\d.]+)", log)
    nu = re.findall(r"nu = ([\d.]+)\s+FE\s+([\d.-]+)", log)
    conv = re.findall(r"(coarse|medium|fine)\s+(\d+) elements.*?delta\s+([\d.]+) mm.*?delta\s+([\d.]+) mm", log)

    fig, ax = plt.subplots(1, 4, figsize=(16, 3.8))
    lam = np.linspace(1, 3, 100)
    for name, mat, c in [("neo-Hookean", Material(0.1), "#2274a5"), ("Yeoh", Material(0.1, 0.01, 0.002), "#e4572e")]:
        ax[0].plot(lam, mat.nominal_stress(lam), color=c, lw=1.8, label=f"exact, {name}")
        pts = np.array([(float(l), float(s)) for n, l, s in uni if n == name])
        ax[0].plot(pts[:, 0], pts[:, 1], "o", color=c, mfc="white", ms=6, label=f"FE, {name}")
    ax[0].set(xlabel="stretch λ", ylabel="nominal stress [MPa]", title="(a) Uniaxial tension")
    ax[0].legend(fontsize=7.5, frameon=False)

    A, B, mu = 4.0, 10.0, 0.2
    def exact(a):
        c = a**2 - A**2
        f = lambda rho: mu * (rho**2 / (rho**2 - c) - (rho**2 - c) / rho**2) / rho
        return quad(f, a, np.sqrt(B**2 + c))[0] / mu
    aa = np.linspace(1, 2.0, 80)
    ax[1].plot(aa, [exact(a * A) for a in aa], color="#2274a5", lw=1.8, label="exact (neo-Hookean)")
    pts = np.array([(float(a), float(p)) for p, a in tube])
    ax[1].plot(pts[:, 0], pts[:, 1], "o", color="#e4572e", mfc="white", ms=6, label="FE")
    ax[1].set(xlabel="inner radius a / A", ylabel="P / μ", title="(b) Thick-walled tube inflation")
    ax[1].legend(fontsize=8, frameon=False)

    pts = np.array([(float(n), float(r)) for n, r in nu])
    nn = np.linspace(0, 0.5, 50)
    ax[2].plot(nn, 1 - 2 * nn, color="#2274a5", lw=1.8, label="exact: 1 − 2ν")
    ax[2].plot(pts[:, 0], pts[:, 1], "o", color="#e4572e", mfc="white", ms=6, label="FE")
    ax[2].set(xlabel="Poisson's ratio ν", ylabel="κ_FE / κ_beam", title="(c) Small-pressure bending")
    ax[2].legend(fontsize=8, frameon=False)

    ne = np.array([int(c[1]) for c in conv])
    d30, d45 = np.array([float(c[2]) for c in conv]), np.array([float(c[3]) for c in conv])
    ax[3].plot(ne, 100 * (d30 / d30[-1] - 1), "o-", color="#2274a5", label="P/μ = 0.30")
    ax[3].plot(ne, 100 * (d45 / d45[-1] - 1), "s-", color="#e4572e", label="P/μ = 0.45")
    ax[3].axhline(0, color="0.5", lw=0.8)
    for n, lab in zip(ne, [c[0] for c in conv]):
        ax[3].annotate(lab, (n, 0.15), ha="center", fontsize=8, color="0.35")
    ax[3].set(xscale="log", xlabel="number of elements", ylabel="δ error vs fine mesh [%]",
              title="(d) Mesh convergence")
    ax[3].set_xticks(ne, [str(n) for n in ne])
    ax[3].xaxis.set_minor_locator(matplotlib.ticker.NullLocator())
    ax[3].legend(fontsize=8, frameon=False)
    for a in ax:
        a.grid(alpha=0.3)
    fig.tight_layout()
    fig.savefig(OUT / "fe_verification.png", dpi=200, bbox_inches="tight")
    plt.close(fig)

    # (1 - 2 nu) check alone, for the derivation slide
    fig, a = plt.subplots(figsize=(4.6, 3.4))
    pts = np.array([(float(n), float(r)) for n, r in nu])
    a.plot(nn, 1 - 2 * nn, color="#2274a5", lw=2, label="theory: 1 − 2ν")
    a.plot(pts[:, 0], pts[:, 1], "o", color="#e4572e", mfc="white", ms=7, mew=2, label="3-D FE")
    a.annotate("silicone\nν ≈ 0.5", (0.4995, 0.0012), (0.33, 0.25), fontsize=9,
               arrowprops=dict(arrowstyle="->", lw=0.8))
    a.set(xlabel="Poisson's ratio ν", ylabel="FE curvature / beam curvature")
    a.grid(alpha=0.3); a.legend(frameon=False, fontsize=9)
    fig.tight_layout()
    fig.savefig(OUT / "fe_poisson_check.png", dpi=220, bbox_inches="tight")
    plt.close(fig)


def limit_point():
    """Pressure against cavity volume: pressure control, then volume control past the peak."""
    b, s = read_summary("bare"), read_summary("sleeve")
    V0 = b["V_mm3"][0]
    fig, ax = plt.subplots(figsize=(6.4, 4.2))
    ax.plot(s["V_mm3"] / V0, s["P_kPa"], "o-", ms=3.5, color="#f0a202", label="sleeved (pressure control)")
    pc, vc = b["mode"] == "pressure", b["mode"] == "volume"
    ax.plot(b["V_mm3"][pc] / V0, b["P_kPa"][pc], "o-", ms=3.5, color="#2274a5", label="bare, pressure control")
    ax.plot(b["V_mm3"][vc] / V0, b["P_kPa"][vc], "s-", ms=4, color="#c0392b", label="bare, volume control")
    i = np.argmax(b["P_kPa"])
    ax.annotate(f"limit point\nP = {b['P_kPa'][i]:.1f} kPa", (b["V_mm3"][i] / V0, b["P_kPa"][i]),
                (2.6, 80), fontsize=9, arrowprops=dict(arrowstyle="->", lw=0.8))
    ax.set(xlabel="cavity volume V / V₀", ylabel="cavity pressure P [kPa]")
    ax.grid(alpha=0.3); ax.legend(frameon=False, fontsize=8.5, loc="lower right")
    fig.tight_layout()
    fig.savefig(OUT / "fe_limit_point.png", dpi=220, bbox_inches="tight")
    plt.close(fig)


def wall_state():
    """Cavity volume, thin-wall thickness and peak stretch against pressure."""
    b, s = read_summary("bare"), read_summary("sleeve")
    fig, ax = plt.subplots(1, 3, figsize=(13, 3.6))
    for d, c, lab in [(s, "#f0a202", "sleeved"), (b, "#2274a5", "bare")]:
        ax[0].plot(d["P_kPa"], d["V_mm3"] / d["V_mm3"][0], "o-", ms=3, color=c, label=lab)
        ax[1].plot(d["P_kPa"], 100 * d["wall_thin"], "o-", ms=3, color=c, label=lab)
        ax[2].plot(d["P_kPa"], d["lam_max"], "o-", ms=3, color=c, label=lab)
    ax[0].set(ylabel="cavity volume V / V₀", title="Cavity grows")
    ax[1].set(ylabel="thin wall, % of original", title="Thin wall thins")
    ax[2].set(ylabel="peak principal stretch", title="Peak stretch")
    for a in ax:
        a.axvline(104.65, color="#c0392b", lw=0.9, ls="--")
        a.set_xlabel("cavity pressure P [kPa]")
        a.grid(alpha=0.3)
        a.legend(frameon=False, fontsize=9)
    fig.tight_layout()
    fig.savefig(OUT / "fe_wall_state.png", dpi=200, bbox_inches="tight")
    plt.close(fig)


def scaling():
    """Bare-actuator curvature at low pressure: FE against the beam law and the P^2 estimate."""
    b = read_summary("bare")
    R, r, rp, E = 10, 4, 3, 0.6
    I = np.pi * (R**4 - r**4) / 4 - np.pi * R**2 * r**2 * rp**2 / (R**2 - r**2)
    P = np.linspace(2, 60, 100)
    Pm = P / 1000
    k_beam = np.pi * Pm * R**2 * r**2 * rp / (E * I * (R**2 - r**2))
    eh = lambda t: 0.75 * Pm * r / (E * t)
    k_wall = (2 / 3) * (eh(3.0)**2 - eh(9.0)**2) / 14.0
    sel = (b["P_kPa"] > 0) & (b["P_kPa"] <= 60)
    fig, ax = plt.subplots(figsize=(5.6, 4.0))
    ax.loglog(P, k_beam, color="#e4572e", lw=2, label="beam theory (slope 1)")
    ax.loglog(P, k_wall, color="#2274a5", lw=2, ls="--", label="⅔ε² estimate (slope 2)")
    ax.loglog(b["P_kPa"][sel], b["kappa_mid_1_per_mm"][sel], "o", color="#1f4e79", ms=5,
              mfc="white", mew=1.6, label="3-D FE, bare actuator")
    ax.set(xlabel="cavity pressure P [kPa]", ylabel="mid-span curvature κ [1/mm]")
    ax.set_xticks([2, 5, 10, 20, 50], ["2", "5", "10", "20", "50"])
    ax.xaxis.set_minor_locator(matplotlib.ticker.NullLocator())
    ax.grid(alpha=0.3, which="both"); ax.legend(frameon=False, fontsize=9)
    fig.tight_layout()
    fig.savefig(OUT / "fe_scaling.png", dpi=220, bbox_inches="tight")
    plt.close(fig)


if __name__ == "__main__":
    deformed_sections()
    mesh_cross_section()
    response_summary()
    mesh_3d()
    verification()
    limit_point()
    wall_state()
    scaling()
    print("figures written to", OUT)
