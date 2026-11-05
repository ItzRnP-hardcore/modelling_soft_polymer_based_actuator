import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;

public class BuildActuator4Cavity {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.modelPath("c:\\Users\\rudra\\Documents\\BTP\\comsol\\multi_cavity");

        // 1. Parameters
        model.param().set("R_out", "10[mm]", "Outer radius");
        model.param().set("r_cav", "4[mm]", "Cavity radius");
        model.param().set("r_p", "3[mm]", "Cavity eccentricity");
        model.param().set("L_tot", "101[mm]", "Total actuator length");
        model.param().set("E_mod", "0.6[MPa]", "Young's modulus");
        model.param().set("nu_val", "0.4995", "Poisson ratio");
        model.param().set("mu_mod", "E_mod/(2*(1+nu_val))", "Shear modulus");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))", "Bulk modulus");
        model.param().set("ks_sleeve", "100*E_mod*R_out", "Sleeve hoop stiffness");

        model.param().set("P_val", "50[kPa]", "Operating pressure");
        model.param().set("w1", "1", "Chamber 1 weight");
        model.param().set("w2", "1", "Chamber 2 weight");
        model.param().set("w3", "1", "Chamber 3 weight");
        model.param().set("w4", "1", "Chamber 4 weight");
        model.param().set("P1", "w1*P_val", "Chamber 1 pressure");
        model.param().set("P2", "w2*P_val", "Chamber 2 pressure");
        model.param().set("P3", "w3*P_val", "Chamber 3 pressure");
        model.param().set("P4", "w4*P_val", "Chamber 4 pressure");

        // 2. Geometry
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        // Outer cylinder
        model.component("comp1").geom("geom1").create("cyl_out", "Cylinder");
        model.component("comp1").geom("geom1").feature("cyl_out").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cyl_out").set("pos", new double[]{0.0, 0.0, 0.0});
        model.component("comp1").geom("geom1").feature("cyl_out").set("r", "R_out");
        model.component("comp1").geom("geom1").feature("cyl_out").set("h", "L_tot");

        // 4 Separate Cavities
        double[] x_starts = {2.0, 27.0, 52.0, 77.0};
        double cav_len = 22.0;

        for (int k = 1; k <= 4; k++) {
            String tag = "cyl_cav" + k;
            model.component("comp1").geom("geom1").create(tag, "Cylinder");
            model.component("comp1").geom("geom1").feature(tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(tag).set("pos", new double[]{x_starts[k-1], 3.0, 0.0});
            model.component("comp1").geom("geom1").feature(tag).set("r", "r_cav");
            model.component("comp1").geom("geom1").feature(tag).set("h", cav_len);
        }

        // Difference: subtract all 4 cavities
        model.component("comp1").geom("geom1").create("dif1", "Difference");
        model.component("comp1").geom("geom1").feature("dif1").selection("input").set("cyl_out");
        model.component("comp1").geom("geom1").feature("dif1").selection("input2").set("cyl_cav1", "cyl_cav2", "cyl_cav3", "cyl_cav4");

        // Half-symmetry block (z >= 0)
        model.component("comp1").geom("geom1").create("blk_half", "Block");
        model.component("comp1").geom("geom1").feature("blk_half").set("pos", new double[]{-1.0, -15.0, 0.0});
        model.component("comp1").geom("geom1").feature("blk_half").set("size", new double[]{110.0, 30.0, 15.0});

        model.component("comp1").geom("geom1").create("int1", "Intersection");
        model.component("comp1").geom("geom1").feature("int1").selection("input").set("dif1", "blk_half");

        model.component("comp1").geom("geom1").run();

        // 3. Selections
        // Base: x = 0
        model.component("comp1").selection().create("sel_base", "Box");
        model.component("comp1").selection("sel_base").set("entitydim", 2);
        model.component("comp1").selection("sel_base").set("condition", "inside");
        model.component("comp1").selection("sel_base").set("xmin", "-0.01[mm]");
        model.component("comp1").selection("sel_base").set("xmax", "0.01[mm]");
        model.component("comp1").selection("sel_base").set("ymin", "-15[mm]");
        model.component("comp1").selection("sel_base").set("ymax", "15[mm]");
        model.component("comp1").selection("sel_base").set("zmin", "-1[mm]");
        model.component("comp1").selection("sel_base").set("zmax", "15[mm]");

        // Symmetry: z = 0
        model.component("comp1").selection().create("sel_sym", "Box");
        model.component("comp1").selection("sel_sym").set("entitydim", 2);
        model.component("comp1").selection("sel_sym").set("condition", "inside");
        model.component("comp1").selection("sel_sym").set("xmin", "-1[mm]");
        model.component("comp1").selection("sel_sym").set("xmax", "110[mm]");
        model.component("comp1").selection("sel_sym").set("ymin", "-15[mm]");
        model.component("comp1").selection("sel_sym").set("ymax", "15[mm]");
        model.component("comp1").selection("sel_sym").set("zmin", "-0.01[mm]");
        model.component("comp1").selection("sel_sym").set("zmax", "0.01[mm]");

        // Outer surface (for sleeve constraint)
        model.component("comp1").selection().create("sel_outer_box", "Cylinder");
        model.component("comp1").selection("sel_outer_box").set("entitydim", 2);
        model.component("comp1").selection("sel_outer_box").set("condition", "allvertices");
        model.component("comp1").selection("sel_outer_box").set("axistype", "x");
        model.component("comp1").selection("sel_outer_box").set("r", "R_out+0.1[mm]");
        model.component("comp1").selection("sel_outer_box").set("rin", "R_out-0.1[mm]");
        model.component("comp1").selection("sel_outer_box").set("top", "L_tot+0.1[mm]");
        model.component("comp1").selection("sel_outer_box").set("bottom", "-0.1[mm]");

        model.component("comp1").selection().create("sel_outer", "Difference");
        model.component("comp1").selection("sel_outer").set("entitydim", 2);
        model.component("comp1").selection("sel_outer").set("add", new String[]{"sel_outer_box"});
        model.component("comp1").selection("sel_outer").set("subtract", new String[]{"sel_sym", "sel_base"});

        // Selections for Cavities 1..4
        for (int k = 1; k <= 4; k++) {
            String boxTag = "sel_cav_box" + k;
            String cavTag = "sel_cav" + k;
            double xs = x_starts[k-1];
            model.component("comp1").selection().create(boxTag, "Box");
            model.component("comp1").selection(boxTag).set("entitydim", 2);
            model.component("comp1").selection(boxTag).set("condition", "inside");
            model.component("comp1").selection(boxTag).set("xmin", (xs - 0.1) + "[mm]");
            model.component("comp1").selection(boxTag).set("xmax", (xs + cav_len + 0.1) + "[mm]");
            model.component("comp1").selection(boxTag).set("ymin", "-1.05[mm]");
            model.component("comp1").selection(boxTag).set("ymax", "7.05[mm]");
            model.component("comp1").selection(boxTag).set("zmin", "-0.01[mm]");
            model.component("comp1").selection(boxTag).set("zmax", "4.05[mm]");

            model.component("comp1").selection().create(cavTag, "Difference");
            model.component("comp1").selection(cavTag).set("entitydim", 2);
            model.component("comp1").selection(cavTag).set("add", new String[]{boxTag});
            model.component("comp1").selection(cavTag).set("subtract", new String[]{"sel_sym", "sel_base"});
        }

        // 4. Physics
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

        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().named("sel_base");

        model.component("comp1").physics("solid").create("sym1", "SymmetrySolid", 2);
        model.component("comp1").physics("solid").feature("sym1").selection().named("sel_sym");

        // Follower Pressures for the 4 Cavities
        for (int k = 1; k <= 4; k++) {
            String bndTag = "bndl" + k;
            model.component("comp1").physics("solid").create(bndTag, "BoundaryLoad", 2);
            model.component("comp1").physics("solid").feature(bndTag).selection().named("sel_cav" + k);
            model.component("comp1").physics("solid").feature(bndTag).set("forceType", "FollowerPressure");
            model.component("comp1").physics("solid").feature(bndTag).set("pressure", "P" + k);
        }

        // Sleeve Weak Contribution (penalizes radial dilation (y*v + z*w)/R)
        model.component("comp1").physics("solid").create("wf_sleeve", "WeakContribution", 2);
        model.component("comp1").physics("solid").feature("wf_sleeve").selection().named("sel_outer");
        model.component("comp1").physics("solid").feature("wf_sleeve").set("weakExpression", 
            "-ks_sleeve/(R_out^4) * (y*v + z*w) * (y*test(v) + z*test(w))");

        // 5. Mesh
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(4);
        model.component("comp1").mesh("mesh1").run();

        // 6. Study (Stationary Geometric Nonlinear)
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);

        // 7. Results Setup: Datasets for tip evaluation at x = 101 mm
        // We will run study and evaluate for each case
        String[] caseNames = {
            "all_4_chambers",
            "chamber_1_base",
            "chamber_2",
            "chamber_3",
            "chamber_4_tip",
            "chambers_1_2_proximal",
            "chambers_3_4_distal"
        };
        int[][] caseWeights = {
            {1, 1, 1, 1},
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1},
            {1, 1, 0, 0},
            {0, 0, 1, 1}
        };

        // Create 3D Plot Groups
        model.result().create("pg_disp", "PlotGroup3D");
        model.result("pg_disp").label("Total Displacement (solid.disp)");
        model.result("pg_disp").create("surf_d", "Surface");
        model.result("pg_disp").feature("surf_d").set("expr", "solid.disp");
        model.result("pg_disp").feature("surf_d").set("unit", "mm");
        model.result("pg_disp").feature("surf_d").create("def", "Deform");
        model.result("pg_disp").feature("surf_d").feature("def").set("expr", new String[]{"u", "v", "w"});

        model.result().create("pg_stress", "PlotGroup3D");
        model.result("pg_stress").label("Stress (solid.mises)");
        model.result("pg_stress").create("surf_s", "Surface");
        model.result("pg_stress").feature("surf_s").set("expr", "solid.mises");
        model.result("pg_stress").feature("surf_s").set("unit", "MPa");
        model.result("pg_stress").feature("surf_s").create("def", "Deform");
        model.result("pg_stress").feature("surf_s").feature("def").set("expr", new String[]{"u", "v", "w"});

        System.out.println("=== COMSOL_4CAVITY_CSV_START ===");
        System.out.println("case_name,w1,w2,w3,w4,u_tip_mm,v_tip_mm,x_tip_mm,delta_mm,theta_deg");

        for (int c = 0; c < caseNames.length; c++) {
            String cname = caseNames[c];
            int[] w = caseWeights[c];
            model.param().set("w1", String.valueOf(w[0]));
            model.param().set("w2", String.valueOf(w[1]));
            model.param().set("w3", String.valueOf(w[2]));
            model.param().set("w4", String.valueOf(w[3]));

            model.study("std1").run();

            // Set up CutPoints if not existing
            if (c == 0) {
                model.result().dataset().create("cpt_top", "CutPoint3D");
                model.result().dataset("cpt_top").set("data", "dset1");
                model.result().dataset("cpt_top").set("pointx", "101");
                model.result().dataset("cpt_top").set("pointy", "10");
                model.result().dataset("cpt_top").set("pointz", "0");

                model.result().dataset().create("cpt_bot", "CutPoint3D");
                model.result().dataset("cpt_bot").set("data", "dset1");
                model.result().dataset("cpt_bot").set("pointx", "101");
                model.result().dataset("cpt_bot").set("pointy", "-10");
                model.result().dataset("cpt_bot").set("pointz", "0");

                model.result().numerical().create("pev_top", "EvalPoint");
                model.result().numerical("pev_top").set("data", "cpt_top");
                model.result().numerical("pev_top").set("expr", new String[]{"u", "v"});
                model.result().numerical("pev_top").set("unit", new String[]{"mm", "mm"});

                model.result().numerical().create("pev_bot", "EvalPoint");
                model.result().numerical("pev_bot").set("data", "cpt_bot");
                model.result().numerical("pev_bot").set("expr", new String[]{"u", "v"});
                model.result().numerical("pev_bot").set("unit", new String[]{"mm", "mm"});
            }

            double[][] uv_top = model.result().numerical("pev_top").getReal();
            double[][] uv_bot = model.result().numerical("pev_bot").getReal();

            double ut = uv_top[0][uv_top[0].length - 1];
            double vt = uv_top[1][uv_top[1].length - 1];
            double ub = uv_bot[0][uv_bot[0].length - 1];
            double vb = uv_bot[1][uv_bot[1].length - 1];

            double x_top = 101.0 + ut;
            double y_top = 10.0 + vt;
            double x_bot = 101.0 + ub;
            double y_bot = -10.0 + vb;

            double u_tip = 0.5 * (ut + ub);
            double v_tip = 0.5 * (vt + vb);
            double x_tip = 0.5 * (x_top + x_bot);
            double delta = -v_tip;
            double theta_deg = Math.atan2(x_top - x_bot, y_top - y_bot) * 180.0 / Math.PI;

            System.out.printf("%s,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                              cname, w[0], w[1], w[2], w[3], u_tip, v_tip, x_tip, delta, theta_deg);

            // Export images for key cases: All 4, Base only, Tip only
            if (c == 0 || c == 1 || c == 4) {
                try {
                    model.result("pg_disp").run();
                    model.result().export().create("exp_d_" + cname, "Image3D");
                    model.result().export("exp_d_" + cname).set("sourceobject", "pg_disp");
                    model.result().export("exp_d_" + cname).set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\multi_cavity\\comsol_" + cname + "_disp.png");
                    model.result().export("exp_d_" + cname).set("size", "manualweb");
                    model.result().export("exp_d_" + cname).set("unit", "px");
                    model.result().export("exp_d_" + cname).set("width", 1280);
                    model.result().export("exp_d_" + cname).set("height", 720);
                    model.result().export("exp_d_" + cname).set("colortheme", "default");
                    model.result().export("exp_d_" + cname).run();

                    model.result("pg_stress").run();
                    model.result().export().create("exp_s_" + cname, "Image3D");
                    model.result().export("exp_s_" + cname).set("sourceobject", "pg_stress");
                    model.result().export("exp_s_" + cname).set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\multi_cavity\\comsol_" + cname + "_stress.png");
                    model.result().export("exp_s_" + cname).set("size", "manualweb");
                    model.result().export("exp_s_" + cname).set("unit", "px");
                    model.result().export("exp_s_" + cname).set("width", 1280);
                    model.result().export("exp_s_" + cname).set("height", 720);
                    model.result().export("exp_s_" + cname).set("colortheme", "default");
                    model.result().export("exp_s_" + cname).run();
                } catch (Exception e) {
                    System.out.println("Export error: " + e.getMessage());
                }
            }
        }
        System.out.println("=== COMSOL_4CAVITY_CSV_END ===");

        return model;
    }

    public static void main(String[] args) {
        run();
    }
}

