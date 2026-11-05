"""
Grasp model: how much upward force can a pair of pneumatic fingers exert on a
rigid object before it slips out?

Each finger is a hoop-reinforced silicone tube with a strain-limiting layer
(an inextensible strip) along the side that faces the object.  The layer pins
the neutral axis, so the finger is modelled as a planar inextensible Kirchhoff
rod whose reference line is the layer itself, i.e. the contact surface.

Pressure enters as a spontaneous curvature.  Cut the finger at arc length s and
keep the tip piece together with the fluid in it: the fluid beyond the cut
pushes with P*pi*r(s)^2 through the cavity centre, a lever d(s) from the layer,
so

    m(s) = EI(s) * (kappa(s) - kappa0(s)),   kappa0(s) = P*pi*r(s)^2*d(s) / EI(s)

where m is the moment of the external (contact) loads.  A section without the
layer has no lever arm about a pinned axis (concentric cavity, sleeve only) and
gets kappa0 = 0: it only lengthens, which this inextensible model ignores.

Sections may taper: outer radius R(s), wall t(s) linear in s, concentric
cavity r = R - t.  For thin walls kappa0 ~ P/(3 E t): the wall thickness, not
the outer radius, sets how tightly a section curls.

Segmented fingers (n_seg > 1) have that many sealed chambers in series, each
with its own pressure: a cut inside chamber k sees only P_k, so kappa0(s) is
piecewise constant and a finger bends only where its chambers are inflated.

Grasp set-up (planar, two mirrored fingers): the palm is the line y = 0, the
fingers hang down from x = -b and x = +b with their layers facing the object,
a rigid cylinder of radius a centred on x = 0.  Only the left finger is
solved; the right one is its mirror, and the plane x = 0 acts as a frictionless
wall (the fingers may touch each other under the object).

Contact: penalty normal force, Coulomb friction with stick/slip from an
elastic-plastic tangential spring (closed-form return map, convex and C1),
normal force lagged one fixed-point iteration.  Loading is quasi-static:
  1. inflate 0 -> P with the object held still,
  2. pull the object down (displacement control) and record the force the
     fingers exert on it.  The peak upward force is the holding capacity.

Units: mm, N, MPa.  Forces are for the whole actuator section (the planar rod
carries the resultant across the finger width).  Linear beam constitutive law;
see the caveats in the study script before reading large curls quantitatively.
"""
from __future__ import annotations
from dataclasses import dataclass
import numpy as np
import jax
import jax.numpy as jnp
from scipy.optimize import minimize

jax.config.update("jax_enable_x64", True)


# ----------------------------------------------------------------------------
# 1. Finger and object
# ----------------------------------------------------------------------------
@dataclass
class Finger:
    L: float = 100.0       # length [mm]
    R0: float = 10.0       # outer radius at the base [mm]
    Rt: float = 10.0       # outer radius at the tip [mm]
    t0: float = 2.0        # wall thickness at the base [mm]
    tt: float = 2.0        # wall thickness at the tip [mm]
    layer_from: float = 0.0  # layer covers s >= layer_from * L
    E: float = 0.15        # small-strain Young's modulus [MPa]
    N: int = 100           # rod segments
    n_seg: int = 1         # independently pressurised chambers of equal length, base to tip

    @property
    def ds(self): return self.L / self.N

    def pressure_field(self, P):
        """Pressure at every rod joint [MPa].  P is one value (all chambers) or one value per chamber.
        Each chamber is sealed, so a cut inside chamber k only sees that chamber's pressure."""
        P = np.atleast_1d(np.asarray(P, float))
        if P.size == 1:
            return np.full(self.N, P[0])
        assert P.size == self.n_seg, f"need {self.n_seg} chamber pressures"
        seg = np.minimum((np.arange(self.N) * self.n_seg) // self.N, self.n_seg - 1)
        return P[seg]

    def sections(self):
        """Joint positions s, bending stiffness EI(s) [N mm^2], pressure moment per unit P [mm^3]."""
        s = np.arange(self.N) * self.ds
        R = self.R0 + (self.Rt - self.R0) * s / self.L
        t = self.t0 + (self.tt - self.t0) * s / self.L
        r = R - t
        assert np.all(r > 0), "wall thicker than the radius"
        Ic = np.pi * (R**4 - r**4) / 4
        A = np.pi * (R**2 - r**2)
        layer = s >= self.layer_from * self.L - 1e-9
        I = np.where(layer, Ic + A * R**2, Ic)           # about the layer, or the centroid
        mp = np.where(layer, np.pi * r**2 * R, 0.0)      # lever = R from layer to cavity centre
        return s, self.E * I, mp

    def kappa0(self, P):
        s, EI, mp = self.sections()
        return self.pressure_field(P) * mp / EI


@dataclass
class Grasp:
    finger: Finger
    a: float = 15.0        # object radius [mm]
    gap: float = 1.0       # initial clearance between layer and object [mm]
    depth: float = 50.0    # object centre below the palm [mm]
    kn: float = 20.0       # contact penalty per node [N/mm]

    @property
    def b(self): return self.a + self.gap


# ----------------------------------------------------------------------------
# 2. Energy
# ----------------------------------------------------------------------------
def _build(g: Grasp):
    f = g.finger
    _, EI, mp = f.sections()
    EI, mp, ds = jnp.asarray(EI), jnp.asarray(mp), f.ds
    base = jnp.array([-g.b, 0.0])
    a, kn, kt = g.a, g.kn, g.kn

    def nodes(theta):
        t = jnp.stack([jnp.sin(theta), -jnp.cos(theta)], axis=1)   # tangent, theta = 0 points down
        return base + ds * jnp.cumsum(t, axis=0)                    # nodes 1..N

    def arc(p, c):
        d = p - c
        return a * jnp.arctan2(d[:, 1], -d[:, 0])   # angle from the -x direction; cut lies at x > 0

    def parts(theta, P, c, xi, fn_old, mu):
        kap = jnp.diff(theta, prepend=0.0) / ds                     # clamp angle 0 at the palm
        Ub = 0.5 * jnp.sum(EI * (kap - P * mp / EI) ** 2) * ds
        p = nodes(theta)
        dist = jnp.sqrt(jnp.sum((p - c) ** 2, axis=1))
        pen = jnp.maximum(a - dist, 0.0)
        Un = 0.5 * kn * jnp.sum(pen**2)
        Uw = 0.5 * kn * jnp.sum(jnp.maximum(p[:, 0], 0.0) ** 2)     # mirror finger / symmetry plane
        dd = arc(p, c) - xi
        q = mu * fn_old / kt
        Uf = jnp.sum(jnp.where(jnp.abs(dd) <= q, 0.5 * kt * dd**2,
                               mu * fn_old * (jnp.abs(dd) - 0.5 * q)))
        return Ub, Un, Uw, Uf

    total = lambda *args: sum(parts(*args))
    fns = dict(
        nodes=jax.jit(nodes),
        arc=jax.jit(arc),
        U=jax.jit(total),
        dU=jax.jit(jax.grad(total)),
        d2U=jax.jit(jax.hessian(total)),
        # force the finger exerts on the object = -dU/dc (normal and friction parts)
        Fn=jax.jit(jax.grad(lambda *a_: parts(*a_)[1], argnums=2)),
        Ff=jax.jit(jax.grad(lambda *a_: parts(*a_)[3], argnums=2)),
        pen=jax.jit(lambda th, c: jnp.maximum(a - jnp.sqrt(jnp.sum((nodes(th) - c) ** 2, axis=1)), 0.0)),
    )
    return fns


# ----------------------------------------------------------------------------
# 3. Quasi-static solver
# ----------------------------------------------------------------------------
class GraspSim:
    def __init__(self, g: Grasp, mu: float):
        self.g, self.mu = g, mu
        self.k = _build(g)
        N = g.finger.N
        self.theta = np.zeros(N)
        self.xi = np.zeros(N)
        self.fn = np.zeros(N)
        self.c = np.array([0.0, -g.depth])
        self.P = np.zeros(N)              # pressure at every joint
        self.xi = np.asarray(self.k["arc"](self.k["nodes"](self.theta), self.c))
        self.history = []

    def _solve(self, fn_lag):
        k, args = self.k, (self.P, self.c, self.xi, fn_lag, self.mu)
        fun = lambda x: float(k["U"](x, *args))
        jac = lambda x: np.asarray(k["dU"](x, *args))
        hess = lambda x: np.asarray(k["d2U"](x, *args))
        try:
            res = minimize(fun, self.theta, jac=jac, hess=hess, method="trust-exact",
                           options=dict(gtol=1e-9, maxiter=500))
            if np.all(np.isfinite(res.x)):
                return res.x, res
        except (ValueError, np.linalg.LinAlgError):
            pass   # trust-exact's subproblem can break down on a near-singular Hessian
        res = minimize(fun, self.theta, jac=jac, hess=hess, method="trust-krylov",
                       options=dict(gtol=1e-9, maxiter=2000))
        return res.x, res

    def step(self, P=None, c=None, iters=60, tol=1e-5):
        """One load increment.  The Coulomb limit uses a lagged normal force, updated by
        fixed-point iteration.  At high friction the plain update overshoots, so it is
        under-relaxed, and the relaxation is halved whenever the residual grows."""
        if P is not None: self.P = self.g.finger.pressure_field(P)
        if c is not None: self.c = np.asarray(c, float)
        k, kn = self.k, self.g.kn
        theta0, fn_lag, w, last = self.theta.copy(), self.fn.copy(), 1.0, np.inf
        for _ in range(iters):
            self.theta = theta0.copy()          # solve each pass from the start of the step
            theta, res = self._solve(fn_lag)
            self.theta = theta
            fn_new = kn * np.asarray(k["pen"](theta, self.c))
            resid = float(np.max(np.abs(fn_new - fn_lag)))
            if resid < tol:
                break
            if resid > last:
                w = max(0.5 * w, 0.05)
            last = resid
            fn_lag = fn_lag + w * (fn_new - fn_lag)
        args = (self.theta, self.P, self.c, self.xi, fn_lag, self.mu)
        F_n = -np.asarray(k["Fn"](*args))
        F_f = -np.asarray(k["Ff"](*args))
        # friction update (return map) and anchor reset for nodes out of contact
        u = np.asarray(k["arc"](k["nodes"](self.theta), self.c))
        q = self.mu * fn_new / kn
        self.xi = np.where(fn_new > 0, u - np.clip(u - self.xi, -q, q), u)
        self.fn = fn_new
        rec = dict(P=float(np.max(self.P)), cy=self.c[1], Fy=2 * (F_n[1] + F_f[1]), Fy_normal=2 * F_n[1],
                   Fy_friction=2 * F_f[1], Fx_finger=F_n[0] + F_f[0], n_contact=int(np.sum(fn_new > 0)),
                   max_pen=float(np.max(fn_new) / kn), converged=resid < 1e-3, residual_N=resid)
        self.history.append(rec)
        return rec

    def nodes(self):
        return np.vstack([[-self.g.b, 0.0], np.asarray(self.k["nodes"](self.theta))])

    def snapshot(self):
        """Node positions, normal and friction force on each node (from the object)."""
        k, g = self.k, self.g
        p = np.asarray(k["nodes"](self.theta))
        d = p - self.c
        nrm = d / np.linalg.norm(d, axis=1)[:, None]
        tan = np.stack([nrm[:, 1], -nrm[:, 0]], axis=1)       # direction of increasing arc()
        u = np.asarray(k["arc"](p, self.c))
        ft = -g.kn * (u - self.xi) if self.mu > 0 else np.zeros_like(u)   # xi is already return-mapped
        return dict(nodes=self.nodes(), normal=self.fn[:, None] * nrm, friction=ft[:, None] * tan,
                    c=self.c.copy(), a=g.a)


def run_grasp(g: Grasp, mu: float, P, n_inflate=25, pull=40.0, n_pull=80, keep=None):
    """Inflate around the object (P: one pressure, or one per chamber, all ramped together),
    then pull it down.  Returns (history, snapshots)."""
    sim = GraspSim(g, mu)
    snaps = {}
    for Pk in np.linspace(0, P, n_inflate + 1)[1:]:
        sim.step(P=Pk)
    snaps["grasped"] = sim.snapshot()
    best, c0 = -np.inf, sim.c.copy()
    for d in np.linspace(0, pull, n_pull + 1)[1:]:
        rec = sim.step(c=c0 + np.array([0.0, -d]))
        rec["pull"] = d
        if rec["Fy"] > best:
            best, snaps["peak"] = rec["Fy"], sim.snapshot()
        if keep is not None and d >= keep and "late" not in snaps:
            snaps["late"] = sim.snapshot()
    return sim.history, snaps


def free_curl(f: Finger, P: float):
    """Unloaded shape: nodes of the clamped rod curling under pressure only."""
    theta = np.cumsum(f.kappa0(P)) * f.ds
    t = np.stack([np.sin(theta), -np.cos(theta)], axis=1)
    return np.vstack([[0.0, 0.0], f.ds * np.cumsum(t, axis=0)]), theta[-1]
