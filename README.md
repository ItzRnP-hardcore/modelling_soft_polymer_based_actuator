# Eccentric-cavity soft bending actuator: pressure to deflection

BTP project. A silicone cylinder (outer radius R, length L) with an off-centre
circular cavity (radius r, offset r_p) bends toward its thick wall when the cavity
is pressurised. The goal is the map from cavity pressure P to tip deflection δ.

Units everywhere: mm, N, MPa (1 MPa = 1000 kPa).

## Files

| File | What it is |
|---|---|
| `BTP_260825_144004.pdf` | Handwritten derivation (the "notes") |
| `modeling_guideline.md`, `actuator_guideline.html` | Review of the notes, model tiers, FE findings, workflow |
| `actuator_model.py` | Beam model, Tiers 1–3 (closed form and hyperelastic section) |
| `actuator_fea.py` | Tier 4: nonlinear 3-D finite-element model with a pressure sweep |
| `fea_verify.py` | Verification of the FE solver against exact solutions |
| `fea_viewer.py` | Builds the interactive viewer for a finished FE run |
| `fea_compare.py` | Report figure: FE against the beam model |
| `results/` | One folder per FE run |

## Requirements

Python 3.10 or later with `numpy`, `scipy`, `matplotlib`, `jax` and `plotly`.

## Running a pressure sweep

```bash
python actuator_fea.py --R 10 --r 4 --rp 3 --L 100 --E 0.6 --pmax 110 --steps 22 --out results/bare
```

Then open `results/bare/viewer.html` in a browser: drag the pressure slider or
press Play to see the deformed shape, tip deflection and tip angle at each state.
The page is self-contained and works offline. `python fea_viewer.py results/bare`
rebuilds it from the saved results.

Options of `actuator_fea.py` (all have defaults, see `--help`):

| Option | Meaning |
|---|---|
| `--R --r --rp --L --tcap` | geometry [mm]; `--tcap` is the tip cap thickness (default R/2) |
| `--E` | small-strain Young's modulus [MPa], neo-Hookean |
| `--yeoh C10 C20 C30` | Yeoh coefficients [MPa] instead of `--E` |
| `--pmax --steps` | pressure ramp, 0 to pmax [kPa] in equal steps |
| `--mesh` | `coarse` (about 1 min), `medium` (about 3 min), `fine` (slower; used for the convergence check) |
| `--sleeve` | add an inextensible hoop-fibre sleeve on the outer surface |
| `--nu` | Poisson's ratio, default 0.4995 (nearly incompressible) |

Each run writes `summary.csv` (one row per converged state: pressure, tip
deflection, tip angle, cavity volume, peak stretch, wall thinning), `results.npz`
(mesh and displacement fields), `run.json` (inputs and the ballooning limit
pressure, if one was reached) and `viewer.html`.

The two runs already in `results/` are the placeholder design with E = 0.6 MPa:
`bare` (no reinforcement) and `sleeve` (same actuator with `--sleeve`).

If the requested pressure exceeds the ballooning limit, the solver switches from
pressure control to volume control, follows the path over the pressure peak and
reports the peak as the limit pressure.

## Other commands

```bash
python actuator_model.py
```

```bash
python -u fea_verify.py
```

```bash
python fea_compare.py results/bare results/sleeve
```

## What the FE model is

27-node hexahedra in a mixed pressure–displacement formulation (Q2/P1), Yeoh or
neo-Hookean material, follower pressure derived from the enclosed cavity volume,
Newton iteration with tangents from automatic differentiation. Half the actuator
is meshed (symmetry about the bending plane), the base is clamped, and a solid cap
closes the cavity at the tip. See the docstring of `actuator_fea.py`.
