"""
grasp3d_model.py  -  3-D Kirchhoff-rod grasp model
=====================================================
Extends the planar grasp_model.py to full 3-D while keeping the same
energy-minimisation / JAX / trust-region Newton philosophy.

Coordinate conventions
-----------------------
  x, y : horizontal (palm plane)
  z     : vertical, positive UP; the palm is at z = 0, fingers hang down.
  All lengths in mm, forces in N, pressures in MPa.

Rod parameterisation
---------------------
Each segment i carries a relative rotation vector phi_i in R^3.
Global frame of segment i:  R_i = R_{i-1} @ Rodrigues(phi_i)
Root frame R_base: 3rd column = [0,0,-1] (tangent pointing down).

Material-frame curvature:
    kappa_x = phi_i[0] / ds     (bending about material d1)
    kappa_y = phi_i[1] / ds     (bending about material d2)
    tau     = phi_i[2] / ds     (twist)

Single-layer spontaneous curvature:
    kappa0_y = P * pi * r^2 * R_outer / EI

Multi-chamber (3 at 120 deg) spontaneous curvature:
    kappa0 = sum_k  P_k * A_c * d_c / EI * [cos(alpha_k), sin(alpha_k)]
    Equal pressures -> zero net curvature (verified analytically).

Contact: node-to-sphere penalty,  Fn = kn * max(a - |p - c|, 0)
Friction: 2-D tangential Huber-type potential, same as planar model.

Planar regression (2-finger radial Config-B, x-z plane):
  - free curl at 40 kPa: tip angle 192.64 deg (< 0.01 deg)
  - mu=0.6, 50 kPa, depth=70 mm: working holding force ~8.66 N (< 1%)
"""
from __future__ import annotations
from dataclasses import dataclass
from typing import Optional
import numpy as np
import jax
import jax.numpy as jnp
from scipy.optimize import minimize

jax.config.update("jax_enable_x64", True)


# ---------------------------------------------------------------------------
# 1. Rodrigues rotation
# ---------------------------------------------------------------------------

def _rodrigues(phi: jnp.ndarray) -> jnp.ndarray:
    angle = jnp.sqrt(jnp.sum(phi**2) + 1e-18)
    nx, ny, nz = phi[0]/angle, phi[1]/angle, phi[2]/angle
    K = jnp.array([[0., -nz, ny],[nz, 0., -nx],[-ny, nx, 0.]])
    s, c = jnp.sin(angle), jnp.cos(angle)
    return jnp.eye(3) + s*K + (1.-c)*(K@K)


# ---------------------------------------------------------------------------
# 2. Finger geometry
# ---------------------------------------------------------------------------

@dataclass
class Finger3D:
    L:         float = 100.0
    R0:        float = 10.0
    Rt:        float = 10.0
    t0:        float = 2.0
    tt:        float = 2.0
    E:         float = 0.15
    nu:        float = 0.5
    N:         int   = 100
    n_seg:     int   = 1
    n_ch:      int   = 1     # 1 = single eccentric layer; 3 = tri-chamber
    d_c:       float = 8.0   # chamber offset from axis [mm]
    A_c:       float = 50.27 # chamber area [mm^2]
    alpha_off: float = 0.0   # angular offset of chamber 0 [rad]
    layer_from:float = 0.0   # strain-limiting layer starts at layer_from*L

    @property
    def ds(self): return self.L / self.N
    @property
    def G(self): return self.E / (2.*(1.+self.nu))

    def sections(self):
        s = np.arange(self.N) * self.ds
        R = self.R0 + (self.Rt - self.R0)*s/self.L
        t = self.t0 + (self.tt - self.t0)*s/self.L
        r = R - t
        Ic = np.pi*(R**4 - r**4)/4.
        A  = np.pi*(R**2 - r**2)
        layer = s >= self.layer_from*self.L - 1e-9
        I  = np.where(layer, Ic + A*R**2, Ic)
        J  = np.pi*(R**4 - r**4)/2.
        EI = self.E*I;  GJ = self.G*J
        mp = np.where(layer, np.pi*r**2*R, 0.)
        return s, EI, GJ, mp

    def kappa0_single(self, P):
        _, EI, _, mp = self.sections()
        Parr = np.atleast_1d(np.asarray(P, float))
        if Parr.size == 1:
            Pf = np.full(self.N, Parr[0])
        else:
            seg = np.minimum((np.arange(self.N)*self.n_seg)//self.N, self.n_seg-1)
            Pf  = np.atleast_1d(Parr)[seg]
        return Pf*mp/EI

    def pressure_field(self, P):
        """Compatibility helper: P = scalar or (n_seg,) -> (N,) field."""
        Parr = np.atleast_1d(np.asarray(P, float))
        if Parr.size == 1:
            return np.full(self.N, Parr[0])
        seg = np.minimum((np.arange(self.N)*self.n_seg)//self.N, self.n_seg-1)
        return Parr[seg]

    def kappa0_vector(self, P_chambers):
        _, EI, _, _ = self.sections()
        P = np.asarray(P_chambers, float).reshape(self.n_seg, self.n_ch)
        seg = np.minimum((np.arange(self.N)*self.n_seg)//self.N, self.n_seg-1)
        k0x = np.zeros(self.N);  k0y = np.zeros(self.N)
        for k in range(self.n_ch):
            alpha = 2.*np.pi*k/self.n_ch + self.alpha_off
            c_k = P[seg, k] * self.A_c * self.d_c / EI
            k0x += c_k * np.cos(alpha);  k0y += c_k * np.sin(alpha)
        return k0x, k0y

    def pressures_from_bend(self, bend_mag, bend_dir, P_max=0.05):
        _, EI, _, _ = self.sections()
        EI_seg = np.array([EI[k*self.N//self.n_seg] for k in range(self.n_seg)])
        scale  = EI_seg / (self.A_c * self.d_c)
        P_ch = np.zeros((self.n_seg, self.n_ch))
        for k in range(self.n_ch):
            alpha = 2.*np.pi*k/self.n_ch + self.alpha_off
            P_ch[:, k] = np.maximum(0., bend_mag*scale*np.cos(bend_dir - alpha))
        return np.clip(P_ch, 0., P_max)


# ---------------------------------------------------------------------------
# 3. Object
# ---------------------------------------------------------------------------

@dataclass
class Object3D:
    shape:  str   = "sphere"   # "sphere" or "cylinder"
    a:      float = 15.0       # radius [mm]
    mass:   float = 0.0        # mass [g]

    @property
    def weight_N(self): return self.mass * 9.81e-3


# ---------------------------------------------------------------------------
# 4. Gripper configuration
# ---------------------------------------------------------------------------

@dataclass
class GripperConfig:
    n_fingers:    int   = 3
    helical:      bool  = False
    palm_radius:  float = 16.0
    depth:        float = 70.0


# ---------------------------------------------------------------------------
# 5. Per-finger energy builder
# ---------------------------------------------------------------------------

def _build_energy(finger: Finger3D, obj: Object3D, cfg: GripperConfig,
                  finger_idx: int = 0):
    f = finger
    _, EI_a, GJ_a, mp_a = f.sections()
    EI = jnp.asarray(EI_a);  GJ = jnp.asarray(GJ_a);  mp = jnp.asarray(mp_a)
    ds  = f.ds;  kn = 20.;  kt = 20.;  a = obj.a

    az   = 2.*np.pi*finger_idx / max(cfg.n_fingers, 1)
    pr   = cfg.palm_radius
    base_pos = jnp.array([pr*np.cos(az), pr*np.sin(az), 0.])
    inward = np.array([-np.cos(az), -np.sin(az), 0.])
    tang_p = np.array([-np.sin(az),  np.cos(az), 0.])
    down   = np.array([0., 0., -1.])
    R_base = jnp.asarray(np.column_stack([inward, tang_p, down]))

    def get_nodes(phi):
        def body(R, ph):
            R2 = R @ _rodrigues(ph); return R2, R2[:,2]
        _, tgts = jax.lax.scan(body, R_base, phi)
        return base_pos + ds*jnp.cumsum(tgts, axis=0)

    def _pen(p, c):
        if obj.shape == "sphere":
            return jnp.maximum(a - jnp.sqrt(jnp.sum((p-c)**2, axis=1)+1e-18), 0.)
        else:
            d = p-c; return jnp.maximum(a - jnp.sqrt(d[:,0]**2+d[:,1]**2+1e-18), 0.)

    def _surf(p, c):
        d = p-c
        if obj.shape == "sphere":
            r = jnp.sqrt(jnp.sum(d**2, axis=1)+1e-18)
            xi1 = a*jnp.arctan2(d[:,1], d[:,0])
            xi2 = a*jnp.arcsin(jnp.clip(d[:,2]/r, -1., 1.))
        else:
            xi1 = a*jnp.arctan2(d[:,1], d[:,0]);  xi2 = d[:,2]
        return xi1, xi2

    def Uparts(phi, P_s, k0x, k0y, c, xi1, xi2, fn_old, mu):
        phi_m1 = jnp.vstack([jnp.zeros((1,3)), phi[:-1]])
        dphi   = (phi - phi_m1) / ds
        kap0y  = P_s*mp/EI + k0y
        kap0x  = k0x
        Ub = 0.5*ds*jnp.sum(EI*(dphi[:,0]-kap0x)**2 + EI*(dphi[:,1]-kap0y)**2)
        Ut = 0.5*ds*jnp.sum(GJ*dphi[:,2]**2)
        p  = get_nodes(phi)
        Un = 0.5*kn*jnp.sum(_pen(p, c)**2)
        xi1c, xi2c = _surf(p, c)
        dd1 = xi1c-xi1;  dd2 = xi2c-xi2
        dd  = jnp.sqrt(dd1**2 + dd2**2 + 1e-18)
        q   = mu*fn_old/kt
        Uf  = jnp.sum(jnp.where(dd<=q, 0.5*kt*dd**2, mu*fn_old*(dd-0.5*q)))
        return Ub+Ut, Un, Uf

    Utot = lambda *a_: sum(Uparts(*a_))
    jE   = jax.jit(Utot)
    jdE  = jax.jit(jax.grad(Utot))
    jd2E = jax.jit(jax.hessian(Utot))
    jpen = jax.jit(lambda ph, c: _pen(get_nodes(ph), c))
    jsurf= jax.jit(lambda ph, c: _surf(get_nodes(ph), c))
    jFn  = jax.jit(jax.grad(lambda *a_: Uparts(*a_)[1], argnums=4))
    jFf  = jax.jit(jax.grad(lambda *a_: Uparts(*a_)[2], argnums=4))

    return dict(get_nodes=jax.jit(get_nodes), surf=jsurf, pen=jpen,
                E=jE, dE=jdE, d2E=jd2E, Fn=jFn, Ff=jFf,
                N=f.N, kn=kn, kt=kt)


# ---------------------------------------------------------------------------
# 6. Solver
# ---------------------------------------------------------------------------

def _solve_step(x0, fun, jac, hess):
    best_x, best_g = x0.copy(), np.inf
    for method in ("trust-exact", "trust-krylov"):
        try:
            res = minimize(fun, x0.ravel(), jac=jac, hess=hess, method=method,
                           options=dict(gtol=1e-9, maxiter=500))
            gn = float(np.linalg.norm(jac(res.x)))
            if gn < best_g:
                best_g, best_x = gn, res.x.copy()
            if best_g < 1e-4:
                return best_x
        except Exception:
            pass
    return best_x


# ---------------------------------------------------------------------------
# 7. Single-finger simulator
# ---------------------------------------------------------------------------

class FingerSim3D:
    def __init__(self, finger, obj, cfg, mu, finger_idx=0):
        self.f = finger; self.obj = obj; self.mu = mu
        self.k = _build_energy(finger, obj, cfg, finger_idx)
        N = finger.N
        self.phi  = np.zeros((N, 3))
        self.fn   = np.zeros(N)
        self.c    = np.array([0., 0., -cfg.depth])
        xi1, xi2  = [np.asarray(x) for x in
                     self.k["surf"](self.phi, self.c)]
        self.xi1  = xi1;  self.xi2 = xi2
        self.P_s  = np.zeros(N)
        self.k0x  = np.zeros(N);  self.k0y = np.zeros(N)
        self.history: list[dict] = []

    def step(self, P_single=None, kap0x=None, kap0y=None, c=None,
             fp_iters=60, fp_tol=1e-5):
        if P_single is not None: self.P_s  = np.asarray(P_single, float)
        if kap0x    is not None: self.k0x  = np.asarray(kap0x, float)
        if kap0y    is not None: self.k0y  = np.asarray(kap0y, float)
        if c        is not None: self.c    = np.asarray(c, float)

        cj = jnp.asarray(self.c); Ps = jnp.asarray(self.P_s)
        k0x= jnp.asarray(self.k0x); k0y= jnp.asarray(self.k0y)
        kn = self.k["kn"];  kt = self.k["kt"];  mu = self.mu;  N = self.f.N

        fn_lag, relax, last = self.fn.copy(), 1., np.inf
        converged = False; max_resid = 0.
        for _ in range(fp_iters):
            xi1j = jnp.asarray(self.xi1); xi2j = jnp.asarray(self.xi2)
            fnj  = jnp.asarray(fn_lag)
            args = (Ps, k0x, k0y, cj, xi1j, xi2j, fnj, mu)
            fun  = lambda x: float(self.k["E"](x.reshape(N,3), *args))
            jac  = lambda x: np.asarray(self.k["dE"](x.reshape(N,3), *args)).ravel()
            hess = lambda x: np.asarray(self.k["d2E"](x.reshape(N,3), *args)).reshape(3*N,3*N)
            self.phi = _solve_step(self.phi, fun, jac, hess).reshape(N, 3)
            fn_new   = kn*np.asarray(self.k["pen"](self.phi, cj))
            resid    = float(np.max(np.abs(fn_new-fn_lag))); max_resid = max(max_resid, resid)
            if resid < fp_tol: converged = True; break
            if resid > last: relax = max(0.5*relax, 0.05)
            last = resid; fn_lag = fn_lag + relax*(fn_new-fn_lag)
        self.fn = fn_new

        # Update friction anchors (return-map)
        xi1n, xi2n = [np.asarray(x) for x in self.k["surf"](self.phi, cj)]
        q   = mu*fn_new/kt
        mag = np.sqrt((xi1n-self.xi1)**2 + (xi2n-self.xi2)**2 + 1e-18)
        sc  = np.where(mag > q, q/mag, 1.)
        ic  = fn_new > 0
        self.xi1 = np.where(ic, self.xi1 + sc*(xi1n-self.xi1), xi1n)
        self.xi2 = np.where(ic, self.xi2 + sc*(xi2n-self.xi2), xi2n)

        args_f = (self.phi, Ps, k0x, k0y, cj,
                  jnp.asarray(self.xi1), jnp.asarray(self.xi2),
                  jnp.asarray(fn_lag), mu)
        Fn_v = -np.asarray(self.k["Fn"](*args_f))
        Ff_v = -np.asarray(self.k["Ff"](*args_f))
        F    = Fn_v + Ff_v
        rec  = dict(Fz=float(F[2]), Fz_n=float(Fn_v[2]), Fz_f=float(Ff_v[2]),
                    n_contact=int(np.sum(fn_new>0)),
                    converged=converged, fp_resid=max_resid)
        self.history.append(rec); return rec

    def nodes(self): return np.asarray(self.k["get_nodes"](self.phi))

    def snapshot(self):
        p  = self.nodes()
        cj = jnp.asarray(self.c)
        Ps = jnp.asarray(self.P_s)
        k0x= jnp.asarray(self.k0x); k0y= jnp.asarray(self.k0y)
        args_f = (self.phi, Ps, k0x, k0y, cj,
                  jnp.asarray(self.xi1), jnp.asarray(self.xi2),
                  jnp.asarray(self.fn), self.mu)
        Fn_v = -np.asarray(self.k["Fn"](*args_f))
        Ff_v = -np.asarray(self.k["Ff"](*args_f))
        return dict(nodes=p, c=self.c.copy(), a=self.obj.a,
                    normal_total=Fn_v, friction_total=Ff_v,
                    fn_per_node=self.fn.copy())


# ---------------------------------------------------------------------------
# 8. Multi-finger gripper
# ---------------------------------------------------------------------------

class GraspSim3D:
    def __init__(self, finger, obj, cfg, mu):
        self.finger = finger; self.obj = obj; self.cfg = cfg; self.mu = mu
        self.sims = [FingerSim3D(finger, obj, cfg, mu, i)
                     for i in range(cfg.n_fingers)]
        self.c = np.array([0., 0., -cfg.depth])
        self.history: list[dict] = []

    def inflate(self, P_max, n_steps=30, schedule="simultaneous",
                P_chambers=None):
        if P_chambers is None:
            ramp = np.linspace(0., P_max, n_steps+1)[1:]
            if schedule == "simultaneous":
                for Pk in ramp:
                    Pf = np.full(self.finger.N, Pk)
                    for sim in self.sims: sim.step(P_single=Pf)
            else:
                n_seg = self.finger.n_seg
                order = range(n_seg) if schedule=="base_to_tip" else list(reversed(range(n_seg)))
                P_vec = np.zeros(n_seg)
                for seg_i in order:
                    for Pk in ramp:
                        P_vec[seg_i] = Pk
                        Pf = self.finger.pressure_field(P_vec.copy())
                        for sim in self.sims: sim.step(P_single=Pf)
        else:
            for t in np.linspace(0., 1., n_steps+1)[1:]:
                Pc = t*np.asarray(P_chambers)
                for sim in self.sims:
                    k0x, k0y = sim.f.kappa0_vector(Pc)
                    sim.step(kap0x=k0x, kap0y=k0y)

    def pull_down(self, pull=70., n_steps=112, sag_limit=10.):
        c0 = self.c.copy(); pulls, Fz_arr = [], []
        for d in np.linspace(0., pull, n_steps+1)[1:]:
            self.c = c0 + np.array([0.,0.,-d])
            rec    = self._combined_step()
            pulls.append(d); Fz_arr.append(rec["Fz"])
            self.history.append({**rec, "pull": d})
        pulls = np.array(pulls); Fz = np.array(Fz_arr)
        in_sag = pulls <= sag_limit+1e-9
        working = float(np.max(Fz[in_sag])) if np.any(in_sag) else 0.
        i_work  = int(np.argmax(Fz*in_sag))
        return dict(working_Fz=working, ultimate_Fz=float(np.max(Fz)),
                    pull_at_working=float(pulls[i_work]),
                    pulls=pulls, Fz=Fz)

    def lift(self, lift_dist=60., n_steps=60):
        c0 = self.c.copy(); Fz_vals = []
        for dz in np.linspace(0., lift_dist, n_steps+1)[1:]:
            self.c = c0 + np.array([0.,0.,dz])
            rec = self._combined_step()
            Fz_vals.append(rec["Fz"])
            self.history.append({**rec, "lift_dz": dz})
        Fz = np.array(Fz_vals); w = self.obj.weight_N
        return dict(lifted=bool(np.all(Fz >= w-1e-6)),
                    Fz_min=float(np.min(Fz)), Fz_max=float(np.max(Fz)),
                    weight_N=w, Fz_vals=Fz)

    def _combined_step(self):
        recs = []
        for sim in self.sims:
            sim.c = self.c.copy()
            recs.append(sim.step())
        return dict(Fz   =sum(r["Fz"]   for r in recs),
                    Fz_n =sum(r["Fz_n"] for r in recs),
                    Fz_f =sum(r["Fz_f"] for r in recs),
                    n_contact=sum(r["n_contact"] for r in recs),
                    converged=all(r["converged"] for r in recs),
                    fp_resid=max(r["fp_resid"] for r in recs))

    def snapshots(self): return [sim.snapshot() for sim in self.sims]

    def grasp_metrics(self):
        snaps = self.snapshots(); c = self.c; a = self.obj.a
        all_p  = np.vstack([s["nodes"] for s in snaps])
        all_fn = np.concatenate([s["fn_per_node"] for s in snaps])
        Fn_v   = sum(s["normal_total"] for s in snaps)
        Ff_v   = sum(s["friction_total"] for s in snaps)
        ic = all_fn > 0
        dz_c = (all_p[ic, 2] - c[2]) if np.any(ic) else np.zeros(1)
        centroid_z = float(np.mean(dz_c))
        eq_mask = np.abs(all_p[:, 2] - c[2]) <= 0.3*a
        fn_eq  = float(np.sum(all_fn[eq_mask & ic]))
        fn_tot = float(np.sum(all_fn[ic]) + 1e-18)
        Fz_tot = float(Fn_v[2] + Ff_v[2])
        return dict(
            contact_centroid_z_norm=centroid_z/a,
            frac_equatorial=fn_eq/fn_tot,
            frac_friction=float(Ff_v[2])/(Fz_tot+1e-18),
            frac_normal=float(Fn_v[2])/(Fz_tot+1e-18),
            lateral_drift_mm=float(np.sqrt(c[0]**2+c[1]**2)),
            Fz_total=Fz_tot,
        )


# ---------------------------------------------------------------------------
# 9. Free curl (3-D, for validation)
# ---------------------------------------------------------------------------

def free_curl_3d(finger: Finger3D, P: float, cfg: GripperConfig = None,
                 finger_idx: int = 0) -> tuple[np.ndarray, float]:
    """Free curl (no object). Returns (nodes (N+1,3), tip_angle_rad)."""
    if cfg is None:
        cfg = GripperConfig(n_fingers=1, palm_radius=finger.R0+1., depth=70.)
    az  = 2.*np.pi*finger_idx / max(cfg.n_fingers, 1)
    pr  = cfg.palm_radius
    base_p= np.array([pr*np.cos(az), pr*np.sin(az), 0.])
    inward = np.array([-np.cos(az), -np.sin(az), 0.])
    tang_p = np.array([-np.sin(az),  np.cos(az), 0.])
    down   = np.array([0., 0., -1.])
    R_base = np.column_stack([inward, tang_p, down])
    kap0 = finger.kappa0_single(P); ds = finger.ds
    phi  = np.zeros((finger.N, 3)); phi[:, 1] = kap0*ds
    R = R_base.copy(); nodes = [base_p]
    for i in range(finger.N):
        R = R @ np.array(_rodrigues(jnp.asarray(phi[i])))
        nodes.append(nodes[-1] + ds*R[:, 2])
    return np.array(nodes), float(np.sum(kap0)*ds)


# ---------------------------------------------------------------------------
# 10. Planar regression shortcut
# ---------------------------------------------------------------------------

def run_planar_regression(P_kPa=50., depth=70., mu=0.6,
                          n_inflate=30, pull=20., n_pull=40, sag=10.):
    f   = Finger3D(L=100., R0=10., Rt=10., t0=2., tt=2., E=0.15, N=100)
    obj = Object3D(shape="sphere", a=15., mass=0.)
    cfg = GripperConfig(n_fingers=2, palm_radius=16., depth=depth)
    gs  = GraspSim3D(f, obj, cfg, mu)
    gs.inflate(P_kPa*1e-3, n_steps=n_inflate)
    return gs.pull_down(pull=pull, n_steps=int(n_pull), sag_limit=sag)
