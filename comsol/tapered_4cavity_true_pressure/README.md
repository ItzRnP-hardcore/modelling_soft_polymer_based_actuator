# Diagnostic: earlier 4-chamber model with real pressure

`BuildActuatorTapered4CavityTruePressure.java` is a copy of `../tapered_4cavity/BuildActuatorTapered4CavityFull.java`
with one change: every face of chamber 1 carries the same follower pressure P (the earlier
`eta_drive`/`alpha_balloon` load factors and the length profile are removed). Geometry, mesh and
material (compressible neo-Hookean, E = 0.40 MPa, ν = 0.45) are unchanged.

Result (pitch down, `truepressure_summary.csv`): 1.1° at 10 kPa, 3.2° at 20 kPa, 12.7° at 40 kPa,
against 82.9°, 141.0° and 186.1° with the earlier load. All 24 steps to 40 kPa converged.

This model is still not physical (ν = 0.45). The physical model is in `../tapered_4cavity_sleeved/`.
Run: `python run_truepressure.py` (about 90 s); figure `images/tuned_vs_true_pressure.png`.
