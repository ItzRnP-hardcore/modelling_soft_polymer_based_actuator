# COMSOL Modeling & Results Master Directory Registry

This master index maps all COMSOL Multiphysics models, datasets, and generated visualization renders across the research project.

---

## 1. Directory Tree & Architecture

```
comsol/
├── bare/                               # Uniform Cylindrical Actuator (No Sleeve, No Taper)
│   ├── actuator_bare_solved.mph        # Solved model (0 - 40 kPa)
│   ├── BuildActuatorBare.java          # Java build code
│   ├── comsol_bare_summary.csv         # Kinematic summary data
│   ├── images/                         # Renders: deformed stress, dashboard, verification
│   └── archive/scratch_scripts/        # Export/extraction test scripts
│
├── sleeved/                            # Braided Sleeve Cylinder (Constant Cross-Section)
│   ├── actuator_sleeve_solved.mph      # Solved model (0 - 110 kPa)
│   ├── BuildActuatorSleeve.java        # Java build code
│   ├── comsol_sleeve_summary.csv       # Kinematic summary data
│   ├── images/                         # Renders: displacement P0, P50, P110, stress, dashboard
│   └── archive/scratch_scripts/        # Selection & probe diagnostics
│
├── tapered/                            # Tapered 1-Cavity Actuator (Single Eccentric Lumen)
│   ├── actuator_tapered_solved.mph     # Solved model (215.9 deg bend at 19.3 kPa)
│   ├── BuildActuatorTapered.java       # Java build code
│   ├── comsol_tapered_summary.csv      # Kinematic summary data
│   ├── images/                         # Renders: displacement, stress, dashboard
│   └── archive/scratch_scripts/        # Extraction & cone inspection scripts
│
├── large_deflection_4cavity/           # Theoretical Large-Deflection & Cosserat Validation
│   ├── Solve180And220.java             # Large deflection verification code
│   ├── actuator_3d_continuum...py      # Continuum integration script
│   ├── images/                         # Renders: 180 deg, 220 deg comparison dashboards
│   └── archive/scratch_scripts/        # Test scripts for hyperelastic curling
│
├── tapered_4cavity/                    # PRIMARY DELIVERABLE: Tapered 4-Cavity Continuum Robot
│   ├── actuator_tapered_4cavity_solved.mph # CANONICAL 180° - 192° 3D SOLVED MODEL
│   ├── BuildActuatorTapered4CavityFull.java # Canonical Java build code
│   ├── comsol_multiaxial_doe_summary.csv   # Verified 3D DoE dataset (Pitch, Yaw, Diagonal)
│   ├── images/
│   │   ├── 180deg_verified/           # 180° verified renders: disp, stress, sec2d
│   │   ├── legacy_multiaxial/         # 4-cavity steering mode renders (Pitch Up/Down, Yaw L/R)
│   │   └── legacy_ribbed_sleeved/     # Earlier iteration renders (ribbed, sleeved, cross-sections)
│   └── archive/
│       ├── models/                    # Milestone failure models (100kpa, ribbed, sleeved .mph)
│       ├── scratch_scripts/           # 45 diagnostic Java classes & test harnesses
│       └── ARCHIVE_REGISTRY.md        # Detailed breakdown of archived scripts
│
├── contact_grasp/                     # CONTACT MECHANICS & GRASPING FORCES (Cylindrical Object Sweep)
│   ├── actuator_contact_cylinder_R12mm_solved.mph # Fixed contact model for R = 12 mm
│   ├── actuator_contact_cylinder_R15mm_solved.mph # Fixed contact model for R = 15 mm
│   ├── actuator_contact_cylinder_R18mm_solved.mph # Fixed contact model for R = 18 mm
│   ├── actuator_contact_cylinder_R22mm_solved.mph # Fixed contact model for R = 22 mm
│   ├── actuator_floating_R12mm_*_solved.mph       # Floating cylinder models (Proximal, Central, Distal)
│   ├── actuator_floating_R15mm_*_solved.mph       # Floating cylinder models (Proximal, Central, Distal)
│   ├── actuator_floating_R18mm_*_solved.mph       # Floating cylinder models (Proximal, Central, Distal)
│   ├── actuator_floating_R22mm_*_solved.mph       # Floating cylinder models (Proximal, Central, Distal)
│   ├── BuildActuatorCylinderContactSweep.java     # Fixed cylinder contact sweep runner
│   ├── BuildFloatingCylinderLocationSweep.java    # Floating cylinder 6-DOF location sweep runner
│   ├── run_batch_sweep.py                         # Automated batch orchestrator
│   ├── plot_contact_grasp_curves.py              # Fixed grasp plotting script
│   ├── plot_floating_location_sweep.py           # Floating grasp & spatial location plotting script
│   ├── ExportFloatingGraspImages.java            # 3D render exporter for optimal grasp states
│   ├── contact_grasp_multiradius_summary.csv     # 36-point fixed cylinder DoE dataset
│   ├── floating_contact_location_sweep.csv       # 60-point floating cylinder spatial DoE dataset
│   ├── images/
│   │   ├── contact_force_vs_pressure_multiradius.png  # Fn & Ff vs. Pressure (Fixed)
│   │   ├── peak_grasp_force_vs_cylinder_radius.png    # Grasp capacity vs. Radius (Fixed)
│   │   ├── floating_grasp_fn_vs_pressure_locations.png # Fn vs P across Proximal, Central, Distal
│   │   ├── floating_grasp_peak_force_vs_location.png   # Peak Fn vs Initial Xc location
│   │   ├── floating_grasp_cylinder_displacements.png   # Floating object displacement trajectories (ux, uy)
│   │   ├── floating_grasp_optimal_friction_capacity.png # Friction holding capacity vs Pressure
│   │   ├── actuator_floating_R*mm_distal_disp.png      # 3D displacement renders at peak grasp
│   │   ├── actuator_floating_R*mm_distal_stress.png    # 3D von Mises stress renders at peak grasp
│   │   ├── contact_grasp_R12mm_disp_p.png, _stress.png # Fixed contact renders
│   │   ├── contact_grasp_R15mm_disp_p.png, _stress.png
│   │   ├── contact_grasp_R18mm_disp_p.png, _stress.png
│   │   └── contact_grasp_R22mm_disp_p.png, _stress.png
│   ├── force_optimization/                        # HIGH-PRESSURE SCALING (100 kPa) & FULL-WRAP (>270°) GRASP
│   │   ├── actuator_scaled_S1.00x_R22mm_solved.mph # Scale 1.00x up to 100 kPa (Fn = 1.59 N)
│   │   ├── actuator_scaled_S1.50x_R22mm_solved.mph # Scale 1.50x up to 100 kPa (Fn = 2.01 N)
│   │   ├── actuator_scaled_S1.00x_R15mm_solved.mph # Scale 1.00x (R=15mm) up to 100 kPa
│   │   ├── actuator_fullwrap_R20mm_solved.mph      # Full-wrap model (>306° at 60 kPa, 353° at 80 kPa)
│   │   ├── BuildHighPressureScaleSweep.java        # High-pressure geometric scaling engine
│   │   ├── BuildFullWrapGraspModel.java            # Slender high-curvature full-wrap model generator
│   │   ├── ExportAllOptimizationImages.java        # 3D render exporter for scaled & wrap models
│   │   ├── run_high_pressure_scaling.py            # High-pressure scaling batch runner
│   │   ├── run_full_wrap_study.py                  # Full-wrap batch runner
│   │   ├── plot_force_optimization_studies.py      # Publication dashboards plotting script
│   │   ├── scale_high_pressure_sweep.csv           # 18-point 100 kPa continuation dataset
│   │   ├── full_wrap_grasp_summary.csv             # 6-point wrap angle & force dataset (0-100 kPa)
│   │   └── images/
│   │       ├── scale_force_vs_pressure_100kpa.png  # Force vs pressure up to 100 kPa
│   │       ├── scale_factor_force_scaling_law.png  # Quadratic force scaling law
│   │       ├── scale_stress_vs_pressure_safety.png # Peak stress & burst limit safety factors
│   │       ├── full_wrap_grasp_mechanics.png       # Wrap angle & normal force vs pressure
│   │       ├── full_wrap_floating_trajectories.png # Floating object self-centering trajectories
│   │       ├── actuator_fullwrap_R20mm_disp.png    # 3D displacement render of >270° wrap at 60 kPa
│   │       ├── actuator_fullwrap_R20mm_stress.png  # 3D von Mises stress render at 60 kPa
│   │       ├── actuator_fullwrap_R20mm_P80_loop.png # 3D displacement render of 353° full loop at 80 kPa
│   │       ├── actuator_scaled_S1.50x_disp_P100.png # 3D displacement render of 1.50x at 100 kPa
│   │       ├── actuator_scaled_S1.50x_stress_P100.png # 3D stress render of 1.50x at 100 kPa
│   │       ├── actuator_scaled_S1.00x_disp_P100.png # 3D displacement render of 1.00x at 100 kPa
│   │       └── actuator_scaled_S1.00x_stress_P100.png # 3D stress render of 1.00x at 100 kPa
│   ├── curling_vortex_grasp/                      # CURLING VORTEX ENCLOSURE GRASP OPTIMIZATION (R = 8, 10, 12 mm)
│   │   ├── actuator_vortex_R8mm_Vortex_Xc52_Yc-58_solved.mph   # Peak clamping (Fn = 0.1993 N at 40 kPa, +1281%)
│   │   ├── actuator_vortex_R10mm_Vortex_Xc50_Yc-55_solved.mph  # Optimal vortex cinch (Fn = 0.1924 N at 40 kPa, +1233%)
│   │   ├── actuator_vortex_R12mm_Vortex_Xc48_Yc-52_solved.mph  # Vortex wrap (Fn = 0.1841 N at 40 kPa, +1175%)
│   │   ├── actuator_vortex_R12mm_Baseline_Xc42_Yc-25_solved.mph # Baseline proximal underbelly (Fn collapses to 0.0144 N)
│   │   ├── BuildCurlingVortexGraspStudy.java       # COMSOL 6.4 batch compilation & FEA solver engine
│   │   ├── compare_geometry.py                     # Kinematic curling trajectory & eyelet center extraction
│   │   ├── run_vortex_study.py                     # Automated batch runner & publication plotting
│   │   ├── curling_vortex_grasp_summary.csv        # 20-point parametric force & floating trajectory dataset
│   │   └── images/
│   │       ├── curling_vortex_grasp_comparison.png # Publication comparison chart: Normal force & inward draw
│   │       ├── geometric_curl_comparison.png       # 2D kinematic trajectory vs target cylinder placement
│   │       ├── actuator_vortex_R8mm_Vortex_Xc52_Yc-58_disp_P40.png # 3D displacement render at 40 kPa
│   │       ├── actuator_vortex_R10mm_Vortex_Xc50_Yc-55_disp_P40.png # 3D displacement render at 40 kPa
│   │       ├── actuator_vortex_R12mm_Vortex_Xc48_Yc-52_disp_P40.png # 3D displacement render at 40 kPa
│   │       └── actuator_vortex_R12mm_Baseline_Xc42_Yc-25_disp_P40.png # 3D render showing slip-off at 40 kPa
│   └── archive/                                  # Diagnostic API tests & boundary inspectors
│
└── benched/                            # Deprecated early prototypes
    ├── multi_cavity/                  # Untapered 4-cavity models
    └── standalone_scripts/            # Early geometry utilities
```

---

## 2. Image & Model Cross-Reference Guide

When writing reports, referencing findings, or inspecting simulation results, use this mapping:

### A. Contact Grasp Models & Multiradius Force Sweep (`contact_grasp/`)
* **Fixed Cylinder Model Files**:
  - [`contact_grasp/actuator_contact_cylinder_R12mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_contact_cylinder_R12mm_solved.mph)
  - [`contact_grasp/actuator_contact_cylinder_R15mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_contact_cylinder_R15mm_solved.mph)
  - [`contact_grasp/actuator_contact_cylinder_R18mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_contact_cylinder_R18mm_solved.mph)
  - [`contact_grasp/actuator_contact_cylinder_R22mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_contact_cylinder_R22mm_solved.mph)
* **Floating Cylinder Model Files**:
  - [`contact_grasp/actuator_floating_R12mm_distal_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_floating_R12mm_distal_solved.mph) (and `_central`, `_proximal`)
  - [`contact_grasp/actuator_floating_R15mm_distal_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_floating_R15mm_distal_solved.mph) (and `_central`, `_proximal`)
  - [`contact_grasp/actuator_floating_R18mm_distal_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_floating_R18mm_distal_solved.mph) (and `_central`, `_proximal`)
  - [`contact_grasp/actuator_floating_R22mm_distal_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_floating_R22mm_distal_solved.mph) (and `_central`, `_proximal`)
* **Java Generators**:
  - [`contact_grasp/BuildActuatorCylinderContactSweep.java`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/BuildActuatorCylinderContactSweep.java) (Fixed Object)
  - [`contact_grasp/BuildFloatingCylinderLocationSweep.java`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/BuildFloatingCylinderLocationSweep.java) (Floating 6-DOF Object)
  - [`contact_grasp/ExportFloatingGraspImages.java`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/ExportFloatingGraspImages.java) (3D Render Exporter)
* **Python Scripts**:
  - [`contact_grasp/run_batch_sweep.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/run_batch_sweep.py) (Batch orchestrator)
  - [`contact_grasp/plot_contact_grasp_curves.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/plot_contact_grasp_curves.py) (Fixed grasp plotting)
  - [`contact_grasp/plot_floating_location_sweep.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/plot_floating_location_sweep.py) (Floating grasp & spatial location plotting)
* **Numerical Datasets**:
  - [`contact_grasp/contact_grasp_multiradius_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/contact_grasp_multiradius_summary.csv) (36 records)
  - [`contact_grasp/floating_contact_location_sweep.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/floating_contact_location_sweep.csv) (60 records)
* **Images (`contact_grasp/images/`)**:
  - `floating_grasp_fn_vs_pressure_locations.png`: 4-panel plot of $F_N$ vs. $P$ comparing Proximal, Central, and Distal locations.
  - `floating_grasp_peak_force_vs_location.png`: Peak normal force vs. initial cylinder coordinate $X_c$ proving Distal ($X_c = 42\text{ mm}$) is the global optimum.
  - `floating_grasp_cylinder_displacements.png`: Floating cylinder 2D kinematic displacement trajectories $(\Delta X_c, \Delta Y_c)$ demonstrating self-centering behavior up to $7.88\text{ mm}$.
  - `floating_grasp_optimal_friction_capacity.png`: Friction force capacity $F_f = \mu F_N$ vs. pressure at optimal locations.
  - `actuator_floating_R*mm_distal_disp.png`, `_stress.png`: 3D deformation and contact stress renders.

### A2. Force Optimization & Full-Wrap (>270°) Grasp Models (`contact_grasp/force_optimization/`)
* **Solved Model Files**:
  - [`contact_grasp/force_optimization/actuator_scaled_S1.00x_R22mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/actuator_scaled_S1.00x_R22mm_solved.mph) (Scale 1.00x up to 100 kPa, $F_N = 1.5907\text{ N}$)
  - [`contact_grasp/force_optimization/actuator_scaled_S1.50x_R22mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/actuator_scaled_S1.50x_R22mm_solved.mph) (Scale 1.50x up to 100 kPa, $F_N = 2.0101\text{ N}$)
  - [`contact_grasp/force_optimization/actuator_scaled_S1.00x_R15mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/actuator_scaled_S1.00x_R15mm_solved.mph) (Scale 1.00x against $R=15\text{ mm}$ cylinder)
  - [`contact_grasp/force_optimization/actuator_fullwrap_R20mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/actuator_fullwrap_R20mm_solved.mph) (Elongated $L=220\text{ mm}$ model: $306.8^\circ$ wrap at $60\text{ kPa}$, $353.0^\circ$ at $80\text{ kPa}$)
* **Java Generators**:
  - [`contact_grasp/force_optimization/BuildHighPressureScaleSweep.java`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/BuildHighPressureScaleSweep.java)
  - [`contact_grasp/force_optimization/BuildFullWrapGraspModel.java`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/BuildFullWrapGraspModel.java)
  - [`contact_grasp/force_optimization/ExportAllOptimizationImages.java`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/ExportAllOptimizationImages.java)
* **Python Scripts**:
  - [`contact_grasp/force_optimization/run_high_pressure_scaling.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/run_high_pressure_scaling.py)
  - [`contact_grasp/force_optimization/run_full_wrap_study.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/run_full_wrap_study.py)
  - [`contact_grasp/force_optimization/plot_force_optimization_studies.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/plot_force_optimization_studies.py)
* **Numerical Datasets**:
  - [`contact_grasp/force_optimization/scale_high_pressure_sweep.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/scale_high_pressure_sweep.csv) (18 records, 0 - 100 kPa)
  - [`contact_grasp/force_optimization/full_wrap_grasp_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/force_optimization/full_wrap_grasp_summary.csv) (6 records, 0 - 100 kPa, wrap angle up to $353.0^\circ$)
* **Images (`contact_grasp/force_optimization/images/`)**:
  - `scale_force_vs_pressure_100kpa.png`: Clamping force continuation up to $100\text{ kPa}$ comparing scales and radii.
  - `scale_factor_force_scaling_law.png`: Empirical verification of $F_N \propto S^2$ quadratic scaling.
  - `scale_stress_vs_pressure_safety.png`: Von Mises stress continuation proving $>3\times$ factor of safety below rupture.
  - `full_wrap_grasp_mechanics.png`: Wrap angle, normal force, and contact stress vs. pressure.
  - `full_wrap_floating_trajectories.png`: Tip locus & floating object inward translation trajectories.
  - `actuator_fullwrap_R20mm_disp.png`: 3D displacement render showing $306.8^\circ$ wrap ($> 270^\circ$).
  - `actuator_fullwrap_R20mm_stress.png`: 3D von Mises stress render at $60\text{ kPa}$.
  - `actuator_fullwrap_R20mm_P80_loop.png`: 3D render of complete $353^\circ$ circle enclosure loop at $80\text{ kPa}$.
  - `actuator_scaled_S1.50x_disp_P100.png`: 3D displacement render of Scale 1.50x delivering $2.01\text{ N}$ force at $100\text{ kPa}$.
  - `actuator_scaled_S1.50x_stress_P100.png`: 3D stress render of Scale 1.50x at $100\text{ kPa}$.
  - `actuator_scaled_S1.00x_disp_P100.png`: 3D displacement render of Scale 1.00x delivering $1.59\text{ N}$ force at $100\text{ kPa}$.
  - `actuator_scaled_S1.00x_stress_P100.png`: 3D stress render of Scale 1.00x at $100\text{ kPa}$.

### A.3 Curling Vortex Grasp Optimization Models (`contact_grasp/curling_vortex_grasp/`)
* **Solved MPH Models**:
  - [`contact_grasp/curling_vortex_grasp/actuator_vortex_R8mm_Vortex_Xc52_Yc-58_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/curling_vortex_grasp/actuator_vortex_R8mm_Vortex_Xc52_Yc-58_solved.mph) (Peak force $F_N = 0.1993\text{ N}$)
  - [`contact_grasp/curling_vortex_grasp/actuator_vortex_R10mm_Vortex_Xc50_Yc-55_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/curling_vortex_grasp/actuator_vortex_R10mm_Vortex_Xc50_Yc-55_solved.mph) (Optimal vortex cinch $F_N = 0.1924\text{ N}$)
  - [`contact_grasp/curling_vortex_grasp/actuator_vortex_R12mm_Vortex_Xc48_Yc-52_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/curling_vortex_grasp/actuator_vortex_R12mm_Vortex_Xc48_Yc-52_solved.mph) ($F_N = 0.1841\text{ N}$)
  - [`contact_grasp/curling_vortex_grasp/actuator_vortex_R12mm_Baseline_Xc42_Yc-25_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/curling_vortex_grasp/actuator_vortex_R12mm_Baseline_Xc42_Yc-25_solved.mph) (Baseline comparison, slips off to $0.0144\text{ N}$)
* **Generators & Batch Runners**:
  - [`contact_grasp/curling_vortex_grasp/BuildCurlingVortexGraspStudy.java`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/curling_vortex_grasp/BuildCurlingVortexGraspStudy.java) (COMSOL 6.4 batch compilation source)
  - [`contact_grasp/curling_vortex_grasp/run_vortex_study.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/curling_vortex_grasp/run_vortex_study.py) (Batch orchestrator & plotter)
  - [`contact_grasp/curling_vortex_grasp/compare_geometry.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/curling_vortex_grasp/compare_geometry.py) (Kinematic trajectory extractor)
* **Dataset**:
  - [`contact_grasp/curling_vortex_grasp/curling_vortex_grasp_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/curling_vortex_grasp/curling_vortex_grasp_summary.csv) (20 records, 4 cases across $P \in [0, 40]\text{ kPa}$)
* **Images (`contact_grasp/curling_vortex_grasp/images/`)**:
  - `curling_vortex_grasp_comparison.png`: Publication 2-panel chart (Normal clamping force & self-centering inward shift vs. pressure).
  - `geometric_curl_comparison.png`: 2D spatial trajectory of curling midline vs. target cylinder placement.
  - `actuator_vortex_R8mm_Vortex_Xc52_Yc-58_disp_P40.png`: 3D render at $40\text{ kPa}$ showing $R=8\text{ mm}$ cinch grasp.
  - `actuator_vortex_R10mm_Vortex_Xc50_Yc-55_disp_P40.png`: 3D render at $40\text{ kPa}$ showing $R=10\text{ mm}$ optimal vortex grasp.
  - `actuator_vortex_R12mm_Vortex_Xc48_Yc-52_disp_P40.png`: 3D render at $40\text{ kPa}$ showing $R=12\text{ mm}$ vortex wrap.
  - `actuator_vortex_R12mm_Baseline_Xc42_Yc-25_disp_P40.png`: 3D render showing slip-off at $40\text{ kPa}$ under proximal baseline placement.

### B. Tapered 4-Cavity Model (Canonical $180^\circ$ Solved Model)
* **Model File**: [`tapered_4cavity/actuator_tapered_4cavity_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity/actuator_tapered_4cavity_solved.mph)
* **Java Generator**: [`tapered_4cavity/BuildActuatorTapered4CavityFull.java`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity/BuildActuatorTapered4CavityFull.java)
* **Numerical Data**: [`tapered_4cavity/comsol_multiaxial_doe_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity/comsol_multiaxial_doe_summary.csv)
* **Images (`tapered_4cavity/images/180deg_verified/`)**:
  - `comsol_pitch_down_disp.png`: 3D Total Displacement at $\theta_{\text{tip}} = 180.03^\circ$ (Pitch Down, $P = 35\text{ kPa}$).
  - `comsol_pitch_down_stress.png`: 3D von Mises Stress ($\sigma_{\text{vm,max}} = 122.7\text{ kPa}$) at $180^\circ$.
  - `comsol_pitch_down_sec2d_stress.png`: Mid-cross-section $X = 70\text{ mm}$ showing intact 4 ribs and zero roof blow-out.
  - `comsol_yaw_left_disp.png`: 3D Total Displacement at $\theta_{\text{tip}} = 180.03^\circ$ (Yaw Left, $P = 35\text{ kPa}$).
  - `comsol_yaw_left_stress.png`: 3D von Mises Stress ($\sigma_{\text{vm,max}} = 123.5\text{ kPa}$) at $180^\circ$ Yaw.
  - `comsol_yaw_left_sec2d_stress.png`: Mid-cross-section stress under pure Yaw.
  - `comsol_diagonal_45deg_disp.png`: 3D Total Displacement at $\theta_{\text{tip}} = 180.48^\circ$ ($45^\circ$ Diagonal, $P = 35\text{ kPa}$).
  - `comsol_diagonal_45deg_stress.png`: 3D von Mises Stress at $180^\circ$ Diagonal bend.
  - `comsol_diagonal_45deg_sec2d_stress.png`: Mid-cross-section stress under simultaneous Pitch + Yaw inflation.

### B. Tapered 1-Cavity Model ($215.9^\circ$ Single-Chamber Benchmark)
* **Model File**: [`tapered/actuator_tapered_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered/actuator_tapered_solved.mph)
* **Java Generator**: [`tapered/BuildActuatorTapered.java`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered/BuildActuatorTapered.java)
* **Numerical Data**: [`tapered/comsol_tapered_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered/comsol_tapered_summary.csv)
* **Images (`tapered/images/`)**:
  - `comsol_tapered_disp.png`: Total displacement curling past $180^\circ$ into $215.9^\circ$ loop.
  - `comsol_tapered_stress.png`: Stress distribution in 1-cavity tapered silicone body.
  - `tapered_dashboard.png`: 4-panel kinematic trajectory and tip orientation summary.

### C. Braided Sleeve Model (Straight Cylinder, Constant Diameter)
* **Model File**: [`sleeved/actuator_sleeve_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/sleeved/actuator_sleeve_solved.mph)
* **Java Generator**: [`sleeved/BuildActuatorSleeve.java`](file:///c:/Users/rudra/Documents/BTP/comsol/sleeved/BuildActuatorSleeve.java)
* **Numerical Data**: [`sleeved/comsol_sleeve_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/sleeved/comsol_sleeve_summary.csv)
* **Images (`sleeved/images/`)**:
  - `comsol_sleeve_disp_P0.png`, `comsol_sleeve_disp_P50.png`, `comsol_sleeve_disp_P110.png`: Staged deformation showing how the braided sleeve prevents ballooning up to $110\text{ kPa}$.
  - `comsol_sleeve_stress_P110.png`: Stress profile at maximum pressure.
  - `sleeve_dashboard.png`: Pressure-deflection comparison vs. bare actuator.

### D. Bare Actuator Model (Unreinforced Silicone Baseline)
* **Model File**: [`bare/actuator_bare_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/bare/actuator_bare_solved.mph)
* **Java Generator**: [`bare/BuildActuatorBare.java`](file:///c:/Users/rudra/Documents/BTP/comsol/bare/BuildActuatorBare.java)
* **Numerical Data**: [`bare/comsol_bare_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/bare/comsol_bare_summary.csv)
* **Images (`bare/images/`)**:
  - `comsol_bare_deformed_stress.png`: Aneurysm/ballooning failure of unreinforced silicone chamber.
  - `comsol_dashboard.png`: Baseline performance summary.
  - `comsol_verification.png`: Experimental vs. numerical verification comparison.

---

## 3. Maintenance Policy for Future Agents
1. **Never edit files in `archive/`**: Those serve as historical baseline evidence.
2. **Canonical model for further simulation**: Always load or modify [`tapered_4cavity/BuildActuatorTapered4CavityFull.java`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity/BuildActuatorTapered4CavityFull.java) and its solved MPH file.
3. **Temporary files policy**: Always delete intermediate `*_Model.mph` and `tmp_*.mph` files after running COMSOL batch jobs to keep workspace clean.

