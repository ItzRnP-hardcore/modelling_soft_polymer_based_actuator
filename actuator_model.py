"""
Pressure -> deflection model for an eccentric-cavity cylindrical soft actuator.

Geometry (matches the handwritten notes):
    outer radius R, cavity radius r, cavity centre offset r_p along +y,
    length L.  Thin wall on the +y side (t_min = R - r_p - r),
    thick wall on the -y side (t_max = R + r_p - r).

Three model tiers are implemented:
    Tier 1  linear Euler-Bernoulli with eccentric pressure thrust (closed form,
            equations (1)-(7) of the notes)
    Tier 2  constant-curvature (large-rotation) kinematics on top of Tier 1,
            including the axial stretch from the thrust
    Tier 3  hyperelastic (Yeoh / neo-Hookean) section equilibrium solved
            numerically.  Reduces to Tier 1 for small strain.

All three tiers treat every longitudinal fibre as being in uniaxial stress, i.e.
they ignore the hoop and radial stresses that the pressure sets up in the wall.
That is right when a hoop-fibre sleeve carries those stresses.  For the bare
actuator it is not: exact linear elasticity gives (1 - 2 nu) times the Tier 1
curvature, which is zero for an incompressible silicone, and the bending that is
observed comes from finite-strain ballooning.  Use actuator_fea.py (Tier 4) there.

Units: mm, N, MPa (1 MPa = 1000 kPa = 1 N/mm^2).
"""
from __future__ import annotations
from dataclasses import dataclass
import numpy as np
from scipy.optimize import fsolve

# ----------------------------------------------------------------------------
# 1. System representation
# ----------------------------------------------------------------------------
@dataclass
class Geometry:
    R: float    # outer radius [mm]
    r: float    # cavity radius [mm]
    rp: float   # cavity eccentricity along +y [mm]
    L: float    # actuator length [mm]

    def __post_init__(self):
        assert self.r > 0 and self.R > self.r
        assert self.R - self.rp - self.r > 0, "cavity breaks through the wall"

    @property
    def t_min(self): return self.R - self.rp - self.r
    @property
    def t_max(self): return self.R + self.rp - self.r


@dataclass
class Material:
    """Yeoh coefficients [MPa]. Neo-Hookean = (C10 only), with C10 = E/6."""
    C10: float
    C20: float = 0.0
    C30: float = 0.0

    @classmethod
    def from_E(cls, E: float):
        """Incompressible neo-Hookean from small-strain Young's modulus E."""
        return cls(C10=E / 6.0)

    @property
    def E(self):
        """Small-strain Young's modulus, E = 6*C10 (incompressible)."""
        return 6.0 * self.C10

    def nominal_stress(self, lam):
        """Uniaxial incompressible nominal (1st Piola) stress vs stretch."""
        lam = np.asarray(lam, dtype=float)
        I1 = lam**2 + 2.0 / lam
        dW = self.C10 + 2 * self.C20 * (I1 - 3) + 3 * self.C30 * (I1 - 3) ** 2
        return 2.0 * (lam - lam**-2) * dW


# ----------------------------------------------------------------------------
# 2. Tier 1: linear section properties and closed-form response
# ----------------------------------------------------------------------------
def linear_section(g: Geometry):
    A = np.pi * (g.R**2 - g.r**2)                                   # (1)
    ybar = -g.r**2 * g.rp / (g.R**2 - g.r**2)                       # (2)
    Izz = np.pi * (g.R**4 - g.r**4) / 4 - np.pi * g.R**2 * g.r**2 * g.rp**2 / (g.R**2 - g.r**2)  # (3)
    e = g.R**2 * g.rp / (g.R**2 - g.r**2)                           # (5)
    return dict(A=A, ybar=ybar, Izz=Izz, e=e)


def linear_response(P, g: Geometry, E: float, nu: float | None = None):
    """P in MPa. Returns dict of arrays (Tier 1 + Tier 2 kinematics).
    nu=None is the beam model of the notes (uniaxial fibres; hoop-reinforced actuator).
    Passing Poisson's ratio gives the exact linear-elastic response of the bare
    actuator, in which the in-plane pressure stresses scale kappa and eps0 by (1 - 2 nu)."""
    P = np.atleast_1d(np.asarray(P, dtype=float))
    s = linear_section(g)
    F = P * np.pi * g.r**2                                          # (4)
    M = F * s["e"]                                                  # (6)
    bare = 1.0 if nu is None else 1.0 - 2.0 * nu
    kappa = bare * M / (E * s["Izz"])                               # (7)
    eps0 = bare * F / (E * s["A"])     # axial strain of the centroid
    theta = kappa * g.L                # tip angle [rad]
    # Tier 1 small-deflection tip deflection
    delta_small = kappa * g.L**2 / 2
    # Tier 2 constant-curvature arc, including axial stretch of the centroid
    Lc = g.L * (1 + eps0)
    with np.errstate(divide="ignore", invalid="ignore"):
        rho = np.where(kappa > 0, Lc / theta, np.inf)
        delta_arc = np.where(kappa > 0, rho * (1 - np.cos(theta)), 0.0)
        x_tip = np.where(kappa > 0, rho * np.sin(theta), Lc)
    # peak fibre strain, used as a validity check for the linear model
    eps_max = eps0 + kappa * (g.R - s["ybar"])
    eps_min = eps0 - kappa * (g.R + s["ybar"])
    return dict(P=P, F=F, M=M, kappa=kappa, eps0=eps0, theta=theta,
                delta_small=delta_small, delta_arc=delta_arc, x_tip=x_tip,
                eps_max=eps_max, eps_min=eps_min, **s)


# ----------------------------------------------------------------------------
# 3. Tier 3: hyperelastic section equilibrium
# ----------------------------------------------------------------------------
def section_mesh(g: Geometry, n_r=60, n_t=180):
    """Polar quadrature over the elastomer annulus (outer disc minus cavity)."""
    rr = (np.arange(n_r) + 0.5) / n_r * g.R
    tt = (np.arange(n_t) + 0.5) / n_t * 2 * np.pi
    Rm, Tm = np.meshgrid(rr, tt, indexing="ij")
    y = Rm * np.sin(Tm)
    z = Rm * np.cos(Tm)
    dA = Rm * (g.R / n_r) * (2 * np.pi / n_t)
    inside_cavity = (z**2 + (y - g.rp) ** 2) < g.r**2
    keep = ~inside_cavity
    return y[keep], dA[keep]


def hyperelastic_response(P, g: Geometry, mat: Material, mesh=None):
    """
    Plane sections remain plane: stretch lambda(y) = lam_c + k*(y - rp).
    Unknowns (lam_c, k) from section equilibrium of the free body beyond a cut:
        sum s(lambda) dA            = P*pi*r^2      (axial force)
        sum s(lambda) (y - rp) dA   = 0             (moment about cavity centre:
                                                     the pressure resultant
                                                     passes through it)
    k = dtheta/dS is the rotation per unit *reference* length, so theta = k*L.
    """
    y, dA = mesh if mesh is not None else section_mesh(g)
    P = np.atleast_1d(np.asarray(P, dtype=float))
    s_lin = linear_section(g)
    out = dict(P=P, theta=np.zeros_like(P), kappa=np.zeros_like(P),
               lam_c=np.zeros_like(P), delta=np.zeros_like(P),
               x_tip=np.zeros_like(P), lam_max=np.zeros_like(P), lam_min=np.zeros_like(P))
    guess = np.array([1.0, 0.0])
    for i, p in enumerate(P):
        N = p * np.pi * g.r**2

        def resid(u):
            lam = u[0] + u[1] * (y - g.rp)
            if np.any(lam <= 0.05):
                return np.array([1e6, 1e6])
            st = mat.nominal_stress(lam)
            return np.array([np.sum(st * dA) - N, np.sum(st * (y - g.rp) * dA)])

        sol, info, ier, msg = fsolve(resid, guess, full_output=True)
        if ier != 1:
            raise RuntimeError(f"section solve failed at P={p} MPa: {msg}")
        guess = sol
        lam_c, k = sol
        lam = lam_c + k * (y - g.rp)
        theta = k * g.L
        lam_cent = lam_c + k * (s_lin["ybar"] - g.rp)   # stretch of the centroidal fibre
        if k > 1e-12:
            rho = lam_cent / k
            delta = rho * (1 - np.cos(theta)); x_tip = rho * np.sin(theta)
        else:
            delta = 0.0; x_tip = lam_cent * g.L
        out["theta"][i] = theta; out["kappa"][i] = k; out["lam_c"][i] = lam_c
        out["delta"][i] = delta; out["x_tip"][i] = x_tip
        out["lam_max"][i] = lam.max(); out["lam_min"][i] = lam.min()
    return out


# ----------------------------------------------------------------------------
# 4. Wall-thickness distribution and ballooning indicators (validity checks)
# ----------------------------------------------------------------------------
def wall_thickness(g: Geometry, phi):
    """Radial wall thickness measured from the cavity centre at polar angle phi,
    with phi = 0 pointing to +y (the thin side).  t(0) = t_min, t(pi) = t_max."""
    return np.sqrt(g.R**2 - g.rp**2 * np.sin(phi) ** 2) - g.rp * np.cos(phi) - g.r


def ballooning_index(P, g: Geometry, E: float):
    """Dimensionless hoop-strain estimate in the thinnest wall, P*r/(E*t_min).
    For the reference geometry (R=10, r=4, rp=3) the FE model puts the ballooning
    limit of a bare neo-Hookean actuator at an index of about 0.23."""
    return np.asarray(P) * g.r / (E * g.t_min)


def kappa_wall_heuristic(P, g: Geometry, E: float):
    """The heuristic 'wall expansion' curvature from page 6 of the notes, with the
    sign as defined there: (P r / 2 E R)(1/t_min - 1/t_max).  The FE model confirms
    the mechanism (ballooning bends the bare actuator) but not this formula: the
    real effect is not linear in P."""
    return np.asarray(P) * g.r / (2 * E * g.R) * (1 / g.t_min - 1 / g.t_max)


# ----------------------------------------------------------------------------
# 5. Demo
# ----------------------------------------------------------------------------
if __name__ == "__main__":
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    g = Geometry(R=10.0, r=4.0, rp=3.0, L=100.0)
    mats = {
        "Dragon Skin 30-like, E = 0.6 MPa": Material.from_E(0.60),
        "Ecoflex 00-30-like, E = 0.08 MPa": Material.from_E(0.08),
    }
    P_kPa = np.linspace(0, 60, 61)
    P = P_kPa / 1000.0

    s = linear_section(g)
    print("Section properties:")
    for k_, v in s.items():
        print(f"  {k_:5s} = {v:10.4f}")
    print(f"  t_min = {g.t_min:.3f} mm, t_max = {g.t_max:.3f} mm\n")

    fig, axes = plt.subplots(1, 3, figsize=(15, 4.6))
    mesh = section_mesh(g)
    for name, mat in mats.items():
        lin = linear_response(P, g, mat.E)
        hyp = hyperelastic_response(P, g, mat, mesh)
        print(f"{name}")
        print("   P[kPa]  theta_lin[deg]  theta_hyp[deg]  d_small[mm]  d_arc[mm]  d_hyp[mm]  eps_max  balloon  k_wall/k_thrust")
        for j in range(0, len(P), 10):
            kw = kappa_wall_heuristic(P[j], g, mat.E)
            ratio = kw / lin["kappa"][j] if lin["kappa"][j] > 0 else float("nan")
            print(f"  {P_kPa[j]:6.0f}  {np.degrees(lin['theta'][j]):13.2f}  {np.degrees(hyp['theta'][j]):13.2f}"
                  f"  {lin['delta_small'][j]:10.2f}  {lin['delta_arc'][j]:9.2f}  {hyp['delta'][j]:9.2f}"
                  f"  {lin['eps_max'][j]:7.3f}  {ballooning_index(P[j], g, mat.E):7.3f}  {ratio:8.2f}")
        print()
        lab = name.split(",")[0]
        axes[0].plot(P_kPa, np.degrees(lin["theta"]), "--", label=f"{lab} linear")
        axes[0].plot(P_kPa, np.degrees(hyp["theta"]), "-", label=f"{lab} hyperelastic")
        axes[1].plot(P_kPa, lin["delta_small"], ":", label=f"{lab} small-defl.")
        axes[1].plot(P_kPa, lin["delta_arc"], "--", label=f"{lab} arc (linear)")
        axes[1].plot(P_kPa, hyp["delta"], "-", label=f"{lab} arc (hyperelastic)")
        # deformed centreline at 40 kPa (hyperelastic)
        j = 40
        th = hyp["theta"][j]; k = hyp["kappa"][j]
        lam_cent = hyp["lam_c"][j] + k * (s["ybar"] - g.rp)
        S = np.linspace(0, g.L, 200)
        if k > 1e-12:
            rho = lam_cent / k
            axes[2].plot(rho * np.sin(k * S), -rho * (1 - np.cos(k * S)), label=f"{lab} @ 40 kPa")
        else:
            axes[2].plot(lam_cent * S, 0 * S, label=f"{lab} @ 40 kPa")
    axes[0].set_xlabel("pressure P [kPa]"); axes[0].set_ylabel("tip angle θ [deg]"); axes[0].legend(fontsize=7)
    axes[1].set_xlabel("pressure P [kPa]"); axes[1].set_ylabel("tip deflection δ [mm]"); axes[1].legend(fontsize=7)
    axes[2].set_xlabel("x [mm]"); axes[2].set_ylabel("y [mm]  (bends toward thick wall, -y)")
    axes[2].set_aspect("equal"); axes[2].legend(fontsize=7)
    for ax in axes: ax.grid(alpha=.3)
    fig.suptitle(f"Beam model (hoop-reinforced actuator)  R={g.R} r={g.r} r_p={g.rp} L={g.L} mm")
    fig.tight_layout()
    from pathlib import Path
    fig.savefig(Path(__file__).with_name("actuator_response.png"), dpi=150)
    print("saved actuator_response.png")
