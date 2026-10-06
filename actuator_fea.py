"""
Tier 4: nonlinear 3-D finite-element model of the eccentric-cavity actuator.

Geometry and symbols follow actuator_model.py: outer radius R, cavity radius r,
cavity centre at y = +r_p, pressurised length L.  The cavity is closed by a solid
cap of thickness t_cap at x = L and the base face x = 0 is clamped.  Only the
half z >= 0 is meshed (symmetry about the bending plane).

Formulation:
    elements   27-node hexahedra, 3x3x3 Gauss points
    material   Yeoh / neo-Hookean, isochoric-volumetric split,
               W = C10(I1b-3) + C20(I1b-3)^2 + C30(I1b-3)^3 + p(J-1) - p^2/(2 kappa)
    locking    mixed Q2/P1 element: the pressure p is linear and discontinuous in
               each element and is condensed out element by element
    load       follower pressure from the potential -P*V, V = enclosed cavity volume
    solver     Newton, tangent by automatic differentiation (jax), pressure control,
               switching to volume control when a pressure step fails (limit point)

Units: mm, N, MPa (1 MPa = 1000 kPa = 1 N/mm^2).
"""
from __future__ import annotations
import argparse
import json
import time
from pathlib import Path
import numpy as np
import scipy.sparse as sp
import scipy.sparse.linalg as spla
from scipy.sparse.csgraph import connected_components
from scipy.spatial import cKDTree
import jax
import jax.numpy as jnp

from actuator_model import Geometry, Material

jax.config.update("jax_enable_x64", True)

# ----------------------------------------------------------------------------
# 1. Reference elements: 27-node hexahedron, 9-node face, 3-point Gauss rule
# ----------------------------------------------------------------------------
def _lagrange(x):
    """1-D quadratic Lagrange basis on nodes (-1, 0, 1) and its derivative."""
    x = np.asarray(x, dtype=float)
    N = np.stack([x * (x - 1) / 2, 1 - x**2, x * (x + 1) / 2], -1)
    dN = np.stack([x - 0.5, -2 * x, x + 0.5], -1)
    return N, dN


_GP = np.sqrt(0.6) * np.array([-1.0, 0.0, 1.0])
_GW = np.array([5.0, 8.0, 5.0]) / 9.0
_N1, _D1 = _lagrange(_GP)                                   # (gauss point, node)

# local node a + 3b + 9c, gauss point 9i + 3j + k  (a,i along xi; b,j eta; c,k zeta)
def _tp3(A, B, C): return np.einsum("ia,jb,kc->ijkcba", A, B, C).reshape(27, 27)
def _tp2(A, B): return np.einsum("ia,jb->ijba", A, B).reshape(9, 9)

DN3 = np.stack([_tp3(_D1, _N1, _N1), _tp3(_N1, _D1, _N1), _tp3(_N1, _N1, _D1)], -1)  # (g, a, 3)
W3 = np.einsum("i,j,k->ijk", _GW, _GW, _GW).ravel()
PSI = np.c_[np.ones(27), np.stack(np.meshgrid(_GP, _GP, _GP, indexing="ij"), -1).reshape(27, 3)]
N2 = _tp2(_N1, _N1)                                                                   # (g, a)
DN2 = np.stack([_tp2(_D1, _N1), _tp2(_N1, _D1)], -1)                                  # (g, a, 2)
W2 = np.einsum("i,j->ij", _GW, _GW).ravel()


# ----------------------------------------------------------------------------
# 2. Mesh
# ----------------------------------------------------------------------------
MESH_PRESETS = {
    "coarse": dict(n_phi=8, n_r=2, n_x=12, n_cap=1, n_core=1),
    "medium": dict(n_phi=12, n_r=2, n_x=20, n_cap=2, n_core=1),
    "fine": dict(n_phi=16, n_r=3, n_x=30, n_cap=2, n_core=2),
}


def _hexes(G):
    """hex27 connectivity from a structured node-id array of shape (2m0+1, 2m1+1, 2m2+1)."""
    w = np.lib.stride_tricks.sliding_window_view(G, (3, 3, 3))[::2, ::2, ::2]
    return w.transpose(0, 1, 2, 5, 4, 3).reshape(-1, 27)


def _quads(G):
    """quad9 connectivity from a structured node-id array of shape (2m0+1, 2m1+1)."""
    w = np.lib.stride_tricks.sliding_window_view(G, (3, 3))[::2, ::2]
    return w.transpose(0, 1, 3, 2).reshape(-1, 9)


def merge_blocks(blocks, tol):
    """Merge coincident points of structured blocks.  Nodes are numbered along x so
    that the stiffness matrix stays banded.  Returns (X, list of node-id arrays)."""
    pts = np.concatenate([B.reshape(-1, 3) for B in blocks])
    pairs = cKDTree(pts).query_pairs(tol, output_type="ndarray")
    adj = sp.coo_matrix((np.ones(len(pairs)), (pairs[:, 0], pairs[:, 1])), shape=(len(pts),) * 2)
    lab = connected_components(adj, directed=False)[1]
    X = pts[np.unique(lab, return_index=True)[1]]
    order = np.lexsort((X[:, 2], X[:, 1], X[:, 0]))
    rank = np.empty_like(order)
    rank[order] = np.arange(len(order))
    ids, G, k = rank[lab], [], 0
    for B in blocks:
        m = int(np.prod(B.shape[:3]))
        G.append(ids[k:k + m].reshape(B.shape[:3]))
        k += m
    return X[order], G


def right_handed(X, conn):
    """Reverse the local xi axis of elements with a negative Jacobian."""
    neg = np.linalg.det(np.einsum("eai,aj->eij", X[conn], DN3[13])) < 0
    conn = conn.copy()
    conn[neg] = conn[neg].reshape(-1, 3, 3, 3)[:, :, :, ::-1].reshape(-1, 27)
    return conn


def orient_faces(X, faces, ref):
    """Flip quad9 faces so that their normal has a positive component along ref(centre)."""
    Xf = X[faces]
    nrm = np.cross(np.einsum("fai,a->fi", Xf, DN2[4, :, 0]), np.einsum("fai,a->fi", Xf, DN2[4, :, 1]))
    flip = np.einsum("fi,fi->f", nrm, ref(Xf[:, 4])) < 0
    faces = faces.copy()
    faces[flip] = faces[flip].reshape(-1, 3, 3).transpose(0, 2, 1).reshape(-1, 9)
    return faces


def _extrude(sec, xs):
    P = np.empty((len(xs),) + sec.shape[:2] + (3,))
    P[..., 0] = xs[:, None, None]
    P[..., 1:] = sec[None]
    return P


def build_mesh(g: Geometry, t_cap=None, n_phi=12, n_r=2, n_x=20, n_cap=2, n_core=1):
    """Half model (z >= 0).  n_phi elements round the half circumference (multiple
    of 4), n_r through the wall, n_x along the cavity, n_cap through the cap."""
    assert n_phi % 4 == 0, "n_phi must be a multiple of 4"
    t_cap = 0.5 * g.R if t_cap is None else t_cap
    n1 = n_phi // 4
    phi = np.linspace(0.0, np.pi, 2 * n_phi + 1)        # from +y (thin side), about the cavity centre
    cph, sph = np.cos(phi), np.sin(phi)
    x_all = np.r_[np.linspace(0, g.L, 2 * n_x + 1), np.linspace(g.L, g.L + t_cap, 2 * n_cap + 1)[1:]]
    x_cap = x_all[2 * n_x:]

    # wall: rays from the cavity centre, cavity radius -> outer surface (t(phi) of the notes)
    rho_out = np.sqrt(g.R**2 - g.rp**2 * sph**2) - g.rp * cph
    rho = g.r + np.linspace(0, 1, 2 * n_r + 1)[None, :] * (rho_out[:, None] - g.r)
    wall = np.stack([g.rp + rho * cph[:, None], rho * sph[:, None]], -1)

    # cap core: half-square block in the middle, ring of elements out to the cavity radius
    h = 0.5 * g.r
    side, top = np.linspace(0, h, 2 * n1 + 1), np.linspace(h, -h, 4 * n1 + 1)
    per = np.r_[np.c_[np.full_like(side, h), side], np.c_[top, np.full_like(top, h)][1:],
                np.c_[np.full_like(side, -h), side[::-1]][1:]]
    sc = np.linspace(0, 1, 2 * n_core + 1)
    ring = per[:, None, :] + sc[None, :, None] * (g.r * np.c_[cph, sph] - per)[:, None, :] + [g.rp, 0.0]
    a, b = np.meshgrid(top, side, indexing="ij")
    rect = np.stack([g.rp + a, b], -1)

    tol = 1e-6 * g.R
    X, (Gw, Gring, Grect) = merge_blocks(
        [_extrude(wall, x_all), _extrude(ring, x_cap), _extrude(rect, x_cap)], tol)
    conn = right_handed(X, np.concatenate([_hexes(Gw), _hexes(Gring), _hexes(Grect)]))

    # wetted cavity surface, normal pointing out of the fluid
    cav = np.concatenate([_quads(Gw[:2 * n_x + 1, :, 0]), _quads(Gring[0]), _quads(Grect[0])])
    cav = orient_faces(X, cav, lambda c: np.where(c[:, :1] > g.L - tol, [[1.0, 0.0, 0.0]],
                                                  np.c_[0 * c[:, 0], c[:, 1] - g.rp, c[:, 2]]))
    return dict(
        X=X, conn=conn, cav=cav,
        outer=_quads(Gw[:, :, -1]),                     # local axes (x, hoop), used by the sleeve
        base=np.flatnonzero(X[:, 0] < tol), sym=np.flatnonzero(X[:, 2] < tol),
        top=Gw[:, 0, -1], bot=Gw[:, -1, -1],            # outer surface lines at y = +R and y = -R
        in_top=Gw[:, 0, 0], in_bot=Gw[:, -1, 0],        # cavity wall lines (valid for x <= L)
        iL=2 * n_x, t_cap=t_cap)


# ----------------------------------------------------------------------------
# 3. Element energies (differentiated automatically)
# ----------------------------------------------------------------------------
def _det3(F):
    return (F[..., 0, 0] * (F[..., 1, 1] * F[..., 2, 2] - F[..., 1, 2] * F[..., 2, 1])
            - F[..., 0, 1] * (F[..., 1, 0] * F[..., 2, 2] - F[..., 1, 2] * F[..., 2, 0])
            + F[..., 0, 2] * (F[..., 1, 0] * F[..., 2, 1] - F[..., 1, 1] * F[..., 2, 0]))


def _elem_pi(z, dNdX, wdV, c):
    """Mixed potential of one hex27.  z = 81 nodal coordinates followed by the 4
    pressure coefficients, c = (C10, C20, C30, kappa).  p enforces J = 1 + p/kappa."""
    xe, p = z[:81].reshape(27, 3), PSI @ z[81:]
    F = jnp.einsum("ai,gaj->gij", xe, dNdX)
    J = _det3(F)
    d = J ** (-2.0 / 3.0) * jnp.sum(F * F, axis=(1, 2)) - 3.0
    return jnp.sum(wdV * (c[0] * d + c[1] * d**2 + c[2] * d**3 + p * (J - 1.0) - 0.5 * p**2 / c[3]))


def _face_volume(xf):
    """Contribution of one cavity face to the enclosed volume, V = oint (y n_y + z n_z)/2 da.
    This field has no flux through planes x = const or through z = 0, so the open
    base and the symmetry plane need no faces."""
    x = N2 @ xf
    n = jnp.cross(DN2[:, :, 0] @ xf, DN2[:, :, 1] @ xf)
    return jnp.sum(W2 * 0.5 * (x[:, 1] * n[:, 1] + x[:, 2] * n[:, 2]))


def _sleeve_energy(xf, Xf):
    """Unit-stiffness energy of hoop fibres on one outer face: int (lam_h^2 - 1)^2 / 8 dA."""
    T1, T2, t2 = DN2[:, :, 0] @ Xf, DN2[:, :, 1] @ Xf, DN2[:, :, 1] @ xf
    dA = jnp.linalg.norm(jnp.cross(T1, T2), axis=1)
    lam2 = jnp.sum(t2 * t2, axis=1) / jnp.sum(T2 * T2, axis=1)
    return jnp.sum(W2 * dA * 0.125 * (lam2 - 1.0) ** 2)


class _Assembler:
    """Sums element matrices into a CSC matrix on the free dofs (fixed sparsity pattern)."""

    def __init__(self, dof_groups, fmap, n):
        rows, cols = [], []
        for dofs in dof_groups:
            f = fmap[dofs]
            rows.append(np.broadcast_to(f[:, :, None], f.shape + f.shape[1:]).reshape(-1))
            cols.append(np.broadcast_to(f[:, None, :], f.shape + f.shape[1:]).reshape(-1))
        rows, cols = np.concatenate(rows), np.concatenate(cols)
        self.keep = (rows >= 0) & (cols >= 0)
        key = cols[self.keep].astype(np.int64) * n + rows[self.keep]
        self.order = np.argsort(key, kind="stable")
        key = key[self.order]
        first = np.r_[True, key[1:] != key[:-1]]
        self.starts = np.flatnonzero(first)
        self.indices = (key[first] % n).astype(np.int32)
        self.indptr = np.searchsorted(key[first] // n, np.arange(n + 1)).astype(np.int32)
        self.n = n

    def __call__(self, data):
        d = np.add.reduceat(data[self.keep][self.order], self.starts)
        return sp.csc_matrix((d, self.indices, self.indptr), shape=(self.n, self.n))


# ----------------------------------------------------------------------------
# 4. Finite-element model
# ----------------------------------------------------------------------------
class FEModel:
    """mesh: dict with X, conn and optionally cav (pressurised faces) and outer
    (sleeve faces).  fixed: boolean (n_nodes, 3) mask of prescribed dofs.
    The state is (u, p): nodal displacements (n_nodes, 3) and element pressure
    coefficients (n_elem, 4)."""

    def __init__(self, mesh, mat: Material, fixed, nu=0.4995, sleeve_k=0.0):
        X, conn = mesh["X"], mesh["conn"]
        none = np.zeros((0, 9), int)
        self.X, self.conn, self.nn, self.ne = X, conn, len(X), len(conn)
        self.cav = mesh.get("cav", none)
        self.outer = mesh.get("outer", none) if sleeve_k > 0 else none
        self.ks = sleeve_k
        mu = 2.0 * mat.C10
        self.kappa = 2 * mu * (1 + nu) / (3 * (1 - 2 * nu))

        J0 = np.einsum("eai,gaj->egij", X[conn], DN3)
        det0 = np.linalg.det(J0)
        assert det0.min() > 0, "inverted element in the mesh"
        self.dNdX = np.einsum("gaj,egjk->egak", DN3, np.linalg.inv(J0))
        self.wdV = W3 * det0
        # A = -(d2Pi/dp2)^-1 = kappa * (pressure mass matrix)^-1, used to condense p
        self.A = self.kappa * np.linalg.inv(np.einsum("eg,gp,gq->epq", self.wdV, PSI, PSI))
        self._el = (jnp.asarray(self.dNdX), jnp.asarray(self.wdV),
                    jnp.array([mat.C10, mat.C20, mat.C30, self.kappa]))
        self._Xo = jnp.asarray(X[self.outer])

        vol = lambda x, cav: jnp.sum(jax.vmap(_face_volume)(x[cav]))
        slv = lambda x, outer, Xo: jnp.sum(jax.vmap(_sleeve_energy)(x[outer], Xo))
        self._g_el = jax.jit(jax.vmap(jax.grad(_elem_pi), (0, 0, 0, None)))
        self._h_el = jax.jit(jax.vmap(jax.hessian(_elem_pi), (0, 0, 0, None)))
        self._vol, self._g_vol = jax.jit(vol), jax.jit(jax.grad(vol))
        self._h_vol = jax.jit(jax.vmap(jax.hessian(_face_volume)))
        self._g_sl = jax.jit(jax.grad(slv))
        self._h_sl = jax.jit(jax.vmap(jax.hessian(_sleeve_energy)))

        self.free = np.flatnonzero(~fixed.reshape(-1))
        fmap = np.full(3 * self.nn, -1)
        fmap[self.free] = np.arange(len(self.free))
        dofs = lambda c: (3 * c[:, :, None] + np.arange(3)).reshape(len(c), 3 * c.shape[1])
        self.edofs = dofs(conn)
        self._asm = _Assembler([self.edofs, dofs(self.outer), dofs(self.cav)], fmap, len(self.free))
        self.V0 = self.volume(np.zeros_like(X))

    def zero_state(self):
        return np.zeros_like(self.X), np.zeros((self.ne, 4))

    def _z(self, u, p):
        return jnp.asarray(np.c_[(self.X + u)[self.conn].reshape(self.ne, 81), p])

    def volume(self, u):
        return float(self._vol(jnp.asarray(self.X + u), self.cav)) if len(self.cav) else 0.0

    def forces(self, u, p):
        """Internal nodal forces (n_nodes, 3), dV/du (n_nodes, 3) and the element
        incompressibility residuals dPi/dp (n_elem, 4)."""
        ge = np.asarray(self._g_el(self._z(u, p), *self._el))
        f = np.bincount(self.edofs.reshape(-1), ge[:, :81].reshape(-1), 3 * self.nn).reshape(-1, 3)
        x = jnp.asarray(self.X + u)
        if len(self.outer):
            f = f + self.ks * np.asarray(self._g_sl(x, self.outer, self._Xo))
        gV = np.asarray(self._g_vol(x, self.cav)) if len(self.cav) else np.zeros_like(f)
        return f, gV, ge[:, 81:]

    def stretch(self, u):
        """Largest principal stretch in each element (over its Gauss points)."""
        F = np.einsum("eai,egaj->egij", (self.X + u)[self.conn], self.dNdX)
        lam2 = np.linalg.eigvalsh(np.einsum("egki,egkj->egij", F, F))[..., -1]
        return np.sqrt(lam2.max(axis=1))

    def solve(self, u, p, P, V_target=None, tol=1e-9, maxit=15):
        """Newton iteration from the guess (u, p); prescribed dofs must already be set in u.
        Pressure control if V_target is None; otherwise the cavity pressure is the
        unknown that enforces the cavity volume.
        Returns (u, p, P, converged, iterations)."""
        u, p = np.array(u, dtype=float), np.array(p, dtype=float)
        f, gV, rp = self.forces(u, p)
        size, step, hist = np.ptp(self.X, axis=0).max(), np.inf, []
        for it in range(maxit + 1):
            R = (f - P * gV).reshape(-1)[self.free]
            rn = np.linalg.norm(R) / (np.linalg.norm(f) + 1e-30)
            rc = np.abs(np.einsum("epq,eq->ep", self.A, rp)).max() / self.kappa   # defect in J
            cV = 0.0 if V_target is None else self.volume(u) - V_target
            hist.append(rn)
            if not np.isfinite(rn) or (it >= 6 and rn > 0.5 * hist[it - 4]):    # diverging or stalled
                break
            # converged on the residual, or on a vanishing correction once round-off limits the residual
            if (rn < tol or step < 1e-12 * size) and rc < tol and abs(cV) <= tol * max(self.V0, 1e-30):
                return u, p, P, True, it
            if it == maxit:
                break
            # element tangents with the pressure condensed out
            H = np.asarray(self._h_el(self._z(u, p), *self._el))
            Kup = H[:, :81, 81:]
            KA = Kup @ self.A
            parts = [(H[:, :81, :81] + KA @ Kup.transpose(0, 2, 1)).reshape(-1)]
            x = jnp.asarray(self.X + u)
            if len(self.outer):
                parts.append(self.ks * np.asarray(self._h_sl(x[self.outer], self._Xo)).reshape(-1))
            if len(self.cav):
                parts.append(-P * np.asarray(self._h_vol(x[self.cav])).reshape(-1))
            Rc = np.bincount(self.edofs.reshape(-1), np.einsum("eip,ep->ei", KA, rp).reshape(-1), 3 * self.nn)
            lu = spla.splu(self._asm(np.concatenate(parts)), permc_spec="MMD_AT_PLUS_A",
                           diag_pivot_thresh=0.0, options=dict(SymmetricMode=True))
            du, dP = lu.solve(-(R + Rc[self.free])), 0.0
            if V_target is not None:
                g = gV.reshape(-1)[self.free]
                b = lu.solve(g)
                dP = -(cV + g @ du) / (g @ b)
                du = du + dP * b
            dU = np.zeros(3 * self.nn)
            dU[self.free] = du
            dp = np.einsum("epq,eq->ep", self.A, rp + np.einsum("eip,ei->ep", Kup, dU[self.edofs]))
            # halve the step while it inverts an element (J <= 0 gives NaN forces)
            alpha = 1.0
            while alpha > 1e-3:
                ut, pt = u + alpha * dU.reshape(-1, 3), p + alpha * dp
                ft, gVt, rpt = self.forces(ut, pt)
                if np.isfinite(ft).all():
                    break
                alpha *= 0.5
            else:
                break
            u, p, P, f, gV, rp = ut, pt, P + alpha * dP, ft, gVt, rpt
            step = alpha * np.abs(dU).max()
        return u, p, P, False, it


# ----------------------------------------------------------------------------
# 5. Actuator simulation: pressure ramp, then volume control past a limit point
# ----------------------------------------------------------------------------
def measure(mesh, g: Geometry, u):
    """Bending measures taken at the end of the pressurised length (X = L)."""
    X, x = mesh["X"], mesh["X"] + u
    iL, im = mesh["iL"], mesh["iL"] // 2
    top, bot = x[mesh["top"]], x[mesh["bot"]]
    d, c = top - bot, 0.5 * (top + bot)                 # section diameter vector, axis position
    theta = np.arctan2(d[:, 0], d[:, 1])                # section rotation, + toward the thick wall
    Xs = X[mesh["top"], 0]
    mid = (Xs > 0.25 * g.L) & (Xs < 0.75 * g.L)
    return dict(
        theta=theta[iL], delta=-c[iL, 1], x_tip=c[iL, 0],
        kappa_mid=np.polyfit(Xs[mid], theta[mid], 1)[0],
        lam_axis=np.sum(np.linalg.norm(np.diff(c[:iL + 1], axis=0), axis=1)) / g.L,
        lam_outer=np.linalg.norm(d[im]) / (2 * g.R),
        lam_cavity=np.linalg.norm(x[mesh["in_top"][im]] - x[mesh["in_bot"][im]]) / (2 * g.r),
        wall_thin=np.linalg.norm(top[im] - x[mesh["in_top"][im]]) / g.t_min)


def make_actuator(g: Geometry, mat: Material, mesh="medium", nu=0.4995, sleeve=False, t_cap=None):
    """Mesh the actuator and apply the clamp and symmetry conditions."""
    kw = MESH_PRESETS[mesh] if isinstance(mesh, str) else mesh
    m = build_mesh(g, t_cap=t_cap, **kw)
    fixed = np.zeros(m["X"].shape, bool)
    fixed[m["base"]] = True
    fixed[m["sym"], 2] = True
    # sleeve: hoop fibres stiff enough to hold the hoop strain near P/(100 E)
    return m, FEModel(m, mat, fixed, nu=nu, sleeve_k=100 * mat.E * g.R if sleeve else 0.0)


def simulate(g: Geometry, mat: Material, pressures, mesh="medium", nu=0.4995, sleeve=False,
             t_cap=None, v_max=6.0, max_bisect=2, log=print):
    """Quasi-static pressure ramp through `pressures` [MPa] (ascending, starting at 0).
    If a pressure step cannot be solved (ballooning limit point) the path is
    continued under volume control until the pressure turns down (the peak is the
    limit pressure), the last requested pressure is reached, or the cavity volume
    exceeds v_max * V0.
    Returns a dict of arrays, one entry per converged state."""
    t0 = time.time()
    m, fe = make_actuator(g, mat, mesh, nu, sleeve, t_cap)
    log(f"mesh: {fe.ne} hex27, {fe.nn} nodes, {len(fe.free)} dofs, "
        f"cavity volume {2 * fe.V0:.1f} mm^3 (exact {np.pi * g.r**2 * g.L:.1f})")
    u, p = fe.zero_state()
    states = [dict(P=0.0, V=fe.V0, u=u, p=p, mode="pressure")]

    def predictor(s):
        """Secant extrapolation of the last two states, by the factor s (at most 2)."""
        a, b = states[-2], states[-1]
        s = min(s, 2.0)
        return b["u"] + s * (b["u"] - a["u"]), b["p"] + s * (b["p"] - a["p"])

    def accept(u, p, P, mode, its):
        states.append(dict(P=P, V=fe.volume(u), u=u, p=p, mode=mode))
        log(f"  {mode:8s} P = {1e3 * P:8.3f} kPa   V/V0 = {states[-1]['V'] / fe.V0:6.3f}   "
            f"delta = {measure(m, g, u)['delta']:8.3f} mm   ({its} it, {time.time() - t0:5.0f} s)")

    failed = False
    for Pt in pressures[1:]:
        stack, fails = [float(Pt)], 0
        while stack:
            last, Pn = states[-1], stack[-1]
            dPl = last["P"] - states[-2]["P"] if len(states) > 1 else 0.0
            guesses = ([predictor((Pn - last["P"]) / dPl)] if dPl > 0 else []) + [(last["u"], last["p"])]
            for u0, p0 in guesses:
                u, p, _, ok, its = fe.solve(u0, p0, Pn)
                ok = ok and fe.volume(u) - last["V"] < 0.25 * fe.V0     # reject snap-through jumps
                if ok:
                    break
            if ok:
                accept(u, p, Pn, "pressure", its)
                stack.pop()
            elif fails == max_bisect:
                failed = True
                break
            else:
                fails += 1
                stack.append(0.5 * (last["P"] + Pn))
        if failed:
            break

    if failed:
        log(f"  pressure step beyond {1e3 * states[-1]['P']:.2f} kPa failed; continuing under volume control")
        dV, fails = 0.02 * fe.V0, 0
        while fails < 4:
            last = states[-1]
            if last["P"] >= pressures[-1] or last["P"] < states[-2]["P"] or last["V"] > v_max * fe.V0:
                break
            dVl = last["V"] - states[-2]["V"] if len(states) > 1 else 0.0
            u0, p0 = predictor(dV / dVl) if dVl > 0 else (last["u"], last["p"])
            u, p, Pn, ok, its = fe.solve(u0, p0, last["P"], V_target=last["V"] + dV)
            if ok:
                accept(u, p, Pn, "volume", its)
                dV, fails = min(dV * (1.5 if its <= 5 else 1.0), 0.1 * fe.V0), 0
            else:
                dV, fails = 0.5 * dV, fails + 1

    out = dict(P=np.array([s["P"] for s in states]), V=2 * np.array([s["V"] for s in states]),
               u=np.array([s["u"] for s in states]), mode=np.array([s["mode"] for s in states]),
               lam_elem=np.array([fe.stretch(s["u"]) for s in states]))
    for k in ("theta", "delta", "x_tip", "kappa_mid", "lam_axis", "lam_outer", "lam_cavity", "wall_thin"):
        out[k] = np.array([measure(m, g, s["u"])[k] for s in states])
    out["lam_max"] = out["lam_elem"].max(axis=1)
    # a limit point was passed if the pressure came down again under volume control
    out["P_limit"] = out["P"].max() if out["P"][-1] < out["P"].max() else np.nan
    out["mesh"] = m
    log(f"done: {len(states)} states in {time.time() - t0:.0f} s"
        + (f", ballooning limit pressure {1e3 * out['P_limit']:.2f} kPa" if np.isfinite(out["P_limit"]) else ""))
    return out


def save(res, g: Geometry, mat: Material, meta, outdir):
    """results.npz (fields for the viewer), summary.csv (one row per state), run.json."""
    outdir = Path(outdir)
    outdir.mkdir(parents=True, exist_ok=True)
    m = res["mesh"]
    np.savez_compressed(outdir / "results.npz", X=m["X"], conn=m["conn"], u=res["u"].astype(np.float32),
                        lam_elem=res["lam_elem"].astype(np.float32), P=res["P"], V=res["V"])
    cols = ["P", "V", "theta", "delta", "x_tip", "kappa_mid", "lam_axis", "lam_outer", "lam_cavity",
            "wall_thin", "lam_max"]
    tab = np.column_stack([res[k] for k in cols])
    tab[:, 0] *= 1e3
    tab[:, 2] = np.degrees(tab[:, 2])
    head = "P_kPa,V_mm3,theta_deg,delta_mm,x_tip_mm,kappa_mid_1_per_mm,lam_axis,lam_outer,lam_cavity,wall_thin,lam_max,mode"
    with open(outdir / "summary.csv", "w") as fh:
        fh.write(head + "\n")
        for row, mode in zip(tab, res["mode"]):
            fh.write(",".join(f"{v:.6g}" for v in row) + f",{mode}\n")
    info = dict(geometry=dict(R=g.R, r=g.r, rp=g.rp, L=g.L, t_cap=m["t_cap"]),
                material=dict(C10=mat.C10, C20=mat.C20, C30=mat.C30, E=mat.E),
                P_limit_kPa=float(1e3 * res["P_limit"]) if np.isfinite(res["P_limit"]) else None, **meta)
    (outdir / "run.json").write_text(json.dumps(info, indent=2))
    return outdir


# ----------------------------------------------------------------------------
# 6. Command line
# ----------------------------------------------------------------------------
if __name__ == "__main__":
    ap = argparse.ArgumentParser(description="Pressure sweep of the eccentric-cavity actuator (3-D FE).")
    ap.add_argument("--R", type=float, default=10.0, help="outer radius [mm]")
    ap.add_argument("--r", type=float, default=4.0, help="cavity radius [mm]")
    ap.add_argument("--rp", type=float, default=3.0, help="cavity eccentricity [mm]")
    ap.add_argument("--L", type=float, default=100.0, help="pressurised length [mm]")
    ap.add_argument("--tcap", type=float, default=None, help="tip cap thickness [mm] (default R/2)")
    ap.add_argument("--E", type=float, default=0.6, help="small-strain Young's modulus [MPa] (neo-Hookean)")
    ap.add_argument("--yeoh", type=float, nargs=3, metavar=("C10", "C20", "C30"), help="Yeoh coefficients [MPa], overrides --E")
    ap.add_argument("--nu", type=float, default=0.4995, help="Poisson's ratio (sets the bulk modulus)")
    ap.add_argument("--pmax", type=float, default=60.0, help="maximum pressure [kPa]")
    ap.add_argument("--steps", type=int, default=12, help="number of pressure steps")
    ap.add_argument("--mesh", choices=list(MESH_PRESETS), default="medium")
    ap.add_argument("--sleeve", action="store_true", help="add an inextensible hoop-fibre sleeve on the outer surface")
    ap.add_argument("--out", default="results/run", help="output directory")
    a = ap.parse_args()

    g = Geometry(R=a.R, r=a.r, rp=a.rp, L=a.L)
    mat = Material(*a.yeoh) if a.yeoh else Material.from_E(a.E)
    res = simulate(g, mat, np.linspace(0, a.pmax / 1e3, a.steps + 1), mesh=a.mesh, nu=a.nu,
                   sleeve=a.sleeve, t_cap=a.tcap)
    out = save(res, g, mat, dict(nu=a.nu, sleeve=a.sleeve, mesh=a.mesh), a.out)
    print(f"saved {out / 'summary.csv'}")
    try:
        from fea_viewer import build
        print(f"viewer: {build(out)}")
    except ImportError:
        print(f"plotly not installed; for the interactive viewer install it and run: python fea_viewer.py {out}")
