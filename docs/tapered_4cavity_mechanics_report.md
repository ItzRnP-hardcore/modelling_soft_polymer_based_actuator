# Tapered 4-chamber actuator in COMSOL: verified status

Rewritten on 9 November 2026 after an audit of every model, data file and figure behind the earlier
version of this document. The earlier version (Sections 1–17, "180°–192° verified", contact-grasp
forces, full-wrap and loop-caging results) contained results that do not hold. They are listed in
Section 3 with the reason. Only results that were checked against their source files are kept.

All geometry and material values are **placeholders** (no experiment yet). Units: mm, kPa, MPa.

---

## 1. Cross-verification of COMSOL against the in-house Python FE

Same geometry (R = 10, r = 4, r_p = 3, l = 100 mm, 5 mm tip cap), neo-Hookean, E = 0.6 MPa,
ν = 0.4995 (mixed u–p), real follower pressure on the cavity, clamped base, half model.
Tip angle measured at x = 100 mm in both codes.

| Case | Agreement in tip angle and tip deflection | Files |
|---|---|---|
| Hoop-sleeved | within 0.7 % from 10 to 110 kPa (43.0° vs 42.7° at 110 kPa) | `comsol/sleeved/`, `results/sleeve/` |
| Bare | within 3 % from 60 to 90 kPa; up to 11 % at 10 kPa, where the angle is only 0.24° | `comsol/bare/`, `results/bare/` |

The COMSOL bare run stops at 90 kPa (its pressure list ends there). The ballooning limit
P_lim = 0.523 μ = 104.65 kPa comes from the Python FE only.

## 2. Tapered 4-chamber actuator with real pressure

Geometry (canonical, `comsol/tapered_4cavity/BuildActuatorTapered4CavityFull.java`): L = 140 mm,
outer radius 9.5 → 3.8 mm (homothetic 2.5 : 1 taper), four 56° chambers separated by 34° ribs,
chambers from x = 4 to 136 mm, roof thickness 2.0 → 0.8 mm, chamber depth 2.5 → 1.0 mm.
Chamber 1 (+Y) pressurised, chambers 2–4 vented (pitch-down bending).

### 2.1 Effect of replacing the earlier load model (`comsol/tapered_4cavity_true_pressure/`)
Same geometry, mesh and material as the earlier model (compressible neo-Hookean, E = 0.40 MPa,
ν = 0.45); only the load changed to the real pressure P on every face of chamber 1.

| P [kPa] | earlier load model | real pressure |
|---|---|---|
| 10 | 82.9° | 1.1° |
| 20 | 141.0° | 3.2° |
| 40 | 186.1° | 12.7° |

### 2.2 Physical model (`comsol/tapered_4cavity_sleeved/`)
Real pressure, nearly incompressible silicone (ν = 0.4995, mixed u–p, as in Section 1),
E = 0.07 MPa (assumed, soft platinum-cure silicone class), optional inextensible hoop sleeve on the
outer skin (same formulation as the cross-verified sleeved model). Mesh "fine"; a "finer" mesh
changes the tip angle at 10 kPa by 1.2 % and the tip deflection by 0.9 %.

| Case | Tip rotation | Last converged step and what limits it |
|---|---|---|
| Bare | 7.9° at 6 kPa, 36.6° at 10 kPa | 11.7 kPa: roof ballooning, P/μ = 0.50 |
| Sleeved | 47.1° at 10, 68.0° at 12.5, 95.3° at 15 kPa | 20 kPa: local stress runaway (below) |
| Sleeved, 0.6 mm chamber fillets | 40.6° at 10, 84.6° at 15 kPa | 21 kPa: same runaway |
| Sleeved, deeper/wider chambers (5.0 → 1.5 mm, 70°) | 55.3° at 8 kPa, then decreases | 8.5 kPa: ribs and core collapse |

- Low-pressure check: at 2 kPa the sleeved model gives 6.1° against 5.7° from the hoop-reinforced
  beam formula (eq. 7 of the notes integrated over the tapered section). At higher pressure the FE
  bends more than the formula, as in the Python sleeved FE.
- Stress runaway: in the filleted model the peak von Mises stress on the junction between the roof
  of the pressurised chamber and its side walls rises from 0.24 MPa at 15 kPa to 0.6–0.7 MPa at
  17.5 kPa and to tens of MPa at 20 kPa. In the model without fillets the peaks are localised in
  short stretches (x ≈ 0–40 mm and 90–130 mm) while the region 40–90 mm stays smooth. Rounding the
  chamber corners does not remove it, so it is not a sharp-corner singularity. It coincides with
  severe local element distortion: the smallest local volume ratio J falls from 0.92 at 2 kPa to 0.30
  at 15 kPa and 0.01 at 20 kPa (0.17 at 15 kPa with fillets), while the volume-averaged J stays at
  1.0000. Whether the runaway is a physical instability of the roof–side-wall junction or a numerical
  breakdown of the mesh/element is **not resolved**. (The cross-verified uniform sleeved model shows
  local J down to 0.64 while its tip angle agrees with the Python FE within 0.7 %, so tip kinematics
  are less sensitive to local J than stresses are.)
- Range of trust: the mesh check was done at 10 kPa (tip angle changes 1.2 % with a finer mesh).
  Results up to 15 kPa (P/μ ≈ 0.64; about 85–95° tip rotation) are reported, with 12.5–15 kPa not
  mesh-checked. Steps beyond 15 kPa (120–184°) are not used: local stress and element distortion
  are not reliable there, and the computed peak stress exceeds the ~1.4 MPa tensile strength of
  Ecoflex 00-30.
- A neo-Hookean response depends on P/μ only: a stiffer silicone gives the same curve at
  proportionally higher pressure. Real silicone stiffens at large stretch (Yeoh), which may delay the
  runaway; this needs measured material data.

## 3. Withdrawn results

| Earlier claim | Why it is withdrawn |
|---|---|
| 180° at 35 kPa, 192° at 50 kPa, pitch/yaw/diagonal 180° (`comsol/tapered_4cavity/`) | The load was not the cavity pressure: `0.10·P·(0.20+1.80(X/L)^1.5)` on the inner wall and tip face, `0.005·P` on the other faces, tuned until 180° appeared. With ν = 0.45 compressible. The 50 kPa row is not in the data file. |
| All contact-grasp forces, payloads, "self-centring", "vortex", "full wrap", "loop caging", "maximum force" (`comsol/contact_grasp/`, Sections 11–17) | Same non-physical load. Contact penalty 1e6 N/m³ lets the actuator sink 0.5–3 mm into the object, and the R = 22 mm fixed-cylinder run already reports 0.12 N at P = 0 (bodies overlapping). With a spring-supported object the reported force is, in most runs, close to spring stiffness × object displacement, i.e. set by the chosen spring. The reported "wrap angle" is the tip rotation, not the contact arc; several table values were changed by hand from the data (e.g. 21.0° reported as 381°), and the R = 20 mm "proper grasp" case is the full-wrap run reused. The L = 250 mm "peak at 71.1 kPa" is where the solver stopped. |
| "Path 2: membrane base, 180°" and "Path 3: Cosserat beam, 220°" | Never solved in COMSOL. The "COMSOL 6.4" 180°/220° images were drawn with matplotlib from a beam-formula integration with a fitted factor 1.18 (`comsol/large_deflection_4cavity/render_comsol_180_220.py`); `Solve180And220.java` builds geometry only. |
| Tapered 1-chamber actuator: 215.9° at 19.3 kPa | No script produces 19.3 kPa; `comsol/tapered/comsol_tapered_summary.csv` has three rows and no pressure column. |
| Synthesis table (old Section 12): bare 3.2° at 40 kPa, sleeved 8.4° at 110 kPa "stiff / no bending" | The data files give 2.78° and 43.0°. |
| F ∝ S² force scaling | The two scales differ by 1.26×, not 2.25×, and the object spring was scaled too. |
| Capstan amplification (23×, 81 000×) for axial pull-out | The capstan relation applies to a band sliding round a drum, not to axial pull-out; no friction or pull-out was simulated. |
| Braided-sleeve "rules" (54.7° braid, 284 N/mm hoop stiffness, 2.38 MPa burst) | Not simulated or tested; kept out until checked. |
| "COMSOL confirms the 104.7 kPa ballooning limit" | The COMSOL bare run ends at 90 kPa. |
| Kevlar-core and earlier iterations (old Sections 1–4) | Historical design history from archived models; not re-verified. |

## 4. Open work
1. A much finer local mesh (or hexahedral wall elements) and thicker chamber side walls, to decide whether the stress runaway is physical or numerical.
2. Measured material: uniaxial tests, Yeoh fit.
3. Self-contact and object contact with a much stiffer penalty or augmented Lagrangian, a fixed or
   stiffly supported object, and a contact-arc definition of wrap.
4. Yaw and diagonal bending with real pressure (same structure rotated; not yet run).
5. Fabrication and experiment.
