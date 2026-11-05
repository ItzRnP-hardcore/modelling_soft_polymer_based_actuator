# Tapered 4-Cavity Model & Scratch Script Archive Registry

This document records the archival history of legacy models, diagnostic scratch scripts, and numerical iterations for future agents and human collaborators.

---

## 1. Primary Active Canonical Files (`comsol/tapered_4cavity/`)

* **Primary Solved Model**: [`actuator_tapered_4cavity_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity/actuator_tapered_4cavity_solved.mph) (11.4 MB)
  - Full $180^\circ\text{--}192^\circ$ bending verified across **Pitch Down** ($[1,0,0,0]$), **Yaw Left** ($[0,1,0,0]$), and **Diagonal $45^\circ$** ($[0.707, 0.707, 0, 0]$).
  - Solves $0 \to 40\text{ kPa}$ (with $180.03^\circ$ at $35\text{ kPa}$, $\min(J) = 0.9259$, $\sigma_{\text{vm,max}} = 122.7\text{ kPa}$).
* **Primary Java Source**: [`BuildActuatorTapered4CavityFull.java`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity/BuildActuatorTapered4CavityFull.java)
* **Dataset**: [`comsol_multiaxial_doe_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity/comsol_multiaxial_doe_summary.csv)
* **Visualizations**: `comsol_pitch_down_*.png`, `comsol_yaw_left_*.png`, `comsol_diagonal_45deg_*.png`.

---

## 2. Archived Milestone Models (`archive/models/`)

| Filename | Size | Purpose / Physical Failure Mode Documented |
| :--- | :---: | :--- |
| `actuator_tapered_4cavity_100kpa_solved.mph` | 19.9 MB | Initial stocky actuator ($L=100\text{ mm}, E=1.65\text{ MPa}$, thick core). Only achieved $\theta = 4.75^\circ$ at $100\text{ kPa}$ due to high core bending stiffness and shear dominance. |
| `actuator_tapered_4cavity_ribbed_solved.mph` | 19.9 MB | First attempt at discrete rib modeling. Stalled at $\theta = 26.7^\circ$ due to overlapping Boolean wedge kerfs (which severed the radial ribs) and Saint Venant–Kirchhoff compressive loss of ellipticity. |
| `actuator_tapered_4cavity_sleeved_solved.mph` | 22.2 MB | Explored external sleeve constraint using surface weak-form penalty formulation (`wf_sleeve` with `uTY, vTY`). Stalled at $\theta = 13.2^\circ$ due to conical metric coupling between hoop restraint and finite axial rotation. |

---

## 3. Archived Scratch Scripts (`archive/scratch_scripts/`)

These 45 diagnostic Java classes and batch scripts were developed to isolate specific numerical/geometrical failure modes:

| Script Family | Files Included | Purpose / Finding |
| :--- | :--- | :--- |
| **Wedge & Rib Geometry** | `VerifyRibGeometry.java`, `TestWorkPlane.java`, `TestRibbedPressure.java`, `CheckCylProps.java` | Diagnosed Boolean intersection mechanics. Revealed that rotated blocks produce cavity half-angle $90^\circ - \phi$, leading to the discovery of the rib-severing bug. |
| **Bulge & Ballooning Diagnostics** | `DiagnoseBulge.java`, `CheckBulgeCase1.java`, `TestExteriorOnly.java` | Isolated local thin-wall blowout on outer conical roof vs. spine drive. |
| **Material & Coordinate Systems** | `TestCylindricalOrtho.java`, `TestOrthoSolve.java`, `TestStringGeom.java` | Evaluated cylindrical orthotropy and coordinate transformations in COMSOL 6.4 API. |
| **Solver & Weak Form Prototyping** | `TestSolveClean.java`, `TestSolveLinear.java`, `TestSolveWithSleeve.java`, `RunCleanStudy.java`, `TestStudy1.java` | Benchmarked Newton solver settings, parameter continuation steps, and weak-form boundary constraint conditioning. |
| **Visualization & Extraction** | `TestCrossSectionPlots.java`, `TestRibbedVisuals.java` | Scripted automated extraction of 2D cross-sectional cut-planes, deformed directors, and high-resolution OpenGL PNG exports. |

---

## 4. Temporary Files Purged
All intermediate solution caches (`*_Model.mph`, `tmp_inspect*.mph`, `*.lock`, `*.status`, `*.log`) totaling ~95 MB were permanently deleted to prevent repository bloat while preserving reproducible Java build definitions.

