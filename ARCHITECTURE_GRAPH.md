# System Architecture & Worktree Dependency Graph

> [!NOTE]
> This document maps every component, script, data pipeline, and mathematical derivation across the **BTP Soft Robotics & Continuum Actuator** repository. Any AI agent or researcher can look up this graph to trace connected worktrees, understand dependencies, and find relevant context.

---

## 1. High-Level Dependency & Context Graph

```mermaid
graph TD
    %% SUBGRAPH 1: THEORY & FOUNDATIONAL NOTES
    subgraph G_DOCS ["1. Theory, Notes & Guidelines (docs/)"]
        D_PDF["docs/BTP_260825_144004.pdf<br/><b>Handwritten Notes</b><br/>Thrust Moment M = F·e, Linear Beam Theory"]
        D_MD["docs/modeling_guideline.md<br/><b>Scientific Guideline</b><br/>Incompressible Cancellation, Ballooning Physics"]
        D_HTML["docs/actuator_guideline.html<br/><b>Interactive Web Guide</b><br/>Multi-Tier Equations & Trade-offs"]
        README["README.md & EXPLANATION.md<br/><b>Entry Points & Physical Intuition</b>"]
    end

    %% SUBGRAPH 2: CORE PYTHON SOLVER (ROOT)
    subgraph G_CORE ["2. Core Actuator Models & 3D FEA (Root)"]
        M_BEAM["actuator_model.py<br/><b>Analytical Beam Models</b><br/>Tiers 1-3: Linear, Arc Kinematics, Hyperelastic"]
        M_FEA["actuator_fea.py<br/><b>Tier 4: 3D Nonlinear FEA</b><br/>hex27 Q2/P1, JAX Neo-Hookean, Follower Pressure"]
        M_VER["fea_verify.py<br/><b>FEA Solver Verification</b><br/>Patch, Cantilever, Hoop Benchmarks"]
        M_CMP["fea_compare.py<br/><b>Model vs FEA Evaluator</b>"]
        M_VIEW["fea_viewer.py<br/><b>Interactive 3D Viewer Builder</b><br/>Plotly Deformed State Viewer"]
        FIG_RESP["actuator_response.png<br/><b>Analytical Curves</b>"]
        FIG_FEA["fea_comparison.png<br/><b>Beam vs FEA Figure</b>"]
    end

    %% SUBGRAPH 3: SIMULATION RESULTS
    subgraph G_RES ["3. Numerical Results Database (results/)"]
        R_BARE["results/bare/<br/>summary.csv, results.npz<br/>viewer.html (Ballooning Run)"]
        R_SLEEVE["results/sleeve/<br/>summary.csv, results.npz<br/>viewer.html (Sleeved Run)"]
        R_GRASP["results/grasp/<br/>sweep.csv, curves.csv<br/>sequence.csv"]
    end

    %% SUBGRAPH 4: MULTI-FINGER GRASPING & CONTINUUM MANIPULATION
    subgraph G_GRASP ["4. Continuum Arm Grasping (grasp/)"]
        G_MOD["grasp/grasp_model.py<br/><b>2D Contact & Grasp Mechanics</b><br/>Finger, Grasp, GraspSim (Coulomb Contact)"]
        G_3D["grasp/grasp3d_model.py<br/><b>3D Multi-Axis Contact</b>"]
        G_STD["grasp/grasp_study.py<br/><b>Parametric Sweep</b><br/>Uniform vs Tapered vs Tip-Hook"]
        G_SEQ["grasp/grasp_sequence.py<br/><b>Actuation Sequencing</b><br/>Chamber-by-Chamber vs Uniform"]
        G_FIG["grasp/grasp_figures.py<br/><b>Paper Figures Generator</b>"]
        G_ANI["grasp/tentacle_animation.py<br/><b>Physics Animations</b><br/>Pull-down and Wrapping Dynamics"]
    end

    %% SUBGRAPH 5: CROSS-SOFTWARE FEA VERIFICATION
    subgraph G_VERIF ["5. Multi-Software Cross-Verification (ansys/ & comsol/)"]
        subgraph G_ANSYS ["ANSYS Mechanical APDL (ansys/)"]
            A_EXP["ansys/export_ansys.py<br/><b>APDL Batch Exporter</b>"]
            A_MAC["ansys/*.mac<br/>actuator_bare.mac, actuator_sleeve.mac<br/>grasp_2d_verify.mac"]
        end

        subgraph G_COMSOL ["COMSOL Multiphysics 6.4 (comsol/)"]
            C_BARE["comsol/bare/<br/><b>Bare Actuator</b><br/>BuildActuatorBare.java, comsol_bare_summary.csv<br/>comsol_compare.py, comsol_dashboard.png"]
            C_SLEEVE["comsol/sleeved/<br/><b>Sleeved Actuator</b><br/>BuildActuatorSleeve.java, ReexportTrueScale.java<br/>plot_sleeve_comparison.py, sleeve_dashboard.png"]
            C_TAPER["comsol/tapered/<br/><b>Tapered Actuator (1-Cavity)</b><br/>BuildActuatorTapered.java<br/>(curl claim unsupported, see registry)"]
            C_4CAV["comsol/tapered_4cavity/<br/><b>4-Cavity Arm (non-physical load, record only)</b><br/>BuildActuatorTapered4CavityFull.java"]
            C_4SLV["comsol/tapered_4cavity_sleeved/<br/><b>4-Cavity Arm, real pressure, nu=0.4995</b><br/>BuildActuator4CavitySleeved.java<br/>run_sleeved_study.py"]
            C_BENCH["comsol/benched/<br/>multi_cavity/ (Uniform 4-Cavity)<br/>standalone_scripts/ (API Explorations)"]
        end
    end

    %% SUBGRAPH 6: REPORTS & PUBLICATIONS
    subgraph G_REP ["6. Academic Reports & Presentation (report/)"]
        REP_TEX["report/grasp_analysis.tex<br/><b>LaTeX Paper</b><br/>Continuum Grasping Mechanics"]
        REP_PDF["report/grasp_analysis.pdf<br/><b>Compiled Manuscript</b>"]
        REP_HAND["report/btp_derivation.pdf<br/>report/fea_handbook.pdf<br/><b>Mathematical & FEA Handbooks</b>"]
        REP_SLIDE["report/slides/ & with_notes/<br/><b>Presentation Decks</b>"]
        REP_FIGS["report/figures/<br/>grasp_capacity.png, grasp_curves.png<br/>grasp_maps.png, grasp_shapes.png"]
        REP_ANIS["report/animations/<br/>*.mp4, *.gif (Pull & Enveloping)"]
    end

    %% CONNECTIONS & DATAFLOWS
    %% Theory to Core Models
    D_PDF -->|Derivation foundation| D_MD
    D_MD -->|Theory guide| README
    D_PDF -.->|Handwritten formulas| M_BEAM
    D_MD -->|Tier 1-3 Formulation| M_BEAM
    D_MD -->|Tier 4 Nonlinear Formulation| M_FEA
    M_BEAM -->|Imports Geometry & Material| M_FEA
    M_BEAM -->|Outputs curves| FIG_RESP
    M_BEAM -->|Analytical check| M_VER
    M_FEA -->|Solver verified by| M_VER

    %% Core Models to Results
    M_FEA -->|Sweeps P to 104.7 kPa| R_BARE
    M_FEA -->|Sweeps P with hoop constraint| R_SLEEVE
    R_BARE -->|Generates 3D HTML| M_VIEW
    R_SLEEVE -->|Generates 3D HTML| M_VIEW
    R_BARE -->|Evaluates vs Beam Theory| M_CMP
    R_SLEEVE -->|Evaluates vs Beam Theory| M_CMP
    M_CMP -->|Outputs comparison figure| FIG_FEA

    %% Core Models to ANSYS
    M_FEA -->|Hex27 Mesh & Nodes| A_EXP
    M_BEAM -->|Geometry & Parameters| A_EXP
    G_MOD -->|Finger Contact Params| A_EXP
    A_EXP -->|Writes SOLID186 + SURF154| A_MAC

    %% Core Models to COMSOL
    R_BARE -->|Validates against| C_BARE
    R_SLEEVE -->|Validates against| C_SLEEVE
    C_BARE -->|1:1 3D FEA Verification| G_CORE

    %% Grasp Module Worktree
    M_BEAM -.->|Spontaneous curvature kappa0(P)| G_MOD
    G_MOD -->|2D kinematics & friction| G_3D
    G_MOD -->|Simulates holding force| G_STD
    G_MOD -->|Simulates chamber orders| G_SEQ
    G_STD -->|Writes sweep & curves| R_GRASP
    G_SEQ -->|Writes sequence CSV| R_GRASP
    R_GRASP -->|Plots hold capacity & envelopes| G_FIG
    G_MOD -->|Renders physics frames| G_ANI

    %% Outputs to Report
    G_FIG -->|Publishes visual plots| REP_FIGS
    G_ANI -->|Publishes media| REP_ANIS
    REP_FIGS -->|Included in manuscript| REP_TEX
    REP_TEX -->|Compiles to| REP_PDF
    C_SLEEVE -->|Validated sleeve formulation| C_4SLV
```

---

## 2. Worktree & Domain Breakdown

### Worktree A: Actuator Modeling & Custom FEA Solver
* **Core Files**: [`actuator_model.py`](file:///c:/Users/rudra/Documents/BTP/actuator_model.py), [`actuator_fea.py`](file:///c:/Users/rudra/Documents/BTP/actuator_fea.py), [`fea_viewer.py`](file:///c:/Users/rudra/Documents/BTP/fea_viewer.py), [`fea_compare.py`](file:///c:/Users/rudra/Documents/BTP/fea_compare.py), [`fea_verify.py`](file:///c:/Users/rudra/Documents/BTP/fea_verify.py).
* **Role**: Develops the fundamental continuum mechanics. Defines `Geometry` (outer cylinder $R=10\text{ mm}$, off-center cavity $r=4\text{ mm}$, eccentricity $r_p=3\text{ mm}$) and hyperelastic `Material` parameters. Implements Tiers 1–3 (closed-form linear, arc kinematics, numerical hyperelastic section) and Tier 4 (27-node hexahedral FEA with follower pressure and JAX automatic differentiation).
* **Key Artifacts Produced**: [`actuator_response.png`](file:///c:/Users/rudra/Documents/BTP/actuator_response.png), [`fea_comparison.png`](file:///c:/Users/rudra/Documents/BTP/fea_comparison.png), [`results/bare/viewer.html`](file:///c:/Users/rudra/Documents/BTP/results/bare/viewer.html), [`results/sleeve/viewer.html`](file:///c:/Users/rudra/Documents/BTP/results/sleeve/viewer.html).

---

### Worktree B: Multi-Finger Grasping & Continuum Manipulation
* **Core Files**: [`grasp/grasp_model.py`](file:///c:/Users/rudra/Documents/BTP/grasp/grasp_model.py), [`grasp/grasp3d_model.py`](file:///c:/Users/rudra/Documents/BTP/grasp/grasp3d_model.py), [`grasp/grasp_study.py`](file:///c:/Users/rudra/Documents/BTP/grasp/grasp_study.py), [`grasp/grasp_sequence.py`](file:///c:/Users/rudra/Documents/BTP/grasp/grasp_sequence.py), [`grasp/grasp_figures.py`](file:///c:/Users/rudra/Documents/BTP/grasp/grasp_figures.py), [`grasp/tentacle_animation.py`](file:///c:/Users/rudra/Documents/BTP/grasp/tentacle_animation.py).
* **Role**: Simulates 2-finger and multi-chamber soft robotic gripping under unilateral contact and Coulomb friction ($\mu$). Compares 3 finger architectures:
  1. `uniform`: Constant cross-section ($R=10\text{ mm}$, $t=2\text{ mm}$).
  2. `tapered`: Distally thinning cross-section ($R: 10 \rightarrow 5\text{ mm}$, $t: 2 \rightarrow 1\text{ mm}$).
  3. `tip-hook`: Uniform finger with proximal strain-limiting layer starting at 60% length to curl the tip.
* **Key Artifacts Produced**: Holding force databases ([`results/grasp/sweep.csv`](file:///c:/Users/rudra/Documents/BTP/results/grasp/sweep.csv), [`results/grasp/curves.csv`](file:///c:/Users/rudra/Documents/BTP/results/grasp/curves.csv)), manuscript figures in [`report/figures/`](file:///c:/Users/rudra/Documents/BTP/report/figures), and physics animations in [`report/animations/`](file:///c:/Users/rudra/Documents/BTP/report/animations).

---

### Worktree C: Multi-Software FEA Verification (COMSOL & ANSYS)
* **Core Folders**: [`comsol/`](file:///c:/Users/rudra/Documents/BTP/comsol), [`ansys/`](file:///c:/Users/rudra/Documents/BTP/ansys).
* **Role**: Cross-verification of the Python FE in COMSOL 6.4 and the tapered 4-chamber design. Status of every folder: [`comsol/COMSOL_MASTER_REGISTRY.md`](file:///c:/Users/rudra/Documents/BTP/comsol/COMSOL_MASTER_REGISTRY.md).
  - [`comsol/bare/`](file:///c:/Users/rudra/Documents/BTP/comsol/bare): agrees with the Python FE within 3 % from 60 to 90 kPa (not run to the ballooning limit).
  - [`comsol/sleeved/`](file:///c:/Users/rudra/Documents/BTP/comsol/sleeved): agrees with the Python FE within 0.7 % up to 110 kPa.
  - [`comsol/tapered_4cavity_sleeved/`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity_sleeved): tapered 4-chamber actuator with real pressure and real silicone; about 95° tip rotation at 15 kPa (E = 0.07 MPa, sleeved).
  - `comsol/tapered/`, `comsol/tapered_4cavity/`, `comsol/contact_grasp/`, `comsol/large_deflection_4cavity/`: results withdrawn (non-physical load or unsupported), kept as a record.
  - [`ansys/export_ansys.py`](file:///c:/Users/rudra/Documents/BTP/ansys/export_ansys.py): converts the 27-node hex mesh into ANSYS `SOLID186` + `SURF154` macros. The macros have not been run (no ANSYS licence on this machine).

---

### Worktree D: Academic Documentation, Reporting & Presentations
* **Core Folders**: [`docs/`](file:///c:/Users/rudra/Documents/BTP/docs), [`report/`](file:///c:/Users/rudra/Documents/BTP/report).
* **Role**: Translates engineering mechanics into peer-reviewed manuscripts, presentation slides, and technical handbooks:
  - [`report/grasp_analysis.tex`](file:///c:/Users/rudra/Documents/BTP/report/grasp_analysis.tex) $\rightarrow$ [`report/grasp_analysis.pdf`](file:///c:/Users/rudra/Documents/BTP/report/grasp_analysis.pdf): Complete research paper on continuum grasping mechanics.
  - [`report/btp_derivation.pdf`](file:///c:/Users/rudra/Documents/BTP/report/btp_derivation.pdf): Step-by-step mathematical handbook.
  - [`report/fea_handbook.pdf`](file:///c:/Users/rudra/Documents/BTP/report/fea_handbook.pdf): Detailed formulation of the nonlinear continuum finite element implementation.
  - [`report/slides/`](file:///c:/Users/rudra/Documents/BTP/report/slides) & [`report/final/`](file:///c:/Users/rudra/Documents/BTP/report/final): Presentation slides.

---

## 3. Agent Context Routing Lookup Table

When an agent is tasked with a specific query or feature request, use this table to immediately identify which files to read and modify:

| Context / Question Being Asked | Primary Files to Inspect | Connecting Worktrees & Upstream/Downstream Files |
| :--- | :--- | :--- |
| **"Why does the bare actuator balloon?" / "What is the limit pressure?"** | [`docs/modeling_guideline.md`](file:///c:/Users/rudra/Documents/BTP/docs/modeling_guideline.md)<br/>[`EXPLANATION.md`](file:///c:/Users/rudra/Documents/BTP/EXPLANATION.md) | **Upstream**: [`docs/BTP_260825_144004.pdf`](file:///c:/Users/rudra/Documents/BTP/docs/BTP_260825_144004.pdf)<br/>**Downstream**: [`actuator_fea.py`](file:///c:/Users/rudra/Documents/BTP/actuator_fea.py), [`results/bare/summary.csv`](file:///c:/Users/rudra/Documents/BTP/results/bare/summary.csv), [`comsol/bare/`](file:///c:/Users/rudra/Documents/BTP/comsol/bare) |
| **"How does the beam theory model work?" / "Where are section properties calculated?"** | [`actuator_model.py`](file:///c:/Users/rudra/Documents/BTP/actuator_model.py) | **Downstream**: [`fea_compare.py`](file:///c:/Users/rudra/Documents/BTP/fea_compare.py), [`fea_verify.py`](file:///c:/Users/rudra/Documents/BTP/fea_verify.py), [`actuator_response.png`](file:///c:/Users/rudra/Documents/BTP/actuator_response.png) |
| **"How does the custom 3D FEA work?" / "Element formulation & follower pressure"** | [`actuator_fea.py`](file:///c:/Users/rudra/Documents/BTP/actuator_fea.py) | **Verification**: [`fea_verify.py`](file:///c:/Users/rudra/Documents/BTP/fea_verify.py)<br/>**Outputs**: [`results/bare/`](file:///c:/Users/rudra/Documents/BTP/results/bare), [`results/sleeve/`](file:///c:/Users/rudra/Documents/BTP/results/sleeve)<br/>**ANSYS Export**: [`ansys/export_ansys.py`](file:///c:/Users/rudra/Documents/BTP/ansys/export_ansys.py) |
| **"How does grasping contact & holding force work?"** | [`grasp/grasp_model.py`](file:///c:/Users/rudra/Documents/BTP/grasp/grasp_model.py) | **Parametric Sweep**: [`grasp/grasp_study.py`](file:///c:/Users/rudra/Documents/BTP/grasp/grasp_study.py)<br/>**Sequencing**: [`grasp/grasp_sequence.py`](file:///c:/Users/rudra/Documents/BTP/grasp/grasp_sequence.py)<br/>**Manuscript**: [`report/grasp_analysis.tex`](file:///c:/Users/rudra/Documents/BTP/report/grasp_analysis.tex) |
| **"Holding force vs friction or depth results"** | [`results/grasp/sweep.csv`](file:///c:/Users/rudra/Documents/BTP/results/grasp/sweep.csv)<br/>[`grasp/grasp_figures.py`](file:///c:/Users/rudra/Documents/BTP/grasp/grasp_figures.py) | **Visual Figures**: [`report/figures/`](file:///c:/Users/rudra/Documents/BTP/report/figures)<br/>**Manuscript Section**: [`report/grasp_analysis.tex`](file:///c:/Users/rudra/Documents/BTP/report/grasp_analysis.tex) |
| **"COMSOL cross-verification against Python FEA"** | [`comsol/bare/comsol_compare.py`](file:///c:/Users/rudra/Documents/BTP/comsol/bare/comsol_compare.py)<br/>[`comsol/sleeved/plot_sleeve_comparison.py`](file:///c:/Users/rudra/Documents/BTP/comsol/sleeved/plot_sleeve_comparison.py) | **COMSOL Solved Models**: [`comsol/bare/actuator_bare_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/bare/actuator_bare_solved.mph), [`comsol/sleeved/actuator_sleeve_solved.mph`](file:///c:/Users/rudra/Documents/BTP/comsol/sleeved/actuator_sleeve_solved.mph)<br/>**Python FEA Results**: [`results/bare/summary.csv`](file:///c:/Users/rudra/Documents/BTP/results/bare/summary.csv), [`results/sleeve/summary.csv`](file:///c:/Users/rudra/Documents/BTP/results/sleeve/summary.csv) |
| **"4-Cavity actuator with real pressure"** | [`comsol/tapered_4cavity_sleeved/BuildActuator4CavitySleeved.java`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity_sleeved/BuildActuator4CavitySleeved.java)<br/>[`comsol/tapered_4cavity_sleeved/run_sleeved_study.py`](file:///c:/Users/rudra/Documents/BTP/comsol/tapered_4cavity_sleeved/run_sleeved_study.py) | **Status and withdrawn results**: [`docs/tapered_4cavity_mechanics_report.md`](file:///c:/Users/rudra/Documents/BTP/docs/tapered_4cavity_mechanics_report.md), [`comsol/COMSOL_MASTER_REGISTRY.md`](file:///c:/Users/rudra/Documents/BTP/comsol/COMSOL_MASTER_REGISTRY.md) |
| **"ANSYS APDL verification"** | [`ansys/export_ansys.py`](file:///c:/Users/rudra/Documents/BTP/ansys/export_ansys.py) | **Generated Macros**: [`ansys/actuator_bare.mac`](file:///c:/Users/rudra/Documents/BTP/ansys/actuator_bare.mac), [`ansys/actuator_sleeve.mac`](file:///c:/Users/rudra/Documents/BTP/ansys/actuator_sleeve.mac), [`ansys/grasp_2d_verify.mac`](file:///c:/Users/rudra/Documents/BTP/ansys/grasp_2d_verify.mac) |
| **"Re-compiling the LaTeX paper or presentation slides"** | [`report/grasp_analysis.tex`](file:///c:/Users/rudra/Documents/BTP/report/grasp_analysis.tex)<br/>[`report/render_slides.py`](file:///c:/Users/rudra/Documents/BTP/report/render_slides.py) | **Compiled Output**: [`report/grasp_analysis.pdf`](file:///c:/Users/rudra/Documents/BTP/report/grasp_analysis.pdf)<br/>**Figures**: [`report/figures/`](file:///c:/Users/rudra/Documents/BTP/report/figures)<br/>**Slides**: [`report/slides/`](file:///c:/Users/rudra/Documents/BTP/report/slides) |
