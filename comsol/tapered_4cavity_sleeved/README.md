# Tapered 4-cavity actuator — physical model (real pressure, real silicone, hoop sleeve)

Replaces the tuned-load models in `../tapered_4cavity/` and `../contact_grasp/` for any claim about
how the actuator bends under a given pressure.

## What the model is
- Geometry: the canonical tapered 4-cavity actuator (L = 140 mm, R 9.5 → 3.8 mm, 4 chambers, base plug 4 mm).
- Load: **real gauge pressure P on every wetted face of chamber 1** (pitch down). No scaling factors.
- Material: neo-Hookean, **ν = 0.4995, nearly incompressible, mixed u–p** (same as `../sleeved/`, which
  matches the verified Python FE). E = 0.07 MPa: assumed, of the order of the 100 % modulus (69 kPa)
  in the Ecoflex 00-30 datasheet; not measured.
- Sleeve (optional): inextensible hoop fibres on the outer skin, same weak-form energy as `../sleeved/`.
- Mesh: COMSOL "fine". Check: "finer" changes tip angle at 10 kPa by 1.2 %, tip deflection by 0.9 %.

## Files
- `BuildActuator4CavitySleeved.java` — builder; case values are inlined by the runner (COMSOL blocks `getenv`).
- `run_sleeved_study.py` — runs the cases, writes the CSVs, plots `images/sleeved_study.png`.
- `sleeved_study_summary.csv` — one row per converged step; `sleeved_study_centreline.csv` — deformed axis.
- `*_solved.mph`, `log_*.txt`, `images/*_last.png` (last converged step, true scale).

## Results (pitch down, E = 0.07 MPa)
| Case | Last converged P | Tip rotation | What stops it |
|---|---|---|---|
| bare (no sleeve) | 11.7 kPa | 37° at 10 kPa | roof ballooning at P/μ = 0.50 (Python FE limit for the uniform tube: 0.523) |
| sleeved, canonical chambers | 20 kPa | 95° at 15, 133° at 17.5, 184° at 20 kPa | local stress runaway at the roof–side-wall junction: peak 0.25 MPa at 15, 1.04 MPa at 17.5, 53 MPa at 20 kPa |
| sleeved, deep + wide chambers | 8.5 kPa | 55° at 8 kPa, then reverses | thin septa and core collapse (section diameter shrinks) |
| sleeved, canonical chambers, 0.6 mm fillets (0.24 at tip) | 21 kPa | 85° at 15, 119° at 17.5, 170° at 20, 183° at 21 kPa | same as without fillets (see below) |

### Fillets do not remove the stress runaway
Per-face peak von Mises in the filleted model puts the hot spot on the two fillets where the **roof of the
pressurised chamber meets its side walls (septa)**, at the outer radius: 240 kPa at 15 kPa,
610–730 kPa at 17.5 kPa, 16–60 MPa at 20 kPa. (These faces run the whole chamber length; in the
model without fillets, axial bins put the peaks at x ≈ 0–40 and 90–130 mm.) Since a 0.6 mm fillet does not change
this, it is not a sharp-corner singularity. It coincides with severe local element distortion
(smallest local J: 0.30 at 15 kPa, 0.01 at 20 kPa; volume-averaged J = 1.0000). Whether it is a
physical instability of the roof–septum junction (septa loaded by pressure on one side only) or a
numerical breakdown of the mesh/element is **not resolved**; a much finer local mesh or hexahedral
elements in the walls would be needed to decide.
With fillets the actuator bends about 10 % less (more material) and the solve takes 3× longer.

**Results are reported up to P/μ ≈ 0.64 (15 kPa for E = 0.07 MPa), i.e. about 85–95° tip
rotation; the mesh check covers 10 kPa.** Within the next 2.5–5 kPa the computed local stress rises
past the ~1.4 MPa tensile strength of Ecoflex 00-30, so those steps are not used. A real silicone stiffens at large stretch (Yeoh C20, C30 > 0), which may delay this: needs
measured material data.

- At 2 kPa the sleeved FE gives 6.1° vs 5.7° from the hoop-reinforced beam formula (notes eq. 7); at
  higher pressure FE bends more, as in the Python sleeved FE.
- Neo-Hookean response depends on P/μ only: another silicone gives the same curve at P × (E_new / 0.07).
- Steps with peak stress above ~1.4 MPa are drawn as open circles: shape plausible, stress not reliable.

## Not yet done
A much finer local mesh and thicker septa (narrower chambers) to decide whether the stress runaway is physical, measured (Yeoh) material data,
self-contact, object contact with a stiff penalty, yaw/diagonal modes (same structure rotated).
Builder option `FILLET` (base radius in mm) builds the filleted chambers; it requires a homothetic taper.
