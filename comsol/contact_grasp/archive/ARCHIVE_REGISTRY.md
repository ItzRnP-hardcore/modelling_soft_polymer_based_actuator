# Contact Grasp Study Archive Registry

This registry documents all intermediate diagnostic scripts developed during the formulation and numerical validation of the 3D contact grasp simulation in COMSOL Multiphysics 6.4.

---

## Itemized Script Inventory

| Script | Type | Role & Empirical Finding |
| :--- | :--- | :--- |
| `InspectContactPairs.java` | Geometry & Pairs Check | Discovered that `FormAssembly` with initial gap does not create contact pairs automatically (`Pair tags count: 0`), proving an explicit `Contact` pair must be created. |
| `TestContactFeature.java` | Physics API Probe | Proved that COMSOL 6.4 `SolidMechanics/Contact` uses `pairSelection = "list"` or `"contact"`/`"all"`, and identified penalty parameters (`pn_penalty`, `pt_penalty`). |
| `TestPairSelectionAPI.java` | API Validation | Validated that `pairSelection = "all"` and `pairSelection = "contact"` accept explicit contact pairs (`p1`) seamlessly without throwing selection errors. |
| `InspectGeometryBoundaries.java` | Geometry Analysis | Printed all 44 boundaries of the assembly geometry; determined centroid coordinates and areas for exact domain partitioning. |
| `PrintAllBoundaries.java` | Boundary Inspection | Verified boundary areas and detected that curved quarter-cone outer skin patches have centroid radius $r_m \approx 0.90 R(x)$ ($6.29\text{ mm}$ vs $7.06\text{ mm}$), enabling robust boundary identification. |
| `TestActuatorCylinderContactSingle.java` | Benchmark Solver | Successfully solved the single-radius benchmark ($R = 15\text{ mm}$) across all 9 continuation steps ($P = 0\text{ to }40\text{ kPa}$), demonstrating robust penalty convergence without singular Jacobian issues. |
| `InspectContactVariables.java` | Postprocessing Test | Tested evaluation of contact pressure (`solid.cnt1.Tn`) and gap (`solid.cnt1.gap`) on destination boundaries. |

---

## Production Master Script

The canonical production runner for this study is:
- [`comsol/contact_grasp/BuildActuatorCylinderContactSweep.java`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/BuildActuatorCylinderContactSweep.java)
- Plotting utility: [`comsol/contact_grasp/plot_contact_grasp_curves.py`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/plot_contact_grasp_curves.py)
- Summary dataset: [`comsol/contact_grasp/contact_grasp_multiradius_summary.csv`](file:///c:/Users/rudra/Documents/BTP/comsol/contact_grasp/contact_grasp_multiradius_summary.csv)
