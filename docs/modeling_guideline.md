# Modelling guideline: eccentric-cavity pressure-actuated soft bending actuator

Companion files: `actuator_model.py` (beam model, Tiers 1–3), `actuator_fea.py` (3-D finite-element model, Tier 4), `fea_verify.py` (verification of the FE solver), `fea_viewer.py` and `fea_compare.py` (viewer and report figure), `results/` (FE runs), `BTP_260825_144004.pdf` (your handwritten derivation, referred to below as "the notes"). `README.md` lists the commands.

Units used throughout: mm, N, MPa (1 MPa = 1000 kPa = 1 N/mm²).

All numbers below are for the placeholder design R = 10, r = 4, r_p = 3, l = 100 mm with a neo-Hookean silicone. Re-run the scripts when the real geometry and material are fixed.

---

## Revision, 3 October 2026: what the finite-element model changed

The first version of this guideline marked the beam derivation in the notes (eqs. 1–7) as correct and treated ballooning as a correction to it. A 3-D finite-element model now exists and it overturns that picture for the **bare (unreinforced) actuator**:

1. **A bare incompressible actuator does not bend at first order in the pressure.** The pressure loads the wall in its own plane as well as along the axis, and through Poisson's ratio those in-plane stresses cancel the thrust moment. The exact linear-elastic curvature is (1 − 2ν) times eq. 7 of the notes, which is zero for silicone (ν ≈ 0.5). Derivation in Section 2.1, FE confirmation in Section 4.1.
2. **The bending a bare actuator does show is a finite-strain ballooning effect.** It starts quadratically in P, then accelerates and ends at a **ballooning limit pressure** of 0.523 μ (μ = E/3 is the shear modulus), above which there is no stable equilibrium. For E = 0.6 MPa that is 104.7 kPa; for E = 0.08 MPa it is 14 kPa.
3. **Eq. 7 of the notes is the right first-order model for a hoop-reinforced actuator.** With an inextensible hoop-fibre sleeve the FE tip angle matches eq. 7 within 5 % up to P/E ≈ 0.03 and exceeds it at higher pressure (by 25 % at P/E = 0.1).
4. **Your remark on the last page of the notes is confirmed in substance.** Wall expansion is the dominant bending mechanism of the bare actuator. The formula κ_exp is not: the effect is not linear in P.

The earlier example figure plotted the soft silicone to 60 kPa. For a bare actuator that is four times its ballooning limit.

---

## 0. What the notes establish

The notes describe a single-chamber silicone cylinder (outer radius R, length l) with a circular cavity of radius r whose centre is shifted by r_p along +y. Pressurising the cavity bends the actuator toward the thick wall (−y).

| Step                             | Result in the notes                              | Status                                                                                                                                                                                         |
| -------------------------------- | ------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Elastomer area                   | A = π(R² − r²)                                   | correct                                                                                                                                                                                        |
| Neutral-axis offset              | ȳ = −r² r_p / (R² − r²)                          | correct (eq. 2 has a typo: denominator written R² − r)                                                                                                                                         |
| Second moment about NA           | I_zz = π(R⁴ − r⁴)/4 − πR²r²r_p²/(R² − r²)        | correct (verified by expanding the parallel-axis form, and numerically)                                                                                                                        |
| Thrust and moment arm            | F = Pπr², e = R²r_p/(R² − r²)                    | correct                                                                                                                                                                                        |
| Pressure moment                  | M = πPR²r²r_p/(R² − r²)                          | correct as the resultant on a cross-section                                                                                                                                                    |
| Curvature, angle                 | κ = M/(EI_zz), θ = κl                            | **valid only with hoop reinforcement.** Assumes each fibre is in uniaxial stress. For the bare actuator the exact linear result is (1 − 2ν) M/(EI_zz). Page 3 also writes P² in θ; should be P |
| Tip deflection                   | δ = (1 − cos θ)/κ (arc), δ = Ml²/(2EI) (small θ) | kinematics correct; inherits the κ above                                                                                                                                                       |
| Wall thickness t(φ)              | √(R² − r_p² sin²φ) − r_p cos φ − r               | correct with φ measured from +y (t(0) = t_min). The FE mesh is built on this formula                                                                                                           |
| "Wall-expansion" curvature κ_exp | (Pr/2ER)(1/t_min − 1/t_max)                      | **mechanism right, formula wrong.** FE confirms that wall expansion bends the bare actuator, but the effect is nonlinear in P; the sign also flips between definition and use on page 6        |

---

## 1. Representing the system

### 1.1 Coordinate frame and parameters

- x: along the actuator axis, base at x = 0 (clamped), free tip at x = l.
- y: in the cross-section, pointing from the outer-circle centre toward the cavity centre.
- z: completes the right-handed frame; bending is about z.
- Origin of the section: centre of the outer circle. Cavity centre at (y, z) = (r_p, 0).

| Symbol                   | Meaning                                                      | Constraint                                  |
| ------------------------ | ------------------------------------------------------------ | ------------------------------------------- |
| R                        | outer radius                                                 |                                             |
| r                        | cavity radius                                                | r < R                                       |
| r_p                      | cavity eccentricity                                          | t_min = R − r_p − r > 0 (wall must survive) |
| l (L in the scripts)     | pressurised length                                           | l/R ≳ 5 for a beam-like response            |
| t_cap                    | thickness of the solid tip cap                               | FE only; default R/2                        |
| E, μ, or (C10, C20, C30) | small-strain Young's and shear modulus, or Yeoh coefficients | incompressible: E = 3μ = 6 C10              |
| P                        | gauge pressure in the cavity                                 | the single input                            |

Derived section quantities: t_min = R − r_p − r, t_max = R + r_p − r, A, ȳ, I_zz, e.

### 1.2 Assumptions to state explicitly

- Material is isotropic, incompressible, rate-independent, without Mullins or viscoelastic effects (precondition the specimen experimentally).
- Pressure is uniform inside the cavity, gravity is neglected (check: ρgAl²/(EI) ≪ θ).
- Clamped base, free tip, no external payload.
- Beam model only: plane sections remain plane, the cross-section does not change shape, and each longitudinal fibre is in uniaxial stress. The last two fail for the bare actuator (Section 2.1).

### 1.3 Non-dimensional form

The response is governed by the load ratio, the slenderness and the section shape.

- Hoop-reinforced, beam model: θ = (P/E)(l/R) f(r/R, r_p/R), with f = π(r/R)²(r_p/R) / [Î(1 − (r/R)²)], Î = I_zz/R⁴. Linear in P.
- Bare, neo-Hookean: θ = G(P/μ; l/R, r/R, r_p/R), nonlinear, with a limit load (P/μ)_lim that depends on the shape only. One FE sweep in P/μ is therefore valid for every modulus: to change silicone, rescale the pressure axis by μ. For the placeholder shape (P/μ)_lim = 0.523.

---

## 2. Representing the load: the free-body cut

Cut the actuator at an arbitrary x and consider the free body from the cut to the tip (silicone plus the fluid it encloses). No external force acts on it. At the cut:

- the fluid on the far side pushes on the fluid in the free body with resultant Pπr², acting through the cavity centre (y = r_p);
- the silicone on the far side pulls on the annulus with the axial stress σ_x(y, z).

Equilibrium of the free body:

- **Force:** ∫_A σ_x dA = N = Pπr² (the wall is in net tension)
- **Moment about the cavity centre:** ∫_A σ_x (y − r_p) dA = 0 (the pressure resultant passes through it)

These two equations hold at every x and for any constitutive law, so N and M are uniform along the length. They fix the *resultants* of σ_x. They do not, on their own, fix the strain: that needs the relation between σ_x and ε_x, and this is where the notes and the first version of this guideline went wrong.

### 2.1 What the uniaxial assumption leaves out

Setting σ_x = E[ε_0 + κ(y − ȳ)] treats each fibre as if nothing acted on it sideways. But the pressure also loads the wall in the plane of the section: radial compression at the cavity surface and hoop tension around it, both of order P and larger. In linear elasticity

ε_x = [σ_x − ν(σ_y + σ_z)] / E, so σ_x = E[ε_0 + κ(y − ȳ)] + ν(σ_y + σ_z).

The in-plane stress field need not be solved. Its resultant and first moment over the section follow from in-plane equilibrium alone (divergence theorem applied to σ_αβ x_γ, with pressure P on the cavity boundary and a free outer boundary of any shape):

∫_A (σ_y + σ_z) dA = 2Pπr², ∫_A (σ_y + σ_z)(y − ȳ) dA = 2Pπr² e.

Substituting into the two free-body equations gives

**ε_0 = (1 − 2ν) Pπr² / (EA), κ = (1 − 2ν) Pπr² e / (E I_zz).**

For ν = 0.5 both vanish. The first is the textbook result that a closed incompressible tube does not lengthen under internal pressure; the second is its bending counterpart, and it means that **in the small-strain limit a bare silicone actuator with an eccentric cavity does not bend at all**. This is exact for a prismatic section away from the ends. The FE model reproduces the factor (1 − 2ν) to four decimal places (Section 4.1). In the script: `linear_response(P, g, E, nu=...)`.

With a hoop-fibre sleeve the fibres carry the hoop load, the in-plane stresses in the silicone no longer cancel the thrust, and the uniaxial beam model is the correct first-order description (Section 4.3).

---

## 3. Model tiers, from closed form to FE

Tiers 1–3 are the beam model. They describe the **hoop-reinforced** actuator. For the bare actuator go straight to Tier 4.

### Tier 1 — linear Euler–Bernoulli (the notes, eqs. 1–7)

κ = πPR²r²r_p / [E I_zz (R² − r²)], θ = κ l, δ_small = θ l / 2.

Against the sleeved FE model the tip angle is within 5 % up to P/E ≈ 0.03 (θ ≈ 5° for the placeholder design). Beyond that the FE angle is larger: by 14 % at P/E = 0.07 and by 25 % at P/E = 0.1. In the script: `linear_response`.

### Tier 2 — constant-curvature kinematics with axial stretch

Keep κ from Tier 1 but stop linearising the geometry:

- centroidal fibre stretches to l_c = l (1 + ε_0), ε_0 = Pπr² / (EA);
- radius of the arc ρ = l_c / θ;
- tip position: x_tip = ρ sin θ, δ = ρ (1 − cos θ).

This is the "piecewise constant curvature" representation used in continuum-robot kinematics, and it is the form you will want later for control (θ, ρ map directly to a homogeneous transform). In the script: `delta_arc`, `x_tip`.

### Tier 3 — hyperelastic section equilibrium

Keep "plane sections remain plane" but let each longitudinal fibre follow a uniaxial hyperelastic law. Write the stretch as λ(y) = λ_c + k (y − r_p) where k = dθ/dS is the rotation per unit reference length, and solve the two free-body equations of Section 2 numerically for (λ_c, k):

Σ s(λ) dA = Pπr², Σ s(λ)(y − r_p) dA = 0,

with the uniaxial incompressible nominal stress s(λ) = 2(λ − λ⁻²)[C10 + 2C20(I₁−3) + 3C30(I₁−3)²], I₁ = λ² + 2/λ. Neo-Hookean is C10 = E/6 only. Then θ = k l and the arc kinematics of Tier 2 apply with ρ = λ(ȳ)/k. This is the same idea as the Polygerinos et al. (2015) model for fibre-reinforced bending actuators. In the script: `hyperelastic_response`.

Tier 3 closes only part of the gap to the sleeved FE model (the FE angle is 20 % larger at P/E = 0.1, against 25 % for Tier 1). The rest comes from the cavity growing as the actuator lengthens inside a sleeve of fixed circumference: at P/E = 0.1 the cavity area is up by 24 %, and the thrust with it. Tier 3 keeps the cavity area fixed.

### Tier 4 — nonlinear 3-D finite elements (`actuator_fea.py`)

Implemented and verified. No commercial FE package was available on the project machine, so the solver is written in Python:

- 27-node hexahedra in a mixed pressure–displacement formulation (Q2/P1) for incompressibility; Yeoh or neo-Hookean material; ν = 0.4995 by default.
- Half model with symmetry about the bending plane, base clamped, solid tip cap, pressure on every wetted cavity face as a follower load derived from the enclosed cavity volume.
- Pressure ramp with Newton iteration. If a pressure step has no solution the solver switches to volume control (prescribe the cavity volume, solve for the pressure), follows the path over the pressure peak, and reports the peak as the ballooning limit.
- Optional `--sleeve`: inextensible hoop fibres on the outer surface, no axial stiffness.

Verification (`fea_verify.py`, log in `results/verification.log`):

| Test                                            | Reference                                 | Largest deviation                                                                               |
| ----------------------------------------------- | ----------------------------------------- | ----------------------------------------------------------------------------------------------- |
| Uniaxial tension, λ up to 3                     | exact incompressible stress               | 0.2 % neo-Hookean; 2.5 % Yeoh at λ = 3 (finite bulk modulus)                                    |
| Inflation of a thick-walled tube, P/μ up to 0.7 | exact incompressible neo-Hookean solution | 0.05 %                                                                                          |
| Small-pressure bending, ν from 0 to 0.4995      | κ = (1 − 2ν) F e / (E I_zz)               | 0.0002 in units of the Tier 1 curvature                                                         |
| Mesh refinement, δ at P/μ = 0.30 and 0.45       | fine mesh (1664 elements)                 | medium mesh (used for the results here) within 0.5 % of the fine mesh; coarse mesh within 1.7 % |

For the thesis, an independent run of one load case in a commercial code (Abaqus C3D20H or ANSYS SOLID186 with mixed u-P, same geometry and material) would be worth having if the institute provides access. It is a cross-check, not a prerequisite.

### Tier 5 — experiment

The only way to fix E or the Yeoh coefficients for *your* cast, cure and geometry, and to confirm the ballooning limit, which is sensitive to wall-thickness tolerance.

---

## 4. What the finite-element model shows

### 4.1 Small pressure: the (1 − 2ν) law

Mid-span curvature from FE divided by the Tier 1 curvature, at P/E = 10⁻⁵:

| ν      | 0      | 0.2    | 0.3    | 0.4    | 0.45   | 0.4995 |
| ------ | ------ | ------ | ------ | ------ | ------ | ------ |
| FE     | 1.0000 | 0.6001 | 0.4001 | 0.2002 | 0.1002 | 0.0012 |
| 1 − 2ν | 1.0000 | 0.6000 | 0.4000 | 0.2000 | 0.1000 | 0.0010 |

The tip angle of the nearly incompressible actuator is not exactly zero: it is 6 % of the Tier 1 value, and all of it comes from the two ends, where the clamp and the cap stop the wall from expanding and so switch the Poisson cancellation off locally.

### 4.2 Bare actuator, full range (E = 0.6 MPa, μ = 200 kPa)

| P [kPa]        | P/μ   | θ FE [deg] | θ Tier 1 [deg] | FE / Tier 1 | δ FE [mm] | cavity volume V/V₀ | thin wall, % of original |
| -------------- | ----- | ---------- | -------------- | ----------- | --------- | ------------------ | ------------------------ |
| 10             | 0.05  | 0.24       | 2.41           | 0.10        | 0.20      | 1.07               | 98                       |
| 20             | 0.10  | 0.71       | 4.82           | 0.15        | 0.59      | 1.14               | 95                       |
| 40             | 0.20  | 2.66       | 9.64           | 0.28        | 2.26      | 1.33               | 89                       |
| 60             | 0.30  | 6.89       | 14.46          | 0.48        | 5.91      | 1.59               | 81                       |
| 80             | 0.40  | 16.1       | 19.3           | 0.84        | 14.0      | 2.01               | 71                       |
| 90             | 0.45  | 25.3       | 21.7           | 1.16        | 21.9      | 2.35               | 63                       |
| 100            | 0.50  | 44.0       | 24.1           | 1.82        | 37.6      | 2.98               | 52                       |
| 104.65 (limit) | 0.523 | 77         | 25.2           | 3.0         | 60        | 4.05               | 36                       |

Reading the table:

- Below P/μ ≈ 0.2 the beam model over-predicts by a factor of four to ten.
- The curves cross near P/μ = 0.43. The agreement there is a coincidence of two different mechanisms, not a validation.
- The response then steepens without bound. Between 100 kPa and the limit, 4.6 kPa of extra pressure adds 23 mm of deflection.
- At the limit the cavity holds four times its original volume, the thin wall is at a third of its thickness and the peak stretch is 2.5. Above it the neo-Hookean model has no equilibrium: the thin wall inflates without bound. A real silicone stiffens at large stretch (Yeoh C20, C30 > 0), which can raise or remove the limit, so measure the material before trusting this number.
- Why it bends at all: a pressurised rubber tube lengthens at second order in its hoop strain (for a thin closed neo-Hookean tube, ε_axial ≈ ⅔ ε_hoop²). The thin wall has the larger hoop strain, so it lengthens more, and the actuator bends toward the thick wall. This is the mechanism sketched on pages 5–7 of the notes.

For any other neo-Hookean silicone, keep the P/μ column and rescale: P = (P/μ) · E/3.

### 4.3 With a hoop-fibre sleeve (same geometry and material)

| P [kPa] | P/E   | θ FE [deg] | θ Tier 1 [deg] | FE / Tier 1 | δ FE [mm] | V/V₀ |
| ------- | ----- | ---------- | -------------- | ----------- | --------- | ---- |
| 10      | 0.017 | 2.41       | 2.41           | 1.00        | 2.10      | 1.04 |
| 20      | 0.033 | 5.03       | 4.82           | 1.04        | 4.42      | 1.09 |
| 40      | 0.067 | 11.0       | 9.64           | 1.14        | 9.77      | 1.19 |
| 60      | 0.10  | 18.1       | 14.5           | 1.25        | 16.3      | 1.30 |
| 80      | 0.13  | 26.5       | 19.3           | 1.38        | 24.2      | 1.44 |
| 100     | 0.17  | 36.8       | 24.1           | 1.53        | 33.7      | 1.61 |
| 110     | 0.18  | 42.7       | 26.5           | 1.61        | 39.2      | 1.70 |

The response is smooth and close to linear, with no limit point up to 110 kPa. The sleeve in the model is ideal (inextensible, bonded, no axial stiffness); a real wrap will be somewhat softer in hoop and stiffer in bending.

### 4.4 Figure and viewer

`fea_comparison.png` shows both FE runs against Tiers 1–3. `results/bare/viewer.html` and `results/sleeve/viewer.html` are interactive: a pressure slider, the deformed shape, and the δ(P) and θ(P) curves.

### 4.5 Is the concept promising?

Both variants bend usefully at pressures a small pump or syringe can supply, so the concept works. They differ in how usable the map is:

|                                       | Bare                                      | Hoop-reinforced                     |
| ------------------------------------- | ----------------------------------------- | ----------------------------------- |
| δ at 60 kPa (E = 0.6 MPa)             | 5.9 mm                                    | 16.3 mm                             |
| δ at 100 kPa                          | 37.6 mm                                   | 33.7 mm                             |
| Shape of δ(P)                         | strongly nonlinear, vertical at the limit | near linear                         |
| Usable pressure window                | roughly 0.3–0.9 of the limit pressure     | from zero, no limit found           |
| Sensitivity to pressure and tolerance | high near the limit                       | low                                 |
| Closed-form model                     | none; FE or a fitted curve                | eq. 7 of the notes at low pressure  |
| Fabrication                           | one casting                               | casting plus a fibre wrap or sleeve |

Recommendation: reinforce if the aim is a predictable P → δ map or closed-loop control. Keep the actuator bare if the aim is to study the ballooning mechanism itself, and then operate below about 90 % of the measured limit pressure.

---

## 5. Ballooning and the "asymmetric wall expansion" term

Why the notes' κ_exp cannot be kept as written:

- It is derived from linear Poisson coupling. With ν = 0.5, linear elasticity gives no net axial strain and no net curvature from pressure at all (Section 2.1). The effect κ_exp tries to capture exists only at finite strain.
- The real effect is nonlinear: roughly quadratic in P at first, then diverging at the limit pressure. A term linear in P with the small-strain E cannot represent it. For the placeholder geometry the notes' total, κ_thrust + κ_exp, is 2.76 × κ_thrust at every pressure, while the FE curvature runs from 0.1 to 3 times κ_thrust.
- The sign of the bracket flips between its definition and its use on page 6.

What to use instead:

1. **FE** for prediction (`actuator_fea.py`). One sweep in P/μ per geometry.
2. **The tabulated curve** if a map is needed for control: δ(P) from `summary.csv` is monotone up to the limit, so both δ(P) and its inverse P(δ) are plain interpolations of the table. Simple closed forms such as a (P/P_lim)² / (1 − P/P_lim)ᵇ were tried and do not fit (errors of tens of percent at low pressure).
3. The quantity **P/μ** as the design variable. The index B = Pr/(E t_min) used in the first version of this guideline is the same thing scaled by the shape: the ballooning limit of the placeholder design sits at B = 0.23.

---

## 6. Step-by-step workflow

1. **Fix the geometry.** Choose R, r, r_p, l. Check t_min > 0 and manufacturability (t_min ≥ ~1.5 mm for hand-cast silicone). Record the numbers in one place.
2. **Decide bare or hoop-reinforced** (Section 4.5). The decision determines which model applies.
3. **Choose the material and get its constants.** Starting ranges: Ecoflex 00-30 E ≈ 0.05–0.1 MPa, Dragon Skin 30 ≈ 0.5–1 MPa, Sylgard 184 ≈ 1.5–2.5 MPa. Then measure: cast ASTM D412 dumbbells from the same batch, pull at ~50 mm/min to λ ≈ 2–3, precondition 5 cycles, fit Yeoh (C10, C20, C30) to the nominal stress–stretch curve. E = 6 C10.
4. **Run the FE sweep**, first with `--mesh coarse` (about a minute) to find the range, then `--mesh medium` for the record. For a bare actuator set `--pmax` above the expected limit so the limit pressure is found. Use `--yeoh` once the coefficients are measured.
5. **Set the operating window.** Bare: below about 90 % of the limit pressure, and peak stretch below what the silicone tolerates in cyclic use. Reinforced: peak stretch only.
6. **For a reinforced design**, use Tier 1 for quick sizing and the sleeved FE run for the map.
7. **Fabricate** (two-part mould, cavity pin held at the correct eccentricity, degas, cure per datasheet, seal the tip and the inlet with the same silicone). Measure the as-built R, r, r_p, l and wall thicknesses; feed them back into step 4. The bare actuator's limit pressure is sensitive to t_min.
8. **Test.** Quasi-static pressure ramp up and down in ≥ 10 steps, hold 5 s per step, camera perpendicular to the bending plane, markers on the centreline every ~10 mm. For a bare actuator approach the limit from below in small steps, or drive it with a water-filled syringe (volume control), which the model predicts to be stable through the limit. Three specimens, three cycles each after preconditioning. Record hysteresis.
9. **Fit and validate.** Compare θ(P), δ(P) and the limit pressure with FE using the measured material constants. Report RMSE in δ and θ.
10. **Deliver the P → δ map** as a table from FE and experiment, with its validity range, and the inverse map P(δ) for control.

A detailed experimental procedure follows once the design is fixed.

---

## 7. Validation metrics and figures to produce

- θ vs P and δ vs P: experiment (mean ± std), FE, and for a reinforced design Tiers 1–3.
- Deformed centreline at three pressures: experiment vs FE.
- Cavity volume vs pressure: experiment (syringe displacement) vs FE. This is the most direct check of the ballooning model.
- Limit pressure: experiment vs FE.
- FE sweep over (r/R, r_p/R) showing how the limit load (P/μ)_lim and the deflection at a fixed fraction of it depend on the section shape.
- Table of fitted material constants with confidence intervals.

---

## 8. Corrections to carry into the written thesis

- Eq. (2): denominator R² − r² (not R² − r).
- Page 3, tip bending angle: P not P².
- Eqs. (4)–(7): state the assumption (each fibre in uniaxial stress) and the case it holds for (hoop-reinforced). Add the bare-actuator result κ = (1 − 2ν) M/(E I_zz) with its derivation (Section 2.1).
- Page 3, "E = Young's modulus or effective secant modulus": use the small-strain modulus, E = 3μ = 6 C10.
- Page 4 note (a): the nonlinearity of the bare actuator is not a strain-dependent modulus in the beam formula; it is the ballooning mechanism of Section 4.2. Mooney–Rivlin or Yeoh enter through the FE material law.
- Page 4 note (b): for an incompressible bare actuator the first-order axial elongation is zero, not Fl/(EA).
- Page 5, t(φ): use one symbol for the angle (φ) and state that φ is measured from the +y axis so that t(0) = t_min.
- Page 6: replace the linear κ_exp by the FE result (or a fitted curve). Do not superpose κ_thrust + κ_exp: for a bare actuator κ_thrust itself is cancelled.
- Page 7 note: keep the statement that wall compliance dominates; add that it is the only first-order-surviving mechanism and that it ends in a limit pressure.

---

## 9. Starting references (verify the exact citations before use)

- Polygerinos et al., "Modeling of soft fiber-reinforced bending actuators", IEEE Trans. Robotics, 2015 — hyperelastic section-moment balance for a reinforced bending actuator; the template for Tier 3.
- Connolly, Walsh, Bertoldi, "Automatic design of fiber-reinforced soft actuators for trajectory matching", PNAS, 2017 — fibre angle vs extension/bending, useful if you reinforce.
- Webster & Jones, "Design and kinematic modeling of constant curvature continuum robots: a review", IJRR, 2010 — Tier 2 kinematics and how to turn (κ, θ) into a transform for control.
- Gorissen et al., "Elastic inflatable actuators for soft robotic applications", Advanced Materials, 2017 — review covering ballooning, reinforcement and modelling levels.
- Marchese, Katzschmann, Rus, "A recipe for soft fluidic elastomer robots", Soft Robotics, 2015 — fabrication.
- Yeoh, "Some forms of the strain energy function for rubber", Rubber Chem. Technol., 1993 — the material model.
- Holzapfel, "Nonlinear Solid Mechanics", Wiley, 2000 — inflation of rubber tubes, limit-point instability, mixed finite-element formulations.
