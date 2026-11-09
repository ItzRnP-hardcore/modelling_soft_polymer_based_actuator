# COMSOL models: registry and status

Rewritten on 9 November 2026 after an audit. Every folder carries a status:

- **VALID**: real cavity pressure, nearly incompressible silicone (ν = 0.4995, mixed u–p), results
  checked against source data. Use these for any claim.
- **NON-PHYSICAL LOAD**: the load is not the cavity pressure (`eta_drive·P·(0.20+1.80(X/L)^1.5)` on
  the inner wall and tip face, `alpha_balloon·P` = 0.005 P elsewhere) and the material is compressible
  (ν = 0.45). Kept as a record only; no result from these folders may be quoted.
- **UNSUPPORTED**: the claimed result is not backed by the files.

The verified narrative is `docs/tapered_4cavity_mechanics_report.md`.

| Folder | Status | Content |
|---|---|---|
| `bare/` | VALID | Uniform eccentric-cavity actuator (R 10, r 4, r_p 3, L 100 mm, E 0.6 MPa), no sleeve, 0–90 kPa. Agrees with the Python FE within 3 % from 60 to 90 kPa. Not run to the ballooning limit. |
| `sleeved/` | VALID | Same actuator with an inextensible hoop sleeve, 0–110 kPa. Agrees with the Python FE within 0.7 %. |
| `tapered_4cavity_sleeved/` | VALID | Tapered 4-chamber actuator, real pressure, ν = 0.4995, E = 0.07 MPa (assumed): bare, sleeved, deeper chambers, filleted chambers. See its README. |
| `tapered_4cavity_true_pressure/` | Diagnostic | Earlier 4-chamber model with only the load corrected to real pressure (still ν = 0.45). Shows the earlier 180° came from the load model: 12.7° at 40 kPa with real pressure. |
| `tapered_4cavity/` | NON-PHYSICAL LOAD | Earlier "180° at 35 kPa" pitch/yaw/diagonal model. |
| `contact_grasp/` (all subfolders) | NON-PHYSICAL LOAD | Grasp-force, floating-object, vortex, full-wrap, caging and maximum-force studies. Additional faults: contact penalty 1e6 N/m³ (0.5–3 mm penetration), reported force set by the object's support spring, "wrap angle" = tip rotation. |
| `large_deflection_4cavity/` | UNSUPPORTED | `Solve180And220.java` builds geometry only. The "COMSOL" 180°/220° images were drawn by matplotlib (`render_comsol_180_220.py`) from a beam-formula integration. |
| `tapered/` | UNSUPPORTED | Bare tapered 1-chamber actuator. The CSV has three rows without pressures; the "215.9° at 19.3 kPa" claim has no source. |
| `benched/` | Not used | Early prototypes and API probes. |

## Conventions
- Java builders are compiled with `comsolcompile` and run with `comsolbatch -inputfile <class>`.
- COMSOL's security preferences block `System.getenv` and Java file writes in batch mode; runners
  inline case values into the source and parse printed rows (see `tapered_4cavity_sleeved/run_sleeved_study.py`).
- Delete autosaved `*_Model.mph` files after a run.
