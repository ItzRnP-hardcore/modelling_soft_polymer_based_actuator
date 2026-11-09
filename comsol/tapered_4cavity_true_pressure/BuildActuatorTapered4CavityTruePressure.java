import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/*
 * TRUE-PRESSURE version of tapered_4cavity/BuildActuatorTapered4CavityFull.java.
 *
 * Only change (problem 1 of the project audit): the cavity load.
 *   Original: inner wall + tip end face got  eta_drive*P*(0.20+1.80*(X/L)^1.5)  (eta_drive = 0.10),
 *             roof, side walls, base end face got alpha_balloon*P (0.005).
 *   Here:     every face of cavity k gets the same follower pressure P_k (a real gauge pressure).
 * Geometry, mesh, material (compressible neo-Hookean, E = 0.40 MPa, nu = 0.45), clamp and
 * post-processing are identical, so the comparison isolates the effect of the load.
 * Cavity is closed at the base by the 4 mm silicone plug (x_start = 4 mm), as in the original.
 */
public class BuildActuatorTapered4CavityTruePressure {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity_true_pressure";
        model.modelPath(baseDir);

        // 1. Geometric Parameters (2.5:1 Homothetic Taper with 4x 56-deg Cavities and 4x 34-deg Solid Radial Ribs)
        double L_act = 140.0;
        double R0 = 9.5;
        double Rt = 3.8;
        double tw_0 = 2.0;
        double tw_t = 0.8;
        double depth_0 = 2.5;
        double depth_t = 1.0;
        double x_start = 4.0;
        double x_end = 136.0;

        model.param().set("L_act", L_act + "[mm]", "Actuator total length");
        model.param().set("R0", R0 + "[mm]", "Base outer radius");
        model.param().set("Rt", Rt + "[mm]", "Tip outer radius");

        model.param().set("tw_0", tw_0 + "[mm]", "Outer wall thickness at base");
        model.param().set("tw_t", tw_t + "[mm]", "Outer wall thickness at tip");
        model.param().set("depth_0", depth_0 + "[mm]", "Cavity radial depth at base");
        model.param().set("depth_t", depth_t + "[mm]", "Cavity radial depth at tip");

        model.param().set("x_start", x_start + "[mm]", "Cavity start position");
        model.param().set("x_end", x_end + "[mm]", "Cavity end position");
        model.param().set("L_cav", "x_end - x_start", "Cavity active length (132 mm)");

        model.param().set("r_out0", "R0 - tw_0", "Cavity outer radius at base (7.5 mm)");
        model.param().set("r_outt", "Rt - tw_t", "Cavity outer radius at tip (3.0 mm)");
        model.param().set("r_in0", "r_out0 - depth_0", "Cavity inner radius at base (5.0 mm)");
        model.param().set("r_int", "r_outt - depth_t", "Cavity inner radius at tip (2.0 mm)");

        // 2. Hyperelastic Material Parameters (Pure Displacement-Based Polyconvex Neo-Hookean, nu=0.45)
        model.param().set("E_mod", "0.40[MPa]", "Silicone matrix initial Young's modulus");
        model.param().set("nu_val", "0.45", "Silicone effective Poisson ratio");
        model.param().set("mu_mod", "E_mod/(2*(1+nu_val))", "Initial shear modulus G");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))", "Bulk modulus K");

        // 3. Multiaxial Steering Pressures (Pitch, Yaw, and Diagonal)
        model.param().set("P_step", "0[kPa]", "Continuation gauge pressure");
        model.param().set("w1", "1", "Cavity 1 weight (Top +Y Pitch Down)");
        model.param().set("w2", "0", "Cavity 2 weight (Right +Z Yaw Left)");
        model.param().set("w3", "0", "Cavity 3 weight (Bottom -Y Pitch Up)");
        model.param().set("w4", "0", "Cavity 4 weight (Left -Z Yaw Right)");
        model.param().set("P1", "w1*P_step", "Cavity 1 pressure");
        model.param().set("P2", "w2*P_step", "Cavity 2 pressure");
        model.param().set("P3", "w3*P_step", "Cavity 3 pressure");
        model.param().set("P4", "w4*P_step", "Cavity 4 pressure");

        // 4. Geometry Build: Single Unified 3D Solid Body
        // Block has Y >= 0 ([-90, +90] deg). Rotating by +-rotOffset intersects to [-(90-rotOffset), +(90-rotOffset)].
        // Setting rotOffset = 62.0 deg gives exact [-28, +28] deg (56-deg arc cavities) separated by 4x 34-deg intact solid ribs!
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        model.component("comp1").geom("geom1").create("cone_body", "Cone");
        model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cone_body").set("pos", new double[]{0.0, 0.0, 0.0});
        model.component("comp1").geom("geom1").feature("cone_body").set("r", "R0");
        model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_body").set("rtop", "Rt");
        model.component("comp1").geom("geom1").feature("cone_body").set("h", "L_act");

        double rotOffset = 62.0; // Produces exact 2*(90 - 62) = 56 deg arc cavities and 34 deg solid ribs!
        double[] rotAngles = {0.0, -90.0, 180.0, 90.0};
        String[] cavTags = new String[4];

        for (int k = 1; k <= 4; k++) {
            double baseRot = rotAngles[k-1];
            String b1Tag = "blk1_" + k;
            String b2Tag = "blk2_" + k;
            String r1Tag = "rot1_" + k;
            String r2Tag = "rot2_" + k;
            String wTag = "wedge_" + k;
            String sTag = "shell_" + k;
            String cTag = "cav_" + k;
            cavTags[k-1] = cTag;

            model.component("comp1").geom("geom1").create("cone_out_" + k, "Cone");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("pos", new double[]{x_start, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("r", "r_out0");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("rtop", "r_outt");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("h", "L_cav");

            model.component("comp1").geom("geom1").create("cone_in_" + k, "Cone");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("pos", new double[]{x_start, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("r", "r_in0");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("rtop", "r_int");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("h", "L_cav");

            model.component("comp1").geom("geom1").create(sTag, "Difference");
            model.component("comp1").geom("geom1").feature(sTag).selection("input").set("cone_out_" + k);
            model.component("comp1").geom("geom1").feature(sTag).selection("input2").set("cone_in_" + k);

            model.component("comp1").geom("geom1").create(b1Tag, "Block");
            model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{x_start - 0.5, 0.0, -50.0});
            model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{134.0, 50.0, 100.0});

            model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + rotOffset);
            model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

            model.component("comp1").geom("geom1").create(b2Tag, "Block");
            model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{x_start - 0.5, 0.0, -50.0});
            model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{134.0, 50.0, 100.0});

            model.component("comp1").geom("geom1").create(r2Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r2Tag).set("rot", baseRot - rotOffset);
            model.component("comp1").geom("geom1").feature(r2Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r2Tag).selection("input").set(b2Tag);

            model.component("comp1").geom("geom1").create(wTag, "Intersection");
            model.component("comp1").geom("geom1").feature(wTag).selection("input").set(r1Tag, r2Tag);

            model.component("comp1").geom("geom1").create(cTag, "Intersection");
            model.component("comp1").geom("geom1").feature(cTag).selection("input").set(sTag, wTag);
        }

        model.component("comp1").geom("geom1").create("dif_actuator", "Difference");
        model.component("comp1").geom("geom1").feature("dif_actuator").selection("input").set("cone_body");
        model.component("comp1").geom("geom1").feature("dif_actuator").selection("input2").set(cavTags);

        model.component("comp1").geom("geom1").run();

        // 5. Mesh & Exact Sub-Boundary Decomposition
        int nDom = model.component("comp1").geom("geom1").getNDomains();
        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
        System.out.println("Single-Domain Tapered 4-Cavity Geometry: " + nDom + " domain, " + nBnd + " boundaries.");

        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(5);
        model.component("comp1").mesh("mesh1").run();

        model.result().dataset().create("dset_mesh", "Mesh");
        model.result().dataset("dset_mesh").set("mesh", "mesh1");

        List<Integer> list_base = new ArrayList<>();
        List<Integer> list_outer = new ArrayList<>();
        List<List<Integer>> list_drive = new ArrayList<>();
        List<List<Integer>> list_balloon = new ArrayList<>();
        for (int k = 0; k < 4; k++) {
            list_drive.add(new ArrayList<>());
            list_balloon.add(new ArrayList<>());
        }

        double[][] cavAxisYZ = {
            {1.0, 0.0},
            {0.0, 1.0},
            {-1.0, 0.0},
            {0.0, -1.0}
        };

        double r_out0_val = R0 - tw_0;
        double r_outt_val = Rt - tw_t;
        double r_in0_val = r_out0_val - depth_0;
        double r_int_val = r_outt_val - depth_t;
        double L_cav_val = x_end - x_start;

        for (int b = 1; b <= nBnd; b++) {
            model.result().numerical().create("int_b", "IntSurface");
            model.result().numerical("int_b").set("data", "dset_mesh");
            model.result().numerical("int_b").selection().set(new int[]{b});
            model.result().numerical("int_b").set("expr", new String[]{"X*1000", "Y*1000", "Z*1000", "1"});
            double[][] res = model.result().numerical("int_b").getReal();
            double area = res[3][0];
            double xm = res[0][0] / area;
            double ym = res[1][0] / area;
            double zm = res[2][0] / area;
            double rm = Math.sqrt(ym * ym + zm * zm);
            model.result().numerical().remove("int_b");

            if (xm < 0.1) {
                list_base.add(b);
                continue;
            }
            if (xm > L_act - 0.5) continue;

            if (b >= 2 && b <= 5) {
                list_outer.add(b);
                continue;
            }

            int kCav = -1;
            if (ym > Math.abs(zm)) kCav = 0;
            else if (zm > Math.abs(ym)) kCav = 1;
            else if (ym < -Math.abs(zm)) kCav = 2;
            else kCav = 3;

            if (Math.abs(xm - x_start) < 0.8) {
                list_balloon.get(kCav).add(b);
                continue;
            }
            if (Math.abs(xm - x_end) < 0.8) {
                list_drive.get(kCav).add(b);
                continue;
            }

            double cosDeltaPhi = (ym * cavAxisYZ[kCav][0] + zm * cavAxisYZ[kCav][1]) / rm;
            double xi = (xm - x_start) / L_cav_val;
            double r_out_expected = r_out0_val - (r_out0_val - r_outt_val) * xi;
            // Guard: a face whose centroid lies outside the cavity's outer radius is outer skin, not a
            // wetted cavity face. With full P on cavity faces a mislabelled skin patch would be loaded.
            if (rm > r_out_expected + 0.25) {
                System.out.println("GUARD: boundary " + b + " (x=" + xm + ", r=" + rm + ") treated as outer skin");
                list_outer.add(b);
                continue;
            }
            double r_in_expected = r_in0_val - (r_in0_val - r_int_val) * xi;
            double r_mid_expected = 0.5 * (r_out_expected + r_in_expected);

            if (cosDeltaPhi >= 0.92 && rm < r_mid_expected) {
                list_drive.get(kCav).add(b);
            } else {
                list_balloon.get(kCav).add(b);
            }
        }

        int[] arr_base = list_base.stream().mapToInt(i -> i).toArray();
        for (int k = 0; k < 4; k++) {
            System.out.println("Cavity " + (k+1) + " Drive (" + list_drive.get(k).size() + " bnds): " + list_drive.get(k));
            System.out.println("Cavity " + (k+1) + " Balloon (" + list_balloon.get(k).size() + " bnds): " + list_balloon.get(k));
        }

        // 6. Solid Mechanics Physics: Pure Displacement-Based Polyconvex Compressible Neo-Hookean (No LBB/pw saddle-point DOFs)
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").create("hmm1", "HyperelasticModel", 3);
        model.component("comp1").physics("solid").feature("hmm1").selection().all();
        model.component("comp1").physics("solid").feature("hmm1").set("MaterialModel", "NeoHookean");
        model.component("comp1").physics("solid").feature("hmm1").set("Compressibility_NeoHookean", "Compressible");
        model.component("comp1").physics("solid").feature("hmm1").set("IsotropicOption", "KG");
        model.component("comp1").physics("solid").feature("hmm1").set("G_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("G", "mu_mod");
        model.component("comp1").physics("solid").feature("hmm1").set("K_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("K", "kappa_mod");

        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().set(arr_base);

        // TRUE PRESSURE: one follower-pressure load per cavity, the same P_k on every wetted face
        // (inner wall, roof, both side walls, base end face, tip end face).
        for (int k = 1; k <= 4; k++) {
            List<Integer> wetted = new ArrayList<>(list_drive.get(k-1));
            wetted.addAll(list_balloon.get(k-1));
            int[] arr_cav = wetted.stream().mapToInt(i -> i).toArray();
            System.out.println("Cavity " + k + " wetted faces (" + arr_cav.length + "): " + wetted);

            String bTag = "bndl_cav" + k;
            model.component("comp1").physics("solid").create(bTag, "BoundaryLoad", 2);
            model.component("comp1").physics("solid").feature(bTag).selection().set(arr_cav);
            model.component("comp1").physics("solid").feature(bTag).set("forceType", "FollowerPressure");
            model.component("comp1").physics("solid").feature(bTag).set("pressure", "P" + k);
        }

        // 7. Study: fine ramp at low pressure, because a real-pressure bare actuator may balloon early.
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
        model.study("std1").feature("stat").set("plistarr", new String[]{"0 1 2 3 4 5 6 7 8 9 10 12 14 16 18 20 22.5 25 27.5 30 32.5 35 37.5 40"});
        model.study("std1").feature("stat").set("punit", new String[]{"kPa"});
        model.study("std1").showAutoSequences("all");

        // 8. Tip Orientation CutPoints on Distal Face X = 140 mm (Pitch +Y/-Y and Yaw +Z/-Z)
        double rp = 1.8;
        model.result().dataset().create("cpt_tip", "CutPoint3D");
        model.result().dataset("cpt_tip").set("data", "dset1");
        model.result().dataset("cpt_tip").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_tip").set("pointy", "0");
        model.result().dataset("cpt_tip").set("pointz", "0");

        model.result().dataset().create("cpt_py", "CutPoint3D");
        model.result().dataset("cpt_py").set("data", "dset1");
        model.result().dataset("cpt_py").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_py").set("pointy", String.valueOf(rp));
        model.result().dataset("cpt_py").set("pointz", "0");

        model.result().dataset().create("cpt_ny", "CutPoint3D");
        model.result().dataset("cpt_ny").set("data", "dset1");
        model.result().dataset("cpt_ny").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_ny").set("pointy", String.valueOf(-rp));
        model.result().dataset("cpt_ny").set("pointz", "0");

        model.result().dataset().create("cpt_pz", "CutPoint3D");
        model.result().dataset("cpt_pz").set("data", "dset1");
        model.result().dataset("cpt_pz").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_pz").set("pointy", "0");
        model.result().dataset("cpt_pz").set("pointz", String.valueOf(rp));

        model.result().dataset().create("cpt_nz", "CutPoint3D");
        model.result().dataset("cpt_nz").set("data", "dset1");
        model.result().dataset("cpt_nz").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_nz").set("pointy", "0");
        model.result().dataset("cpt_nz").set("pointz", String.valueOf(-rp));

        model.result().dataset().create("cpl_mid", "CutPlane");
        model.result().dataset("cpl_mid").set("data", "dset1");
        model.result().dataset("cpl_mid").set("planetype", "quick");
        model.result().dataset("cpl_mid").set("quickplane", "yz");
        model.result().dataset("cpl_mid").set("quickx", "70[mm]");

        // Ballooning gauge: deformed distance between the two outer-skin points at +Y (cavity-1 roof)
        // and -Y (opposite side) of a section. Rotation-invariant, so it measures diameter change,
        // not bending. Sections at X = 70 mm (mid) and X = 120 mm (thin distal wall).
        double[] xBal = {70.0, 120.0};
        for (int i = 0; i < xBal.length; i++) {
            double Rx = R0 - (R0 - Rt) * xBal[i] / L_act;
            String[] sgn = {"top", "bot"};
            for (int j = 0; j < 2; j++) {
                String dTag = "cpt_bal" + i + "_" + sgn[j];
                model.result().dataset().create(dTag, "CutPoint3D");
                model.result().dataset(dTag).set("data", "dset1");
                model.result().dataset(dTag).set("pointx", String.valueOf(xBal[i]));
                model.result().dataset(dTag).set("pointy", String.valueOf((j == 0 ? 1 : -1) * (Rx - 0.01)));
                model.result().dataset(dTag).set("pointz", "0");
                String nTag = "pev_bal" + i + "_" + sgn[j];
                model.result().numerical().create(nTag, "EvalPoint");
                model.result().numerical(nTag).set("data", dTag);
                model.result().numerical(nTag).set("expr", new String[]{"u", "v", "w"});
                model.result().numerical(nTag).set("unit", new String[]{"mm", "mm", "mm"});
            }
        }

        model.result().numerical().create("pev_tip", "EvalPoint");
        model.result().numerical("pev_tip").set("data", "cpt_tip");
        model.result().numerical("pev_tip").set("expr", new String[]{"P_step", "u", "v", "w", "solid.disp"});
        model.result().numerical("pev_tip").set("unit", new String[]{"kPa", "mm", "mm", "mm", "mm"});

        model.result().numerical().create("pev_py", "EvalPoint");
        model.result().numerical("pev_py").set("data", "cpt_py");
        model.result().numerical("pev_py").set("expr", new String[]{"u", "v", "w"});
        model.result().numerical("pev_py").set("unit", new String[]{"mm", "mm", "mm"});

        model.result().numerical().create("pev_ny", "EvalPoint");
        model.result().numerical("pev_ny").set("data", "cpt_ny");
        model.result().numerical("pev_ny").set("expr", new String[]{"u", "v", "w"});
        model.result().numerical("pev_ny").set("unit", new String[]{"mm", "mm", "mm"});

        model.result().numerical().create("pev_pz", "EvalPoint");
        model.result().numerical("pev_pz").set("data", "cpt_pz");
        model.result().numerical("pev_pz").set("expr", new String[]{"u", "v", "w"});
        model.result().numerical("pev_pz").set("unit", new String[]{"mm", "mm", "mm"});

        model.result().numerical().create("pev_nz", "EvalPoint");
        model.result().numerical("pev_nz").set("data", "cpt_nz");
        model.result().numerical("pev_nz").set("expr", new String[]{"u", "v", "w"});
        model.result().numerical("pev_nz").set("unit", new String[]{"mm", "mm", "mm"});

        model.result().numerical().create("max_vm", "MaxVolume");
        model.result().numerical("max_vm").set("data", "dset1");
        model.result().numerical("max_vm").selection().all();
        model.result().numerical("max_vm").set("expr", new String[]{"solid.mises"});
        model.result().numerical("max_vm").set("unit", new String[]{"kPa"});

        model.result().numerical().create("max_sp1", "MaxVolume");
        model.result().numerical("max_sp1").set("data", "dset1");
        model.result().numerical("max_sp1").selection().all();
        model.result().numerical("max_sp1").set("expr", new String[]{"solid.sp1"});
        model.result().numerical("max_sp1").set("unit", new String[]{"kPa"});

        model.result().numerical().create("min_J", "MinVolume");
        model.result().numerical("min_J").set("data", "dset1");
        model.result().numerical("min_J").selection().all();
        model.result().numerical("min_J").set("expr", new String[]{"solid.J"});

        // 9. Visualization Plot Groups
        model.result().create("pg_disp", "PlotGroup3D");
        model.result("pg_disp").set("data", "dset1");
        model.result("pg_disp").label("3D Total Displacement (mm) - 180 deg Bend");
        model.result("pg_disp").create("surf_d", "Surface");
        model.result("pg_disp").feature("surf_d").set("expr", "solid.disp");
        model.result("pg_disp").feature("surf_d").set("unit", "mm");
        model.result("pg_disp").feature("surf_d").create("def", "Deform");
        model.result("pg_disp").feature("surf_d").feature("def").set("expr", new String[]{"u", "v", "w"});
        model.result("pg_disp").feature("surf_d").feature("def").set("scaleactive", true);
        model.result("pg_disp").feature("surf_d").feature("def").set("scale", 1.0);

        model.result().create("pg_stress", "PlotGroup3D");
        model.result("pg_stress").set("data", "dset1");
        model.result("pg_stress").label("3D von Mises Stress (kPa) - 180 deg Bend");
        model.result("pg_stress").create("surf_s", "Surface");
        model.result("pg_stress").feature("surf_s").set("expr", "solid.mises");
        model.result("pg_stress").feature("surf_s").set("unit", "kPa");
        model.result("pg_stress").feature("surf_s").set("colortable", "ThermalLight");
        model.result("pg_stress").feature("surf_s").create("def", "Deform");
        model.result("pg_stress").feature("surf_s").feature("def").set("expr", new String[]{"u", "v", "w"});
        model.result("pg_stress").feature("surf_s").feature("def").set("scaleactive", true);
        model.result("pg_stress").feature("surf_s").feature("def").set("scale", 1.0);

        model.result().create("pg_sec2d", "PlotGroup2D");
        model.result("pg_sec2d").set("data", "cpl_mid");
        model.result("pg_sec2d").label("2D Mid-Cross-Section X=70mm Stress & Ballooning (kPa)");
        model.result("pg_sec2d").create("surf_sec", "Surface");
        model.result("pg_sec2d").feature("surf_sec").set("expr", "solid.mises");
        model.result("pg_sec2d").feature("surf_sec").set("unit", "kPa");
        model.result("pg_sec2d").feature("surf_sec").set("colortable", "ThermalLight");
        model.result("pg_sec2d").feature("surf_sec").create("def", "Deform");
        model.result("pg_sec2d").feature("surf_sec").feature("def").set("expr", new String[]{"v", "w"});
        model.result("pg_sec2d").feature("surf_sec").feature("def").set("scaleactive", true);
        model.result("pg_sec2d").feature("surf_sec").feature("def").set("scale", 1.0);

        model.result().export().create("img_disp", "Image3D");
        model.result().export("img_disp").set("sourceobject", "pg_disp");
        model.result().export("img_disp").set("size", "manualweb");
        model.result().export("img_disp").set("unit", "px");
        model.result().export("img_disp").set("width", 1400);
        model.result().export("img_disp").set("height", 900);

        model.result().export().create("img_stress", "Image3D");
        model.result().export("img_stress").set("sourceobject", "pg_stress");
        model.result().export("img_stress").set("size", "manualweb");
        model.result().export("img_stress").set("unit", "px");
        model.result().export("img_stress").set("width", 1400);
        model.result().export("img_stress").set("height", 900);

        model.result().export().create("img_sec2d", "Image2D");
        model.result().export("img_sec2d").set("sourceobject", "pg_sec2d");
        model.result().export("img_sec2d").set("size", "manualweb");
        model.result().export("img_sec2d").set("unit", "px");
        model.result().export("img_sec2d").set("width", 1100);
        model.result().export("img_sec2d").set("height", 900);

        // 10. Pitch Down only (cavity 1). Yaw and diagonal are the same structure rotated 90/45 deg.
        String[] modeNames = {"pitch_down"};
        double[][] modeWeights = {
            {1.0, 0.0, 0.0, 0.0}
        };

        StringBuilder csvOut = new StringBuilder();
        csvOut.append("mode,w1,w2,w3,w4,P_step_kPa,u_tip_mm,v_tip_mm,w_tip_mm,disp_tip_mm,delta_trans_mm,theta_tip_deg,phi_plane_deg,vm_max_kPa,sp1_max_kPa,min_J,D_ratio_x70,D_ratio_x120\n");

        for (int m = 0; m < modeNames.length; m++) {
            String mName = modeNames[m];
            double mw1 = modeWeights[m][0];
            double mw2 = modeWeights[m][1];
            double mw3 = modeWeights[m][2];
            double mw4 = modeWeights[m][3];
            model.param().set("w1", String.valueOf(mw1));
            model.param().set("w2", String.valueOf(mw2));
            model.param().set("w3", String.valueOf(mw3));
            model.param().set("w4", String.valueOf(mw4));

            System.out.println(">>> Solving Multiaxial Mode: " + mName + " [w1=" + mw1 + ", w2=" + mw2 + "] <<<");
            try {
                model.study("std1").run();
            } catch (Exception eRun) {
                System.out.println("SOLVER_STOPPED: " + eRun.getMessage());
            }
            double[] solvedP = model.sol("sol1").getPVals();
            System.out.println("CONVERGED_STEPS: " + (solvedP == null ? 0 : solvedP.length)
                + (solvedP == null || solvedP.length == 0 ? "" : ", last P = " + solvedP[solvedP.length - 1] / 1000.0 + " kPa"));
            if (solvedP == null || solvedP.length == 0) continue;

            double[][][] bal = new double[4][][];
            String[] balTags = {"pev_bal0_top", "pev_bal0_bot", "pev_bal1_top", "pev_bal1_bot"};
            for (int q = 0; q < 4; q++) bal[q] = model.result().numerical(balTags[q]).getReal();

            double[][] uvw = model.result().numerical("pev_tip").getReal();
            double[][] uvw_py = model.result().numerical("pev_py").getReal();
            double[][] uvw_ny = model.result().numerical("pev_ny").getReal();
            double[][] uvw_pz = model.result().numerical("pev_pz").getReal();
            double[][] uvw_nz = model.result().numerical("pev_nz").getReal();
            double[][] vm_arr = model.result().numerical("max_vm").getReal();
            double[][] sp1_arr = model.result().numerical("max_sp1").getReal();
            double[][] minJ_arr = model.result().numerical("min_J").getReal();

            double wNorm = Math.sqrt(mw1 * mw1 + mw2 * mw2);
            double dy_dir = mw1 / wNorm;
            double dz_dir = mw2 / wNorm;

            int nSteps = uvw[0].length;
            int stepLast = nSteps; // last converged step (1-based solnum)
            double[] Dratio = new double[2];

            for (int s = 0; s < nSteps; s++) {
                double p = uvw[0][s];
                double u = uvw[1][s];
                double v = uvw[2][s];
                double w = uvw[3][s];
                double disp = uvw[4][s];
                double delta = Math.sqrt(v * v + w * w);

                // Deformed material directors on distal face X = L_act
                double ey_x = (uvw_py[0][s] - uvw_ny[0][s]) / (2.0 * rp);
                double ey_y = (2.0 * rp + uvw_py[1][s] - uvw_ny[1][s]) / (2.0 * rp);
                double ey_z = (uvw_py[2][s] - uvw_ny[2][s]) / (2.0 * rp);

                double ez_x = (uvw_pz[0][s] - uvw_nz[0][s]) / (2.0 * rp);
                double ez_y = (uvw_pz[1][s] - uvw_nz[1][s]) / (2.0 * rp);
                double ez_z = (2.0 * rp + uvw_pz[2][s] - uvw_nz[2][s]) / (2.0 * rp);

                // Director along active steering plane (dy_dir, dz_dir)
                double ed_x = dy_dir * ey_x + dz_dir * ez_x;
                double ed_y = dy_dir * ey_y + dz_dir * ez_y;
                double ed_z = dy_dir * ey_z + dz_dir * ez_z;
                double ed_perp = dy_dir * ed_y + dz_dir * ed_z;

                double theta_deg = Math.atan2(ed_x, ed_perp) * 180.0 / Math.PI;
                if (theta_deg < -10.0) theta_deg += 360.0;

                double phi_deg = (delta > 0.05) ? (Math.atan2(-w, -v) * 180.0 / Math.PI) : 0.0;
                double vm = vm_arr[0][s];
                double sp1 = sp1_arr[0][s];
                double jmin = minJ_arr[0][s];

                for (int i = 0; i < 2; i++) {
                    double xb = (i == 0) ? 70.0 : 120.0;
                    double Rx = R0 - (R0 - Rt) * xb / L_act - 0.01;
                    double[][] top = bal[2 * i], bot = bal[2 * i + 1];
                    double dx = top[0][s] - bot[0][s];
                    double dy = 2.0 * Rx + top[1][s] - bot[1][s];
                    double dz = top[2][s] - bot[2][s];
                    Dratio[i] = Math.sqrt(dx * dx + dy * dy + dz * dz) / (2.0 * Rx);
                }

                String row = String.format(Locale.US, "%s,%.4f,%.4f,%.4f,%.4f,%.2f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.2f,%.2f,%.2f,%.4f,%.4f,%.4f",
                    mName, mw1, mw2, mw3, mw4, p, u, v, w, disp, delta, theta_deg, phi_deg, vm, sp1, jmin, Dratio[0], Dratio[1]);
                System.out.println("DOE_ROW: " + row);
                csvOut.append(row).append("\n");
            }

            // Export 3D & 2D plots at the last converged step (true scale, no exaggeration)
            try {
                model.result("pg_disp").set("solnum", String.valueOf(stepLast));
                model.result("pg_disp").label("3D Total Displacement (mm) - TRUE cavity pressure");
                model.result("pg_disp").run();
                model.result().export("img_disp").set("filename", baseDir + "\\images\\truepressure_" + mName + "_disp_last.png");
                model.result().export("img_disp").run();

                model.result("pg_stress").set("solnum", String.valueOf(stepLast));
                model.result("pg_stress").label("3D von Mises Stress (kPa) - TRUE cavity pressure");
                model.result("pg_stress").run();
                model.result().export("img_stress").set("filename", baseDir + "\\images\\truepressure_" + mName + "_stress_last.png");
                model.result().export("img_stress").run();

                model.result("pg_sec2d").set("solnum", String.valueOf(stepLast));
                model.result("pg_sec2d").run();
                model.result().export("img_sec2d").set("filename", baseDir + "\\images\\truepressure_" + mName + "_sec2d_last.png");
                model.result().export("img_sec2d").run();
            } catch (Exception eImg) {
                System.out.println("IMAGE_EXPORT_FAILED: " + eImg.getMessage());
            }
        }

        // CSV is written by run_truepressure.py from the DOE_ROW lines (COMSOL's security
        // preference blocks Java file writes in batch mode).
        try {
            model.save(baseDir + "\\actuator_tapered_4cavity_truepressure_solved.mph");
        } catch (Exception eSave) {
            System.out.println("MPH_SAVE_FAILED: " + eSave.getMessage());
        }

        return model;
    }

    public static void main(String[] args) {
        run();
    }
}
