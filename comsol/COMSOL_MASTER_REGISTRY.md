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
│   ├── actuator_contact_cylinder_R12mm_solved.mph # Solved contact model for R = 12 mm
│   ├── actuator_contact_cylinder_R15mm_solved.mph # Solved contact model for R = 15 mm
│   ├── actuator_contact_cylinder_R18mm_solved.mph # Solved contact model for R = 18 mm
│   ├── actuator_contact_cylinder_R22mm_solved.mph # Solved contact model for R = 22 mm
│   ├── BuildActuatorCylinderContactSweep.java     # Canonical contact sweep Java runner
│   ├── plot_contact_grasp_curves.py              # Publication-quality curve plotting script
│   ├── contact_grasp_multiradius_summary.csv     # 36-point DoE dataset: Fn, Ff, contact pressure, stress
│   ├── images/
│   │   ├── contact_force_vs_pressure_multiradius.png  # Fn & Ff vs. Pressure across all radii
│   │   ├── peak_grasp_force_vs_cylinder_radius.png    # Grasp capacity vs. Cylinder radius
│   │   ├── contact_grasp_R12mm_disp_p.png, _stress.png # 3D contact pressure & stress renders
│   │   ├── contact_grasp_R15mm_disp_p.png, _stress.png
│   │   ├── contact_grasp_R18mm_disp_p.png, _stress.png
│   │   └── contact_grasp_R22mm_disp_p.png, _stress.png
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
* **Model Files**:
  - [`contact_grasp/actuator_contact_cylinder_R12mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_contact_cylinder_R12mm_solved.mph)
  - [`contact_grasp/actuator_contact_cylinder_R15mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_contact_cylinder_R15mm_solved.mph)
  - [`contact_grasp/actuator_contact_cylinder_R18mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_contact_cylinder_R18mm_solved.mph)
  - [`contact_grasp/actuator_contact_cylinder_R22mm_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/actuator_contact_cylinder_R22mm_solved.mph)
* **Java Generator**: [`contact_grasp/BuildActuatorCylinderContactSweep.java`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/BuildActuatorCylinderContactSweep.java)
* **Plotting Script**: [`contact_grasp/plot_contact_grasp_curves.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/plot_contact_grasp_curves.py)
* **Numerical Data**: [`contact_grasp/contact_grasp_multiradius_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/contact_grasp_multiradius_summary.csv)
* **Images (`contact_grasp/images/`)**:
  - `contact_force_vs_pressure_multiradius.png`: Dual-panel plot of Normal Contact Force $F_N(P)$ and Friction Force $F_f(P)$ across $R_{\text{cyl}} \in [12, 15, 18, 22]\text{ mm}$.
  - `peak_grasp_force_vs_cylinder_radius.png`: Grasping capacity curve showing peak normal force scaling from $0.013\text{ N}$ to $0.707\text{ N}$ as radius increases.
  - `contact_grasp_R12mm_disp_p.png`, `contact_grasp_R12mm_stress.png`: 3D contact pressure and stress fields for $R=12\text{ mm}$.
  - `contact_grasp_R15mm_disp_p.png`, `contact_grasp_R15mm_stress.png`: 3D contact pressure and stress fields for $R=15\text{ mm}$ ($F_N = 0.150\text{ N}$).
  - `contact_grasp_R18mm_disp_p.png`, `contact_grasp_R18mm_stress.png`: 3D contact pressure and stress fields for $R=18\text{ mm}$ ($F_N = 0.339\text{ N}$).
  - `contact_grasp_R22mm_disp_p.png`, `contact_grasp_R22mm_stress.png`: 3D contact pressure and stress fields for $R=22\text{ mm}$ ($F_N = 0.707\text{ N}$).

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

