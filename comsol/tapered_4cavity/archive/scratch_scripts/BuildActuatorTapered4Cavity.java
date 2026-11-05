import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class BuildActuatorTapered4Cavity {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity";
        model.modelPath(baseDir);

        // 1. Parameters
        model.param().set("L_act", "100[mm]", "Actuator total length");
        model.param().set("R0", "10[mm]", "Base outer radius");
        model.param().set("Rt", "7[mm]", "Tip outer radius");
        model.param().set("tw_out", "1.3[mm]", "Outer wall thickness");
        model.param().set("depth_cav", "3.0[mm]", "Cavity radial depth");
        model.param().set("x_start", "4[mm]", "Cavity start position");
        model.param().set("x_end", "96[mm]", "Cavity end position");
        model.param().set("L_cav", "x_end - x_start", "Cavity length");

        // Cavity radii at base and tip
        model.param().set("r_out0", "R0 - tw_out", "Cavity outer radius at base");
        model.param().set("r_outt", "Rt - tw_out", "Cavity outer radius at tip");
        model.param().set("r_in0", "r_out0 - depth_cav", "Cavity inner radius at base");
        model.param().set("r_int", "r_outt - depth_cav", "Cavity inner radius at tip");

        // Material Parameters (Silicone elastomer, Neo-Hookean)
        model.param().set("E_mod", "0.6[MPa]", "Young's modulus");
        model.param().set("nu_val", "0.4995", "Poisson ratio");
        model.param().set("mu_mod", "E_mod/(2*(1+nu_val))", "Shear modulus");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))", "Bulk modulus");
        model.param().set("ks_sleeve", "100*E_mod*R0", "Sleeve hoop stiffness");

        // Actuation Pressures
        model.param().set("P_val", "40[kPa]", "Operating base pressure");
        model.param().set("w1", "0", "Cavity 1 weight (Top +Y)");
        model.param().set("w2", "0", "Cavity 2 weight (Right +Z)");
        model.param().set("w3", "0", "Cavity 3 weight (Bottom -Y)");
        model.param().set("w4", "0", "Cavity 4 weight (Left -Z)");
        model.param().set("P1", "w1*P_val", "Cavity 1 pressure");
        model.param().set("P2", "w2*P_val", "Cavity 2 pressure");
        model.param().set("P3", "w3*P_val", "Cavity 3 pressure");
        model.param().set("P4", "w4*P_val", "Cavity 4 pressure");

        // 2. Geometry: Full 3D Actuator
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        // Outer cone body: R0=10 mm -> Rt=7 mm along X in [0, 100] mm
        model.component("comp1").geom("geom1").create("cone_body", "Cone");
        model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cone_body").set("pos", new double[]{0.0, 0.0, 0.0});
        model.component("comp1").geom("geom1").feature("cone_body").set("r", "R0");
        model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_body").set("rtop", "Rt");
        model.component("comp1").geom("geom1").feature("cone_body").set("h", "L_act");

        // 4 Radial Trapezoid Cavities: arc angle = 80 deg, radial depth = 3 mm
        // Spaced at 90 deg around X axis: 0 deg (+Y), -90 deg (+Z), 180 deg (-Y), +90 deg (-Z)
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
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("pos", new double[]{4.0, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("r", "r_out0");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("rtop", "r_outt");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("h", "L_cav");

            model.component("comp1").geom("geom1").create("cone_in_" + k, "Cone");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("pos", new double[]{4.0, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("r", "r_in0");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("rtop", "r_int");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("h", "L_cav");

            model.component("comp1").geom("geom1").create(sTag, "Difference");
            model.component("comp1").geom("geom1").feature(sTag).selection("input").set("cone_out_" + k);
            model.component("comp1").geom("geom1").feature(sTag).selection("input2").set("cone_in_" + k);

            // 80 deg wedge via two rotated half-blocks (+40 deg and -40 deg from center)
            model.component("comp1").geom("geom1").create(b1Tag, "Block");
            model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{3.5, 0.0, -15.0});
            model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{93.0, 15.0, 30.0});

            model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + 40.0);
            model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

            model.component("comp1").geom("geom1").create(b2Tag, "Block");
            model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{3.5, 0.0, -15.0});
            model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{93.0, 15.0, 30.0});

            model.component("comp1").geom("geom1").create(r2Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r2Tag).set("rot", baseRot - 40.0);
            model.component("comp1").geom("geom1").feature(r2Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r2Tag).selection("input").set(b2Tag);

            model.component("comp1").geom("geom1").create(wTag, "Intersection");
            model.component("comp1").geom("geom1").feature(wTag).selection("input").set(r1Tag, r2Tag);

            // Intersect wedge and shell -> cavity k
            model.component("comp1").geom("geom1").create(cTag, "Intersection");
            model.component("comp1").geom("geom1").feature(cTag).selection("input").set(sTag, wTag);
        }

        // Subtract all 4 cavities from outer cone body
        model.component("comp1").geom("geom1").create("dif_actuator", "Difference");
        model.component("comp1").geom("geom1").feature("dif_actuator").selection("input").set("cone_body");
        model.component("comp1").geom("geom1").feature("dif_actuator").selection("input2").set(cavTags);

        model.component("comp1").geom("geom1").run();

        // 3. Automated Geometric Boundary Selections
        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();

        // Base selection at x = 0
        model.component("comp1").selection().create("sel_base", "Box");
        model.component("comp1").selection("sel_base").set("entitydim", 2);
        model.component("comp1").selection("sel_base").set("condition", "inside");
        model.component("comp1").selection("sel_base").set("xmin", "-0.1");
        model.component("comp1").selection("sel_base").set("xmax", "0.1");
        model.component("comp1").selection("sel_base").set("ymin", "-15");
        model.component("comp1").selection("sel_base").set("ymax", "15");
        model.component("comp1").selection("sel_base").set("zmin", "-15");
        model.component("comp1").selection("sel_base").set("zmax", "15");

        // Outer surface of actuator cone
        model.component("comp1").selection().create("sel_outer_box", "Cylinder");
        model.component("comp1").selection("sel_outer_box").set("entitydim", 2);
        model.component("comp1").selection("sel_outer_box").set("condition", "allvertices");
        model.component("comp1").selection("sel_outer_box").set("axistype", "x");
        model.component("comp1").selection("sel_outer_box").set("r", "10.1");
        model.component("comp1").selection("sel_outer_box").set("rin", "6.8");
        model.component("comp1").selection("sel_outer_box").set("top", "100.1");
        model.component("comp1").selection("sel_outer_box").set("bottom", "-0.1");

        model.component("comp1").selection().create("sel_outer", "Difference");
        model.component("comp1").selection("sel_outer").set("entitydim", 2);
        model.component("comp1").selection("sel_outer").set("add", new String[]{"sel_outer_box"});
        model.component("comp1").selection("sel_outer").set("subtract", new String[]{"sel_base"});

        // Mesh for automated boundary classifier
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(5); // fine-medium mesh
        model.component("comp1").mesh("mesh1").run();

        model.result().dataset().create("dset_mesh", "Mesh");
        model.result().dataset("dset_mesh").set("mesh", "mesh1");

        int[] b_base = model.component("comp1").selection("sel_base").entities(2);
        int[] b_outer = model.component("comp1").selection("sel_outer").entities(2);
        Set<Integer> extBnds = new HashSet<>();
        for (int b : b_base) extBnds.add(b);
        for (int b : b_outer) extBnds.add(b);

        List<Integer> list_c1 = new ArrayList<>();
        List<Integer> list_c2 = new ArrayList<>();
        List<Integer> list_c3 = new ArrayList<>();
        List<Integer> list_c4 = new ArrayList<>();

        for (int b = 1; b <= nBnd; b++) {
            if (extBnds.contains(b)) continue; // skip base and outer boundaries

            model.result().numerical().create("int_b", "IntSurface");
            model.result().numerical("int_b").set("data", "dset_mesh");
            model.result().numerical("int_b").selection().set(new int[]{b});
            model.result().numerical("int_b").set("expr", new String[]{"X*1000", "Y*1000", "Z*1000", "1"});
            double[][] res = model.result().numerical("int_b").getReal();
            double area = res[3][0];
            double xm = res[0][0] / area; // in mm
            double ym = res[1][0] / area; // in mm
            double zm = res[2][0] / area; // in mm
            model.result().numerical().remove("int_b");

            if (xm > 99.0) continue; // skip tip end cap face if any

            // Internal cavity boundary: classify by angular sector
            if (ym > Math.abs(zm)) {
                list_c1.add(b);
            } else if (zm > Math.abs(ym)) {
                list_c2.add(b);
            } else if (ym < -Math.abs(zm)) {
                list_c3.add(b);
            } else if (zm < -Math.abs(ym)) {
                list_c4.add(b);
            }
        }

        int[] arr_c1 = list_c1.stream().mapToInt(i -> i).toArray();
        int[] arr_c2 = list_c2.stream().mapToInt(i -> i).toArray();
        int[] arr_c3 = list_c3.stream().mapToInt(i -> i).toArray();
        int[] arr_c4 = list_c4.stream().mapToInt(i -> i).toArray();

        System.out.println("Automated Selections Confirmed:");
        System.out.println("  Cavity 1 (+Y Top): " + arr_c1.length + " boundaries: " + Arrays.toString(arr_c1));
        System.out.println("  Cavity 2 (+Z Right): " + arr_c2.length + " boundaries: " + Arrays.toString(arr_c2));
        System.out.println("  Cavity 3 (-Y Bottom): " + arr_c3.length + " boundaries: " + Arrays.toString(arr_c3));
        System.out.println("  Cavity 4 (-Z Left): " + arr_c4.length + " boundaries: " + Arrays.toString(arr_c4));

        // 4. Solid Mechanics Physics
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").create("hmm1", "HyperelasticModel", 3);
        model.component("comp1").physics("solid").feature("hmm1").selection().all();
        model.component("comp1").physics("solid").feature("hmm1").set("MaterialModel", "NeoHookean");
        model.component("comp1").physics("solid").feature("hmm1").set("Compressibility_NeoHookean", "NearlyIncompressible");
        model.component("comp1").physics("solid").feature("hmm1").set("VolumetricEnergyUncoupled", "quadratic");
        model.component("comp1").physics("solid").feature("hmm1").set("MixedFormulation", "pFormulation");
        model.component("comp1").physics("solid").feature("hmm1").set("IsotropicOption", "KG");
        model.component("comp1").physics("solid").feature("hmm1").set("G_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("G", "mu_mod");
        model.component("comp1").physics("solid").feature("hmm1").set("K_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("K", "kappa_mod");

        // Clamped Base Boundary Condition
        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().named("sel_base");

        // Follower Pressures for the 4 Radial Cavities (Direct selection assignment)
        int[][] allCavArrays = {arr_c1, arr_c2, arr_c3, arr_c4};
        for (int k = 1; k <= 4; k++) {
            String bndTag = "bndl" + k;
            model.component("comp1").physics("solid").create(bndTag, "BoundaryLoad", 2);
            model.component("comp1").physics("solid").feature(bndTag).selection().set(allCavArrays[k-1]);
            model.component("comp1").physics("solid").feature(bndTag).set("forceType", "FollowerPressure");
            model.component("comp1").physics("solid").feature(bndTag).set("pressure", "P" + k);
        }

        // Exact Green-Lagrange hoop-stretch sleeve weak contribution on outer surface
        model.component("comp1").variable().create("var_sleeve");
        model.component("comp1").variable("var_sleeve").selection().geom("geom1", 2);
        model.component("comp1").variable("var_sleeve").selection().named("sel_outer");
        model.component("comp1").variable("var_sleeve").set("Rr", "sqrt(Y^2+Z^2)");
        model.component("comp1").variable("var_sleeve").set("dut", "(-Z*uTY + Y*uTZ)/Rr");
        model.component("comp1").variable("var_sleeve").set("dvt", "(-Z*vTY + Y*vTZ)/Rr");
        model.component("comp1").variable("var_sleeve").set("dwt", "(-Z*wTY + Y*wTZ)/Rr");
        model.component("comp1").variable("var_sleeve").set("eh", "(-Z*dvt + Y*dwt)/Rr + 0.5*(dut^2 + dvt^2 + dwt^2)");

        model.component("comp1").physics("solid").create("wf_sleeve", "WeakContribution", 2);
        model.component("comp1").physics("solid").feature("wf_sleeve").selection().named("sel_outer");
        model.component("comp1").physics("solid").feature("wf_sleeve").set("weakExpression",
            "-ks_sleeve*eh*test(eh)");

        // 5. Study Setup (Geometric Nonlinearity)
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);

        // Actuation Test Matrix
        String[] caseNames = {
            "cavity_1_top_pitch_down",       // [1, 0, 0, 0] -> -Y bending
            "cavity_2_right_yaw_left",        // [0, 1, 0, 0] -> -Z bending
            "cavity_3_bottom_pitch_up",      // [0, 0, 1, 0] -> +Y bending
            "cavity_4_left_yaw_right",       // [0, 0, 0, 1] -> +Z bending
            "cavities_1_2_diagonal_45deg",   // [1, 1, 0, 0] -> -Y, -Z bending
            "cavities_1_4_diagonal_135deg",  // [1, 0, 0, 1] -> -Y, +Z bending
            "all_4_cavities_axial_stiffening"// [1, 1, 1, 1] -> pure extension
        };
        int[][] caseWeights = {
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1},
            {1, 1, 0, 0},
            {1, 0, 0, 1},
            {1, 1, 1, 1}
        };

        StringBuilder csvSummary = new StringBuilder();
        csvSummary.append("case_name,w1,w2,w3,w4,P_kPa,u_tip_mm,v_tip_mm,w_tip_mm,delta_3d_mm,phi_bend_deg,theta_deg\n");

        System.out.println("=== COMSOL_TAPERED_4CAVITY_START ===");
        System.out.println("case_name,w1,w2,w3,w4,P_kPa,u_tip_mm,v_tip_mm,w_tip_mm,delta_3d_mm,phi_bend_deg,theta_deg");

        for (int c = 0; c < caseNames.length; c++) {
            String cname = caseNames[c];
            int[] w = caseWeights[c];
            model.param().set("w1", String.valueOf(w[0]));
            model.param().set("w2", String.valueOf(w[1]));
            model.param().set("w3", String.valueOf(w[2]));
            model.param().set("w4", String.valueOf(w[3]));
            model.param().set("P_val", "40[kPa]");

            model.study("std1").run();

            if (c == 0) {
                // Tip Center CutPoint at (X=100, Y=0, Z=0)
                model.result().dataset().create("cpt_tip", "CutPoint3D");
                model.result().dataset("cpt_tip").set("data", "dset1");
                model.result().dataset("cpt_tip").set("pointx", "100");
                model.result().dataset("cpt_tip").set("pointy", "0");
                model.result().dataset("cpt_tip").set("pointz", "0");

                model.result().numerical().create("pev_tip", "EvalPoint");
                model.result().numerical("pev_tip").set("data", "cpt_tip");
                model.result().numerical("pev_tip").set("expr", new String[]{"u", "v", "w"});
                model.result().numerical("pev_tip").set("unit", new String[]{"mm", "mm", "mm"});

                // 3D Plot Groups (with 1:1 true physical scale)
                model.result().create("pg_disp", "PlotGroup3D");
                model.result("pg_disp").set("data", "dset1");
                model.result("pg_disp").label("Total Displacement (solid.disp)");
                model.result("pg_disp").create("surf_d", "Surface");
                model.result("pg_disp").feature("surf_d").set("expr", "solid.disp");
                model.result("pg_disp").feature("surf_d").set("unit", "mm");
                model.result("pg_disp").feature("surf_d").create("def", "Deform");
                model.result("pg_disp").feature("surf_d").feature("def").set("expr", new String[]{"u", "v", "w"});
                model.result("pg_disp").feature("surf_d").feature("def").set("scaleactive", true);
                model.result("pg_disp").feature("surf_d").feature("def").set("scale", 1);

                model.result().create("pg_stress", "PlotGroup3D");
                model.result("pg_stress").set("data", "dset1");
                model.result("pg_stress").label("Stress (solid.mises)");
                model.result("pg_stress").create("surf_s", "Surface");
                model.result("pg_stress").feature("surf_s").set("expr", "solid.mises");
                model.result("pg_stress").feature("surf_s").set("unit", "MPa");
                model.result("pg_stress").feature("surf_s").create("def", "Deform");
                model.result("pg_stress").feature("surf_s").feature("def").set("expr", new String[]{"u", "v", "w"});
                model.result("pg_stress").feature("surf_s").feature("def").set("scaleactive", true);
                model.result("pg_stress").feature("surf_s").feature("def").set("scale", 1);
            }

            double[][] uvw = model.result().numerical("pev_tip").getReal();
            int lastSol = uvw[0].length - 1;
            double u_t = uvw[0][lastSol];
            double v_t = uvw[1][lastSol];
            double w_t = uvw[2][lastSol];

            double delta_3d = Math.sqrt(v_t * v_t + w_t * w_t);
            double phi_bend = Math.atan2(w_t, v_t) * 180.0 / Math.PI;
            double theta_deg = Math.atan2(delta_3d, 100.0 + u_t) * 180.0 / Math.PI;

            String row = String.format(Locale.US, "%s,%d,%d,%d,%d,40.0,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f",
                cname, w[0], w[1], w[2], w[3], u_t, v_t, w_t, delta_3d, phi_bend, theta_deg);
            System.out.println(row);
            csvSummary.append(row).append("\n");

            // Export 3D images for key cases (Top pitch down, Right yaw left, Diagonal 45 deg, All 4 stiffening)
            try {
                model.result("pg_disp").run();
                String expD = "exp_d_" + cname;
                model.result().export().create(expD, "Image3D");
                model.result().export(expD).set("sourceobject", "pg_disp");
                model.result().export(expD).set("filename", baseDir + "\\comsol_" + cname + "_disp.png");
                model.result().export(expD).set("size", "manualweb");
                model.result().export(expD).set("unit", "px");
                model.result().export(expD).set("width", 1280);
                model.result().export(expD).set("height", 720);
                model.result().export(expD).set("colortheme", "default");
                model.result().export(expD).run();

                model.result("pg_stress").run();
                String expS = "exp_s_" + cname;
                model.result().export().create(expS, "Image3D");
                model.result().export(expS).set("sourceobject", "pg_stress");
                model.result().export(expS).set("filename", baseDir + "\\comsol_" + cname + "_stress.png");
                model.result().export(expS).set("size", "manualweb");
                model.result().export(expS).set("unit", "px");
                model.result().export(expS).set("width", 1280);
                model.result().export(expS).set("height", 720);
                model.result().export(expS).set("colortheme", "default");
                model.result().export(expS).run();
            } catch (Exception e) {
                System.out.println("Export note (" + cname + "): " + e.getMessage());
            }

            if (c == 0) {
                try {
                    model.save(baseDir + "\\actuator_tapered_4cavity_solved.mph");
                } catch (Exception e) {
                    System.out.println("Save note: " + e.getMessage());
                }
            }
        }
        System.out.println("=== COMSOL_TAPERED_4CAVITY_END ===");

        try (PrintWriter out = new PrintWriter(new File(baseDir, "comsol_tapered_4cavity_summary.csv"))) {
            out.print(csvSummary.toString());
        } catch (Exception e) {
            System.out.println("CSV write: " + e.getMessage());
        }

        return model;
    }

    public static void main(String[] args) {
        run();
    }
}
