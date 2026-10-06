"""
Verification of actuator_fea.py against problems with known answers.

    1  uniaxial tension of a block      -> Material.nominal_stress (exact, incompressible)
    2  inflation of a thick-walled tube -> exact incompressible neo-Hookean solution
    3  small-pressure bending           -> exact linear-elastic result
                                           kappa = (1 - 2 nu) F e / (E Izz)
    4  mesh convergence at finite pressure

Tests 1-3 have exact answers.  Test 3 is also the central physical result: an
incompressible bare actuator does not bend at first order in the pressure.

Run:  python -u fea_verify.py         (add --fine to include the fine mesh in test 4)
"""
from __future__ import annotations
import sys
import time
import numpy as np
from scipy.integrate import quad

from actuator_model import Geometry, Material, linear_response
import actuator_fea as fea


def block_mesh(a, n):
    """Cube [0, a]^3 with n hex27 per edge."""
    s = np.linspace(0, a, 2 * n + 1)
    X, (G,) = fea.merge_blocks([np.stack(np.meshgrid(s, s, s, indexing="ij"), -1)], 1e-9 * a)
    return dict(X=X, conn=fea.right_handed(X, fea._hexes(G)))


def tube_mesh(A, B, h, n_phi=12, n_r=4):
    """Half of a concentric tube slice of length h (one element along x)."""
    phi, rho, xs = np.linspace(0, np.pi, 2 * n_phi + 1), np.linspace(A, B, 2 * n_r + 1), np.linspace(0, h, 3)
    P = np.empty((3, len(phi), len(rho), 3))
    P[..., 0] = xs[:, None, None]
    P[..., 1] = (rho[None, :] * np.cos(phi)[:, None])[None]
    P[..., 2] = (rho[None, :] * np.sin(phi)[:, None])[None]
    X, (G,) = fea.merge_blocks([P], 1e-9 * B)
    cav = fea.orient_faces(X, fea._quads(G[:, :, 0]), lambda c: np.c_[0 * c[:, 0], c[:, 1], c[:, 2]])
    return dict(X=X, conn=fea.right_handed(X, fea._hexes(G)), cav=cav, inner=G[0, 0, 0])


def test_uniaxial():
    print("1. Uniaxial tension, nominal stress [MPa]: FE vs exact incompressible")
    worst = 0.0
    for name, mat in [("neo-Hookean", Material(0.1)), ("Yeoh", Material(0.1, 0.01, 0.002))]:
        m = block_mesh(10.0, 2)
        X = m["X"]
        end = X[:, 0] > 10 - 1e-6
        fixed = np.zeros(X.shape, bool)
        fixed[:, 0] = (X[:, 0] < 1e-6) | end
        fixed[:, 1] = X[:, 1] < 1e-6
        fixed[:, 2] = X[:, 2] < 1e-6
        fe = fea.FEModel(m, mat, fixed)
        u, p = fe.zero_state()
        for lam in (1.1, 1.25, 1.5, 2.0, 2.5, 3.0):
            u[end, 0] = (lam - 1) * 10.0
            u, p, _, ok, its = fe.solve(u, p, 0.0)
            s_fe = fe.forces(u, p)[0][end, 0].sum() / 100.0
            s_ex = float(mat.nominal_stress(lam))
            worst = max(worst, abs(s_fe / s_ex - 1))
            print(f"   {name:12s} lambda = {lam:4.2f}   FE {s_fe:9.5f}   exact {s_ex:9.5f}   "
                  f"error {100 * (s_fe / s_ex - 1):+.3f} %   ({its} it, converged {ok})")
    return worst


def test_tube():
    print("2. Thick-walled tube (A = 4, B = 10 mm), plane strain: pressure at the FE inner radius")
    A, B, mat = 4.0, 10.0, Material(0.1)
    mu = 2 * mat.C10
    m = tube_mesh(A, B, 2.0)
    X = m["X"]
    fixed = np.zeros(X.shape, bool)
    fixed[:, 0] = True
    fixed[:, 1] = np.abs(X[:, 1]) < 1e-6
    fixed[:, 2] = X[:, 2] < 1e-6
    fe = fea.FEModel(m, mat, fixed)

    def exact(a):
        """Incompressible neo-Hookean: P = int_a^b mu (lam^2 - lam^-2) / rho d rho."""
        c = a**2 - A**2
        f = lambda rho: mu * (rho**2 / (rho**2 - c) - (rho**2 - c) / rho**2) / rho
        return quad(f, a, np.sqrt(B**2 + c))[0]

    u, p = fe.zero_state()
    worst = 0.0
    for Pm in (0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7):
        u, p, _, ok, its = fe.solve(u, p, Pm * mu)
        a = A + u[m["inner"], 1]
        worst = max(worst, abs(exact(a) / (Pm * mu) - 1))
        print(f"   P/mu = {Pm:.1f}   a/A = {a / A:6.4f}   exact P/mu at that radius {exact(a) / mu:.5f}   "
              f"error {100 * (exact(a) / (Pm * mu) - 1):+.3f} %   ({its} it, converged {ok})")
    return worst


def test_linear_bending(g, mesh):
    print("3. Small-pressure bending of the bare actuator: mid-span curvature / Tier 1 curvature")
    print("   (Tier 1 = F e / (E Izz) with E = 2 mu (1 + nu); exact linear elasticity gives 1 - 2 nu)")
    mat, worst = Material.from_E(0.6), 0.0
    for nu in (0.0, 0.2, 0.3, 0.4, 0.45, 0.4995):
        m, fe = fea.make_actuator(g, mat, mesh, nu=nu)
        E = 2 * (2 * mat.C10) * (1 + nu)
        P = 1e-5 * E
        u, p, _, ok, its = fe.solve(*fe.zero_state(), P)
        r = fea.measure(m, g, u)
        lin = linear_response(P, g, E)
        ratio = r["kappa_mid"] / lin["kappa"][0]
        worst = max(worst, abs(ratio - (1 - 2 * nu)))
        print(f"   nu = {nu:6.4f}   FE {ratio:7.4f}   exact {1 - 2 * nu:7.4f}   "
              f"tip angle / Tier 1 {r['theta'] / lin['theta'][0]:7.4f}   ({its} it, converged {ok})")
    return worst


def test_convergence(g, meshes):
    print("4. Mesh convergence, bare actuator, neo-Hookean, P/mu = 0.30 and 0.45")
    mat = Material.from_E(0.6)
    mu, rows = 2 * mat.C10, []
    for mesh in meshes:
        t0 = time.time()
        res = fea.simulate(g, mat, mu * np.array([0, 0.1, 0.2, 0.3, 0.375, 0.45]), mesh=mesh, log=lambda s: None)
        at = [int(np.argmin(np.abs(res["P"] / mu - q))) for q in (0.30, 0.45)]   # the ramp may insert extra states
        rows.append((mesh, [res["delta"][i] for i in at]))
        print(f"   {mesh:7s} {len(res['mesh']['conn']):5d} elements   "
              + "   ".join(f"P/mu = {res['P'][i] / mu:.2f}: delta {res['delta'][i]:7.3f} mm, theta {np.degrees(res['theta'][i]):6.2f} deg"
                           for i in at)
              + f"   ({time.time() - t0:.0f} s)")
    ref = rows[-1][1]
    for mesh, d in rows[:-1]:
        print(f"   {mesh} vs {rows[-1][0]}: delta differs by " + ", ".join(f"{100 * (a / b - 1):+.2f} %" for a, b in zip(d, ref)))
    return abs(rows[-2][1][1] / ref[1] - 1) if len(rows) > 1 else np.nan


if __name__ == "__main__":
    g = Geometry(R=10.0, r=4.0, rp=3.0, L=100.0)
    t0 = time.time()
    e1 = test_uniaxial(); print()
    e2 = test_tube(); print()
    e3 = test_linear_bending(g, "medium"); print()
    e4 = test_convergence(g, ["coarse", "medium"] + (["fine"] if "--fine" in sys.argv else [])); print()
    print("Summary (largest deviation from the reference in each test)")
    print(f"   1 uniaxial stress        {100 * e1:.3f} %   (grows with stretch: the bulk modulus is finite, nu = 0.4995)")
    print(f"   2 tube inflation         {100 * e2:.3f} %")
    print(f"   3 (1 - 2 nu) law         {e3:.4f}    (absolute, in units of the Tier 1 curvature)")
    print(f"   4 mesh, last refinement  {100 * e4:.2f} %   in delta at P/mu = 0.45")
    print(f"total time {time.time() - t0:.0f} s")
